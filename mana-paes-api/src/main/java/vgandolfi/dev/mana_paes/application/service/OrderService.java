package vgandolfi.dev.mana_paes.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vgandolfi.dev.mana_paes.application.dto.request.OrderItemRequest;
import vgandolfi.dev.mana_paes.application.dto.request.OrderRequest;
import vgandolfi.dev.mana_paes.application.dto.request.OrderStatusUpdateRequest;
import vgandolfi.dev.mana_paes.application.dto.request.PublicOrderRequest;
import vgandolfi.dev.mana_paes.application.dto.request.SelectionRequest;
import vgandolfi.dev.mana_paes.application.dto.response.OrderResponse;
import vgandolfi.dev.mana_paes.application.event.OrderCreatedEvent;
import vgandolfi.dev.mana_paes.domain.exception.BusinessException;
import vgandolfi.dev.mana_paes.domain.exception.EntityNotFoundException;
import vgandolfi.dev.mana_paes.domain.model.Order;
import vgandolfi.dev.mana_paes.domain.model.OrderItem;
import vgandolfi.dev.mana_paes.domain.model.OrderItemOption;
import vgandolfi.dev.mana_paes.domain.model.OrderStatusHistory;
import vgandolfi.dev.mana_paes.domain.model.Product;
import vgandolfi.dev.mana_paes.domain.model.ProductOption;
import vgandolfi.dev.mana_paes.domain.model.ProductOptionGroup;
import vgandolfi.dev.mana_paes.domain.model.Tenant;
import vgandolfi.dev.mana_paes.domain.model.User;
import vgandolfi.dev.mana_paes.domain.model.Vendor;
import vgandolfi.dev.mana_paes.domain.model.enums.OrderStatus;
import vgandolfi.dev.mana_paes.domain.repository.OrderRepository;
import vgandolfi.dev.mana_paes.domain.repository.ProductRepository;
import vgandolfi.dev.mana_paes.domain.repository.UserRepository;
import vgandolfi.dev.mana_paes.domain.repository.VendorRepository;
import vgandolfi.dev.mana_paes.domain.service.OrderEventPublisher;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Criação e gestão de pedidos: via link público (vendedor, sem login) e
 * autenticado (SYS_ADMIN/ADMIN). Calcula o total por item como
 * totalQuantidade(turnos) × unitPrice, onde unitPrice = preço base do produto
 * + Σ priceAdjustment das opções escolhidas (snapshot).
 */
