package vgandolfi.dev.mana_paes.infrastructure.web.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vgandolfi.dev.mana_paes.application.dto.response.MessageResponse;
import vgandolfi.dev.mana_paes.application.dto.response.WhatsAppStatus;
import vgandolfi.dev.mana_paes.domain.model.enums.UserRole;
import vgandolfi.dev.mana_paes.infrastructure.security.AuthenticatedUser;
import vgandolfi.dev.mana_paes.infrastructure.whatsapp.EvolutionConnectionManager;
import vgandolfi.dev.mana_paes.infrastructure.whatsapp.MockEvolutionConnectionServiceImpl;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Controllers de /api/v1/whatsapp (com o mock ativo): delegação ao
 * {@link EvolutionConnectionManager} com o userId do ADMIN logado. A
 * autorização (@PreAuthorize SYS_ADMIN/ADMIN) é coberta pelo teste de
 * integração.
 */
@ExtendWith(MockitoExtension.class)
class WhatsAppControllerTest {

    @Mock
    private EvolutionConnectionManager connectionManager;
    @Mock
    private MockEvolutionConnectionServiceImpl mockService;

    private final Instant now = Instant.now();
    private final UUID userId = UUID.randomUUID();
    private final AuthenticatedUser principal =
            new AuthenticatedUser(userId, "breno@example.com", UUID.randomUUID(), UserRole.ROLE_ADMIN);

    @Test
    void connectReturnsStatusFromManager() {
        WhatsAppStatus expected = new WhatsAppStatus("CONNECTING", "data:image/svg+xml;base64,x", null, now);
        when(connectionManager.startConnection(userId)).thenReturn(expected);

        WhatsAppStatus result = new WhatsAppController(connectionManager).connect(principal).getBody();

        assertThat(result).isEqualTo(expected);
        verify(connectionManager).startConnection(userId);
    }

    @Test
    void statusReturnsManagerStatus() {
        WhatsAppStatus expected = new WhatsAppStatus("OPEN", null, "5511999999999", now);
        when(connectionManager.getStatus(userId)).thenReturn(expected);

        WhatsAppStatus result = new WhatsAppController(connectionManager).status(principal).getBody();

        assertThat(result).isEqualTo(expected);
        verify(connectionManager).getStatus(userId);
    }

    @Test
    void disconnectReturnsConfirmationMessage() {
        MessageResponse result = new WhatsAppController(connectionManager).disconnect(principal).getBody();

        assertThat(result.message()).isNotBlank();
        verify(connectionManager).disconnect(userId);
    }

    @Test
    void testReturnsMessageFromManagerResult() {
        when(connectionManager.testConnection(userId))
                .thenReturn(new EvolutionConnectionManager.TestMessage(true, "Mensagem de teste enviada para 5511999999999"));

        MessageResponse result = new WhatsAppController(connectionManager).test(principal).getBody();

        assertThat(result.message()).contains("5511999999999");
        verify(connectionManager).testConnection(userId);
    }

    @Test
    void simulateScanForcesOpenInMockForPrincipal() {
        WhatsAppStatus expected = new WhatsAppStatus("OPEN", null, "5511999999999", now);
        when(mockService.simulateScan(userId)).thenReturn(expected);

        WhatsAppStatus result = new MockWhatsappController(mockService).simulateScan(principal).getBody();

        assertThat(result).isEqualTo(expected);
        verify(mockService).simulateScan(userId);
    }
}