package vgandolfi.dev.mana_paes.infrastructure.web.controller;

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
import vgandolfi.dev.mana_paes.application.dto.response.MessageResponse;
import vgandolfi.dev.mana_paes.application.dto.response.OrderResponse;
import vgandolfi.dev.mana_paes.application.service.OrderService;
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
    @PreAuthorize("hasAnyRole('SYS_ADMIN', 'ADMIN')")
    public ResponseEntity<OrderResponse> create(@AuthenticationPrincipal AuthenticatedUser principal,
                                                @Valid @RequestBody OrderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(orderService.create(principal.tenantId(), principal.userId(), request));
    }

    @GetMapping
    public ResponseEntity<Page<OrderResponse>> list(@AuthenticationPrincipal AuthenticatedUser principal,
                                                    @PageableDefault(size = 20, sort = "createdAt",
                                                            direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(orderService.list(principal.tenantId(), pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getById(@AuthenticationPrincipal AuthenticatedUser principal,
                                                 @PathVariable UUID id) {
        return ResponseEntity.ok(orderService.getById(principal.tenantId(), id));
    }

    @PostMapping("/accept-all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MessageResponse> acceptAll(@AuthenticationPrincipal AuthenticatedUser principal) {
        int accepted = orderService.acceptAll(principal.tenantId(), principal.userId()).accepted();
        return ResponseEntity.ok(new MessageResponse(accepted + " pedido(s) aceito(s)"));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<OrderResponse> updateStatus(@AuthenticationPrincipal AuthenticatedUser principal,
                                                      @PathVariable UUID id,
                                                      @Valid @RequestBody OrderStatusUpdateRequest request) {
        return ResponseEntity.ok(orderService.updateStatus(principal.tenantId(), principal.userId(), id, request));
    }
}