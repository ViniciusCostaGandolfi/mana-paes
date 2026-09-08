package vgandolfi.dev.mana_paes.infrastructure.web.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vgandolfi.dev.mana_paes.application.dto.response.MessageResponse;
import vgandolfi.dev.mana_paes.application.dto.response.WhatsAppStatus;
import vgandolfi.dev.mana_paes.infrastructure.security.AuthenticatedUser;
import vgandolfi.dev.mana_paes.infrastructure.whatsapp.EvolutionConnectionManager;

/**
 * Conexão WhatsApp via QR code (Evolution API) — UMA instância por ADMIN. O
 * SYS_ADMIN conecta o WhatsApp durante o onboarding.
 *
 * <p>Os endpoints são os mesmos para a implementação REAL (produção) e MOCK
 * (dev/test): {@code POST /connect} gera o QR, {@code GET /status} consulta o
 * estado, {@code POST /disconnect} encerra e {@code POST /test} envia uma
 * mensagem de teste. O endpoint de simulação de scan existe apenas no mock
 * ({@link MockWhatsappController}).</p>
 */
@RestController
@RequestMapping("/api/v1/whatsapp")
public class WhatsAppController {

    private final EvolutionConnectionManager connectionManager;

    public WhatsAppController(EvolutionConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    @PostMapping("/connect")
    @PreAuthorize("hasAnyRole('SYS_ADMIN', 'ADMIN')")
    public ResponseEntity<WhatsAppStatus> connect(@AuthenticationPrincipal AuthenticatedUser principal) {
        return ResponseEntity.ok(connectionManager.startConnection(principal.userId()));
    }

    @GetMapping("/status")
    @PreAuthorize("hasAnyRole('SYS_ADMIN', 'ADMIN')")
    public ResponseEntity<WhatsAppStatus> status(@AuthenticationPrincipal AuthenticatedUser principal) {
        return ResponseEntity.ok(connectionManager.getStatus(principal.userId()));
    }

    @PostMapping("/disconnect")
    @PreAuthorize("hasAnyRole('SYS_ADMIN', 'ADMIN')")
    public ResponseEntity<MessageResponse> disconnect(@AuthenticationPrincipal AuthenticatedUser principal) {
        connectionManager.disconnect(principal.userId());
        return ResponseEntity.ok(new MessageResponse("Conexão WhatsApp encerrada"));
    }

    @PostMapping("/test")
    @PreAuthorize("hasAnyRole('SYS_ADMIN', 'ADMIN')")
    public ResponseEntity<MessageResponse> test(@AuthenticationPrincipal AuthenticatedUser principal) {
        EvolutionConnectionManager.TestMessage result = connectionManager.testConnection(principal.userId());
        return ResponseEntity.ok(new MessageResponse(result.message()));
    }
}