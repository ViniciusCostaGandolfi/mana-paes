package vgandolfi.dev.mana_paes.application.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import vgandolfi.dev.mana_paes.domain.model.enums.UserRole;

/**
 * Criação/atualização de usuário. As regras variam pelo papel do ator:
 * <ul>
 *   <li>{@code SYS_ADMIN}: cria um tenant novo + um {@code ADMIN} — exige
 *       {@code email} e {@code password}.</li>
 *   <li>{@code ADMIN}: cria um {@code MOTOBOY} no próprio tenant — exige pelo
 *       menos {@code email} ou {@code phone}; o motoboy nasce inativo e define
 *       a senha ao aceitar o convite ({@code password} ignorada).</li>
 * </ul>
 */
public record UserRequest(
        @NotBlank @Size(max = 150) String name,
        @Email @Size(max = 150) String email,
        @Size(max = 20) String phone,
        @Size(max = 20) String whatsappNumber,
        @NotNull UserRole role,
        @Size(min = 6, max = 72) String password) {
}