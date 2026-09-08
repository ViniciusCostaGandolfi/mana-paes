package vgandolfi.dev.mana_paes.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vgandolfi.dev.mana_paes.application.dto.request.VendorRequest;
import vgandolfi.dev.mana_paes.application.dto.response.VendorResponse;
import vgandolfi.dev.mana_paes.domain.exception.BusinessException;
import vgandolfi.dev.mana_paes.domain.exception.EntityNotFoundException;
import vgandolfi.dev.mana_paes.domain.model.Tenant;
import vgandolfi.dev.mana_paes.domain.model.Vendor;
import vgandolfi.dev.mana_paes.domain.repository.TenantRepository;
import vgandolfi.dev.mana_paes.domain.repository.VendorRepository;

import java.util.List;
import java.util.UUID;

/**
 * CRUD de vendedores restrito ao tenant do usuário autenticado (multi-tenant).
 * Telefone é único por tenant (identificador do link público de pedido).
 */
@Service
public class VendorService {

    private static final Logger log = LoggerFactory.getLogger(VendorService.class);

    private final VendorRepository vendorRepository;
    private final TenantRepository tenantRepository;

    public VendorService(VendorRepository vendorRepository, TenantRepository tenantRepository) {
        this.vendorRepository = vendorRepository;
        this.tenantRepository = tenantRepository;
    }

    @Transactional(readOnly = true)
    public List<VendorResponse> list(UUID tenantId) {
        return vendorRepository.findByTenantId(tenantId).stream().map(VendorResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public VendorResponse getById(UUID tenantId, UUID vendorId) {
        return VendorResponse.from(findScoped(tenantId, vendorId));
    }

    @Transactional
    public VendorResponse create(UUID tenantId, VendorRequest request) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> EntityNotFoundException.of("Tenant", tenantId));
        String phone = request.phone().trim();
        if (vendorRepository.findByTenantIdAndPhone(tenantId, phone).isPresent()) {
            throw new BusinessException("Já existe um vendedor com este telefone neste tenant");
        }

        Vendor vendor = new Vendor();
        vendor.setTenant(tenant);
        vendor.setName(request.name().trim());
        vendor.setPhone(phone);
        vendor.setWhatsappNumber(request.whatsappNumber());
        vendor.setActive(true);
        Vendor saved = vendorRepository.save(vendor);

        log.info("vendor_created vendorId={} tenantId={} phone={}", saved.getId(), tenantId, phone);
        return VendorResponse.from(saved);
    }

    @Transactional
    public VendorResponse update(UUID tenantId, UUID vendorId, VendorRequest request) {
        Vendor vendor = findScoped(tenantId, vendorId);
        String phone = request.phone().trim();
        vendorRepository.findByTenantIdAndPhone(tenantId, phone)
                .filter(existing -> !existing.getId().equals(vendorId))
                .ifPresent(existing -> {
                    throw new BusinessException("Já existe um vendedor com este telefone neste tenant");
                });
        vendor.setName(request.name().trim());
        vendor.setPhone(phone);
        vendor.setWhatsappNumber(request.whatsappNumber());
        Vendor saved = vendorRepository.save(vendor);

        log.info("vendor_updated vendorId={} tenantId={}", vendorId, tenantId);
        return VendorResponse.from(saved);
    }

    @Transactional
    public VendorResponse setActive(UUID tenantId, UUID vendorId, boolean active) {
        Vendor vendor = findScoped(tenantId, vendorId);
        vendor.setActive(active);
        Vendor saved = vendorRepository.save(vendor);

        log.info("vendor_status_changed vendorId={} tenantId={} active={}", vendorId, tenantId, active);
        return VendorResponse.from(saved);
    }

    private Vendor findScoped(UUID tenantId, UUID vendorId) {
        return vendorRepository.findByIdAndTenantId(vendorId, tenantId)
                .orElseThrow(() -> EntityNotFoundException.of("Vendedor", vendorId));
    }

    /**
     * Resolve o vendedor ATIVO pelo telefone (link público), distinguindo
     * "não cadastrado" de "inativo". Compartilhado pelos fluxos públicos
     * (catálogo e pedido via link mágico).
     */
    @Transactional(readOnly = true)
    public Vendor resolveActiveVendor(String phone) {
        String normalized = phone == null ? null : phone.trim();
        Vendor vendor = vendorRepository.findFirstByPhone(normalized)
                .orElseThrow(() -> new BusinessException("Vendedor não cadastrado"));
        if (!vendor.isActive()) {
            throw new BusinessException("Vendedor inativo");
        }
        return vendor;
    }
}