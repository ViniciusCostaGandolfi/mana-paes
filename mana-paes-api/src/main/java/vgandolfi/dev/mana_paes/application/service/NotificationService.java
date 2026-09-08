package vgandolfi.dev.mana_paes.domain.service;

import vgandolfi.dev.mana_paes.application.dto.response.DailyFinancialReportResponse;
import vgandolfi.dev.mana_paes.application.dto.response.DailyProductionReportResponse;
import vgandolfi.dev.mana_paes.application.event.OrderCreatedEvent;
import vgandolfi.dev.mana_paes.domain.model.User;

import java.time.Instant;
import java.util.UUID;

/**
 * Porta de envio de notificações (implementações em infrastructure/notification).
 *
 * <p>Todas as implementações devem ser tolerantes a falhas: em ambientes sem
 * SMTP/Evolution API configurados, apenas registram o {@code NotificationLog}
 * como FAILED (nunca derrubam o fluxo principal — criação de pedido, auth, etc.).</p>
 */
public interface NotificationService {

    /**
     * Envia o e-mail de recuperação de senha. Implementações devem ser
     * tolerantes a falhas: em ambientes sem SMTP, apenas logam (não lançam).
     */
    void sendForgotPasswordEmail(User user, String token, Instant expiryDate);

    /**
     * Orquestra as notificações de um pedido recém-criado (WhatsApp/e-mail para
     * admin e solicitante), respeitando a {@code NotificationConfig} do tenant
     * e persistindo um {@code NotificationLog} por envio. Nunca lança exceções
     * em falhas de canal (Evolution/SMTP): registra FAILED e segue.
     */
    void sendOrderNotifications(OrderCreatedEvent event);

    /**
     * Envia uma mensagem de teste para o {@code adminWhatsappNumber} do tenant.
     * Registra o {@code NotificationLog} com o resultado (SENT/FAILED).
     */
    WhatsAppTestResult sendTestWhatsApp(UUID tenantId);

    /**
     * Envia o relatório diário ao admin (WhatsApp e/ou e-mail conforme a
     * {@code NotificationConfig} e as flags de canal). Registra um
     * {@code NotificationLog} (type DAILY_REPORT) por envio. Nunca lança.
     */
    DailyReportSendResult sendDailyReportNotifications(UUID tenantId,
                                                       DailyProductionReportResponse production,
                                                       DailyFinancialReportResponse financial);

    record WhatsAppTestResult(boolean success, String message) {
    }

    record DailyReportSendResult(boolean whatsappSent, boolean emailSent,
                                 String whatsappMessage, String emailMessage) {
    }
}