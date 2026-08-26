package vgandolfi.dev.mana_paes.domain.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vgandolfi.dev.mana_paes.application.dto.request.TenantRequest;
import vgandolfi.dev.mana_paes.application.dto.response.TenantResponse;
import vgandolfi.dev.mana_paes.domain.exception.EntityNotFoundException;
import vgandolfi.dev.mana_paes.domain.model.Tenant;
import vgandolfi.dev.mana_paes.domain.repository.TenantRepository;

import java.util.UUID;

/**
 * Dados do tenant (padaria) do usuário autenticado.
 */
@Service
public class TenantService {

    private static final Logger log = LoggerFactory.getLogger(TenantService.class);

    private final TenantRepository tenantRepository;

    public TenantService(TenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
    }

    @Transactional(readOnly = true)
    public TenantResponse getById(UUID tenantId) {
        return TenantResponse.from(find(tenantId));
    }

    @Transactional
    public TenantResponse update(UUID tenantId, TenantRequest request) {
        Tenant tenant = find(tenantId);
        tenant.setName(request.name());
        if (request.document() != null) {
            tenant.setDocument(request.document());
        }
        if (request.phone() != null) {
            tenant.setPhone(request.phone());
        }
        if (request.address() != null) {
            tenant.setAddress(request.address());
        }
        Tenant saved = tenantRepository.save(tenant);

        log.info("tenant_updated tenantId={}", tenantId);
        return TenantResponse.from(saved);
    }

    private Tenant find(UUID tenantId) {
        return tenantRepository.findById(tenantId)
                .orElseThrow(() -> EntityNotFoundException.of("Tenant", tenantId));
    }
}