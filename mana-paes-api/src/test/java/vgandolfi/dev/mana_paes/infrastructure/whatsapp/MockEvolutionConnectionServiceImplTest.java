package vgandolfi.dev.mana_paes.infrastructure.whatsapp;

import org.junit.jupiter.api.Test;
import vgandolfi.dev.mana_paes.application.dto.response.WhatsAppStatus;
import vgandolfi.dev.mana_paes.config.AppProperties;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Mock da conexão WhatsApp (dev/test): simula o fluxo completo em memória —
 * connect → CONNECTING + QR fake → (simulate-scan ou auto) → OPEN com número
 * fixo → disconnect → CLOSE. Nenhuma chamada externa (sem colaboradores).
 */
class MockEvolutionConnectionServiceImplTest {

    private MockEvolutionConnectionServiceImpl mock(long delayMs) {
        AppProperties props = new AppProperties(
                new AppProperties.Jwt("test-secret-test-secret-test-secret-test-secret-1234", 3600000L, 86400000L),
                new AppProperties.Encryption("mana-paes-test-master-key-32chars!"),
                new AppProperties.Evolution("", "", delayMs),
                new AppProperties.Backend(""),
                new AppProperties.Frontend("http://localhost"),
                new AppProperties.Mail(false),
                new AppProperties.Notifications(false, 2),
                new AppProperties.Scheduler(false));
        return new MockEvolutionConnectionServiceImpl(props);
    }

    @Test
    void startConnectionEntersConnectingWithFakeQr() {
        MockEvolutionConnectionServiceImpl mock = mock(60_000);

        WhatsAppStatus status = mock.startConnection();

        assertThat(status.state()).isEqualTo("CONNECTING");
        assertThat(status.qrCodeBase64()).isEqualTo(MockEvolutionConnectionServiceImpl.MOCK_QR_DATA_URI);
        assertThat(status.qrCodeBase64()).startsWith("data:image/svg+xml;base64,");
        assertThat(status.connectedNumber()).isNull();
    }

    @Test
    void simulateScanForcesOpenWithMockNumber() {
        MockEvolutionConnectionServiceImpl mock = mock(60_000);
        mock.startConnection();

        WhatsAppStatus status = mock.simulateScan();

        assertThat(status.state()).isEqualTo("OPEN");
        assertThat(status.connectedNumber()).isEqualTo(MockEvolutionConnectionServiceImpl.MOCK_CONNECTED_NUMBER);
        assertThat(status.qrCodeBase64()).isNull();
    }

    @Test
    void disconnectReturnsToClose() {
        MockEvolutionConnectionServiceImpl mock = mock(60_000);
        mock.startConnection();
        mock.simulateScan();

        mock.disconnect();
        WhatsAppStatus status = mock.getStatus();

        assertThat(status.state()).isEqualTo("CLOSE");
        assertThat(status.connectedNumber()).isNull();
        assertThat(status.qrCodeBase64()).isNull();
    }

    @Test
    void testConnectionSucceedsWhenOpenAndFailsWhenClosed() {
        MockEvolutionConnectionServiceImpl mock = mock(60_000);

        EvolutionConnectionManager.TestMessage closed = mock.testConnection();
        assertThat(closed.success()).isFalse();

        mock.startConnection();
        mock.simulateScan();

        EvolutionConnectionManager.TestMessage open = mock.testConnection();
        assertThat(open.success()).isTrue();
        assertThat(open.message()).contains("5511999999999");
    }

    @Test
    void autoTransitionToOpenAfterConfiguredDelay() throws InterruptedException {
        MockEvolutionConnectionServiceImpl mock = mock(0);
        mock.startConnection();

        // com delay 0, a transição automática deve abrir a conexão em pouco tempo
        WhatsAppStatus status = null;
        long deadline = System.currentTimeMillis() + 2_000;
        while (System.currentTimeMillis() < deadline) {
            status = mock.getStatus();
            if ("OPEN".equals(status.state())) {
                break;
            }
            Thread.sleep(20);
        }

        assertThat(status).isNotNull();
        assertThat(status.state()).isEqualTo("OPEN");
        assertThat(status.connectedNumber()).isEqualTo(MockEvolutionConnectionServiceImpl.MOCK_CONNECTED_NUMBER);
    }

    @Test
    void negativeDelayIsTreatedAsZero() throws InterruptedException {
        MockEvolutionConnectionServiceImpl mock = mock(-5);
        mock.startConnection();

        WhatsAppStatus status = null;
        long deadline = System.currentTimeMillis() + 2_000;
        while (System.currentTimeMillis() < deadline) {
            status = mock.getStatus();
            if ("OPEN".equals(status.state())) {
                break;
            }
            Thread.sleep(20);
        }

        assertThat(status).isNotNull();
        assertThat(status.state()).isEqualTo("OPEN");
    }
}