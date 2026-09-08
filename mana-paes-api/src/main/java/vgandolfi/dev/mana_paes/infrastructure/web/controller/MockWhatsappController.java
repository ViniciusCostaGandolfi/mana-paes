package vgandolfi.dev.mana_paes.api.controller;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vgandolfi.dev.mana_paes.application.dto.response.WhatsAppStatus;
import vgandolfi.dev.mana_paes.infrastructure.whatsapp.MockEvolutionConnectionServiceImpl;

/**
 * Endpoint EXCLUSIVO do mock de conexão WhatsApp (dev/test): força a conexão
 * para OPEN sem depender de nenhuma Evolution API real.
 *
 * <p>O bean só existe quando {@code app.evolution.url} está em branco — em
 * produção este controller não é registrado.</p>
 */
@RestController
@RequestMapping("/api/v1/whatsapp")
@ConditionalOnExpression("!T(org.springframework.util.StringUtils).hasText('${app.evolution.url:}')")
public class MockWhatsappController {

    private final MockEvolutionConnectionServiceImpl mockService;

    public MockWhatsappController(MockEvolutionConnectionServiceImpl mockService) {
        this.mockService = mockService;
    }

    @PostMapping("/simulate-scan")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<WhatsAppStatus> simulateScan() {
        return ResponseEntity.ok(mockService.simulateScan());
    }
}