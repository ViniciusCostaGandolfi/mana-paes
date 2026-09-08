package vgandolfi.dev.mana_paes.infrastructure.whatsapp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;
import vgandolfi.dev.mana_paes.application.dto.response.WhatsAppStatus;
import vgandolfi.dev.mana_paes.infrastructure.config.AppProperties;
import vgandolfi.dev.mana_paes.domain.model.enums.ConnectionState;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Implementação MOCK do gerenciador de conexão WhatsApp (DEV/TEST): ativa
 * somente quando {@code app.evolution.url} está EM BRANCO. NUNCA chama a
 * Evolution API — o estado é mantido em memória POR USUÁRIO ADMIN (cada admin
 * tem a própria instância).
 *
 * <p>Fluxo simulado por usuário:</p>
 * <ul>
 *   <li>{@code startConnection(userId)} → CONNECTING com um QR fake; agenda
 *       transição automática para OPEN após {@code app.evolution.mock-connect-delay-ms};</li>
 *   <li>{@code simulateScan(userId)} → força OPEN com número fixo
 *       {@code 5511999999999} (endpoint {@code POST /api/v1/whatsapp/simulate-scan});</li>
 *   <li>{@code disconnect/testConnection(userId)} análogos ao real.</li>
 * </ul>
 */
@Service
@ConditionalOnExpression("!T(org.springframework.util.StringUtils).hasText('${app.evolution.url:}')")
public class MockEvolutionConnectionServiceImpl implements EvolutionConnectionManager {

    private static final Logger log = LoggerFactory.getLogger(MockEvolutionConnectionServiceImpl.class);

    /** Número fixo "conectado" pelo mock. */
    public static final String MOCK_CONNECTED_NUMBER = "5511999999999";

    /** QR fake: data URI de um SVG placeholder com a marca "MOCK". */
    public static final String MOCK_QR_DATA_URI = "data:image/svg+xml;base64,"
            + "PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIyNDAiIGhlaWdodD0iMjQwIiB2aWV3Qm94PSIwIDAgMjQwIDI0MCI+"
            + "PHJlY3Qgd2lkdGg9IjI0MCIgaGVpZ2h0PSIyNDAiIGZpbGw9IiNmZWZlZmUiLz48ZyBmaWxsPSIjMTExIj48cmVjdCB4PSIyMCIgeT0iMjAiIHdpZHRoPSI2MCIgaGVpZ2h0PSI2MCIvPjxyZWN0IHg9IjI4IiB5PSIyOCIgd2lkdGg9IjQ0IiBoZWlnaHQ9IjQ0IiBmaWxsPSIjZmVmZWZlIi8+PHJlY3QgeD0iMzYiIHk9IjM2IiB3aWR0aD0iMjgiIGhlaWdodD0iMjgiLz48cmVjdCB4PSIxNjAiIHk9IjIwIiB3aWR0aD0iNjAiIGhlaWdodD0iNjAiLz48cmVjdCB4PSIxNjgiIHk9IjI4IiB3aWR0aD0iNDQiIGhlaWdodD0iNDQiIGZpbGw9IiNmZWZlZmUiLz48cmVjdCB4PSIxNzYiIHk9IjM2IiB3aWR0aD0iMjgiIGhlaWdodD0iMjgiLz48cmVjdCB4PSIyMCIgeT0iMTYwIiB3aWR0aD0iNjAiIGhlaWdodD0iNjAiLz48cmVjdCB4PSIyOCIgeT0iMTY4IiB3aWR0aD0iNDQiIGhlaWdodD0iNDQiIGZpbGw9IiNmZWZlZmUiLz48cmVjdCB4PSIzNiIgeT0iMTc2IiB3aWR0aD0iMjgiIGhlaWdodD0iMjgiLz48cmVjdCB4PSIxMjAiIHk9IjIwIiB3aWR0aD0iMTIiIGhlaWdodD0iMTIiLz48cmVjdCB4PSIxNDAiIHk9IjIwIiB3aWR0aD0iMTIiIGhlaWdodD0iMTIiLz48cmVjdCB4PSIxMjAiIHk9IjQwIiB3aWR0aD0iMTIiIGhlaWdodD0iMTIiLz48cmVjdCB4PSIxMDAiIHk9IjYwIiB3aWR0aD0iMTIiIGhlaWdodD0iMTIiLz48cmVjdCB4PSIxMjAiIHk9IjYwIiB3aWR0aD0iMTIiIGhlaWdodD0iMTIiLz48cmVjdCB4PSIxNDAiIHk9IjYwIiB3aWR0aD0iMTIiIGhlaWdodD0iMTIiLz48cmVjdCB4PSIxNjAiIHk9IjYwIiB3aWR0aD0iMTIiIGhlaWdodD0iMTIiLz48cmVjdCB4PSIxMDAiIHk9IjgwIiB3aWR0aD0iMTIiIGhlaWdodD0iMTIiLz48cmVjdCB4PSIxNjAiIHk9IjgwIiB3aWR0aD0iMTIiIGhlaWdodD0iMTIiLz48cmVjdCB4PSIxMjAiIHk9IjEwMCIgd2lkdGg9IjEyIiBoZWlnaHQ9IjEyIi8+PHJlY3QgeD0iMTQwIiB5PSIxMDAiIHdpZHRoPSIxMiIgaGVpZ2h0PSIxMiIvPjxyZWN0IHg9IjE2MCIgeT0iMTAwIiB3aWR0aD0iMTIiIGhlaWdodD0iMTIiLz48cmVjdCB4PSIxMDAiIHk9IjEyMCIgd2lkdGg9IjEyIiBoZWlnaHQ9IjEyIi8+PHJlY3QgeD0iMTIwIiB5PSIxMjAiIHdpZHRoPSIxMiIgaGVpZ2h0PSIxMiIvPjxyZWN0IHg9IjE2MCIgeT0iMTIwIiB3aWR0aD0iMTIiIGhlaWdodD0iMTIiLz48cmVjdCB4PSIxMDAiIHk9IjE0MCIgd2lkdGg9IjEyIiBoZWlnaHQ9IjEyIi8+PHJlY3QgeD0iMTIwIiB5PSIxNDAiIHdpZHRoPSIxMiIgaGVpZ2h0PSIxMiIvPjxyZWN0IHg9IjE0MCIgeT0iMTQwIiB3aWR0aD0iMTIiIGhlaWdodD0iMTIiLz48cmVjdCB4PSIxNjAiIHk9IjE0MCIgd2lkdGg9IjEyIiBoZWlnaHQ9IjEyIi8+PHJlY3QgeD0iMTIwIiB5PSIxNjAiIHdpZHRoPSIxMiIgaGVpZ2h0PSIxMiIvPjxyZWN0IHg9IjE0MCIgeT0iMTYwIiB3aWR0aD0iMTIiIGhlaWdodD0iMTIiLz48cmVjdCB4PSIxNjAiIHk9IjE2MCIgd2w=";

