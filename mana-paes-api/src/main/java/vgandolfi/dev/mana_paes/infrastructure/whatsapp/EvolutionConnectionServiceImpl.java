package vgandolfi.dev.mana_paes.infrastructure.whatsapp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vgandolfi.dev.mana_paes.application.dto.response.WhatsAppStatus;
import vgandolfi.dev.mana_paes.config.AppProperties;
import vgandolfi.dev.mana_paes.domain.model.EvolutionConnection;
import vgandolfi.dev.mana_paes.domain.model.enums.ConnectionState;
import vgandolfi.dev.mana_paes.domain.repository.EvolutionConnectionRepository;
import vgandolfi.dev.mana_paes.infrastructure.notification.EvolutionApiClient;
import vgandolfi.dev.mana_paes.infrastructure.notification.EvolutionApiClient.ConnectionStateInfo;
import vgandolfi.dev.mana_paes.infrastructure.webhook.EvolutionWebhookHandler;
import org.springframework.security.crypto.encrypt.TextEncryptor;

import java.util.Objects;

/**
 * Implementação REAL do gerenciador de conexão WhatsApp (instância global
 * "mana-paes"): ativa somente quando {@code app.evolution.url} está preenchida.
 *
 * <p>Fluxo:</p>
 * <ul>
 *   <li>{@code startConnection()} — se não existe linha, cria a instância na
 *       Evolution API ({@code createInstance}) e guarda o token CRIPTOGRAFADO;
 *       depois pede o QR ({@code connectInstance}) e entra em CONNECTING;</li>
 *   <li>{@code getStatus()} — OPEN expõe o número conectado; CONNECTING expõe o
 *       QR code;</li>
 *   <li>{@code disconnect()} — {@code logoutInstance} + limpa estado/número/QR;</li>
 *   <li>{@code testConnection()} — {@code sendText} para o número conectado;</li>
 *   <li>{@code run()} ({@link ApplicationRunner}) — sincroniza
 *       state/connectedNumber com a Evolution API na subida do app.</li>
 * </ul>
 *
 * <p>O token da instância NUNCA é armazenado em texto puro: criptografado com o
 * {@code TextEncryptor} (AES-256/GCM) e descriptografado apenas em memória.</p>
 */
