package vgandolfi.dev.mana_paes.application.event;

import vgandolfi.dev.mana_paes.domain.model.enums.UnitMeasure;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Evento disparado quando um pedido é criado (autenticado ou público via
 * link). Dados essenciais para as notificações WhatsApp (confirmação ao
 * vendedor e alerta ao admin).
 */
public record OrderCreatedEvent(
        UUID orderId,
        UUID tenantId,
        UUID vendorId,
        String vendorName,
        String vendorPhone,
        BigDecimal totalAmount,
        LocalDate deliveryDate,
        List<Item> items) {

    public record Item(UUID productId, String productName, BigDecimal totalQuantity, UnitMeasure unitMeasure) {
    }
}