    private final long connectDelayMs;
    private final Map<UUID, ConnectionState> states = new ConcurrentHashMap<>();
    private final Map<UUID, String> qrCodes = new ConcurrentHashMap<>();
    private final Map<UUID, String> connectedNumbers = new ConcurrentHashMap<>();
    private final Map<UUID, Instant> updatedAts = new ConcurrentHashMap<>();

    public MockEvolutionConnectionServiceImpl(AppProperties appProperties) {
        this.connectDelayMs = Math.max(0, appProperties.evolution().mockConnectDelayMs());
    }

    @Override
    public WhatsAppStatus startConnection(UUID userId) {
        states.put(userId, ConnectionState.CONNECTING);
        qrCodes.put(userId, MOCK_QR_DATA_URI);
        connectedNumbers.remove(userId);
        updatedAts.put(userId, Instant.now());
        scheduleAutoConnect(userId);
        log.info("mock_whatsapp_start userId={} state=CONNECTING (auto-open em {}ms)",
                userId, connectDelayMs);
        return getStatus(userId);
    }

    /**
     * Força a conexão do usuário para OPEN (número fixo). Exclusivo do mock —
     * exposto no endpoint {@code POST /api/v1/whatsapp/simulate-scan}.
     */
    public WhatsAppStatus simulateScan(UUID userId) {
        states.put(userId, ConnectionState.OPEN);
        qrCodes.remove(userId);
        connectedNumbers.put(userId, MOCK_CONNECTED_NUMBER);
        updatedAts.put(userId, Instant.now());
        log.info("mock_whatsapp_scan userId={} state=OPEN number={}", userId, MOCK_CONNECTED_NUMBER);
        return getStatus(userId);
    }

    @Override
    public WhatsAppStatus getStatus(UUID userId) {
        ConnectionState state = states.getOrDefault(userId, ConnectionState.CLOSE);
        return new WhatsAppStatus(state.name(),
                state == ConnectionState.CONNECTING ? qrCodes.get(userId) : null,
                state == ConnectionState.OPEN ? connectedNumbers.get(userId) : null,
                updatedAts.get(userId));
    }

    @Override
    public void disconnect(UUID userId) {
        states.put(userId, ConnectionState.CLOSE);
        qrCodes.remove(userId);
        connectedNumbers.remove(userId);
        updatedAts.put(userId, Instant.now());
        log.info("mock_whatsapp_disconnect userId={} state=CLOSE", userId);
    }

    @Override
    public TestMessage testConnection(UUID userId) {
        ConnectionState state = states.getOrDefault(userId, ConnectionState.CLOSE);
        if (state == ConnectionState.OPEN && connectedNumbers.get(userId) != null) {
            return new TestMessage(true,
                    "Mensagem de teste enviada para " + connectedNumbers.get(userId) + " (mock)");
        }
        return new TestMessage(false, "WhatsApp não está conectado (mock)");
    }

    private void scheduleAutoConnect(UUID userId) {
        CompletableFuture.delayedExecutor(connectDelayMs, TimeUnit.MILLISECONDS)
                .execute(() -> {
                    try {
                        simulateScan(userId);
                    } catch (Exception ex) {
                        log.warn("mock_whatsapp_auto_connect_failed userId={} reason={}", userId, ex.getMessage());
                    }
                });
    }
}