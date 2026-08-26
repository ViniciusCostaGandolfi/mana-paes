package vgandolfi.dev.mana_paes.api.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vgandolfi.dev.mana_paes.application.dto.response.MessageResponse;
import vgandolfi.dev.mana_paes.application.dto.response.WhatsAppStatus;
import vgandolfi.dev.mana_paes.infrastructure.whatsapp.EvolutionConnectionManager;

/**
 * Conexão WhatsApp GLOBAL via QR code (Evolution API). Somente ADMIN.
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
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<WhatsAppStatus> connect() {
        return ResponseEntity.ok(connectionManager.startConnection());
    }

    @GetMapping("/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<WhatsAppStatus> status() {
        return ResponseEntity.ok(connectionManager.getStatus());
    }

    @PostMapping("/disconnect")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MessageResponse> disconnect() {
        connectionManager.disconnect();
        return ResponseEntity.ok(new MessageResponse("Conexão WhatsApp encerrada"));
    }

    @PostMapping("/test")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MessageResponse> test() {
        EvolutionConnectionManager.TestMessage result = connectionManager.testConnection();
        return ResponseEntity.ok(new MessageResponse(result.message()));
    }
}