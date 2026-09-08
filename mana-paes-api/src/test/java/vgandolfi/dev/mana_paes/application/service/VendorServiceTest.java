package vgandolfi.dev.mana_paes.application.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vgandolfi.dev.mana_paes.application.dto.request.VendorRequest;
import vgandolfi.dev.mana_paes.application.dto.response.VendorResponse;
import vgandolfi.dev.mana_paes.domain.exception.BusinessException;
import vgandolfi.dev.mana_paes.domain.exception.EntityNotFoundException;
import vgandolfi.dev.mana_paes.domain.model.Tenant;
import vgandolfi.dev.mana_paes.domain.model.Vendor;
import vgandolfi.dev.mana_paes.domain.repository.TenantRepository;
import vgandolfi.dev.mana_paes.domain.repository.VendorRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VendorServiceTest {

    @Mock
    private VendorRepository vendorRepository;
    @Mock
    private TenantRepository tenantRepository;

    @InjectMocks
    private VendorService vendorService;

    private final UUID tenantId = UUID.randomUUID();
    private final UUID vendorId = UUID.randomUUID();

    private Tenant tenant() {
        Tenant tenant = new Tenant();
        tenant.setId(tenantId);
        return tenant;
    }

    private Vendor vendor() {
        Vendor vendor = new Vendor();
        vendor.setId(vendorId);
        vendor.setTenant(tenant());
        vendor.setName("Everton");
        vendor.setPhone("5511900000001");
        vendor.setActive(true);
        return vendor;
    }

    private VendorRequest request(String name, String phone) {
        return new VendorRequest(name, phone, null);
    }

    @Test
    void createSavesVendor() {
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant()));
        when(vendorRepository.findByTenantIdAndPhone(tenantId, "5511900000001")).thenReturn(Optional.empty());
        when(vendorRepository.save(any(Vendor.class))).thenAnswer(inv -> inv.getArgument(0));

        VendorResponse response = vendorService.create(tenantId, request("Everton", "5511900000001"));

        assertThat(response.name()).isEqualTo("Everton");
        assertThat(response.phone()).isEqualTo("5511900000001");
        assertThat(response.tenantId()).isEqualTo(tenantId);
        assertThat(response.active()).isTrue();
    }

    @Test
    void createWithDuplicatePhoneThrows() {
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant()));
        when(vendorRepository.findByTenantIdAndPhone(tenantId, "5511900000001"))
                .thenReturn(Optional.of(vendor()));

        assertThatThrownBy(() -> vendorService.create(tenantId, request("Everton", "5511900000001")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("telefone");
    }

    @Test
    void listReturnsTenantScopedVendors() {
        when(vendorRepository.findByTenantId(tenantId)).thenReturn(List.of(vendor()));

        List<VendorResponse> result = vendorService.list(tenantId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(vendorId);
    }

    @Test
    void getByIdFromAnotherTenantThrows() {
        when(vendorRepository.findByIdAndTenantId(vendorId, tenantId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> vendorService.getById(tenantId, vendorId))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void updateModifiesVendor() {
        Vendor existing = vendor();
        when(vendorRepository.findByIdAndTenantId(vendorId, tenantId)).thenReturn(Optional.of(existing));
        when(vendorRepository.findByTenantIdAndPhone(tenantId, "5511900000002")).thenReturn(Optional.empty());
        when(vendorRepository.save(any(Vendor.class))).thenAnswer(inv -> inv.getArgument(0));

        VendorResponse response = vendorService.update(tenantId, vendorId, request("Everton", "5511900000002"));

        assertThat(response.phone()).isEqualTo("5511900000002");
        verify(vendorRepository).save(existing);
    }

    @Test
    void setActiveTogglesVendor() {
        Vendor existing = vendor();
        when(vendorRepository.findByIdAndTenantId(vendorId, tenantId)).thenReturn(Optional.of(existing));
        when(vendorRepository.save(any(Vendor.class))).thenAnswer(inv -> inv.getArgument(0));

        VendorResponse response = vendorService.setActive(tenantId, vendorId, false);

        assertThat(response.active()).isFalse();
    }
}