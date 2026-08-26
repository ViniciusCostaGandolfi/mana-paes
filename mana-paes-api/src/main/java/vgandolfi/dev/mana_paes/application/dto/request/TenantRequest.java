package vgandolfi.dev.mana_paes.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TenantRequest(
        @NotBlank @Size(max = 150) String name,
        @Size(max = 18) String document,
        @Size(max = 20) String phone,
        @Size(max = 255) String address) {
}