package vgandolfi.dev.mana_paes.infrastructure.whatsapp;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import vgandolfi.dev.mana_paes.application.dto.response.WhatsAppStatus;
import vgandolfi.dev.mana_paes.config.AppProperties;
import vgandolfi.dev.mana_paes.config.EncryptionConfig;
import vgandolfi.dev.mana_paes.domain.model.EvolutionConnection;
import vgandolfi.dev.mana_paes.domain.model.enums.ConnectionState;
import vgandolfi.dev.mana_paes.domain.repository.EvolutionConnectionRepository;
import vgandolfi.dev.mana_paes.infrastructure.notification.EvolutionApiClient;
import vgandolfi.dev.mana_paes.infrastructure.notification.EvolutionApiException;
import vgandolfi.dev.mana_paes.infrastructure.notification.EvolutionApiNotConfiguredException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Implementação REAL do gerenciador de conexão WhatsApp: orquestra
 * createInstance/connectInstance/logoutInstance/sendText da Evolution API e
 * persiste o estado na {@code evolution_connections}. O token é criptografado
 * em repouso (TextEncryptor real, AES-256/GCM).
 */
@ExtendWith(MockitoExtension.class)
class EvolutionConnectionServiceImplTest {

    private static final String MASTER_KEY = "mana-paes-test-master-key-32chars!";

    @Mock
    private EvolutionApiClient evolutionApiClient;
    @Mock
    private EvolutionConnectionRepository connectionRepository;

    private final TextEncryptor textEncryptor =
            Encryptors.delux(MASTER_KEY, EncryptionConfig.MASTER_KEY_SALT_HEX);

    private AppProperties appProperties(String backendUrl) {
        return new AppProperties(
                new AppProperties.Jwt("test-secret-test-secret-test-secret-test-secret-1234", 3600000L, 86400000L),
                new AppProperties.Encryption(MASTER_KEY),
                new AppProperties.Evolution("http://evolution:8080", "global-key", 0L),
                new AppProperties.Backend(backendUrl),
                new AppProperties.Frontend("http://localhost"),
                new AppProperties.Mail(false),
                new AppProperties.Notifications(false, 2),
                new AppProperties.Scheduler(false));
    }

    private EvolutionConnectionServiceImpl service() {
        return new EvolutionConnectionServiceImpl(evolutionApiClient, connectionRepository,
                textEncryptor, appProperties("http://backend:8080"));
    }

    private EvolutionConnection connectedConnection(ConnectionState state) {
        EvolutionConnection connection = new EvolutionConnection();
        connection.setInstanceName("mana-paes");
        connection.setInstanceApiKey(textEncryptor.encrypt("instance-token"));
        connection.setConnectionState(state);
        if (state == ConnectionState.OPEN) {
            connection.setConnectedNumber("5511999999999");
        }
        return connection;
    }

    @Test
    void startConnectionCreatesInstanceAndStoresEncryptedToken() {
        when(connectionRepository.findFirstByOrderByCreatedAtAsc()).thenReturn(Optional.empty());
        when(evolutionApiClient.createInstance(eq("mana-paes"), eq("http://backend:8080/api/v1/webhooks/evolution-api")))
                .thenReturn("instance-token");
        when(evolutionApiClient.connectInstance("mana-paes", "instance-token"))
                .thenReturn("data:image/png;base64,QR");

        WhatsAppStatus status = service().startConnection();

        verify(evolutionApiClient).createInstance("mana-paes", "http://backend:8080/api/v1/webhooks/evolution-api");
        verify(evolutionApiClient).connectInstance("mana-paes", "instance-token");
        assertThat(status.state()).isEqualTo("CONNECTING");
        assertThat(status.qrCodeBase64()).isEqualTo("data:image/png;base64,QR");
        assertThat(status.connectedNumber()).isNull();
        // token criptografado (nunca em texto puro)
        org.mockito.ArgumentCaptor<EvolutionConnection> captor =
                org.mockito.ArgumentCaptor.forClass(EvolutionConnection.class);
        verify(connectionRepository, org.mockito.Mockito.times(2)).save(captor.capture());
        EvolutionConnection saved = captor.getAllValues().get(0);
        assertThat(saved.getInstanceApiKey()).isNotEqualTo("instance-token");
        assertThat(textEncryptor.decrypt(saved.getInstanceApiKey())).isEqualTo("instance-token");
    }

    @Test
    void startConnectionReusesExistingTokenWithoutRecreatingInstance() {
        EvolutionConnection existing = connectedConnection(ConnectionState.CLOSE);
        when(connectionRepository.findFirstByOrderByCreatedAtAsc()).thenReturn(Optional.of(existing));
        when(evolutionApiClient.connectInstance("mana-paes", "instance-token"))
                .thenReturn("data:image/png;base64,QR");

        WhatsAppStatus status = service().startConnection();

        verify(evolutionApiClient, never()).createInstance(any(), any());
        verify(evolutionApiClient).connectInstance("mana-paes", "instance-token");
        assertThat(status.state()).isEqualTo("CONNECTING");
    }

    @Test
    void startConnectionWithoutBackendUrlRegistersWebhookDisabled() {
        EvolutionConnectionServiceImpl noBackend = new EvolutionConnectionServiceImpl(
                evolutionApiClient, connectionRepository, textEncryptor,
                appProperties(""));
        when(connectionRepository.findFirstByOrderByCreatedAtAsc()).thenReturn(Optional.empty());
        when(evolutionApiClient.createInstance("mana-paes", null)).thenReturn("instance-token");
        when(evolutionApiClient.connectInstance("mana-paes", "instance-token"))
                .thenReturn("data:image/png;base64,QR");

        WhatsAppStatus status = noBackend.startConnection();

        verify(evolutionApiClient).createInstance("mana-paes", null);
        assertThat(status.state()).isEqualTo("CONNECTING");
    }

