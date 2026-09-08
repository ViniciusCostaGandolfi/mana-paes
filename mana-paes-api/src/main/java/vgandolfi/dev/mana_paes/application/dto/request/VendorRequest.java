package vgandolfi.dev.mana_paes.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Criação/atualização de vendedor (sem login). O {@code phone} é o
 * identificador do link público de pedido e o destino das notificações
 * WhatsApp quando {@code whatsappNumber} não for preenchido.
 */
public record VendorRequest(
        @NotBlank @Size(max = 150) String name,
        @NotBlank @Size(max = 20) String phone,
        @Size(max = 20) String whatsappNumber) {
}