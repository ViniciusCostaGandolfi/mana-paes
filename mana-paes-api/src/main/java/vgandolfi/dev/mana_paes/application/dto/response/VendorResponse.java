package vgandolfi.dev.mana_paes.application.dto.response;

import vgandolfi.dev.mana_paes.domain.model.Vendor;

import java.util.UUID;

public record VendorResponse(
        UUID id,
        String name,
        String phone,
        String whatsappNumber,
        boolean active,
        UUID tenantId) {

    public static VendorResponse from(Vendor vendor) {
        return new VendorResponse(
                vendor.getId(),
                vendor.getName(),
                vendor.getPhone(),
                vendor.getWhatsappNumber(),
                vendor.isActive(),
                vendor.getTenant().getId());
    }
}