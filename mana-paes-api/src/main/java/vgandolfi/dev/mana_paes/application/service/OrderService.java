package vgandolfi.dev.mana_paes.domain.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vgandolfi.dev.mana_paes.application.dto.request.OrderItemRequest;
import vgandolfi.dev.mana_paes.application.dto.request.OrderRequest;
import vgandolfi.dev.mana_paes.application.dto.request.OrderStatusUpdateRequest;
import vgandolfi.dev.mana_paes.application.dto.response.OrderResponse;
import vgandolfi.dev.mana_paes.application.event.OrderCreatedEvent;
import vgandolfi.dev.mana_paes.domain.exception.BusinessException;
import vgandolfi.dev.mana_paes.domain.exception.EntityNotFoundException;
import vgandolfi.dev.mana_paes.domain.model.Order;
import vgandolfi.dev.mana_paes.domain.model.OrderItem;
import vgandolfi.dev.mana_paes.domain.model.OrderStatusHistory;
import vgandolfi.dev.mana_paes.domain.model.Product;
import vgandolfi.dev.mana_paes.domain.model.Tenant;
import vgandolfi.dev.mana_paes.domain.model.User;
import vgandolfi.dev.mana_paes.domain.model.enums.OrderStatus;
import vgandolfi.dev.mana_paes.domain.model.enums.UserRole;
import vgandolfi.dev.mana_paes.domain.repository.OrderRepository;
import vgandolfi.dev.mana_paes.domain.repository.ProductRepository;
import vgandolfi.dev.mana_paes.domain.repository.UserRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Criação e gestão de pedidos com cálculo de total, validação de transição de
 * status, histórico e publicação de evento de pedido criado.
 */
