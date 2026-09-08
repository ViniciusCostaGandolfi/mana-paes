package vgandolfi.dev.mana_paes.infrastructure.whatsapp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vgandolfi.dev.mana_paes.application.dto.response.WhatsAppStatus;
import vgandolfi.dev.mana_paes.infrastructure.config.AppProperties;
import vgandolfi.dev.mana_paes.domain.model.EvolutionConnection;
import vgandolfi.dev.mana_paes.domain.model.User;
import vgandolfi.dev.mana_paes.domain.model.enums.ConnectionState;
import vgandolfi.dev.mana_paes.domain.repository.EvolutionConnectionRepository;
import vgandolfi.dev.mana_paes.domain.repository.UserRepository;
import vgandolfi.dev.mana_paes.infrastructure.notification.EvolutionApiClient;
import vgandolfi.dev.mana_paes.infrastructure.evolution.EvolutionClient.ConnectionStateInfo;
import vgandolfi.dev.mana_paes.infrastructure.webhook.EvolutionWebhookHandler;
import org.springframework.security.crypto.encrypt.TextEncryptor;

import java.util.Objects;
import java.util.UUID;

/**
 * Implementação REAL do gerenciador de conexão WhatsApp — UMA instância por
 * usuário ADMIN: ativa somente quando {@code app.evolution.url} está
 * preenchida.
 *
 * <p>Fluxo:</p>
 * <ul>
 *   <li>{@code startConnection(userId)} — se não existe conexão do usuário,
 *       cria a instância na Evolution API com nome único derivado
 *       ({@code <instanceName>-<8 primeiros dígitos do userId>}) e guarda o
 *       token CRIPTOGRAFADO; depois pede o QR e entra em CONNECTING;</li>
 *   <li>{@code getStatus/disconnect/testConnection(userId)} — operam na
 *       conexão do usuário (pelo {@code instanceName} persistido);</li>
 *   <li>{@code run()} ({@link ApplicationRunner}) — sincroniza
 *       state/connectedNumber de TODAS as conexões por ADMIN na subida do app
 *       (falha de uma não impede as demais).</li>
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
    private final UserRepository userRepository;
    private final TextEncryptor textEncryptor;
    private final AppProperties appProperties;

    public EvolutionConnectionServiceImpl(EvolutionApiClient evolutionApiClient,
                                          EvolutionConnectionRepository connectionRepository,
                                          UserRepository userRepository,
                                          TextEncryptor textEncryptor,
                                          AppProperties appProperties) {
        this.evolutionApiClient = evolutionApiClient;
        this.connectionRepository = connectionRepository;
        this.userRepository = userRepository;
        this.textEncryptor = textEncryptor;
        this.appProperties = appProperties;
    }

    @Override
    @Transactional
    public WhatsAppStatus startConnection(UUID userId) {
        EvolutionConnection connection = connectionRepository.findByUserId(userId).orElse(null);
        if (connection == null) {
            User user = userRepository.findById(userId).orElse(null);
            String instanceName = uniqueInstanceName(appProperties.evolution().instanceName(), userId);
            String token = evolutionApiClient.createInstance(instanceName, buildWebhookUrl());
            connection = new EvolutionConnection();
            connection.setUser(user);
            connection.setInstanceName(instanceName);
            connection.setInstanceApiKey(textEncryptor.encrypt(token));
            connection.setConnectionState(ConnectionState.CLOSE);
            connectionRepository.save(connection);
        } else if (!hasText(connection.getInstanceApiKey())) {
            // situação anômala: linha existe sem token → recria a instância
            String token = evolutionApiClient.createInstance(connection.getInstanceName(), buildWebhookUrl());
            connection.setInstanceApiKey(textEncryptor.encrypt(token));
            connectionRepository.save(connection);
        }

        String qrCode = evolutionApiClient.connectInstance(connection.getInstanceName(), decryptKey(connection));
        connection.setQrCodeBase64(qrCode);
        connection.setConnectionState(ConnectionState.CONNECTING);
        connection.setConnectedNumber(null);
        connectionRepository.save(connection);
        return WhatsAppStatus.from(connection);
    }

    @Override
    @Transactional(readOnly = true)
    public WhatsAppStatus getStatus(UUID userId) {
        return connectionRepository.findByUserId(userId)
                .map(WhatsAppStatus::from)
                .orElse(WhatsAppStatus.closed(null));
    }

    @Override
    @Transactional
    public void disconnect(UUID userId) {
        connectionRepository.findByUserId(userId).ifPresent(connection -> {
            try {
                evolutionApiClient.logoutInstance(connection.getInstanceName(), decryptKey(connection));
            } catch (Exception ex) {
                log.warn("evolution_logout_failed instance={} reason={}",
                        connection.getInstanceName(), ex.getMessage());
            }
            connection.setConnectionState(ConnectionState.CLOSE);
            connection.setConnectedNumber(null);
            connection.setQrCodeBase64(null);
            connectionRepository.save(connection);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public TestMessage testConnection(UUID userId) {
        EvolutionConnection connection = connectionRepository.findByUserId(userId).orElse(null);
        if (connection == null || connection.getConnectionState() != ConnectionState.OPEN
                || !hasText(connection.getConnectedNumber())) {
            return new TestMessage(false,
                    "WhatsApp não está conectado — conecte escaneando o QR code");
        }
        try {
            evolutionApiClient.sendText(connection.getInstanceName(), decryptKey(connection),
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
     * Sincroniza state/connectedNumber de TODAS as conexões por ADMIN com a
     * Evolution API (usada na subida). Falhas não derrubam o boot — loga e
     * segue para as demais.
     */
    void syncStateFromEvolution() {
        for (EvolutionConnection connection : connectionRepository.findAllByUserIdNotNull()) {
            if (!hasText(connection.getInstanceApiKey())) {
                continue;
            }
            try {
                ConnectionStateInfo info =
                        evolutionApiClient.getConnectionState(connection.getInstanceName(), decryptKey(connection));
                applyRemoteState(connection, info);
            } catch (Exception ex) {
                log.warn("evolution_sync_failed instance={} reason={}",
                        connection.getInstanceName(), ex.getMessage());
            }
        }
    }

    @Transactional
    void applyRemoteState(EvolutionConnection connection, ConnectionStateInfo info) {
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
    }

    /**
     * Nome único da instância por ADMIN: {@code <instanceName>-<8 primeiros
     * dígitos do userId>} (ex: {@code VGandolfi Dev-3f2a9c1e}). Usado apenas na
     * auto-criação; conexões pré-cadastradas (seed) usam o nome persistido.
     */
    static String uniqueInstanceName(String envInstanceName, UUID userId) {
        return envInstanceName + "-" + userId.toString().substring(0, 8);
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
            log.error("evolution_api_key_decrypt_failed instance={}", connection.getInstanceName(), ex);
            throw new IllegalStateException("Não foi possível descriptografar o token da instância Evolution", ex);
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}