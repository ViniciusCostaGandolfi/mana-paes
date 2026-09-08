package vgandolfi.dev.mana_paes.api.controller;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vgandolfi.dev.mana_paes.application.dto.request.ProductActiveRequest;
import vgandolfi.dev.mana_paes.application.dto.request.ProductRequest;
import vgandolfi.dev.mana_paes.application.dto.response.ProductResponse;
import vgandolfi.dev.mana_paes.domain.service.ProductService;
import vgandolfi.dev.mana_paes.infrastructure.security.AuthenticatedUser;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public ResponseEntity<Page<ProductResponse>> list(@AuthenticationPrincipal AuthenticatedUser principal,
                                                      @RequestParam(required = false) Boolean active,
                                                      @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        return ResponseEntity.ok(productService.list(principal.tenantId(), active, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getById(@AuthenticationPrincipal AuthenticatedUser principal,
                                                   @PathVariable UUID id) {
        return ResponseEntity.ok(productService.getById(principal.tenantId(), id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductResponse> create(@AuthenticationPrincipal AuthenticatedUser principal,
                                                  @Valid @RequestBody ProductRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.create(principal.tenantId(), request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductResponse> update(@AuthenticationPrincipal AuthenticatedUser principal,
                                                  @PathVariable UUID id,
                                                  @Valid @RequestBody ProductRequest request) {
        return ResponseEntity.ok(productService.update(principal.tenantId(), id, request));
    }

    @PatchMapping("/{id}/active")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductResponse> setActive(@AuthenticationPrincipal AuthenticatedUser principal,
                                                     @PathVariable UUID id,
                                                     @Valid @RequestBody ProductActiveRequest request) {
        return ResponseEntity.ok(productService.setActive(principal.tenantId(), id, request.active()));
    }
}