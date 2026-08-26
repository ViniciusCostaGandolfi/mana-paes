package vgandolfi.dev.mana_paes.api.controller;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vgandolfi.dev.mana_paes.application.dto.request.OrderRequest;
import vgandolfi.dev.mana_paes.application.dto.request.OrderStatusUpdateRequest;
import vgandolfi.dev.mana_paes.application.dto.response.OrderResponse;
import vgandolfi.dev.mana_paes.domain.model.enums.UserRole;
import vgandolfi.dev.mana_paes.domain.service.OrderService;
import vgandolfi.dev.mana_paes.infrastructure.security.AuthenticatedUser;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'REQUESTER')")
    public ResponseEntity<OrderResponse> create(@AuthenticationPrincipal AuthenticatedUser principal,
                                                @Valid @RequestBody OrderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(orderService.create(principal.tenantId(), principal.userId(), principal.role(), request));
    }

    @GetMapping
    public ResponseEntity<Page<OrderResponse>> list(@AuthenticationPrincipal AuthenticatedUser principal,
                                                    @PageableDefault(size = 20, sort = "createdAt",
                                                            direction = Sort.Direction.DESC) Pageable pageable) {
        Page<OrderResponse> page = principal.role() == UserRole.ROLE_REQUESTER
                ? orderService.listByRequester(principal.tenantId(), principal.userId(), pageable)
                : orderService.list(principal.tenantId(), pageable);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getById(@AuthenticationPrincipal AuthenticatedUser principal,
                                                 @PathVariable UUID id) {
        return ResponseEntity.ok(orderService.getById(principal.tenantId(), principal.userId(), principal.role(), id));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'PRODUCTION')")
    public ResponseEntity<OrderResponse> updateStatus(@AuthenticationPrincipal AuthenticatedUser principal,
                                                      @PathVariable UUID id,
                                                      @Valid @RequestBody OrderStatusUpdateRequest request) {
        return ResponseEntity.ok(orderService.updateStatus(principal.tenantId(), principal.userId(), id, request));
    }
}