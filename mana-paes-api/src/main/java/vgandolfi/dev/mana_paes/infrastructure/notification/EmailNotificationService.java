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
import vgandolfi.dev.mana_paes.infrastructure.config.AppProperties;
import vgandolfi.dev.mana_paes.domain.model.User;

import java.time.Instant;

/**
 * Envio de e-mails HTML via JavaMailSender + templates Thymeleaf.
 *
 * <p>No modelo atual o e-mail é usado APENAS para recuperação de senha — todas
 * as demais notificações (pedidos, lembretes, resumo diário) são WhatsApp.
 * Em dev/test o flag {@code app.mail.enabled} (default {@code false}) faz o
 * serviço apenas logar (não lança); em produção o envio é real.</p>
 */
@Component
public class EmailNotificationService {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationService.class);

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
}