package vgandolfi.dev.mana_paes.application.dto.response;

import java.time.LocalDate;

/**
 * Resumo do disparo do relatório diário (manual ou via scheduler).
 *
 * @param dispatched     {@code false} quando o relatório da data já havia sido
 *                       enviado (idempotência)
 * @param whatsappSent   resultado do envio por WhatsApp (admin)
 * @param emailSent      resultado do envio por e-mail (admin)
 */
public record DailyReportDispatchResponse(
        LocalDate date,
        boolean dispatched,
        boolean whatsappSent,
        boolean emailSent,
        String whatsappMessage,
        String emailMessage) {
}