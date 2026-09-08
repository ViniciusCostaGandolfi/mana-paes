package vgandolfi.dev.mana_paes.application.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vgandolfi.dev.mana_paes.application.dto.request.OrderItemRequest;
import vgandolfi.dev.mana_paes.application.dto.request.OrderRequest;
import vgandolfi.dev.mana_paes.application.dto.request.OrderStatusUpdateRequest;
import vgandolfi.dev.mana_paes.application.dto.request.SelectionRequest;
import vgandolfi.dev.mana_paes.application.dto.response.OrderResponse;
import vgandolfi.dev.mana_paes.application.event.OrderCreatedEvent;
import vgandolfi.dev.mana_paes.domain.exception.BusinessException;
import vgandolfi.dev.mana_paes.domain.exception.EntityNotFoundException;
import vgandolfi.dev.mana_paes.domain.model.Order;
import vgandolfi.dev.mana_paes.domain.model.OrderItem;
import vgandolfi.dev.mana_paes.domain.model.OrderStatusHistory;
import vgandolfi.dev.mana_paes.domain.model.Product;
import vgandolfi.dev.mana_paes.domain.model.ProductOption;
import vgandolfi.dev.mana_paes.domain.model.ProductOptionGroup;
import vgandolfi.dev.mana_paes.domain.model.Tenant;
import vgandolfi.dev.mana_paes.domain.model.User;
import vgandolfi.dev.mana_paes.domain.model.Vendor;
import vgandolfi.dev.mana_paes.domain.model.enums.OrderStatus;
import vgandolfi.dev.mana_paes.domain.model.enums.UnitMeasure;
import vgandolfi.dev.mana_paes.domain.model.enums.UserRole;
import vgandolfi.dev.mana_paes.domain.repository.OrderRepository;
import vgandolfi.dev.mana_paes.domain.repository.ProductRepository;
import vgandolfi.dev.mana_paes.domain.repository.UserRepository;
import vgandolfi.dev.mana_paes.domain.repository.VendorRepository;
import vgandolfi.dev.mana_paes.domain.service.OrderEventPublisher;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private VendorRepository vendorRepository;
    @Mock
    private OrderEventPublisher orderEventPublisher;

    @InjectMocks
    private OrderService orderService;

    private final UUID tenantId = UUID.randomUUID();
    private final UUID actorId = UUID.randomUUID();
    private final UUID orderId = UUID.randomUUID();
    private final LocalDate deliveryDate = LocalDate.now().plusDays(3);

    private Tenant tenant() {
        Tenant tenant = new Tenant();
        tenant.setId(tenantId);
        tenant.setName("Padaria Teste");
        return tenant;
    }

    private User admin(UUID id) {
        User user = new User();
        user.setId(id);
        user.setName("Breno");
        user.setEmail("breno@example.com");
        user.setRole(UserRole.ROLE_ADMIN);
        user.setTenant(tenant());
        user.setActive(true);
        return user;
    }

    private Vendor vendor(UUID id) {
        Vendor vendor = new Vendor();
        vendor.setId(id);
        vendor.setName("Everton");
        vendor.setPhone("5511900000001");
        vendor.setWhatsappNumber("5511900000001");
        vendor.setTenant(tenant());
        vendor.setActive(true);
        return vendor;
    }

    private Product product(UUID id, String name, BigDecimal price) {
        Product product = new Product();
        product.setId(id);
        product.setName(name);
        product.setUnitPrice(price);
        product.setUnitMeasure(UnitMeasure.UN);
        product.setActive(true);
        product.setTenant(tenant());
        return product;
    }

    private ProductOptionGroup group(UUID id, ProductOption... options) {
        ProductOptionGroup group = new ProductOptionGroup();
        group.setId(id);
        group.setName("Tamanho");
        for (ProductOption option : options) {
            group.addOption(option);
        }
        return group;
    }

    private ProductOption option(UUID id, String name, String adjustment) {
        ProductOption option = new ProductOption();
        option.setId(id);
        option.setName(name);
        option.setPriceAdjustment(new BigDecimal(adjustment));
        option.setActive(true);
        return option;
    }

    private OrderItemRequest item(UUID productId, BigDecimal manha, BigDecimal tarde, BigDecimal noite,
                                  List<SelectionRequest> selections) {
        return new OrderItemRequest(productId, manha, tarde, noite, selections);
    }

    private OrderRequest request(List<OrderItemRequest> items, UUID vendorId) {
        return new OrderRequest(deliveryDate, items, vendorId);
    }

    private Order order(UUID id, OrderStatus status, Tenant tenant) {
        Order order = new Order();
        order.setId(id);
        order.setRequester(admin(actorId));
        order.setTenant(tenant);
        order.setStatus(status);
        return order;
    }

    private Tenant otherTenant() {
        Tenant tenant = new Tenant();
        tenant.setId(UUID.randomUUID());
        tenant.setName("Outra Padaria");
        return tenant;
    }

    @Test
    void createCalculatesTotalFromShiftsAndPublishesEvent() {
        Product pao = product(UUID.randomUUID(), "Pão Francês", new BigDecimal("0.50"));
        when(userRepository.findByIdAndTenantId(actorId, tenantId)).thenReturn(Optional.of(admin(actorId)));
        when(productRepository.findByIdAndTenantId(pao.getId(), tenantId)).thenReturn(Optional.of(pao));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order order = inv.getArgument(0);
            order.setId(orderId);
            return order;
        });

        // 50 manhã + 50 tarde + 0 noite = 100 x 0,50 = 50,00
        OrderResponse response = orderService.create(tenantId, actorId,
                request(List.of(item(pao.getId(), new BigDecimal("50"), new BigDecimal("50"), BigDecimal.ZERO, null)), null));

        assertThat(response.totalAmount()).isEqualByComparingTo("50.00");
        assertThat(response.status()).isEqualTo(OrderStatus.PENDING);
        assertThat(response.requesterId()).isEqualTo(actorId);
        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).totalQuantity()).isEqualByComparingTo("100");

        ArgumentCaptor<OrderCreatedEvent> eventCaptor = ArgumentCaptor.forClass(OrderCreatedEvent.class);
        verify(orderEventPublisher).publish(eventCaptor.capture());
        assertThat(eventCaptor.getValue().orderId()).isEqualTo(orderId);
        assertThat(eventCaptor.getValue().totalAmount()).isEqualByComparingTo("50.00");
        assertThat(eventCaptor.getValue().items().get(0).totalQuantity()).isEqualByComparingTo("100");
    }

    @Test
    void createWithOptionSelectionsAdjustsUnitPrice() {
        Product pao = product(UUID.randomUUID(), "Pão Francês", new BigDecimal("0.50"));
        ProductOption pequeno = option(UUID.randomUUID(), "Pequeno", "0.00");
        ProductOption grande = option(UUID.randomUUID(), "Grande", "0.20");
        pao.addOptionGroup(group(UUID.randomUUID(), pequeno, grande));

        when(userRepository.findByIdAndTenantId(actorId, tenantId)).thenReturn(Optional.of(admin(actorId)));
        when(productRepository.findByIdAndTenantId(pao.getId(), tenantId)).thenReturn(Optional.of(pao));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        // 10 manhã x (0,50 + 0,20) = 7,00
        OrderResponse response = orderService.create(tenantId, actorId,
                request(List.of(item(pao.getId(), new BigDecimal("10"), BigDecimal.ZERO, BigDecimal.ZERO,
                        List.of(new SelectionRequest(pao.getOptionGroups().get(0).getId(), grande.getId())))), null));

        assertThat(response.items().get(0).unitPrice()).isEqualByComparingTo("0.70");
        assertThat(response.totalAmount()).isEqualByComparingTo("7.00");
        assertThat(response.items().get(0).options()).hasSize(1);
        assertThat(response.items().get(0).options().get(0).optionName()).isEqualTo("Grande");
    }

    @Test
    void createWithAllZeroShiftsThrows() {
        Product pao = product(UUID.randomUUID(), "Pão Francês", new BigDecimal("0.50"));
        when(userRepository.findByIdAndTenantId(actorId, tenantId)).thenReturn(Optional.of(admin(actorId)));
        when(productRepository.findByIdAndTenantId(pao.getId(), tenantId)).thenReturn(Optional.of(pao));

        assertThatThrownBy(() -> orderService.create(tenantId, actorId,
                request(List.of(item(pao.getId(), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, null)), null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("pelo menos um turno");
    }

    @Test
    void createWithInactiveRequesterThrows() {
        User inactive = admin(actorId);
        inactive.setActive(false);
        when(userRepository.findByIdAndTenantId(actorId, tenantId)).thenReturn(Optional.of(inactive));

        assertThatThrownBy(() -> orderService.create(tenantId, actorId,
                request(List.of(item(UUID.randomUUID(), BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ZERO, null)), null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Usuário solicitante inativo");
    }

    @Test
    void createWithInactiveProductThrows() {
        Product inactive = product(UUID.randomUUID(), "Fora", new BigDecimal("1.00"));
        inactive.setActive(false);
        when(userRepository.findByIdAndTenantId(actorId, tenantId)).thenReturn(Optional.of(admin(actorId)));
        when(productRepository.findByIdAndTenantId(inactive.getId(), tenantId)).thenReturn(Optional.of(inactive));

        assertThatThrownBy(() -> orderService.create(tenantId, actorId,
                request(List.of(item(inactive.getId(), BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ZERO, null)), null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Produto inativo");
    }

    @Test
    void createWithMissingProductThrows() {
        UUID missing = UUID.randomUUID();
        when(userRepository.findByIdAndTenantId(actorId, tenantId)).thenReturn(Optional.of(admin(actorId)));
        when(productRepository.findByIdAndTenantId(missing, tenantId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.create(tenantId, actorId,
                request(List.of(item(missing, BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ZERO, null)), null)))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void createForVendorSetsVendorOnOrder() {
        UUID vendorId = UUID.randomUUID();
        Vendor vendor = vendor(vendorId);
        Product pao = product(UUID.randomUUID(), "Pão", new BigDecimal("2.00"));
        when(userRepository.findByIdAndTenantId(actorId, tenantId)).thenReturn(Optional.of(admin(actorId)));
        when(vendorRepository.findByIdAndTenantId(vendorId, tenantId)).thenReturn(Optional.of(vendor));
        when(productRepository.findByIdAndTenantId(pao.getId(), tenantId)).thenReturn(Optional.of(pao));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderResponse response = orderService.create(tenantId, actorId,
                request(List.of(item(pao.getId(), BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ZERO, null)), vendorId));

        assertThat(response.vendorId()).isEqualTo(vendorId);
        assertThat(response.vendorName()).isEqualTo("Everton");
        assertThat(response.requesterId()).isNull();
    }

    @Test
    void updateStatusValidTransitionAddsHistory() {
        Order order = new Order();
        order.setId(orderId);
        order.setRequester(admin(actorId));
        order.setTenant(tenant());
        order.setStatus(OrderStatus.PENDING);

        when(userRepository.findByIdAndTenantId(actorId, tenantId)).thenReturn(Optional.of(admin(actorId)));
        when(orderRepository.findWithItemsAndTenant(orderId, tenantId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderResponse response = orderService.updateStatus(tenantId, actorId, orderId,
                new OrderStatusUpdateRequest(OrderStatus.ACCEPTED, "aceito"));

        assertThat(response.status()).isEqualTo(OrderStatus.ACCEPTED);
        assertThat(order.getStatusHistory()).hasSize(1);
        assertThat(order.getStatusHistory().get(0).getNewStatus()).isEqualTo(OrderStatus.ACCEPTED);
    }

    @Test
    void updateStatusInvalidTransitionThrows() {
        Order order = new Order();
        order.setId(orderId);
        order.setRequester(admin(actorId));
        order.setTenant(tenant());
        order.setStatus(OrderStatus.ACCEPTED);

        when(userRepository.findByIdAndTenantId(actorId, tenantId)).thenReturn(Optional.of(admin(actorId)));
        when(orderRepository.findWithItemsAndTenant(orderId, tenantId)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.updateStatus(tenantId, actorId, orderId,
                new OrderStatusUpdateRequest(OrderStatus.PENDING, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Transição inválida");
    }

    @Test
    void updateStatusSameStatusThrows() {
        Order order = new Order();
        order.setId(orderId);
        order.setRequester(admin(actorId));
        order.setTenant(tenant());
        order.setStatus(OrderStatus.PENDING);

        when(userRepository.findByIdAndTenantId(actorId, tenantId)).thenReturn(Optional.of(admin(actorId)));
        when(orderRepository.findWithItemsAndTenant(orderId, tenantId)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.updateStatus(tenantId, actorId, orderId,
                new OrderStatusUpdateRequest(OrderStatus.PENDING, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("já está no status");
    }

    @Test
    void updateStatusUnknownOrderThrows() {
        when(userRepository.findByIdAndTenantId(actorId, tenantId)).thenReturn(Optional.of(admin(actorId)));
        when(orderRepository.findWithItemsAndTenant(orderId, tenantId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.updateStatus(tenantId, actorId, orderId,
                new OrderStatusUpdateRequest(OrderStatus.ACCEPTED, null)))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void acceptAllAcceptsOnlyPendingOrdersOfTenant() {
        User actor = admin(actorId);
        when(userRepository.findByIdAndTenantId(actorId, tenantId)).thenReturn(Optional.of(actor));

        Order pending1 = order(UUID.randomUUID(), OrderStatus.PENDING, tenant());
        Order pending2 = order(UUID.randomUUID(), OrderStatus.PENDING, tenant());
        Order alreadyAccepted = order(UUID.randomUUID(), OrderStatus.ACCEPTED, tenant());
        Order otherTenantPending = order(UUID.randomUUID(), OrderStatus.PENDING, otherTenant());

        when(orderRepository.findByTenantIdAndStatus(tenantId, OrderStatus.PENDING))
                .thenReturn(List.of(pending1, pending2));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderService.AcceptAllResult result = orderService.acceptAll(tenantId, actorId);

        assertThat(result.accepted()).isEqualTo(2);
        assertThat(pending1.getStatus()).isEqualTo(OrderStatus.ACCEPTED);
        assertThat(pending2.getStatus()).isEqualTo(OrderStatus.ACCEPTED);
        assertThat(alreadyAccepted.getStatus()).isEqualTo(OrderStatus.ACCEPTED);
        assertThat(otherTenantPending.getStatus()).isEqualTo(OrderStatus.PENDING);

        for (Order accepted : List.of(pending1, pending2)) {
            assertThat(accepted.getStatusHistory()).hasSize(1);
            OrderStatusHistory history = accepted.getStatusHistory().get(0);
            assertThat(history.getPreviousStatus()).isEqualTo(OrderStatus.PENDING);
            assertThat(history.getNewStatus()).isEqualTo(OrderStatus.ACCEPTED);
            assertThat(history.getChangedBy()).isEqualTo(actor);
            assertThat(history.getReason()).isEqualTo("Aceito em lote");
        }
        verify(orderRepository, times(2)).save(any(Order.class));
    }

    @Test
    void acceptAllWithNoPendingOrdersReturnsZero() {
        when(userRepository.findByIdAndTenantId(actorId, tenantId)).thenReturn(Optional.of(admin(actorId)));
        when(orderRepository.findByTenantIdAndStatus(tenantId, OrderStatus.PENDING)).thenReturn(List.of());

        OrderService.AcceptAllResult result = orderService.acceptAll(tenantId, actorId);

        assertThat(result.accepted()).isZero();
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void acceptAllUnknownActorThrows() {
        when(userRepository.findByIdAndTenantId(actorId, tenantId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.acceptAll(tenantId, actorId))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void getByIdReturnsOrder() {
        Order order = new Order();
        order.setId(orderId);
        order.setTenant(tenant());
        order.getItems().clear();
        when(orderRepository.findWithItemsAndTenant(orderId, tenantId)).thenReturn(Optional.of(order));

        OrderResponse response = orderService.getById(tenantId, orderId);

        assertThat(response.id()).isEqualTo(orderId);
    }
}