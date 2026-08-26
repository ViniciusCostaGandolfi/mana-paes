package vgandolfi.dev.mana_paes.application.event;

import vgandolfi.dev.mana_paes.domain.model.enums.UnitMeasure;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Evento disparado quando um pedido é criado. Dados essenciais para as
 * notificações (confirmação ao solicitante, alerta ao admin, etc.) que serão
 * consumidos na Fase 4.
 */
public record OrderCreatedEvent(
        UUID orderId,
        UUID tenantId,
        UUID requesterId,
        BigDecimal totalAmount,
        LocalDate deliveryDate,
        List<Item> items) {

    public record Item(UUID productId, String productName, BigDecimal quantity, UnitMeasure unitMeasure) {
    }
}