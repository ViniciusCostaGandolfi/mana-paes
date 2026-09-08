package vgandolfi.dev.mana_paes.application.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vgandolfi.dev.mana_paes.application.dto.request.OrderItemRequest;
import vgandolfi.dev.mana_paes.application.dto.request.PublicOrderRequest;
import vgandolfi.dev.mana_paes.application.dto.request.SelectionRequest;
import vgandolfi.dev.mana_paes.application.dto.response.OrderResponse;
import vgandolfi.dev.mana_paes.application.event.OrderCreatedEvent;
import vgandolfi.dev.mana_paes.domain.exception.BusinessException;
import vgandolfi.dev.mana_paes.domain.model.Order;
import vgandolfi.dev.mana_paes.domain.model.Product;
import vgandolfi.dev.mana_paes.domain.model.ProductOption;
import vgandolfi.dev.mana_paes.domain.model.ProductOptionGroup;
import vgandolfi.dev.mana_paes.domain.model.Tenant;
import vgandolfi.dev.mana_paes.domain.model.Vendor;
import vgandolfi.dev.mana_paes.domain.model.enums.OrderStatus;
import vgandolfi.dev.mana_paes.domain.model.enums.UnitMeasure;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pedido público via link mágico: resolução do vendedor pelo telefone, cálculo
 * com turnos e opções (preço adicional), persistência e evento.
 */
@ExtendWith(MockitoExtension.class)
class PublicOrderServiceTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private VendorRepository vendorRepository;
    @Mock
    private VendorService vendorService;
    @Mock
    private OrderEventPublisher orderEventPublisher;

    @InjectMocks
    private OrderService orderService;

    private final UUID tenantId = UUID.randomUUID();
    private final UUID orderId = UUID.randomUUID();

    private Tenant tenant() {
        Tenant tenant = new Tenant();
        tenant.setId(tenantId);
        return tenant;
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

    @Test
    void createPublicOrderResolvesVendorByPhoneAndCalculatesTotal() {
        UUID vendorId = UUID.randomUUID();
        Vendor everton = vendor(vendorId);
        Product milho = product(UUID.randomUUID(), "Milho", new BigDecimal("16.80"));
        Product paoSal = product(UUID.randomUUID(), "Pão de sal", new BigDecimal("0.55"));

        when(vendorService.resolveActiveVendor("5511900000001")).thenReturn(everton);
        when(productRepository.findByIdAndTenantId(milho.getId(), tenantId)).thenReturn(Optional.of(milho));
        when(productRepository.findByIdAndTenantId(paoSal.getId(), tenantId)).thenReturn(Optional.of(paoSal));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order order = inv.getArgument(0);
            order.setId(orderId);
            return order;
        });

        PublicOrderRequest request = new PublicOrderRequest("5511900000001", "Everton",
                LocalDate.now().plusDays(1),
                List.of(
                        new OrderItemRequest(milho.getId(), new BigDecimal("3"), BigDecimal.ZERO, BigDecimal.ZERO, null),
                        new OrderItemRequest(paoSal.getId(), new BigDecimal("50"), new BigDecimal("50"), BigDecimal.ZERO, null)));

        OrderResponse response = orderService.createPublicOrder(request);

        // 3x16,80 + 100x0,55 = 50,40 + 55,00 = 105,40
        assertThat(response.totalAmount()).isEqualByComparingTo("105.40");
        assertThat(response.vendorId()).isEqualTo(vendorId);
        assertThat(response.vendorName()).isEqualTo("Everton");
        assertThat(response.requesterId()).isNull();
        assertThat(response.status()).isEqualTo(OrderStatus.PENDING);

        ArgumentCaptor<OrderCreatedEvent> eventCaptor = ArgumentCaptor.forClass(OrderCreatedEvent.class);
        verify(orderEventPublisher).publish(eventCaptor.capture());
        assertThat(eventCaptor.getValue().vendorPhone()).isEqualTo("5511900000001");
    }

    @Test
    void createPublicOrderWithOptionAdjustsUnitPrice() {
        UUID vendorId = UUID.randomUUID();
        when(vendorService.resolveActiveVendor("5511900000001"))
                .thenReturn(vendor(vendorId));
        Product paoSal = product(UUID.randomUUID(), "Pão de sal", new BigDecimal("0.55"));
        ProductOption grande = new ProductOption();
        grande.setId(UUID.randomUUID());
        grande.setName("Grande");
        grande.setPriceAdjustment(new BigDecimal("0.20"));
        grande.setActive(true);
        ProductOptionGroup grupo = new ProductOptionGroup();
        grupo.setId(UUID.randomUUID());
        grupo.setName("Tamanho");
        grupo.addOption(grande);
        paoSal.addOptionGroup(grupo);

        when(productRepository.findByIdAndTenantId(paoSal.getId(), tenantId)).thenReturn(Optional.of(paoSal));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        PublicOrderRequest request = new PublicOrderRequest("5511900000001", "Everton",
                LocalDate.now().plusDays(1),
                List.of(new OrderItemRequest(paoSal.getId(), new BigDecimal("10"), BigDecimal.ZERO, BigDecimal.ZERO,
                        List.of(new SelectionRequest(grupo.getId(), grande.getId())))));

        OrderResponse response = orderService.createPublicOrder(request);

        // 10 x (0,55 + 0,20) = 7,50
        assertThat(response.totalAmount()).isEqualByComparingTo("7.50");
        assertThat(response.items().get(0).unitPrice()).isEqualByComparingTo("0.75");
    }

    @Test
    void createPublicOrderWithUnknownVendorThrows() {
        when(vendorService.resolveActiveVendor("5511999999999"))
                .thenThrow(new BusinessException("Vendedor não cadastrado"));

        PublicOrderRequest request = new PublicOrderRequest("5511999999999", "X",
                LocalDate.now().plusDays(1),
                List.of(new OrderItemRequest(UUID.randomUUID(), BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ZERO, null)));

        assertThatThrownBy(() -> orderService.createPublicOrder(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Vendedor não cadastrado");
    }

    @Test
    void createPublicOrderWithAllZeroShiftsThrows() {
        UUID vendorId = UUID.randomUUID();
        when(vendorService.resolveActiveVendor("5511900000001"))
                .thenReturn(vendor(vendorId));
        Product pao = product(UUID.randomUUID(), "Pão", new BigDecimal("0.50"));
        when(productRepository.findByIdAndTenantId(pao.getId(), tenantId)).thenReturn(Optional.of(pao));

        PublicOrderRequest request = new PublicOrderRequest("5511900000001", "Everton",
                LocalDate.now().plusDays(1),
                List.of(new OrderItemRequest(pao.getId(), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, null)));

        assertThatThrownBy(() -> orderService.createPublicOrder(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("pelo menos um turno");
    }
}