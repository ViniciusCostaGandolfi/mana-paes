package vgandolfi.dev.mana_paes.application.dto.response;

import vgandolfi.dev.mana_paes.domain.model.Order;
import vgandolfi.dev.mana_paes.domain.model.enums.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        UUID requesterId,
        String requesterName,
        UUID vendorId,
        String vendorName,
        UUID tenantId,
        Instant createdAt,
        LocalDate deliveryDate,
        OrderStatus status,
        BigDecimal totalAmount,
        List<OrderItemResponse> items) {

    public static OrderResponse from(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getRequester() != null ? order.getRequester().getId() : null,
                order.getRequester() != null ? order.getRequester().getName() : null,
                order.getVendor() != null ? order.getVendor().getId() : null,
                order.getVendor() != null ? order.getVendor().getName() : null,
                order.getTenant().getId(),
                order.getCreatedAt(),
                order.getDeliveryDate(),
                order.getStatus(),
                order.getTotalAmount(),
                order.getItems().stream().map(OrderItemResponse::from).toList());
    }
}