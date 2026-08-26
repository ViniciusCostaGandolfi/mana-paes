package vgandolfi.dev.mana_paes.domain.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vgandolfi.dev.mana_paes.application.dto.request.ProductRequest;
import vgandolfi.dev.mana_paes.application.dto.response.ProductResponse;
import vgandolfi.dev.mana_paes.domain.exception.EntityNotFoundException;
import vgandolfi.dev.mana_paes.domain.model.Product;
import vgandolfi.dev.mana_paes.domain.model.Tenant;
import vgandolfi.dev.mana_paes.domain.repository.ProductRepository;
import vgandolfi.dev.mana_paes.domain.repository.TenantRepository;

import java.util.UUID;

/**
 * CRUD de produtos restrito ao tenant do usuário autenticado (multi-tenant).
 */
@Service
public class ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductService.class);

    private final ProductRepository productRepository;
    private final TenantRepository tenantRepository;

    public ProductService(ProductRepository productRepository, TenantRepository tenantRepository) {
        this.productRepository = productRepository;
        this.tenantRepository = tenantRepository;
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

    private Product findScoped(UUID tenantId, UUID productId) {
        return productRepository.findByIdAndTenantId(productId, tenantId)
                .orElseThrow(() -> EntityNotFoundException.of("Produto", productId));
    }
}