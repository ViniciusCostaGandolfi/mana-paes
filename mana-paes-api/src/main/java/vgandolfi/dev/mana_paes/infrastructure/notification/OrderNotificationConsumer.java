package vgandolfi.dev.mana_paes.infrastructure.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import vgandolfi.dev.mana_paes.application.event.OrderCreatedEvent;
import vgandolfi.dev.mana_paes.config.RabbitConfig;
import vgandolfi.dev.mana_paes.domain.service.NotificationService;

/**
 * Consumidor da fila {@code mana-paes.orders.notifications}: para cada pedido
 * criado delega ao {@link NotificationService} (que cuida dos canais,
 * configuração por tenant e {@code NotificationLog}).
 *
 * <p>Exceções inesperadas propagam e passam pelo retry/DLQ configurado no
 * {@link RabbitConfig} (spring-retry 3 tentativas + DLQ). Falhas de canal
 * (Evolution/SMTP) são absorvidas pelo serviço e não chegam aqui.</p>
 */
@Component
@ConditionalOnProperty(name = "app.notifications.enabled", havingValue = "true")
public class OrderNotificationConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderNotificationConsumer.class);

    private final NotificationService notificationService;

    public OrderNotificationConsumer(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @RabbitListener(queues = RabbitConfig.ORDERS_NOTIFICATIONS_QUEUE)
    public void onOrderCreated(OrderCreatedEvent event) {
        log.info("consume_order_created orderId={} tenantId={}", event.orderId(), event.tenantId());
        notificationService.sendOrderNotifications(event);
    }
}