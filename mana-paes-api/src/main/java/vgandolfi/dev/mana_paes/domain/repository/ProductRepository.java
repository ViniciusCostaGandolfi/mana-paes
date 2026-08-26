package vgandolfi.dev.mana_paes.domain.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import vgandolfi.dev.mana_paes.domain.model.Product;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {

    List<Product> findByTenantIdAndActiveTrue(UUID tenantId);

    Page<Product> findByTenantIdAndActiveTrue(UUID tenantId, Pageable pageable);

    Page<Product> findByTenantId(UUID tenantId, Pageable pageable);

    Optional<Product> findByIdAndTenantId(UUID id, UUID tenantId);
}