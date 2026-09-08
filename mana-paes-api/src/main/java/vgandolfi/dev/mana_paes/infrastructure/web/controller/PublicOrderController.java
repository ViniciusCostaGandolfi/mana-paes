package vgandolfi.dev.mana_paes.infrastructure.web.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vgandolfi.dev.mana_paes.application.dto.request.PublicOrderRequest;
import vgandolfi.dev.mana_paes.application.dto.response.OrderResponse;
import vgandolfi.dev.mana_paes.application.service.OrderService;

/**
 * Pedido público via link mágico (sem autenticação): o vendedor informa o
 * telefone e o backend resolve o tenant correspondente.
 */
@RestController
@RequestMapping("/api/v1/public/orders")
public class PublicOrderController {

    private final OrderService orderService;

    public PublicOrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> create(@Valid @RequestBody PublicOrderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.createPublicOrder(request));
    }
}