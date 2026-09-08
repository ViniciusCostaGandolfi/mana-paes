package vgandolfi.dev.mana_paes.application.service;

import vgandolfi.dev.mana_paes.application.dto.response.DailyFinancialReportResponse;
import vgandolfi.dev.mana_paes.application.dto.response.DailyProductionReportResponse;
import vgandolfi.dev.mana_paes.application.event.OrderCreatedEvent;
import vgandolfi.dev.mana_paes.domain.model.ScheduledReminder;
import vgandolfi.dev.mana_paes.domain.model.User;
import vgandolfi.dev.mana_paes.domain.model.Vendor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
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
     * Orquestra as notificações de um pedido recém-criado (WhatsApp para admin
     * e vendedor), respeitando a {@code NotificationConfig} do tenant e
     * persistindo um {@code NotificationLog} por envio. Nunca lança exceções
     * em falhas de canal (Evolution): registra FAILED e segue.
     */
    void sendOrderNotifications(OrderCreatedEvent event);

    /**
     * Envia uma mensagem de teste para o {@code adminWhatsappNumber} do tenant.
     * Registra o {@code NotificationLog} com o resultado (SENT/FAILED).
     */
    WhatsAppTestResult sendTestWhatsApp(UUID tenantId);

    /**
     * Envia o relatório diário ao admin por WhatsApp (apenas), com o resumo por
     * turno. Registra um {@code NotificationLog} (type DAILY_REPORT) por envio.
     * Nunca lança.
     */
    DailyReportSendResult sendDailyReportNotifications(UUID tenantId,
                                                       DailyProductionReportResponse production,
                                                       DailyFinancialReportResponse financial);

    /**
     * Envia um lembrete (WhatsApp) para cada vendedor ativo do tenant, com o
     * link público de pedido para a {@code deliveryDate} informada. Registra um
     * {@code NotificationLog} (type REMINDER) por envio com retry simples
     * ({@code app.notifications.max-retries}); falhas de canal nunca derrubam.
     */
    VendorReminderResult sendVendorReminder(ScheduledReminder reminder, List<Vendor> vendors, LocalDate deliveryDate);

    record WhatsAppTestResult(boolean success, String message) {
    }

    record DailyReportSendResult(boolean whatsappSent, boolean emailSent,
                                 String whatsappMessage, String emailMessage) {
    }

    record VendorReminderResult(int recipients, int whatsappSent) {
    }
}