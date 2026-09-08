package vgandolfi.dev.mana_paes.config;

import org.aopalliance.intercept.MethodInterceptor;
import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.retry.RejectAndDontRequeueRecoverer;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Declarações do RabbitMQ (exchange, filas e bindings) usadas na Fase 4.
 *
 * <p>Estratégia retry/DLQ:</p>
 * <ul>
 *   <li><b>Nível 1 — canal (serviço):</b> {@code NotificationServiceImpl} tenta
 *       cada envio (Evolution/SMTP) até {@code app.notifications.max-retries}
 *       (default 2) e registra o resultado no {@code NotificationLog}
 *       (SENT/FAILED + retryCount). Falhas de canal nunca rejeitam a mensagem.</li>
 *   <li><b>Nível 2 — entrega (broker):</b> o listener usa o interceptor de retry
 *       ({@link RetryInterceptorBuilder}, 2 retries com backoff 1s→2s→5s). Falhas
 *       inesperadas (DB fora do ar, desserialização, FK do pedido ainda não
 *       commitada) esgotam as tentativas e a mensagem é rejeitada sem requeue
 *       ({@code RejectAndDontRequeueRecoverer}), indo para a DLQ
 *       {@value #ORDERS_NOTIFICATIONS_DLQ} via dead-letter da fila principal.</li>
 * </ul>
 *
 * <p>Todo o bean graph é condicional a {@code app.notifications.enabled=true}
 * (default em dev/test é {@code false}), para que os testes rodem sem RabbitMQ.</p>
 */
@Configuration
@EnableRabbit
@ConditionalOnProperty(name = "app.notifications.enabled", havingValue = "true")
public class RabbitConfig {

    public static final String EXCHANGE = "mana-paes.events";
    public static final String ORDERS_NOTIFICATIONS_QUEUE = "mana-paes.orders.notifications";
    public static final String ORDERS_NOTIFICATIONS_DLQ = "mana-paes.orders.notifications.dlq";
    public static final String ORDERS_CREATED_ROUTING_KEY = "order.created";
    public static final String ORDERS_CREATED_DLQ_ROUTING_KEY = "order.created.dlq";

    @Bean
    public TopicExchange manaPaesEventsExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    @Bean
    public Queue ordersNotificationsQueue() {
        return QueueBuilder.durable(ORDERS_NOTIFICATIONS_QUEUE)
                .withArgument("x-dead-letter-exchange", EXCHANGE)
                .withArgument("x-dead-letter-routing-key", ORDERS_CREATED_DLQ_ROUTING_KEY)
                .build();
    }

    @Bean
    public Queue ordersNotificationsDlq() {
        return QueueBuilder.durable(ORDERS_NOTIFICATIONS_DLQ).build();
    }

    @Bean
    public Binding ordersNotificationsBinding() {
        return BindingBuilder.bind(ordersNotificationsQueue())
                .to(manaPaesEventsExchange())
                .with(ORDERS_CREATED_ROUTING_KEY);
    }

    @Bean
    public Binding ordersNotificationsDlqBinding() {
        return BindingBuilder.bind(ordersNotificationsDlq())
                .to(manaPaesEventsExchange())
                .with(ORDERS_CREATED_DLQ_ROUTING_KEY);
    }

    @Bean
    public Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         Jackson2JsonMessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        return template;
    }

    @Bean
    public MethodInterceptor rabbitRetryInterceptor() {
        return RetryInterceptorBuilder.stateless()
                .maxRetries(2)
                .backOffOptions(1000L, 2.0, 5000L)
                .recoverer(new RejectAndDontRequeueRecoverer())
                .build();
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            Jackson2JsonMessageConverter messageConverter,
            MethodInterceptor rabbitRetryInterceptor) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter);
        factory.setAcknowledgeMode(AcknowledgeMode.AUTO);
        factory.setAdviceChain(rabbitRetryInterceptor);
        return factory;
    }
}