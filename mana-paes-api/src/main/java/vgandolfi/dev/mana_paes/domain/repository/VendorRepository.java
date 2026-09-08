package vgandolfi.dev.mana_paes.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vgandolfi.dev.mana_paes.domain.model.Vendor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VendorRepository extends JpaRepository<Vendor, UUID> {

    List<Vendor> findByTenantId(UUID tenantId);

    List<Vendor> findByTenantIdAndActiveTrue(UUID tenantId);

    Optional<Vendor> findByIdAndTenantId(UUID id, UUID tenantId);

    Optional<Vendor> findByTenantIdAndPhone(UUID tenantId, String phone);

    /**
     * Resolve o vendedor (e, via {@code vendor.tenant}, o tenant) pelo telefone
     * no link público de pedido.
     */
    Optional<Vendor> findFirstByPhoneAndActiveTrue(String phone);

    /**
     * Busca o vendedor pelo telefone, sem filtrar por ativo — permite distinguir
     * "não cadastrado" de "inativo" na resolução pública.
     */
    Optional<Vendor> findFirstByPhone(String phone);
}