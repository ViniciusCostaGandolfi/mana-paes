package vgandolfi.dev.mana_paes.infrastructure.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import vgandolfi.dev.mana_paes.application.event.OrderCreatedEvent;
import vgandolfi.dev.mana_paes.config.RabbitConfig;
import vgandolfi.dev.mana_paes.domain.service.OrderEventPublisher;

/**
 * Publisher real do evento de pedido criado (RabbitMQ), ativo quando
 * {@code app.notifications.enabled=true} (produção). Substitui o
 * {@link LoggingOrderEventPublisher} no contexto.
 *
 * <p>Falhas de publicação NUNCA derrubam o fluxo de criação de pedido: são
 * logadas e a criação segue. (Um refinement futuro — transactional outbox —
 * poderia garantir delivery, mas está fora do escopo do MVP.)</p>
 */
@Component
@ConditionalOnProperty(name = "app.notifications.enabled", havingValue = "true")
public class RabbitOrderEventPublisher implements OrderEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(RabbitOrderEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    public RabbitOrderEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public void publish(OrderCreatedEvent event) {
        try {
            rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE, RabbitConfig.ORDERS_CREATED_ROUTING_KEY, event);
            log.info("order_created_published orderId={} tenantId={}", event.orderId(), event.tenantId());
        } catch (AmqpException ex) {
            log.error("order_created_publish_failed orderId={} tenantId={}", event.orderId(), event.tenantId(), ex);
        }
    }
}