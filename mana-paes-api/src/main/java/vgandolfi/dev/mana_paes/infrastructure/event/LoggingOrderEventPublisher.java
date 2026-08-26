package vgandolfi.dev.mana_paes.infrastructure.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import vgandolfi.dev.mana_paes.application.event.OrderCreatedEvent;
import vgandolfi.dev.mana_paes.domain.service.OrderEventPublisher;

/**
 * Publisher log-only, ativo quando a pipeline de notificações está desabilitada
 * ({@code app.notifications.enabled=false}, default em dev/test). Quando
 * habilitada, o {@link RabbitOrderEventPublisher} assume e este bean é removido
 * (garante exatamente um {@code OrderEventPublisher} no contexto).
 */
@Component
@ConditionalOnProperty(name = "app.notifications.enabled", havingValue = "false", matchIfMissing = true)
public class LoggingOrderEventPublisher implements OrderEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(LoggingOrderEventPublisher.class);

    @Override
    public void publish(OrderCreatedEvent event) {
        log.info("order_created_event orderId={} tenantId={} requesterId={} totalAmount={} deliveryDate={} itemCount={}",
                event.orderId(), event.tenantId(), event.requesterId(),
                event.totalAmount(), event.deliveryDate(), event.items().size());
    }
}