@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED_TRANSITIONS = Map.of(
            OrderStatus.PENDING, Set.of(OrderStatus.ACCEPTED, OrderStatus.CANCELLED),
            OrderStatus.ACCEPTED, Set.of(OrderStatus.CANCELLED),
            OrderStatus.CANCELLED, Set.of());

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final VendorRepository vendorRepository;
    private final VendorService vendorService;
    private final OrderEventPublisher orderEventPublisher;

    public OrderService(OrderRepository orderRepository,
                        ProductRepository productRepository,
                        UserRepository userRepository,
                        VendorRepository vendorRepository,
                        VendorService vendorService,
                        OrderEventPublisher orderEventPublisher) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.vendorRepository = vendorRepository;
        this.vendorService = vendorService;
        this.orderEventPublisher = orderEventPublisher;
    }

    /**
     * Pedido autenticado (SYS_ADMIN/ADMIN): cria para um vendedor informado
     * ({@code vendorId}) ou manualmente (requester = ator autenticado).
     */
    @Transactional
    public OrderResponse create(UUID tenantId, UUID actorUserId, OrderRequest request) {
        User actor = userRepository.findByIdAndTenantId(actorUserId, tenantId)
                .orElseThrow(() -> EntityNotFoundException.of("Usuário", actorUserId));
        if (!actor.isActive()) {
            throw new BusinessException("Usuário solicitante inativo");
        }
        Tenant tenant = actor.getTenant();

        Vendor vendor = null;
        if (request.vendorId() != null) {
            vendor = vendorRepository.findByIdAndTenantId(request.vendorId(), tenantId)
                    .orElseThrow(() -> EntityNotFoundException.of("Vendedor", request.vendorId()));
            if (!vendor.isActive()) {
                throw new BusinessException("Vendedor inativo: " + vendor.getName());
            }
        }

        Order order = buildOrder(tenant, actor, vendor, request.deliveryDate(), request.items());
        addStatusHistory(order, null, OrderStatus.PENDING, actor, "Pedido criado");
        Order saved = orderRepository.save(order);
        publishCreated(saved, vendor, request.items());
        return OrderResponse.from(saved);
    }

    /**
     * Pedido público via link mágico: o vendedor é resolvido pelo telefone
     * (sem autenticação). Requester fica nulo; o vendedor identifica o pedido.
     */
    @Transactional
    public OrderResponse createPublicOrder(PublicOrderRequest request) {
        Vendor vendor = vendorService.resolveActiveVendor(request.vendorPhone());
        Tenant tenant = vendor.getTenant();

        Order order = buildOrder(tenant, null, vendor, request.deliveryDate(), request.items());
        addStatusHistory(order, null, OrderStatus.PENDING, null, "Pedido via link público");
        Order saved = orderRepository.save(order);
        publishCreated(saved, vendor, request.items());
        return OrderResponse.from(saved);
    }

    private Order buildOrder(Tenant tenant, User actor, Vendor vendor, java.time.LocalDate deliveryDate,
                             List<OrderItemRequest> itemRequests) {
        Order order = new Order();
        order.setTenant(tenant);
        // pedido manual: requester = ator; pedido de vendedor: identifica pelo vendor
        order.setRequester(vendor == null ? actor : null);
        order.setVendor(vendor);
        order.setDeliveryDate(deliveryDate);
        order.setStatus(OrderStatus.PENDING);

        BigDecimal total = BigDecimal.ZERO;
        for (OrderItemRequest itemReq : itemRequests) {
            Product product = productRepository.findByIdAndTenantId(itemReq.productId(), tenant.getId())
                    .orElseThrow(() -> EntityNotFoundException.of("Produto", itemReq.productId()));
            if (!product.isActive()) {
                throw new BusinessException("Produto inativo: " + product.getName());
            }
            if (itemReq.getTotalQuantity().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException("Informe pelo menos um turno com quantidade maior que zero para " + product.getName());
            }

            List<OrderItemOption> selected = resolveSelections(product, itemReq.selections());
            BigDecimal unitPrice = product.getUnitPrice().add(
                    selected.stream().map(OrderItemOption::getPriceAdjustment)
                            .reduce(BigDecimal.ZERO, BigDecimal::add));
            BigDecimal subtotal = itemReq.getTotalQuantity().multiply(unitPrice)
                    .setScale(2, RoundingMode.HALF_UP);

            OrderItem item = new OrderItem();
            item.setProduct(product);
            item.setQuantidadeManha(itemReq.quantidadeManha());
            item.setQuantidadeTarde(itemReq.quantidadeTarde());
            item.setQuantidadeNoite(itemReq.quantidadeNoite());
            item.setUnitPrice(unitPrice); // snapshot do preço + opções
            item.setSubtotal(subtotal);
            selected.forEach(item::addOption);
            order.addItem(item);
            total = total.add(subtotal);
        }
        order.setTotalAmount(total);
        return order;
    }

    /**
     * Valida as seleções contra os grupos/opções do produto e devolve os
     * snapshots de {@code OrderItemOption}. Opção inativa é rejeitada.
     */
    private List<OrderItemOption> resolveSelections(Product product, List<SelectionRequest> selections) {
        if (selections == null || selections.isEmpty()) {
            return List.of();
        }
        Map<UUID, ProductOptionGroup> groupsById = new java.util.HashMap<>();
        for (ProductOptionGroup group : product.getOptionGroups()) {
            groupsById.put(group.getId(), group);
        }
        List<OrderItemOption> result = new ArrayList<>();
        for (SelectionRequest selection : selections) {
            ProductOptionGroup group = groupsById.get(selection.groupId());
            if (group == null) {
                throw new BusinessException("Grupo de opção não pertence ao produto " + product.getName());
            }
            ProductOption option = group.getOptions().stream()
                    .filter(o -> o.getId().equals(selection.optionId()))
                    .findFirst()
                    .orElseThrow(() -> new BusinessException("Opção inválida para o grupo " + group.getName()));
            if (!option.isActive()) {
                throw new BusinessException("Opção inativa: " + option.getName());
            }
            OrderItemOption itemOption = new OrderItemOption();
            itemOption.setOptionId(option.getId());
            itemOption.setOptionName(option.getName());
            itemOption.setPriceAdjustment(option.getPriceAdjustment());
            result.add(itemOption);
        }
        return result;
    }

    private void publishCreated(Order saved, Vendor vendor, List<OrderItemRequest> itemRequests) {
        List<OrderCreatedEvent.Item> eventItems = saved.getItems().stream()
                .map(item -> new OrderCreatedEvent.Item(
                        item.getProduct().getId(),
                        item.getProduct().getName(),
                        item.getTotalQuantity(),
                        item.getProduct().getUnitMeasure()))
                .toList();
        String vendorPhone = vendor == null ? null : firstText(vendor.getWhatsappNumber(), vendor.getPhone());
        orderEventPublisher.publish(new OrderCreatedEvent(
                saved.getId(), saved.getTenant().getId(),
                vendor != null ? vendor.getId() : null,
                vendor != null ? vendor.getName() : null,
                vendorPhone,
                saved.getTotalAmount(), saved.getDeliveryDate(), eventItems));

        log.info("order_created orderId={} tenantId={} vendor={} totalAmount={} items={}",
                saved.getId(), saved.getTenant().getId(),
                vendor != null ? vendor.getName() : "manual", saved.getTotalAmount(), eventItems.size());
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> list(UUID tenantId, Pageable pageable) {
        return orderRepository.findByTenantId(tenantId, pageable).map(OrderResponse::from);
    }

    @Transactional(readOnly = true)
    public OrderResponse getById(UUID tenantId, UUID orderId) {
        Order order = loadWithItems(orderId, tenantId);
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

    /**
     * Aceita em lote todos os pedidos {@code PENDING} do tenant: cada um vai
     * para {@code ACCEPTED} com um histórico (previous=PENDING, next=ACCEPTED,
     * changedBy=ator, reason "Aceito em lote"). Não notifica por pedido.
     */
    @Transactional
    public AcceptAllResult acceptAll(UUID tenantId, UUID actorUserId) {
        User actor = userRepository.findByIdAndTenantId(actorUserId, tenantId)
                .orElseThrow(() -> EntityNotFoundException.of("Usuário", actorUserId));
        List<Order> pending = orderRepository.findByTenantIdAndStatus(tenantId, OrderStatus.PENDING);
        for (Order order : pending) {
            order.setStatus(OrderStatus.ACCEPTED);
            addStatusHistory(order, OrderStatus.PENDING, OrderStatus.ACCEPTED, actor, "Aceito em lote");
            orderRepository.save(order);
        }
        if (!pending.isEmpty()) {
            log.info("orders_batch_accepted tenantId={} by={} count={}", tenantId, actorUserId, pending.size());
        }
        return new AcceptAllResult(pending.size());
    }

    public record AcceptAllResult(int accepted) {
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

    private String firstText(String primary, String fallback) {
        return (primary != null && !primary.isBlank()) ? primary : fallback;
    }
}