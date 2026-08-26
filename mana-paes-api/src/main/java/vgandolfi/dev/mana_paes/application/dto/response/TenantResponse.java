package vgandolfi.dev.mana_paes.application.dto.response;

import vgandolfi.dev.mana_paes.domain.model.Tenant;

import java.util.UUID;

public record TenantResponse(
        UUID id,
        String name,
        String document,
        String phone,
        String address,
        boolean active) {

    public static TenantResponse from(Tenant tenant) {
        return new TenantResponse(
                tenant.getId(),
                tenant.getName(),
                tenant.getDocument(),
                tenant.getPhone(),
                tenant.getAddress(),
                tenant.isActive());
    }
}