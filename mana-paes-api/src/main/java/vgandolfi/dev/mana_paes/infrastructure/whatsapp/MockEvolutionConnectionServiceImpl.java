package vgandolfi.dev.mana_paes.infrastructure.whatsapp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;
import vgandolfi.dev.mana_paes.application.dto.response.WhatsAppStatus;
import vgandolfi.dev.mana_paes.config.AppProperties;
import vgandolfi.dev.mana_paes.domain.model.enums.ConnectionState;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Implementação MOCK do gerenciador de conexão WhatsApp (DEV/TEST): ativa
 * somente quando {@code app.evolution.url} está EM BRANCO. NUNCA chama a
 * Evolution API — todo o estado é mantido em memória.
 *
 * <p>Fluxo simulado:</p>
 * <ul>
 *   <li>{@code startConnection()} → CONNECTING com um QR fake (data URI SVG
 *       visualmente identificável como mock); agenda transição automática para
 *       OPEN após {@code app.evolution.mock-connect-delay-ms} (default 10000);</li>
 *   <li>{@code simulateScan()} → força OPEN com número fixo
 *       {@code 5511999999999} (chamado pelo endpoint
 *       {@code POST /api/v1/whatsapp/simulate-scan}, exclusivo do mock);</li>
 *   <li>{@code disconnect()/testConnection()} análogos ao real, sem chamadas
 *       externas.</li>
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
            + "PHJlY3Qgd2lkdGg9IjI0MCIgaGVpZ2h0PSIyNDAiIGZpbGw9IiNmZWZlZmUiLz48ZyBmaWxsPSIjMTExIj48cmVjdCB4PSIyMCIgeT0iMjAiIHdpZHRoPSI2MCIgaGVpZ2h0PSI2MCIvPjxyZWN0IHg9IjI4IiB5PSIyOCIgd2lkdGg9IjQ0IiBoZWlnaHQ9IjQ0IiBmaWxsPSIjZmVmZWZlIi8+PHJlY3QgeD0iMzYiIHk9IjM2IiB3aWR0aD0iMjgiIGhlaWdodD0iMjgiLz48cmVjdCB4PSIxNjAiIHk9IjIwIiB3aWR0aD0iNjAiIGhlaWdodD0iNjAiLz48cmVjdCB4PSIxNjgiIHk9IjI4IiB3aWR0aD0iNDQiIGhlaWdodD0iNDQiIGZpbGw9IiNmZWZlZmUiLz48cmVjdCB4PSIxNzYiIHk9IjM2IiB3aWR0aD0iMjgiIGhlaWdodD0iMjgiLz48cmVjdCB4PSIyMCIgeT0iMTYwIiB3aWR0aD0iNjAiIGhlaWdodD0iNjAiLz48cmVjdCB4PSIyOCIgeT0iMTY4IiB3aWR0aD0iNDQiIGhlaWdodD0iNDQiIGZpbGw9IiNmZWZlZmUiLz48cmVjdCB4PSIzNiIgeT0iMTc2IiB3aWR0aD0iMjgiIGhlaWdodD0iMjgiLz48cmVjdCB4PSIxMjAiIHk9IjIwIiB3aWR0aD0iMTIiIGhlaWdodD0iMTIiLz48cmVjdCB4PSIxNDAiIHk9IjIwIiB3aWR0aD0iMTIiIGhlaWdodD0iMTIiLz48cmVjdCB4PSIxMjAiIHk9IjQwIiB3aWR0aD0iMTIiIGhlaWdodD0iMTIiLz48cmVjdCB4PSIxMDAiIHk9IjYwIiB3aWR0aD0iMTIiIGhlaWdodD0iMTIiLz48cmVjdCB4PSIxMjAiIHk9IjYwIiB3aWR0aD0iMTIiIGhlaWdodD0iMTIiLz48cmVjdCB4PSIxNDAiIHk9IjYwIiB3aWR0aD0iMTIiIGhlaWdodD0iMTIiLz48cmVjdCB4PSIxNjAiIHk9IjYwIiB3aWR0aD0iMTIiIGhlaWdodD0iMTIiLz48cmVjdCB4PSIxMDAiIHk9IjgwIiB3aWR0aD0iMTIiIGhlaWdodD0iMTIiLz48cmVjdCB4PSIxNjAiIHk9IjgwIiB3aWR0aD0iMTIiIGhlaWdodD0iMTIiLz48cmVjdCB4PSIxMjAiIHk9IjEwMCIgd2lkdGg9IjEyIiBoZWlnaHQ9IjEyIi8+PHJlY3QgeD0iMTQwIiB5PSIxMDAiIHdpZHRoPSIxMiIgaGVpZ2h0PSIxMiIvPjxyZWN0IHg9IjE2MCIgeT0iMTAwIiB3aWR0aD0iMTIiIGhlaWdodD0iMTIiLz48cmVjdCB4PSIxMDAiIHk9IjEyMCIgd2lkdGg9IjEyIiBoZWlnaHQ9IjEyIi8+PHJlY3QgeD0iMTIwIiB5PSIxMjAiIHdpZHRoPSIxMiIgaGVpZ2h0PSIxMiIvPjxyZWN0IHg9IjE2MCIgeT0iMTIwIiB3aWR0aD0iMTIiIGhlaWdodD0iMTIiLz48cmVjdCB4PSIxMDAiIHk9IjE0MCIgd2lkdGg9IjEyIiBoZWlnaHQ9IjEyIi8+PHJlY3QgeD0iMTIwIiB5PSIxNDAiIHdpZHRoPSIxMiIgaGVpZ2h0PSIxMiIvPjxyZWN0IHg9IjE0MCIgeT0iMTQwIiB3aWR0aD0iMTIiIGhlaWdodD0iMTIiLz48cmVjdCB4PSIxNjAiIHk9IjE0MCIgd2lkdGg9IjEyIiBoZWlnaHQ9IjEyIi8+PHJlY3QgeD0iMTIwIiB5PSIxNjAiIHdpZHRoPSIxMiIgaGVpZ2h0PSIxMiIvPjxyZWN0IHg9IjE0MCIgeT0iMTYwIiB3aWR0aD0iMTIiIGhlaWdodD0iMTIiLz48cmVjdCB4PSIxNjAiIHk9IjE2MCIgd2lkdGg9IjEyIiBoZWlnaHQ9IjEyIi8+PHJlY3QgeD0iMTIwIiB5PSIxODAiIHdpZHRoPSIxMiIgaGVpZ2h0PSIxMiIvPjxyZWN0IHg9IjE0MCIgeT0iMTgwIiB3aWR0aD0iMTIiIGhlaWdodD0iMTIiLz48cmVjdCB4PSIxNjAiIHk9IjE4MCIgd2lkdGg9IjEyIiBoZWlnaHQ9IjEyIi8+PC9nPjx0ZXh0IHg9IjEyMCIgeT0iMjIyIiBmb250LXNpemU9IjEzIiB0ZXh0LWFuY2hvcj0ibWlkZGxlIiBmaWxsPSIjNjY2IiBmb250LWZhbWlseT0ic2Fucy1zZXJpZiI+UVIgQ09ERSBNT0NLIC0gTWFuYSBQYWVzIChkZXYvdGVzdCk8L3RleHQ+PC9zdmc+Cg==";

    private final long connectDelayMs;
    private final Object lock = new Object();

    private volatile ConnectionState state = ConnectionState.CLOSE;
    private volatile String qrCodeBase64;
    private volatile String connectedNumber;
    private volatile Instant updatedAt;

    public MockEvolutionConnectionServiceImpl(AppProperties appProperties) {
        this.connectDelayMs = Math.max(0, appProperties.evolution().mockConnectDelayMs());
    }

    @Override
    public WhatsAppStatus startConnection() {
        synchronized (lock) {
            state = ConnectionState.CONNECTING;
            qrCodeBase64 = MOCK_QR_DATA_URI;
            connectedNumber = null;
            updatedAt = Instant.now();
        }
        scheduleAutoConnect();
        log.info("mock_whatsapp_start state=CONNECTING (auto-open em {}ms)", connectDelayMs);
        return getStatus();
    }

    /**
     * Força a conexão para OPEN (número fixo). Exclusivo do mock — exposto no
     * endpoint {@code POST /api/v1/whatsapp/simulate-scan}.
     */
    public WhatsAppStatus simulateScan() {
        synchronized (lock) {
            state = ConnectionState.OPEN;
            qrCodeBase64 = null;
            connectedNumber = MOCK_CONNECTED_NUMBER;
            updatedAt = Instant.now();
        }
        log.info("mock_whatsapp_scan state=OPEN number={}", MOCK_CONNECTED_NUMBER);
        return getStatus();
    }

    @Override
    public WhatsAppStatus getStatus() {
        synchronized (lock) {
            return new WhatsAppStatus(state.name(), qrCodeBase64, connectedNumber, updatedAt);
        }
    }

    @Override
    public void disconnect() {
        synchronized (lock) {
            state = ConnectionState.CLOSE;
            qrCodeBase64 = null;
            connectedNumber = null;
            updatedAt = Instant.now();
        }
        log.info("mock_whatsapp_disconnect state=CLOSE");
    }

    @Override
    public TestMessage testConnection() {
        synchronized (lock) {
            if (state == ConnectionState.OPEN && connectedNumber != null && !connectedNumber.isBlank()) {
                return new TestMessage(true,
                        "Mensagem de teste enviada para " + connectedNumber + " (mock)");
            }
            return new TestMessage(false, "WhatsApp não está conectado (mock)");
        }
    }

    private void scheduleAutoConnect() {
        CompletableFuture.delayedExecutor(connectDelayMs, TimeUnit.MILLISECONDS)
                .execute(() -> {
                    try {
                        simulateScan();
                    } catch (Exception ex) {
                        log.warn("mock_whatsapp_auto_connect_failed reason={}", ex.getMessage());
                    }
                });
    }
}