    @Test
    void getStatusWithoutConnectionReturnsClosed() {
        when(connectionRepository.findFirstByOrderByCreatedAtAsc()).thenReturn(Optional.empty());

        WhatsAppStatus status = service().getStatus();

        assertThat(status.state()).isEqualTo("CLOSE");
        assertThat(status.qrCodeBase64()).isNull();
        assertThat(status.connectedNumber()).isNull();
    }

    @Test
    void getStatusMapsOpenConnection() {
        when(connectionRepository.findFirstByOrderByCreatedAtAsc())
                .thenReturn(Optional.of(connectedConnection(ConnectionState.OPEN)));

        WhatsAppStatus status = service().getStatus();

        assertThat(status.state()).isEqualTo("OPEN");
        assertThat(status.connectedNumber()).isEqualTo("5511999999999");
    }

    @Test
    void disconnectCallsLogoutAndClearsState() {
        EvolutionConnection existing = connectedConnection(ConnectionState.OPEN);
        when(connectionRepository.findFirstByOrderByCreatedAtAsc()).thenReturn(Optional.of(existing));

        service().disconnect();

        verify(evolutionApiClient).logoutInstance("mana-paes", "instance-token");
        assertThat(existing.getConnectionState()).isEqualTo(ConnectionState.CLOSE);
        assertThat(existing.getConnectedNumber()).isNull();
        assertThat(existing.getQrCodeBase64()).isNull();
        verify(connectionRepository).save(existing);
    }

    @Test
    void disconnectClearsLocalStateEvenWhenLogoutFails() {
        EvolutionConnection existing = connectedConnection(ConnectionState.OPEN);
        when(connectionRepository.findFirstByOrderByCreatedAtAsc()).thenReturn(Optional.of(existing));
        doThrow(new EvolutionApiException("instância não encontrada"))
                .when(evolutionApiClient).logoutInstance("mana-paes", "instance-token");

        service().disconnect();

        assertThat(existing.getConnectionState()).isEqualTo(ConnectionState.CLOSE);
        assertThat(existing.getConnectedNumber()).isNull();
    }

    @Test
    void testConnectionFailsGracefullyWhenNotConnected() {
        when(connectionRepository.findFirstByOrderByCreatedAtAsc())
                .thenReturn(Optional.of(connectedConnection(ConnectionState.CONNECTING)));

        EvolutionConnectionManager.TestMessage result = service().testConnection();

        assertThat(result.success()).isFalse();
        verify(evolutionApiClient, never()).sendText(any(), any(), any(), any());
    }

    @Test
    void testConnectionSendsToConnectedNumber() {
        when(connectionRepository.findFirstByOrderByCreatedAtAsc())
                .thenReturn(Optional.of(connectedConnection(ConnectionState.OPEN)));

        EvolutionConnectionManager.TestMessage result = service().testConnection();

        assertThat(result.success()).isTrue();
        assertThat(result.message()).contains("5511999999999");
        verify(evolutionApiClient).sendText("mana-paes", "instance-token", "5511999999999",
                "Teste de conexão — Sistema Mana Paes");
    }

    @Test
    void testConnectionReturnsFailureWhenSendFails() {
        when(connectionRepository.findFirstByOrderByCreatedAtAsc())
                .thenReturn(Optional.of(connectedConnection(ConnectionState.OPEN)));
        doThrow(new EvolutionApiNotConfiguredException("Evolution API não configurada"))
                .when(evolutionApiClient).sendText(eq("mana-paes"), eq("instance-token"),
                eq("5511999999999"), any());

        EvolutionConnectionManager.TestMessage result = service().testConnection();

        assertThat(result.success()).isFalse();
        assertThat(result.message()).contains("Falha ao enviar");
    }

    @Test
    void runSyncsOpenStateAndNumberFromEvolution() {
        EvolutionConnection existing = connectedConnection(ConnectionState.CLOSE);
        when(connectionRepository.findFirstByOrderByCreatedAtAsc()).thenReturn(Optional.of(existing));
        when(evolutionApiClient.getConnectionState("mana-paes", "instance-token"))
                .thenReturn(new EvolutionApiClient.ConnectionStateInfo("open", "5511999999999@s.whatsapp.net"));

        service().run(null);

        assertThat(existing.getConnectionState()).isEqualTo(ConnectionState.OPEN);
        assertThat(existing.getConnectedNumber()).isEqualTo("5511999999999");
        verify(connectionRepository).save(existing);
    }

    @Test
    void runSyncsCloseStateFromEvolution() {
        EvolutionConnection existing = connectedConnection(ConnectionState.OPEN);
        when(connectionRepository.findFirstByOrderByCreatedAtAsc()).thenReturn(Optional.of(existing));
        when(evolutionApiClient.getConnectionState("mana-paes", "instance-token"))
                .thenReturn(new EvolutionApiClient.ConnectionStateInfo("close", null));

        service().run(null);

        assertThat(existing.getConnectionState()).isEqualTo(ConnectionState.CLOSE);
        assertThat(existing.getConnectedNumber()).isNull();
    }
}