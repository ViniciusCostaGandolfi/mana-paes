package vgandolfi.dev.mana_paes.infrastructure.whatsapp;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import vgandolfi.dev.mana_paes.application.dto.response.WhatsAppStatus;
import vgandolfi.dev.mana_paes.infrastructure.config.AppProperties;
import vgandolfi.dev.mana_paes.infrastructure.config.EncryptionConfig;
import vgandolfi.dev.mana_paes.domain.model.EvolutionConnection;
import vgandolfi.dev.mana_paes.domain.model.User;
import vgandolfi.dev.mana_paes.domain.model.enums.ConnectionState;
import vgandolfi.dev.mana_paes.domain.repository.EvolutionConnectionRepository;
import vgandolfi.dev.mana_paes.domain.repository.UserRepository;
import vgandolfi.dev.mana_paes.infrastructure.evolution.EvolutionClient;
import vgandolfi.dev.mana_paes.infrastructure.notification.EvolutionApiClient;
import vgandolfi.dev.mana_paes.infrastructure.notification.EvolutionApiException;
import vgandolfi.dev.mana_paes.infrastructure.notification.EvolutionApiNotConfiguredException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Implementação REAL do gerenciador de conexão WhatsApp (1 instância por
 * ADMIN): cria a instância com nome derivado do userId, persiste o token
 * criptografado e sincroniza TODAS as conexões na subida.
 */
@ExtendWith(MockitoExtension.class)
class EvolutionConnectionServiceImplTest {

    private static final String MASTER_KEY = "mana-paes-test-master-key-32chars!";
    private static final String ENV_INSTANCE = "mana-paes";

    @Mock
    private EvolutionApiClient evolutionApiClient;
    @Mock
    private EvolutionConnectionRepository connectionRepository;
    @Mock
    private UserRepository userRepository;

    private final TextEncryptor textEncryptor =
            Encryptors.delux(MASTER_KEY, EncryptionConfig.MASTER_KEY_SALT_HEX);

    private final UUID userId = UUID.randomUUID();

    private AppProperties appProperties(String backendUrl) {
        return new AppProperties(
                new AppProperties.Jwt("test-secret-test-secret-test-secret-test-secret-1234", 3600000L, 86400000L),
                new AppProperties.Encryption(MASTER_KEY),
                new AppProperties.Evolution("http://evolution:8080", "global-key", 0L, ENV_INSTANCE, false),
                new AppProperties.Backend(backendUrl),
                new AppProperties.Frontend("http://localhost"),
                new AppProperties.Mail(false),
                new AppProperties.Notifications(false, 2),
                new AppProperties.Scheduler(false),
                new AppProperties.SystemAdmin("vinicius@vgandolfi.dev", "admin123"));
    }

    private EvolutionConnectionServiceImpl service() {
        return new EvolutionConnectionServiceImpl(evolutionApiClient, connectionRepository,
                userRepository, textEncryptor, appProperties("http://backend:8080"));
    }

    private String uniqueInstance() {
        return EvolutionConnectionServiceImpl.uniqueInstanceName(ENV_INSTANCE, userId);
    }

    private EvolutionConnection connectedConnection(ConnectionState state) {
        EvolutionConnection connection = new EvolutionConnection();
        connection.setInstanceName(uniqueInstance());
        connection.setInstanceApiKey(textEncryptor.encrypt("instance-token"));
        connection.setConnectionState(state);
        if (state == ConnectionState.OPEN) {
            connection.setConnectedNumber("5511999999999");
        }
        return connection;
    }

    @Test
    void uniqueInstanceNameDerivesFromEnvInstanceAndUserId() {
        assertThat(EvolutionConnectionServiceImpl.uniqueInstanceName("VGandolfi Dev", userId))
                .isEqualTo("VGandolfi Dev-" + userId.toString().substring(0, 8));
    }

