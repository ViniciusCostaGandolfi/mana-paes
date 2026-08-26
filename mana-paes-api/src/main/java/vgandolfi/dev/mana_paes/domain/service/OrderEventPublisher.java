package vgandolfi.dev.mana_paes.domain.service;

import vgandolfi.dev.mana_paes.application.event.OrderCreatedEvent;

/**
 * Publica eventos de pedido criado. Na Fase 4 a implementação será o publisher
 * RabbitMQ; hoje existe apenas a implementação log-only ({@code LoggingOrderEventPublisher}).
 */
public interface OrderEventPublisher {

    void publish(OrderCreatedEvent event);
}