@Service
@ConditionalOnExpression("T(org.springframework.util.StringUtils).hasText('${app.evolution.url:}')")
public class EvolutionConnectionServiceImpl implements EvolutionConnectionManager, ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(EvolutionConnectionServiceImpl.class);

    static final String TEST_MESSAGE = "Teste de conexão — Sistema Mana Paes";
    static final String WEBHOOK_PATH = "/api/v1/webhooks/evolution-api";

    private final EvolutionApiClient evolutionApiClient;
    private final EvolutionConnectionRepository connectionRepository;
    private final TextEncryptor textEncryptor;
    private final AppProperties appProperties;

    public EvolutionConnectionServiceImpl(EvolutionApiClient evolutionApiClient,
                                          EvolutionConnectionRepository connectionRepository,
                                          TextEncryptor textEncryptor,
                                          AppProperties appProperties) {
        this.evolutionApiClient = evolutionApiClient;
        this.connectionRepository = connectionRepository;
        this.textEncryptor = textEncryptor;
        this.appProperties = appProperties;
    }

    @Override
    @Transactional
    public WhatsAppStatus startConnection() {
        EvolutionConnection connection = connectionRepository.findFirstByOrderByCreatedAtAsc().orElse(null);
        if (connection == null) {
            String token = evolutionApiClient.createInstance(GLOBAL_INSTANCE_NAME, buildWebhookUrl());
            connection = new EvolutionConnection();
            connection.setInstanceName(GLOBAL_INSTANCE_NAME);
            connection.setInstanceApiKey(textEncryptor.encrypt(token));
            connection.setConnectionState(ConnectionState.CLOSE);
            connectionRepository.save(connection);
        } else if (!hasText(connection.getInstanceApiKey())) {
            // situação anômala: linha existe sem token → recria a instância
            String token = evolutionApiClient.createInstance(GLOBAL_INSTANCE_NAME, buildWebhookUrl());
            connection.setInstanceApiKey(textEncryptor.encrypt(token));
            connectionRepository.save(connection);
        }

        String qrCode = evolutionApiClient.connectInstance(GLOBAL_INSTANCE_NAME, decryptKey(connection));
        connection.setQrCodeBase64(qrCode);
        connection.setConnectionState(ConnectionState.CONNECTING);
        connection.setConnectedNumber(null);
        connectionRepository.save(connection);
        return WhatsAppStatus.from(connection);
    }

    @Override
    @Transactional(readOnly = true)
    public WhatsAppStatus getStatus() {
        return connectionRepository.findFirstByOrderByCreatedAtAsc()
                .map(WhatsAppStatus::from)
                .orElse(WhatsAppStatus.closed(null));
    }

    @Override
    @Transactional
    public void disconnect() {
        connectionRepository.findFirstByOrderByCreatedAtAsc().ifPresent(connection -> {
            try {
                evolutionApiClient.logoutInstance(GLOBAL_INSTANCE_NAME, decryptKey(connection));
            } catch (Exception ex) {
                log.warn("evolution_logout_failed instance={} reason={}",
                        GLOBAL_INSTANCE_NAME, ex.getMessage());
            }
            connection.setConnectionState(ConnectionState.CLOSE);
            connection.setConnectedNumber(null);
            connection.setQrCodeBase64(null);
            connectionRepository.save(connection);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public TestMessage testConnection() {
        EvolutionConnection connection = connectionRepository.findFirstByOrderByCreatedAtAsc().orElse(null);
        if (connection == null || connection.getConnectionState() != ConnectionState.OPEN
                || !hasText(connection.getConnectedNumber())) {
            return new TestMessage(false,
                    "WhatsApp não está conectado — conecte escaneando o QR code");
        }
        try {
            evolutionApiClient.sendText(GLOBAL_INSTANCE_NAME, decryptKey(connection),
                    connection.getConnectedNumber(), TEST_MESSAGE);
            return new TestMessage(true,
                    "Mensagem de teste enviada para " + connection.getConnectedNumber());
        } catch (Exception ex) {
            log.warn("whatsapp_test_failed number={} reason={}", connection.getConnectedNumber(), ex.getMessage());
            return new TestMessage(false, "Falha ao enviar mensagem de teste: " + ex.getMessage());
        }
    }

    @Override
    public void run(ApplicationArguments args) {
        syncStateFromEvolution();
    }

    /**
     * Sincroniza state/connectedNumber com a Evolution API (usada na subida).
     * Falhas não derrubam o boot — apenas loga e mantém o estado persistido.
     */
    void syncStateFromEvolution() {
        connectionRepository.findFirstByOrderByCreatedAtAsc().ifPresent(connection -> {
            if (!hasText(connection.getInstanceApiKey())) {
                return;
            }
            try {
                ConnectionStateInfo info =
                        evolutionApiClient.getConnectionState(GLOBAL_INSTANCE_NAME, decryptKey(connection));
                applyRemoteState(info);
            } catch (Exception ex) {
                log.warn("evolution_sync_failed instance={} reason={}", GLOBAL_INSTANCE_NAME, ex.getMessage());
            }
        });
    }

    @Transactional
    void applyRemoteState(ConnectionStateInfo info) {
        connectionRepository.findFirstByOrderByCreatedAtAsc().ifPresent(connection -> {
            String state = info.state();
            boolean changed = false;
            if ("open".equalsIgnoreCase(state)) {
                String number = EvolutionWebhookHandler.extractNumberFromWuid(info.wuid());
                changed = connection.getConnectionState() != ConnectionState.OPEN
                        || !Objects.equals(number, connection.getConnectedNumber());
                connection.setConnectionState(ConnectionState.OPEN);
                connection.setConnectedNumber(number);
                connection.setQrCodeBase64(null);
            } else if ("close".equalsIgnoreCase(state)) {
                changed = connection.getConnectionState() != ConnectionState.CLOSE
                        || connection.getConnectedNumber() != null || connection.getQrCodeBase64() != null;
                connection.setConnectionState(ConnectionState.CLOSE);
                connection.setConnectedNumber(null);
                connection.setQrCodeBase64(null);
            }
            if (changed) {
                connectionRepository.save(connection);
            }
        });
    }

    private String buildWebhookUrl() {
        String backendUrl = appProperties.backend().url();
        if (!hasText(backendUrl)) {
            return null;
        }
        return backendUrl.replaceAll("/+$", "") + WEBHOOK_PATH;
    }

    private String decryptKey(EvolutionConnection connection) {
        try {
            return textEncryptor.decrypt(connection.getInstanceApiKey());
        } catch (Exception ex) {
            log.error("evolution_api_key_decrypt_failed instance={}", GLOBAL_INSTANCE_NAME, ex);
            throw new IllegalStateException("Não foi possível descriptografar o token da instância Evolution", ex);
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}