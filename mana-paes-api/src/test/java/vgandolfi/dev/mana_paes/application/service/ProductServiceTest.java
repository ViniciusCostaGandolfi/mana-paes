package vgandolfi.dev.mana_paes.application.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import vgandolfi.dev.mana_paes.application.dto.request.ProductRequest;
import vgandolfi.dev.mana_paes.application.dto.response.ProductResponse;
import vgandolfi.dev.mana_paes.domain.exception.BusinessException;
import vgandolfi.dev.mana_paes.domain.exception.EntityNotFoundException;
import vgandolfi.dev.mana_paes.domain.model.Product;
import vgandolfi.dev.mana_paes.domain.model.ProductOption;
import vgandolfi.dev.mana_paes.domain.model.ProductOptionGroup;
import vgandolfi.dev.mana_paes.domain.model.Tenant;
import vgandolfi.dev.mana_paes.domain.model.Vendor;
import vgandolfi.dev.mana_paes.domain.model.enums.UnitMeasure;
import vgandolfi.dev.mana_paes.domain.repository.ProductRepository;
import vgandolfi.dev.mana_paes.domain.repository.TenantRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;
    @Mock
    private TenantRepository tenantRepository;
    @Mock
    private VendorService vendorService;

    @InjectMocks
    private ProductService productService;

    private final UUID tenantId = UUID.randomUUID();
    private final UUID productId = UUID.randomUUID();

    private Tenant tenant() {
        Tenant tenant = new Tenant();
        tenant.setId(tenantId);
        return tenant;
    }

    private Product product() {
        Product product = new Product();
        product.setId(productId);
        product.setTenant(tenant());
        product.setName("Pão Francês");
        product.setUnitPrice(new BigDecimal("0.50"));
        product.setUnitMeasure(UnitMeasure.UN);
        product.setActive(true);
        return product;
    }

    private ProductRequest request() {
        return new ProductRequest("Pão Francês", "Pãozinho", new BigDecimal("0.50"), UnitMeasure.UN, null);
    }

    @Test
    void createSavesAndReturnsProduct() {
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant()));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        ProductResponse response = productService.create(tenantId, request());

        assertThat(response.name()).isEqualTo("Pão Francês");
        assertThat(response.active()).isTrue();
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void createWithMissingTenantThrows() {
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.create(tenantId, request()))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void getByIdNotFoundThrows() {
        when(productRepository.findByIdAndTenantId(productId, tenantId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getById(tenantId, productId))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void updateNotFoundThrows() {
        when(productRepository.findByIdAndTenantId(productId, tenantId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.update(tenantId, productId, request()))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void setActiveTogglesProduct() {
        Product product = product();
        when(productRepository.findByIdAndTenantId(productId, tenantId)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        ProductResponse response = productService.setActive(tenantId, productId, false);

        assertThat(response.active()).isFalse();
        assertThat(product.isActive()).isFalse();
    }

    @Test
    void listWithActiveOnlyUsesActiveQuery() {
        when(productRepository.findByTenantIdAndActiveTrue(tenantId, PageRequest.of(0, 20)))
                .thenReturn(new PageImpl<>(List.of(product()), PageRequest.of(0, 20), 1));

        Page<ProductResponse> page = productService.list(tenantId, Boolean.TRUE, PageRequest.of(0, 20));

        assertThat(page.getContent()).hasSize(1);
        verify(productRepository).findByTenantIdAndActiveTrue(tenantId, PageRequest.of(0, 20));
    }

    @Test
    void listWithoutFilterUsesTenantQuery() {
        when(productRepository.findByTenantId(tenantId, PageRequest.of(0, 20)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        Page<ProductResponse> page = productService.list(tenantId, null, PageRequest.of(0, 20));

        assertThat(page.getContent()).isEmpty();
    }

    @Test
    void createWithOptionGroupsPersistsGroupsAndAdjustments() {
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant()));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        ProductRequest request = new ProductRequest("Pão de sal", "Padaria", new BigDecimal("0.55"),
                UnitMeasure.UN,
                List.of(new ProductRequest.OptionGroupRequest("Tamanho", false, 0,
                        List.of(new ProductRequest.OptionRequest("Pequeno", BigDecimal.ZERO, 0),
                                new ProductRequest.OptionRequest("Grande", new BigDecimal("0.20"), 1)))));

        ProductResponse response = productService.create(tenantId, request);

        assertThat(response.optionGroups()).hasSize(1);
        assertThat(response.optionGroups().get(0).name()).isEqualTo("Tamanho");
        assertThat(response.optionGroups().get(0).options()).hasSize(2);
        assertThat(response.optionGroups().get(0).options().get(1).priceAdjustment())
                .isEqualByComparingTo("0.20");
        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(captor.capture());
        assertThat(captor.getValue().getOptionGroups()).hasSize(1);
        assertThat(captor.getValue().getOptionGroups().get(0).getOptions()).hasSize(2);
    }

    @Test
    void createWithOptionGroupWithoutOptionsThrows() {
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant()));

        ProductRequest request = new ProductRequest("Pão", "Padaria", new BigDecimal("0.55"),
                UnitMeasure.UN,
                List.of(new ProductRequest.OptionGroupRequest("Recheio", false, 0, List.of())));

        assertThatThrownBy(() -> productService.create(tenantId, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("ao menos uma opção");
    }

    @Test
    void updateReplacesOptionGroups() {
        Product product = product();
        when(productRepository.findByIdAndTenantId(productId, tenantId)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        ProductRequest request = new ProductRequest("Pão", "Padaria", new BigDecimal("0.55"),
                UnitMeasure.UN,
                List.of(new ProductRequest.OptionGroupRequest("Tamanho", true, 0,
                        List.of(new ProductRequest.OptionRequest("Grande", new BigDecimal("0.30"), 0)))));

        ProductResponse response = productService.update(tenantId, productId, request);

        assertThat(response.optionGroups()).hasSize(1);
        assertThat(response.optionGroups().get(0).required()).isTrue();
        assertThat(product.getOptionGroups()).hasSize(1);
        assertThat(product.getOptionGroups().get(0).getOptions()).hasSize(1);
    }

    @Test
    void listActivePublicReturnsActiveProductsWithOptionGroups() {
        Vendor vendor = new Vendor();
        vendor.setId(UUID.randomUUID());
        vendor.setName("Everton");
        vendor.setPhone("5511900000001");
        vendor.setTenant(tenant());
        vendor.setActive(true);
        when(vendorService.resolveActiveVendor("5511900000001")).thenReturn(vendor);

        Product pao = product();
        pao.setId(UUID.randomUUID());
        ProductOption grande = new ProductOption();
        grande.setId(UUID.randomUUID());
        grande.setName("Grande");
        grande.setPriceAdjustment(new BigDecimal("0.20"));
        grande.setActive(true);
        ProductOptionGroup grupo = new ProductOptionGroup();
        grupo.setId(UUID.randomUUID());
        grupo.setName("Tamanho");
        grupo.addOption(grande);
        pao.addOptionGroup(grupo);
        when(productRepository.findByTenantIdAndActiveTrue(tenantId)).thenReturn(List.of(pao));

        List<ProductResponse> result = productService.listActivePublic("5511900000001");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).optionGroups()).hasSize(1);
        assertThat(result.get(0).optionGroups().get(0).name()).isEqualTo("Tamanho");
        assertThat(result.get(0).optionGroups().get(0).options()).hasSize(1);
        verify(productRepository).findByTenantIdAndActiveTrue(tenantId);
    }

    @Test
    void listActivePublicWithUnknownVendorThrows() {
        when(vendorService.resolveActiveVendor("5511999999999"))
                .thenThrow(new BusinessException("Vendedor não cadastrado"));

        assertThatThrownBy(() -> productService.listActivePublic("5511999999999"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Vendedor não cadastrado");
    }

    @Test
    void listActivePublicWithInactiveVendorThrows() {
        when(vendorService.resolveActiveVendor("5511900000001"))
                .thenThrow(new BusinessException("Vendedor inativo"));

        assertThatThrownBy(() -> productService.listActivePublic("5511900000001"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Vendedor inativo");
    }
}