package vgandolfi.dev.mana_paes.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vgandolfi.dev.mana_paes.application.dto.request.ProductRequest;
import vgandolfi.dev.mana_paes.application.dto.response.ProductResponse;
import vgandolfi.dev.mana_paes.domain.exception.BusinessException;
import vgandolfi.dev.mana_paes.domain.exception.EntityNotFoundException;
import vgandolfi.dev.mana_paes.domain.model.Product;
import vgandolfi.dev.mana_paes.domain.model.ProductOption;
import vgandolfi.dev.mana_paes.domain.model.ProductOptionGroup;
import vgandolfi.dev.mana_paes.domain.model.Tenant;
import vgandolfi.dev.mana_paes.domain.repository.ProductRepository;
import vgandolfi.dev.mana_paes.domain.repository.TenantRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * CRUD de produtos restrito ao tenant do usuário autenticado (multi-tenant),
 * incluindo grupos de opções (com preço adicional por opção).
 */
@Service
public class ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductService.class);

    private final ProductRepository productRepository;
    private final TenantRepository tenantRepository;
    private final VendorService vendorService;

    public ProductService(ProductRepository productRepository, TenantRepository tenantRepository,
                          VendorService vendorService) {
        this.productRepository = productRepository;
        this.tenantRepository = tenantRepository;
        this.vendorService = vendorService;
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> list(UUID tenantId, Boolean activeOnly, Pageable pageable) {
        Page<Product> page = Boolean.TRUE.equals(activeOnly)
                ? productRepository.findByTenantIdAndActiveTrue(tenantId, pageable)
                : productRepository.findByTenantId(tenantId, pageable);
        return page.map(ProductResponse::from);
    }

    @Transactional(readOnly = true)
    public ProductResponse getById(UUID tenantId, UUID productId) {
        return ProductResponse.from(findScoped(tenantId, productId));
    }

    /**
     * Catálogo público da página {@code /pedido}: produtos ATIVOS do tenant do
     * vendedor (resolvido pelo telefone do link mágico), com grupos de opções.
     */
    @Transactional(readOnly = true)
    public List<ProductResponse> listActivePublic(String vendorPhone) {
        UUID tenantId = vendorService.resolveActiveVendor(vendorPhone).getTenant().getId();
        return productRepository.findByTenantIdAndActiveTrue(tenantId)
                .stream()
                .map(ProductResponse::from)
                .toList();
    }

    @Transactional
    public ProductResponse create(UUID tenantId, ProductRequest request) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> EntityNotFoundException.of("Tenant", tenantId));

        Product product = new Product();
        product.setTenant(tenant);
        product.setName(request.name());
        product.setDescription(request.description());
        product.setUnitPrice(request.unitPrice());
        product.setUnitMeasure(request.unitMeasure());
        product.setActive(true);
        applyOptionGroups(product, request.optionGroups());
        Product saved = productRepository.save(product);

        log.info("product_created productId={} tenantId={}", saved.getId(), tenantId);
        return ProductResponse.from(saved);
    }

    @Transactional
    public ProductResponse update(UUID tenantId, UUID productId, ProductRequest request) {
        Product product = findScoped(tenantId, productId);
        product.setName(request.name());
        product.setDescription(request.description());
        product.setUnitPrice(request.unitPrice());
        product.setUnitMeasure(request.unitMeasure());
        if (request.optionGroups() != null) {
            product.getOptionGroups().clear();
            applyOptionGroups(product, request.optionGroups());
        }
        Product saved = productRepository.save(product);

        log.info("product_updated productId={} tenantId={}", productId, tenantId);
        return ProductResponse.from(saved);
    }

    @Transactional
    public ProductResponse setActive(UUID tenantId, UUID productId, boolean active) {
        Product product = findScoped(tenantId, productId);
        product.setActive(active);
        Product saved = productRepository.save(product);

        log.info("product_status_changed productId={} tenantId={} active={}", productId, tenantId, active);
        return ProductResponse.from(saved);
    }

    /**
     * Substitui os grupos de opções do produto (criação ou atualização).
     * Cada grupo exige ao menos uma opção; {@code priceAdjustment} default 0.
     */
    private void applyOptionGroups(Product product, List<ProductRequest.OptionGroupRequest> groups) {
        if (groups == null) {
            return;
        }
        for (ProductRequest.OptionGroupRequest groupReq : groups) {
            if (groupReq.options() == null || groupReq.options().isEmpty()) {
                throw new BusinessException("Grupo " + groupReq.name() + " precisa de ao menos uma opção");
            }
            ProductOptionGroup group = new ProductOptionGroup();
            group.setName(groupReq.name().trim());
            group.setRequired(groupReq.required() != null && groupReq.required());
            group.setSortOrder(groupReq.sortOrder() == null ? 0 : groupReq.sortOrder());
            for (ProductRequest.OptionRequest optionReq : groupReq.options()) {
                ProductOption option = new ProductOption();
                option.setName(optionReq.name().trim());
                option.setPriceAdjustment(optionReq.priceAdjustment() == null ? BigDecimal.ZERO : optionReq.priceAdjustment());
                option.setSortOrder(optionReq.sortOrder() == null ? 0 : optionReq.sortOrder());
                group.addOption(option);
            }
            product.addOptionGroup(group);
        }
    }

    private Product findScoped(UUID tenantId, UUID productId) {
        return productRepository.findByIdAndTenantId(productId, tenantId)
                .orElseThrow(() -> EntityNotFoundException.of("Produto", productId));
    }
}