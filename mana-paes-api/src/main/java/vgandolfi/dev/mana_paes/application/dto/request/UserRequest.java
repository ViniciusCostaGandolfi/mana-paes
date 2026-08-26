package vgandolfi.dev.mana_paes.application.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import vgandolfi.dev.mana_paes.domain.model.enums.UserRole;

public record UserRequest(
        @NotBlank @Size(max = 150) String name,
        @NotBlank @Email @Size(max = 150) String email,
        @Size(max = 20) String phone,
        @Size(max = 20) String whatsappNumber,
        @NotNull UserRole role) {
}