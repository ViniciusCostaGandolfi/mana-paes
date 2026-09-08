package vgandolfi.dev.mana_paes.infrastructure.web.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vgandolfi.dev.mana_paes.application.dto.request.VendorActiveRequest;
import vgandolfi.dev.mana_paes.application.dto.request.VendorRequest;
import vgandolfi.dev.mana_paes.application.dto.response.VendorResponse;
import vgandolfi.dev.mana_paes.application.service.VendorService;
import vgandolfi.dev.mana_paes.infrastructure.security.AuthenticatedUser;

import java.util.List;
import java.util.UUID;

/**
 * CRUD de vendedores (tenant-scoped). O vendedor faz pedidos pelo link
 * público ({@code /api/v1/public/orders}); aqui o ADMIN gerencia o cadastro.
 */
@RestController
@RequestMapping("/api/v1/vendors")
@PreAuthorize("hasAnyRole('SYS_ADMIN', 'ADMIN')")
public class VendorController {

    private final VendorService vendorService;

    public VendorController(VendorService vendorService) {
        this.vendorService = vendorService;
    }

    @GetMapping
    public ResponseEntity<List<VendorResponse>> list(@AuthenticationPrincipal AuthenticatedUser principal) {
        return ResponseEntity.ok(vendorService.list(principal.tenantId()));
    }

    @PostMapping
    public ResponseEntity<VendorResponse> create(@AuthenticationPrincipal AuthenticatedUser principal,
                                                 @Valid @RequestBody VendorRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(vendorService.create(principal.tenantId(), request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<VendorResponse> getById(@AuthenticationPrincipal AuthenticatedUser principal,
                                                  @PathVariable UUID id) {
        return ResponseEntity.ok(vendorService.getById(principal.tenantId(), id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<VendorResponse> update(@AuthenticationPrincipal AuthenticatedUser principal,
                                                 @PathVariable UUID id,
                                                 @Valid @RequestBody VendorRequest request) {
        return ResponseEntity.ok(vendorService.update(principal.tenantId(), id, request));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<VendorResponse> setStatus(@AuthenticationPrincipal AuthenticatedUser principal,
                                                    @PathVariable UUID id,
                                                    @Valid @RequestBody VendorActiveRequest request) {
        return ResponseEntity.ok(vendorService.setActive(principal.tenantId(), id, request.active()));
    }
}