package vgandolfi.dev.mana_paes.api.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vgandolfi.dev.mana_paes.application.dto.request.TenantRequest;
import vgandolfi.dev.mana_paes.application.dto.response.TenantResponse;
import vgandolfi.dev.mana_paes.domain.service.TenantService;
import vgandolfi.dev.mana_paes.infrastructure.security.AuthenticatedUser;

@RestController
@RequestMapping("/api/v1/tenant")
public class TenantController {

    private final TenantService tenantService;

    public TenantController(TenantService tenantService) {
        this.tenantService = tenantService;
    }

    @GetMapping
    public ResponseEntity<TenantResponse> getMyTenant(@AuthenticationPrincipal AuthenticatedUser principal) {
        return ResponseEntity.ok(tenantService.getById(principal.tenantId()));
    }

    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TenantResponse> update(@AuthenticationPrincipal AuthenticatedUser principal,
                                                 @Valid @RequestBody TenantRequest request) {
        return ResponseEntity.ok(tenantService.update(principal.tenantId(), request));
    }
}