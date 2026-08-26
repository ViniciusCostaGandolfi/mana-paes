package vgandolfi.dev.mana_paes.domain.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import vgandolfi.dev.mana_paes.application.dto.request.OrderItemRequest;
import vgandolfi.dev.mana_paes.application.dto.request.OrderRequest;
import vgandolfi.dev.mana_paes.application.dto.request.OrderStatusUpdateRequest;
import vgandolfi.dev.mana_paes.application.dto.response.OrderResponse;
import vgandolfi.dev.mana_paes.application.event.OrderCreatedEvent;
import vgandolfi.dev.mana_paes.domain.exception.BusinessException;
import vgandolfi.dev.mana_paes.domain.exception.EntityNotFoundException;
import vgandolfi.dev.mana_paes.domain.model.Order;
import vgandolfi.dev.mana_paes.domain.model.Product;
import vgandolfi.dev.mana_paes.domain.model.Tenant;
import vgandolfi.dev.mana_paes.domain.model.User;
import vgandolfi.dev.mana_paes.domain.model.enums.OrderStatus;
import vgandolfi.dev.mana_paes.domain.model.enums.UnitMeasure;
import vgandolfi.dev.mana_paes.domain.model.enums.UserRole;
import vgandolfi.dev.mana_paes.domain.repository.OrderRepository;
import vgandolfi.dev.mana_paes.domain.repository.ProductRepository;
import vgandolfi.dev.mana_paes.domain.repository.UserRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Regras de negócio do pedido: cálculo de total, resolução de solicitante,
 * transições de status e autorização de acesso.
 */
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private OrderEventPublisher orderEventPublisher;

    @InjectMocks
    private OrderService orderService;

    private final UUID tenantId = UUID.randomUUID();
    private final UUID requesterId = UUID.randomUUID();
    private final UUID orderId = UUID.randomUUID();
    private final LocalDate deliveryDate = LocalDate.now().plusDays(3);

    private Tenant tenant() {
        Tenant tenant = new Tenant();
        tenant.setId(tenantId);
        tenant.setName("Padaria Teste");
        return tenant;
    }

    private User user(UUID id, String name, UserRole role) {
        User user = new User();
        user.setId(id);
        user.setName(name);
        user.setEmail(name.toLowerCase() + "@example.com");
        user.setRole(role);
        user.setTenant(tenant());
        user.setActive(true);
        return user;
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

    private OrderRequest request(List<OrderItemRequest> items, UUID requestedRequesterId) {
        return new OrderRequest(deliveryDate, items, requestedRequesterId);
    }

    private OrderItemRequest item(UUID productId, BigDecimal quantity) {
        return new OrderItemRequest(productId, quantity);
    }

    @Test
    void createCalculatesTotalAndPublishesEvent() {
        Product pao = product(UUID.randomUUID(), "Pão Francês", new BigDecimal("0.50"));
        Product bolo = product(UUID.randomUUID(), "Bolo de Chocolate", new BigDecimal("25.00"));
        when(productRepository.findByIdAndTenantId(pao.getId(), tenantId)).thenReturn(Optional.of(pao));
        when(productRepository.findByIdAndTenantId(bolo.getId(), tenantId)).thenReturn(Optional.of(bolo));
        when(userRepository.findByIdAndTenantId(requesterId, tenantId))
                .thenReturn(Optional.of(user(requesterId, "Solicitante", UserRole.ROLE_REQUESTER)));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order order = inv.getArgument(0);
            order.setId(orderId);
            return order;
        });

        OrderResponse response = orderService.create(tenantId, requesterId, UserRole.ROLE_REQUESTER,
                request(List.of(item(pao.getId(), new BigDecimal("10")), item(bolo.getId(), new BigDecimal("2"))), null));

        assertThat(response.totalAmount()).isEqualByComparingTo("55.00");
        assertThat(response.status()).isEqualTo(OrderStatus.PENDING);
        assertThat(response.requesterId()).isEqualTo(requesterId);
        assertThat(response.items()).hasSize(2);

        ArgumentCaptor<OrderCreatedEvent> eventCaptor = ArgumentCaptor.forClass(OrderCreatedEvent.class);
        verify(orderEventPublisher).publish(eventCaptor.capture());
        assertThat(eventCaptor.getValue().orderId()).isEqualTo(orderId);
        assertThat(eventCaptor.getValue().totalAmount()).isEqualByComparingTo("55.00");
        assertThat(eventCaptor.getValue().items()).hasSize(2);
    }

    @Test
    void createByRequesterForAnotherUserThrows() {
        UUID other = UUID.randomUUID();
        assertThatThrownBy(() -> orderService.create(tenantId, requesterId, UserRole.ROLE_REQUESTER,
                request(List.of(item(UUID.randomUUID(), BigDecimal.ONE)), other)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("não pode criar pedido em nome de outro");
        verify(userRepository, never()).findByIdAndTenantId(any(), any());
    }

    @Test
    void createWithInactiveRequesterThrows() {
        User inactive = user(requesterId, "Inativo", UserRole.ROLE_REQUESTER);
        inactive.setActive(false);
        when(userRepository.findByIdAndTenantId(requesterId, tenantId)).thenReturn(Optional.of(inactive));

        assertThatThrownBy(() -> orderService.create(tenantId, requesterId, UserRole.ROLE_REQUESTER,
                request(List.of(item(UUID.randomUUID(), BigDecimal.ONE)), null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("solicitante inativo");
    }

    @Test
    void createWithInactiveProductThrows() {
        Product inactive = product(UUID.randomUUID(), "Fora", new BigDecimal("1.00"));
        inactive.setActive(false);
        when(userRepository.findByIdAndTenantId(requesterId, tenantId))
                .thenReturn(Optional.of(user(requesterId, "Solicitante", UserRole.ROLE_REQUESTER)));
        when(productRepository.findByIdAndTenantId(inactive.getId(), tenantId)).thenReturn(Optional.of(inactive));

        assertThatThrownBy(() -> orderService.create(tenantId, requesterId, UserRole.ROLE_REQUESTER,
                request(List.of(item(inactive.getId(), BigDecimal.ONE)), null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Produto inativo");
    }

    @Test
    void createWithMissingProductThrows() {
        UUID missing = UUID.randomUUID();
        when(userRepository.findByIdAndTenantId(requesterId, tenantId))
                .thenReturn(Optional.of(user(requesterId, "Solicitante", UserRole.ROLE_REQUESTER)));
        when(productRepository.findByIdAndTenantId(missing, tenantId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.create(tenantId, requesterId, UserRole.ROLE_REQUESTER,
                request(List.of(item(missing, BigDecimal.ONE)), null)))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void createByAdminForRequestedRequesterUsesRequestedId() {
        UUID target = UUID.randomUUID();
        Product pao = product(UUID.randomUUID(), "Pão", new BigDecimal("2.00"));
        when(productRepository.findByIdAndTenantId(pao.getId(), tenantId)).thenReturn(Optional.of(pao));
        when(userRepository.findByIdAndTenantId(target, tenantId))
                .thenReturn(Optional.of(user(target, "Funcionário", UserRole.ROLE_REQUESTER)));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderResponse response = orderService.create(tenantId, requesterId, UserRole.ROLE_ADMIN,
                request(List.of(item(pao.getId(), BigDecimal.ONE)), target));

        assertThat(response.requesterId()).isEqualTo(target);
    }

    @Test
    void updateStatusValidTransitionAddsHistory() {
        Order order = new Order();
        order.setId(orderId);
        order.setRequester(user(requesterId, "Solicitante", UserRole.ROLE_REQUESTER));
        order.setTenant(tenant());
        order.setStatus(OrderStatus.PENDING);

        when(userRepository.findByIdAndTenantId(requesterId, tenantId))
                .thenReturn(Optional.of(user(requesterId, "Admin", UserRole.ROLE_ADMIN)));
        when(orderRepository.findWithItemsAndTenant(orderId, tenantId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderResponse response = orderService.updateStatus(tenantId, requesterId, orderId,
                new OrderStatusUpdateRequest(OrderStatus.IN_PRODUCTION, "começando"));

        assertThat(response.status()).isEqualTo(OrderStatus.IN_PRODUCTION);
        assertThat(order.getStatusHistory()).hasSize(1);
        assertThat(order.getStatusHistory().get(0).getNewStatus()).isEqualTo(OrderStatus.IN_PRODUCTION);
    }

    @Test
    void updateStatusInvalidTransitionThrows() {
        Order order = new Order();
        order.setId(orderId);
        order.setRequester(user(requesterId, "Solicitante", UserRole.ROLE_REQUESTER));
        order.setTenant(tenant());
        order.setStatus(OrderStatus.PENDING);

        when(userRepository.findByIdAndTenantId(requesterId, tenantId))
                .thenReturn(Optional.of(user(requesterId, "Admin", UserRole.ROLE_ADMIN)));
        when(orderRepository.findWithItemsAndTenant(orderId, tenantId)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.updateStatus(tenantId, requesterId, orderId,
                new OrderStatusUpdateRequest(OrderStatus.DELIVERED, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Transição inválida");
    }

    @Test
    void updateStatusSameStatusThrows() {
        Order order = new Order();
        order.setId(orderId);
        order.setRequester(user(requesterId, "Solicitante", UserRole.ROLE_REQUESTER));
        order.setTenant(tenant());
        order.setStatus(OrderStatus.PENDING);

        when(userRepository.findByIdAndTenantId(requesterId, tenantId))
                .thenReturn(Optional.of(user(requesterId, "Admin", UserRole.ROLE_ADMIN)));
        when(orderRepository.findWithItemsAndTenant(orderId, tenantId)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.updateStatus(tenantId, requesterId, orderId,
                new OrderStatusUpdateRequest(OrderStatus.PENDING, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("já está no status");
    }

    @Test
    void getByIdDeniedForRequesterOfAnotherOrder() {
        UUID otherRequester = UUID.randomUUID();
        Order order = new Order();
        order.setId(orderId);
        order.setRequester(user(otherRequester, "Outro", UserRole.ROLE_REQUESTER));
        order.setTenant(tenant());

        when(orderRepository.findWithItemsAndTenant(orderId, tenantId)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.getById(tenantId, requesterId, UserRole.ROLE_REQUESTER, orderId))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void updateStatusUnknownOrderThrows() {
        when(userRepository.findByIdAndTenantId(requesterId, tenantId))
                .thenReturn(Optional.of(user(requesterId, "Admin", UserRole.ROLE_ADMIN)));
        when(orderRepository.findWithItemsAndTenant(orderId, tenantId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.updateStatus(tenantId, requesterId, orderId,
                new OrderStatusUpdateRequest(OrderStatus.READY, null)))
                .isInstanceOf(EntityNotFoundException.class);
    }
}