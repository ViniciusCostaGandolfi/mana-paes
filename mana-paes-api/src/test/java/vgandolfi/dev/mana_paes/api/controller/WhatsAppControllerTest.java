package vgandolfi.dev.mana_paes.api.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vgandolfi.dev.mana_paes.application.dto.response.MessageResponse;
import vgandolfi.dev.mana_paes.application.dto.response.WhatsAppStatus;
import vgandolfi.dev.mana_paes.infrastructure.whatsapp.EvolutionConnectionManager;
import vgandolfi.dev.mana_paes.infrastructure.whatsapp.MockEvolutionConnectionServiceImpl;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Controllers de /api/v1/whatsapp (com o mock ativo): delegação ao
 * {@link EvolutionConnectionManager} e mapeamento para os DTOs de resposta.
 * A autorização (@PreAuthorize ADMIN) é coberta pelo teste de integração.
 */
@ExtendWith(MockitoExtension.class)
class WhatsAppControllerTest {

    @Mock
    private EvolutionConnectionManager connectionManager;
    @Mock
    private MockEvolutionConnectionServiceImpl mockService;

    private final Instant now = Instant.now();

    @Test
    void connectReturnsStatusFromManager() {
        WhatsAppStatus expected = new WhatsAppStatus("CONNECTING", "data:image/svg+xml;base64,x", null, now);
        when(connectionManager.startConnection()).thenReturn(expected);

        WhatsAppStatus result = new WhatsAppController(connectionManager).connect().getBody();

        assertThat(result).isEqualTo(expected);
        verify(connectionManager).startConnection();
    }

    @Test
    void statusReturnsManagerStatus() {
        WhatsAppStatus expected = new WhatsAppStatus("OPEN", null, "5511999999999", now);
        when(connectionManager.getStatus()).thenReturn(expected);

        WhatsAppStatus result = new WhatsAppController(connectionManager).status().getBody();

        assertThat(result).isEqualTo(expected);
        verify(connectionManager).getStatus();
    }

    @Test
    void disconnectReturnsConfirmationMessage() {
        MessageResponse result = new WhatsAppController(connectionManager).disconnect().getBody();

        assertThat(result.message()).isNotBlank();
        verify(connectionManager).disconnect();
    }

    @Test
    void testReturnsMessageFromManagerResult() {
        when(connectionManager.testConnection())
                .thenReturn(new EvolutionConnectionManager.TestMessage(true, "Mensagem de teste enviada para 5511999999999"));

        MessageResponse result = new WhatsAppController(connectionManager).test().getBody();

        assertThat(result.message()).contains("5511999999999");
        verify(connectionManager).testConnection();
    }

    @Test
    void simulateScanForcesOpenInMock() {
        WhatsAppStatus expected = new WhatsAppStatus("OPEN", null, "5511999999999", now);
        when(mockService.simulateScan()).thenReturn(expected);

        WhatsAppStatus result = new MockWhatsappController(mockService).simulateScan().getBody();

        assertThat(result).isEqualTo(expected);
        verify(mockService).simulateScan();
    }
}