    @Test
    void startConnectionCreatesUniqueInstanceAndStoresEncryptedToken() {
        when(connectionRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(userRepository.findById(userId)).thenReturn(Optional.of(user(userId)));
        when(evolutionApiClient.createInstance(eq(uniqueInstance()),
                eq("http://backend:8080/api/v1/webhooks/evolution-api")))
                .thenReturn("instance-token");
        when(evolutionApiClient.connectInstance(uniqueInstance(), "instance-token"))
                .thenReturn("data:image/png;base64,QR");

        WhatsAppStatus status = service().startConnection(userId);

        verify(evolutionApiClient).createInstance(uniqueInstance(),
                "http://backend:8080/api/v1/webhooks/evolution-api");
        verify(evolutionApiClient).connectInstance(uniqueInstance(), "instance-token");
        assertThat(status.state()).isEqualTo("CONNECTING");
        assertThat(status.qrCodeBase64()).isEqualTo("data:image/png;base64,QR");
        assertThat(status.connectedNumber()).isNull();
        org.mockito.ArgumentCaptor<EvolutionConnection> captor =
                org.mockito.ArgumentCaptor.forClass(EvolutionConnection.class);
        verify(connectionRepository, org.mockito.Mockito.times(2)).save(captor.capture());
        EvolutionConnection saved = captor.getAllValues().get(0);
        assertThat(saved.getUser().getId()).isEqualTo(userId);
        assertThat(saved.getInstanceName()).isEqualTo(uniqueInstance());
        assertThat(saved.getInstanceApiKey()).isNotEqualTo("instance-token");
        assertThat(textEncryptor.decrypt(saved.getInstanceApiKey())).isEqualTo("instance-token");
    }

    @Test
    void startConnectionReusesExistingConnectionWithoutRecreatingInstance() {
        EvolutionConnection existing = connectedConnection(ConnectionState.CLOSE);
        when(connectionRepository.findByUserId(userId)).thenReturn(Optional.of(existing));
        when(evolutionApiClient.connectInstance(uniqueInstance(), "instance-token"))
                .thenReturn("data:image/png;base64,QR");

        WhatsAppStatus status = service().startConnection(userId);

        verify(evolutionApiClient, never()).createInstance(any(), any());
        verify(evolutionApiClient).connectInstance(uniqueInstance(), "instance-token");
        assertThat(status.state()).isEqualTo("CONNECTING");
    }

    @Test
    void startConnectionWithoutBackendUrlRegistersWebhookDisabled() {
        EvolutionConnectionServiceImpl noBackend = new EvolutionConnectionServiceImpl(
                evolutionApiClient, connectionRepository, userRepository, textEncryptor, appProperties(""));
        when(connectionRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(userRepository.findById(userId)).thenReturn(Optional.of(user(userId)));
        when(evolutionApiClient.createInstance(uniqueInstance(), null)).thenReturn("instance-token");
        when(evolutionApiClient.connectInstance(uniqueInstance(), "instance-token"))
                .thenReturn("data:image/png;base64,QR");

        WhatsAppStatus status = noBackend.startConnection(userId);

        verify(evolutionApiClient).createInstance(uniqueInstance(), null);
        assertThat(status.state()).isEqualTo("CONNECTING");
    }

    @Test
    void getStatusWithoutConnectionReturnsClosed() {
        when(connectionRepository.findByUserId(userId)).thenReturn(Optional.empty());

        WhatsAppStatus status = service().getStatus(userId);

        assertThat(status.state()).isEqualTo("CLOSE");
        assertThat(status.qrCodeBase64()).isNull();
        assertThat(status.connectedNumber()).isNull();
    }

    @Test
    void getStatusMapsOpenConnection() {
        when(connectionRepository.findByUserId(userId))
                .thenReturn(Optional.of(connectedConnection(ConnectionState.OPEN)));

        WhatsAppStatus status = service().getStatus(userId);

        assertThat(status.state()).isEqualTo("OPEN");
        assertThat(status.connectedNumber()).isEqualTo("5511999999999");
    }

    @Test
    void disconnectCallsLogoutAndClearsState() {
        EvolutionConnection existing = connectedConnection(ConnectionState.OPEN);
        when(connectionRepository.findByUserId(userId)).thenReturn(Optional.of(existing));

        service().disconnect(userId);

        verify(evolutionApiClient).logoutInstance(uniqueInstance(), "instance-token");
        assertThat(existing.getConnectionState()).isEqualTo(ConnectionState.CLOSE);
        assertThat(existing.getConnectedNumber()).isNull();
        assertThat(existing.getQrCodeBase64()).isNull();
        verify(connectionRepository).save(existing);
    }

    @Test
    void disconnectClearsLocalStateEvenWhenLogoutFails() {
        EvolutionConnection existing = connectedConnection(ConnectionState.OPEN);
        when(connectionRepository.findByUserId(userId)).thenReturn(Optional.of(existing));
        doThrow(new EvolutionApiException("instância não encontrada"))
                .when(evolutionApiClient).logoutInstance(uniqueInstance(), "instance-token");

        service().disconnect(userId);

        assertThat(existing.getConnectionState()).isEqualTo(ConnectionState.CLOSE);
        assertThat(existing.getConnectedNumber()).isNull();
    }

    @Test
    void testConnectionFailsGracefullyWhenNotConnected() {
        when(connectionRepository.findByUserId(userId))
                .thenReturn(Optional.of(connectedConnection(ConnectionState.CONNECTING)));

        EvolutionConnectionManager.TestMessage result = service().testConnection(userId);

        assertThat(result.success()).isFalse();
        verify(evolutionApiClient, never()).sendText(any(), any(), any(), any());
    }

    @Test
    void testConnectionSendsToConnectedNumber() {
        when(connectionRepository.findByUserId(userId))
                .thenReturn(Optional.of(connectedConnection(ConnectionState.OPEN)));

        EvolutionConnectionManager.TestMessage result = service().testConnection(userId);

        assertThat(result.success()).isTrue();
        assertThat(result.message()).contains("5511999999999");
        verify(evolutionApiClient).sendText(uniqueInstance(), "instance-token", "5511999999999",
                "Teste de conexão — Sistema Mana Paes");
    }

    @Test
    void testConnectionReturnsFailureWhenSendFails() {
        when(connectionRepository.findByUserId(userId))
                .thenReturn(Optional.of(connectedConnection(ConnectionState.OPEN)));
        doThrow(new EvolutionApiNotConfiguredException("Evolution API não configurada"))
                .when(evolutionApiClient).sendText(eq(uniqueInstance()), eq("instance-token"),
                eq("5511999999999"), any());

        EvolutionConnectionManager.TestMessage result = service().testConnection(userId);

        assertThat(result.success()).isFalse();
        assertThat(result.message()).contains("Falha ao enviar");
    }

    @Test
    void runSyncsAllUserConnectionsFromEvolution() {
        UUID otherUserId = UUID.randomUUID();
        EvolutionConnection breno = connectedConnection(ConnectionState.CLOSE);
        EvolutionConnection another = connectedConnection(ConnectionState.OPEN); // será fechado pelo sync
        breno.setInstanceName(uniqueInstance());
        another.setInstanceName(EvolutionConnectionServiceImpl.uniqueInstanceName(ENV_INSTANCE, otherUserId));
        when(connectionRepository.findAllByUserIdNotNull()).thenReturn(List.of(breno, another));
        when(evolutionApiClient.getConnectionState(breno.getInstanceName(), "instance-token"))
                .thenReturn(new EvolutionClient.ConnectionStateInfo("open", "5511999999999@s.whatsapp.net"));
        when(evolutionApiClient.getConnectionState(another.getInstanceName(), "instance-token"))
                .thenReturn(new EvolutionClient.ConnectionStateInfo("close", null));

        service().run(null);

        assertThat(breno.getConnectionState()).isEqualTo(ConnectionState.OPEN);
        assertThat(breno.getConnectedNumber()).isEqualTo("5511999999999");
        assertThat(another.getConnectionState()).isEqualTo(ConnectionState.CLOSE);
        verify(connectionRepository, org.mockito.Mockito.times(2)).save(any());
    }

    @Test
    void runSyncContinuesWhenOneConnectionFails() {
        EvolutionConnection failing = connectedConnection(ConnectionState.CLOSE);
        EvolutionConnection ok = connectedConnection(ConnectionState.CLOSE);
        failing.setInstanceName(uniqueInstance());
        ok.setInstanceName(EvolutionConnectionServiceImpl.uniqueInstanceName(ENV_INSTANCE, UUID.randomUUID()));
        when(connectionRepository.findAllByUserIdNotNull()).thenReturn(List.of(failing, ok));
        doThrow(new EvolutionApiException("boom"))
                .when(evolutionApiClient).getConnectionState(failing.getInstanceName(), "instance-token");
        when(evolutionApiClient.getConnectionState(ok.getInstanceName(), "instance-token"))
                .thenReturn(new EvolutionClient.ConnectionStateInfo("open", "5511999999999@s.whatsapp.net"));

        service().run(null);

        assertThat(ok.getConnectionState()).isEqualTo(ConnectionState.OPEN);
        verify(connectionRepository).save(ok);
    }

    @Test
    void applyRemoteStateOpenSetsNumberAndState() {
        EvolutionConnection connection = connectedConnection(ConnectionState.CLOSE);
        service().applyRemoteState(connection,
                new EvolutionClient.ConnectionStateInfo("open", "5511999999999@s.whatsapp.net"));
        assertThat(connection.getConnectionState()).isEqualTo(ConnectionState.OPEN);
        assertThat(connection.getConnectedNumber()).isEqualTo("5511999999999");
    }

    @Test
    void applyRemoteStateCloseClearsState() {
        EvolutionConnection connection = connectedConnection(ConnectionState.OPEN);
        connection.setQrCodeBase64("data:image/png;base64,x");
        service().applyRemoteState(connection, new EvolutionClient.ConnectionStateInfo("close", null));
        assertThat(connection.getConnectionState()).isEqualTo(ConnectionState.CLOSE);
        assertThat(connection.getConnectedNumber()).isNull();
        assertThat(connection.getQrCodeBase64()).isNull();
    }

    private User user(UUID id) {
        User user = new User();
        user.setId(id);
        return user;
    }
}