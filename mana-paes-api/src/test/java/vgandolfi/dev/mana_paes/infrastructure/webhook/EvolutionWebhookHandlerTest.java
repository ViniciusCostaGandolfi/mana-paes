package vgandolfi.dev.mana_paes.infrastructure.webhook;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vgandolfi.dev.mana_paes.application.dto.request.EvolutionWebhookPayload;
import vgandolfi.dev.mana_paes.domain.model.EvolutionConnection;
import vgandolfi.dev.mana_paes.domain.model.enums.ConnectionState;
import vgandolfi.dev.mana_paes.domain.repository.EvolutionConnectionRepository;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Persistência do estado da conexão global pelos eventos do webhook da
 * Evolution API: QRCODE_UPDATED salva o QR, CONNECTION_UPDATE open extrai o
 * número do wuid/number, close limpa; MESSAGES_UPSERT e eventos desconhecidos
 * apenas logam.
 */
@ExtendWith(MockitoExtension.class)
class EvolutionWebhookHandlerTest {

    @Mock
    private EvolutionConnectionRepository connectionRepository;

    private EvolutionWebhookHandler handler;

    @BeforeEach
    void setUp() {
        handler = new EvolutionWebhookHandler(connectionRepository);
    }

    private EvolutionConnection connection(ConnectionState state) {
        EvolutionConnection connection = new EvolutionConnection();
        connection.setInstanceName("mana-paes");
        connection.setConnectionState(state);
        return connection;
    }

    @Test
    void qrCodeUpdatedSavesBase64FromDataBase64() {
        EvolutionConnection connection = connection(ConnectionState.CONNECTING);
        when(connectionRepository.findByInstanceName("mana-paes")).thenReturn(Optional.of(connection));

        handler.handle(new EvolutionWebhookPayload("QRCODE_UPDATED", "mana-paes",
                Map.of("base64", "QUJDMTAy" )));

        assertThat(connection.getQrCodeBase64()).isEqualTo("data:image/png;base64,QUJDMTAy");
        assertThat(connection.getConnectionState()).isEqualTo(ConnectionState.CONNECTING);
        verify(connectionRepository).save(connection);
    }

    @Test
    void qrCodeUpdatedAcceptsNestedQrcodeBase64() {
        EvolutionConnection connection = connection(ConnectionState.CLOSE);
        when(connectionRepository.findByInstanceName("mana-paes")).thenReturn(Optional.of(connection));

        handler.handle(new EvolutionWebhookPayload("QRCODE_UPDATED", "mana-paes",
                Map.of("qrcode", Map.of("base64", "QUJD"))));

        assertThat(connection.getQrCodeBase64()).isEqualTo("data:image/png;base64,QUJD");
        // QR recebido com estado CLOSE -> CONNECTING
        assertThat(connection.getConnectionState()).isEqualTo(ConnectionState.CONNECTING);
        verify(connectionRepository).save(connection);
    }

    @Test
    void qrCodeUpdatedWithoutQrDoesNotSave() {
        EvolutionConnection connection = connection(ConnectionState.CONNECTING);
        when(connectionRepository.findByInstanceName("mana-paes")).thenReturn(Optional.of(connection));

        handler.handle(new EvolutionWebhookPayload("QRCODE_UPDATED", "mana-paes", Map.of()));

        assertThat(connection.getQrCodeBase64()).isNull();
        verify(connectionRepository, never()).save(connection);
    }

    @Test
    void qrCodeUpdatedForUnknownInstanceIsIgnored() {
        when(connectionRepository.findByInstanceName("other")).thenReturn(Optional.empty());

        handler.handle(new EvolutionWebhookPayload("QRCODE_UPDATED", "other",
                Map.of("base64", "QUJD")));

        verify(connectionRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void connectionUpdateOpenExtractsNumberFromWuid() {
        EvolutionConnection connection = connection(ConnectionState.CONNECTING);
        when(connectionRepository.findByInstanceName("mana-paes")).thenReturn(Optional.of(connection));

        handler.handle(new EvolutionWebhookPayload("CONNECTION_UPDATE", "mana-paes",
                Map.of("state", "open", "wuid", "5511999999999@s.whatsapp.net")));

        assertThat(connection.getConnectionState()).isEqualTo(ConnectionState.OPEN);
        assertThat(connection.getConnectedNumber()).isEqualTo("5511999999999");
        assertThat(connection.getQrCodeBase64()).isNull();
        verify(connectionRepository).save(connection);
    }

    @Test
    void connectionUpdateOpenPrefersDataNumber() {
        EvolutionConnection connection = connection(ConnectionState.CONNECTING);
        when(connectionRepository.findByInstanceName("mana-paes")).thenReturn(Optional.of(connection));

        handler.handle(new EvolutionWebhookPayload("CONNECTION_UPDATE", "mana-paes",
                Map.of("state", "open", "wuid", "999@s.whatsapp.net", "number", "5511888888888")));

        assertThat(connection.getConnectedNumber()).isEqualTo("5511888888888");
    }

    @Test
    void connectionUpdateCloseClearsState() {
        EvolutionConnection connection = connection(ConnectionState.OPEN);
        connection.setConnectedNumber("5511999999999");
        connection.setQrCodeBase64("data:image/png;base64,x");
        when(connectionRepository.findByInstanceName("mana-paes")).thenReturn(Optional.of(connection));

        handler.handle(new EvolutionWebhookPayload("CONNECTION_UPDATE", "mana-paes",
                Map.of("state", "close")));

        assertThat(connection.getConnectionState()).isEqualTo(ConnectionState.CLOSE);
        assertThat(connection.getConnectedNumber()).isNull();
        assertThat(connection.getQrCodeBase64()).isNull();
        verify(connectionRepository).save(connection);
    }

    @Test
    void connectionUpdateWithUnknownStateIsIgnored() {
        EvolutionConnection connection = connection(ConnectionState.CLOSE);
        when(connectionRepository.findByInstanceName("mana-paes")).thenReturn(Optional.of(connection));

        handler.handle(new EvolutionWebhookPayload("CONNECTION_UPDATE", "mana-paes",
                Map.of("state", "connecting")));

        assertThat(connection.getConnectionState()).isEqualTo(ConnectionState.CLOSE);
        verify(connectionRepository, never()).save(connection);
    }

    @Test
    void messagesUpsertIsOnlyLogged() {
        handler.handle(new EvolutionWebhookPayload("MESSAGES_UPSERT", "mana-paes", Map.of()));

        verify(connectionRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void unknownEventIsIgnored() {
        handler.handle(new EvolutionWebhookPayload("SOMETHING_ELSE", "mana-paes", Map.of()));

        verify(connectionRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void extractNumberFromWuidNormalizes() {
        assertThat(EvolutionWebhookHandler.extractNumberFromWuid("5511999999999@s.whatsapp.net"))
                .isEqualTo("5511999999999");
        assertThat(EvolutionWebhookHandler.extractNumberFromWuid("5511999999999")).isEqualTo("5511999999999");
        assertThat(EvolutionWebhookHandler.extractNumberFromWuid(null)).isNull();
        assertThat(EvolutionWebhookHandler.extractNumberFromWuid("  ")).isNull();
    }
}