@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED_TRANSITIONS = Map.of(
            OrderStatus.PENDING, Set.of(OrderStatus.IN_PRODUCTION, OrderStatus.CANCELLED),
            OrderStatus.IN_PRODUCTION, Set.of(OrderStatus.READY, OrderStatus.CANCELLED),
            OrderStatus.READY, Set.of(OrderStatus.DELIVERED),
            OrderStatus.DELIVERED, Set.of(),
            OrderStatus.CANCELLED, Set.of());

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final OrderEventPublisher orderEventPublisher;

    public OrderService(OrderRepository orderRepository,
                        ProductRepository productRepository,
                        UserRepository userRepository,
                        OrderEventPublisher orderEventPublisher) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.orderEventPublisher = orderEventPublisher;
    }

    @Transactional
    public OrderResponse create(UUID tenantId, UUID actorUserId, UserRole actorRole, OrderRequest request) {
        UUID requesterId = resolveRequesterId(tenantId, actorUserId, actorRole, request.requesterId());
        User requester = userRepository.findByIdAndTenantId(requesterId, tenantId)
                .orElseThrow(() -> EntityNotFoundException.of("Usuário", requesterId));
        if (!requester.isActive()) {
            throw new BusinessException("Usuário solicitante inativo");
        }
        Tenant tenant = requester.getTenant();

        Order order = new Order();
        order.setTenant(tenant);
        order.setRequester(requester);
        order.setDeliveryDate(request.deliveryDate());
        order.setStatus(OrderStatus.PENDING);

        BigDecimal total = BigDecimal.ZERO;
        List<OrderCreatedEvent.Item> eventItems = new ArrayList<>();
        for (OrderItemRequest itemReq : request.items()) {
            Product product = productRepository.findByIdAndTenantId(itemReq.productId(), tenantId)
                    .orElseThrow(() -> EntityNotFoundException.of("Produto", itemReq.productId()));
            if (!product.isActive()) {
                throw new BusinessException("Produto inativo: " + product.getName());
            }

            BigDecimal subtotal = itemReq.quantity().multiply(product.getUnitPrice())
                    .setScale(2, RoundingMode.HALF_UP);
            OrderItem item = new OrderItem();
            item.setProduct(product);
            item.setQuantity(itemReq.quantity());
            item.setUnitPrice(product.getUnitPrice()); // snapshot do preço no momento do pedido
            item.setSubtotal(subtotal);
            order.addItem(item);
            total = total.add(subtotal);

            eventItems.add(new OrderCreatedEvent.Item(
                    product.getId(), product.getName(), itemReq.quantity(), product.getUnitMeasure()));
        }
        order.setTotalAmount(total);

        addStatusHistory(order, null, OrderStatus.PENDING, requester, "Pedido criado");
        Order saved = orderRepository.save(order);

        orderEventPublisher.publish(new OrderCreatedEvent(
                saved.getId(), tenantId, requester.getId(), saved.getTotalAmount(),
                saved.getDeliveryDate(), eventItems));

        log.info("order_created orderId={} tenantId={} requesterId={} totalAmount={}",
                saved.getId(), tenantId, requester.getId(), saved.getTotalAmount());
        return OrderResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> list(UUID tenantId, Pageable pageable) {
        return orderRepository.findByTenantId(tenantId, pageable).map(OrderResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> listByRequester(UUID tenantId, UUID requesterId, Pageable pageable) {
        return orderRepository.findByRequesterIdAndTenantId(requesterId, tenantId, pageable)
                .map(OrderResponse::from);
    }

    @Transactional(readOnly = true)
    public OrderResponse getById(UUID tenantId, UUID actorUserId, UserRole actorRole, UUID orderId) {
        Order order = loadWithItems(orderId, tenantId);
        if (actorRole == UserRole.ROLE_REQUESTER && !order.getRequester().getId().equals(actorUserId)) {
            throw new AccessDeniedException("Sem acesso a este pedido");
        }
        return OrderResponse.from(order);
    }

    @Transactional
    public OrderResponse updateStatus(UUID tenantId, UUID actorUserId, UUID orderId,
                                      OrderStatusUpdateRequest request) {
        User actor = userRepository.findByIdAndTenantId(actorUserId, tenantId)
                .orElseThrow(() -> EntityNotFoundException.of("Usuário", actorUserId));
        Order order = loadWithItems(orderId, tenantId);

        OrderStatus previous = order.getStatus();
        OrderStatus next = request.status();
        validateTransition(previous, next);

        order.setStatus(next);
        addStatusHistory(order, previous, next, actor, request.reason());
        Order saved = orderRepository.save(order);

        log.info("order_status_changed orderId={} from={} to={} by={} reason={}",
                orderId, previous, next, actorUserId, request.reason());
        return OrderResponse.from(saved);
    }

    private Order loadWithItems(UUID orderId, UUID tenantId) {
        return orderRepository.findWithItemsAndTenant(orderId, tenantId)
                .orElseThrow(() -> EntityNotFoundException.of("Pedido", orderId));
    }

    private void addStatusHistory(Order order, OrderStatus previous, OrderStatus next,
                                  User changedBy, String reason) {
        OrderStatusHistory history = new OrderStatusHistory();
        history.setPreviousStatus(previous);
        history.setNewStatus(next);
        history.setChangedBy(changedBy);
        history.setChangedAt(Instant.now());
        history.setReason(reason);
        order.addStatusHistory(history);
    }

    private void validateTransition(OrderStatus current, OrderStatus next) {
        if (current == next) {
            throw new BusinessException("Pedido já está no status " + current);
        }
        Set<OrderStatus> allowed = ALLOWED_TRANSITIONS.get(current);
        if (allowed == null || !allowed.contains(next)) {
            throw new BusinessException("Transição inválida de " + current + " para " + next);
        }
    }

    private UUID resolveRequesterId(UUID tenantId, UUID actorUserId, UserRole actorRole, UUID requestedRequesterId) {
        if (actorRole == UserRole.ROLE_REQUESTER) {
            if (requestedRequesterId != null && !requestedRequesterId.equals(actorUserId)) {
                throw new BusinessException("Requester não pode criar pedido em nome de outro usuário");
            }
            return actorUserId;
        }
        // ADMIN pode criar para si ou para um requesterId informado no body
        return requestedRequesterId != null ? requestedRequesterId : actorUserId;
    }
}