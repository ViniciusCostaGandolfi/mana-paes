package vgandolfi.dev.mana_paes.infrastructure.notification;

import jakarta.mail.MessagingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import vgandolfi.dev.mana_paes.application.dto.response.DailyFinancialReportResponse;
import vgandolfi.dev.mana_paes.application.dto.response.DailyProductionReportResponse;
import vgandolfi.dev.mana_paes.config.AppProperties;
import vgandolfi.dev.mana_paes.domain.model.User;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Envio de e-mails HTML via JavaMailSender + templates Thymeleaf.
 *
 * <p>Em dev/test o flag {@code app.mail.enabled} (default {@code false}) faz o
 * serviço apenas logar e retornar {@code false} (não lança); em produção o envio
 * é real. Falhas de SMTP nunca derrubam o fluxo.</p>
 */
@Component
public class EmailNotificationService {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationService.class);

    private static final DecimalFormat MONEY = new DecimalFormat(
            "#,##0.00", DecimalFormatSymbols.getInstance(new Locale("pt", "BR")));

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;
    private final AppProperties appProperties;

    public EmailNotificationService(JavaMailSender mailSender, SpringTemplateEngine templateEngine,
                                    AppProperties appProperties) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
        this.appProperties = appProperties;
    }

    /**
     * E-mail de recuperação de senha. Tolerante a falhas (apenas loga em dev).
     */
    public void sendForgotPasswordEmail(User user, String token, Instant expiryDate) {
        Context context = new Context();
        context.setVariable("name", user.getName());
        context.setVariable("resetLink", appProperties.frontend().url() + "/reset-password?token=" + token);
        context.setVariable("expiryDate", expiryDate);
        sendHtml(user.getEmail(), "Recuperação de senha — Mana Paes", "email/forgot-password", context);
    }

    /**
     * Confirmação de pedido ao solicitante. Retorna {@code true} quando o envio
     * foi efetivado (ou {@code false} se desabilitado/falhou).
     *
     * @param items lista de mapas com chaves productName, quantity, unitMeasure
     */
    public boolean sendOrderConfirmation(String to, String requesterName, UUID orderId,
                                         LocalDate deliveryDate, BigDecimal totalAmount,
                                         List<Map<String, Object>> items) {
        Context context = new Context();
        context.setVariable("requesterName", requesterName);
        context.setVariable("orderId", shortId(orderId));
        context.setVariable("deliveryDate", deliveryDate);
        context.setVariable("totalAmount", MONEY.format(totalAmount));
        context.setVariable("items", items);
        return sendHtml(to, "Pedido confirmado — Mana Paes", "email/order-confirmation", context);
    }

    /**
     * Alerta de novo pedido ao admin. Retorna {@code true} quando o envio foi
     * efetivado (ou {@code false} se desabilitado/falhou).
     *
     * @param items lista de mapas com chaves productName, quantity, unitMeasure
     */
    public boolean sendNewOrderAlert(String to, String tenantName, String requesterName, UUID orderId,
                                     LocalDate deliveryDate, BigDecimal totalAmount,
                                     List<Map<String, Object>> items) {
        Context context = new Context();
        context.setVariable("tenantName", tenantName);
        context.setVariable("requesterName", requesterName);
        context.setVariable("orderId", shortId(orderId));
        context.setVariable("deliveryDate", deliveryDate);
        context.setVariable("totalAmount", MONEY.format(totalAmount));
        context.setVariable("items", items);
        return sendHtml(to, "Novo pedido recebido — Mana Paes", "email/new-order-alert", context);
    }

    /**
     * Relatório diário ao admin. Retorna {@code true} quando o envio foi
     * efetivado (ou {@code false} se desabilitado/falhou).
     */
    public boolean sendDailyReport(String to, DailyProductionReportResponse production,
                                   DailyFinancialReportResponse financial) {
        Context context = new Context();
        context.setVariable("reportDate", production.date());
        context.setVariable("totalOrders", financial.totalOrders());
        context.setVariable("totalAmount", MONEY.format(financial.totalAmount()));
        context.setVariable("items", production.items().stream()
                .map(item -> Map.<String, Object>of(
                        "productName", item.productName(),
                        "quantity", item.totalQuantity().stripTrailingZeros().toPlainString(),
                        "unitMeasure", item.unitMeasure().name()))
                .toList());
        return sendHtml(to, "Relatório diário — Mana Paes", "email/daily-report", context);
    }

    private boolean sendHtml(String to, String subject, String template, Context context) {
        if (!appProperties.mail().enabled()) {
            log.info("skip_email template={} recipient={} reason=mail_disabled", template, to);
            return false;
        }
        try {
            String html = templateEngine.process(template, context);
            var mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(mimeMessage);
            log.info("email_sent template={} recipient={}", template, to);
            return true;
        } catch (MailException | MessagingException ex) {
            log.error("email_send_failed template={} recipient={}", template, to, ex);
            return false;
        }
    }

    private String shortId(UUID id) {
        return id.toString().substring(0, 8);
    }
}