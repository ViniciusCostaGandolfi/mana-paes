package vgandolfi.dev.mana_paes.infrastructure.notification;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.spring6.SpringTemplateEngine;
import vgandolfi.dev.mana_paes.config.AppProperties;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EmailNotificationServiceTest {

    private AppProperties appProperties(boolean mailEnabled) {
        return new AppProperties(
                new AppProperties.Jwt("test-secret-test-secret-test-secret-test-secret-1234", 3600000L, 86400000L),
                new AppProperties.Encryption("mana-paes-test-master-key-32chars!"),
                new AppProperties.Evolution("", "", 0L),
                new AppProperties.Backend("http://localhost:8080"),
                new AppProperties.Frontend("http://localhost"),
                new AppProperties.Mail(mailEnabled),
                new AppProperties.Notifications(false, 2),
                new AppProperties.Scheduler(false));
    }

    private SpringTemplateEngine templateEngine() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(org.thymeleaf.templatemode.TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");
        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
        return engine;
    }

    @Test
    void mailDisabledReturnsFalseWithoutSending() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        EmailNotificationService service = new EmailNotificationService(mailSender, null, appProperties(false));

        boolean sent = service.sendOrderConfirmation("to@example.com", "João", UUID.randomUUID(),
                LocalDate.now(), new BigDecimal("55.00"), List.of(Map.of(
                        "productName", "Pão", "quantity", "10", "unitMeasure", "UN")));

        assertThat(sent).isFalse();
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void mailEnabledRendersTemplateAndSends() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage((Session) null));
        EmailNotificationService service = new EmailNotificationService(mailSender, templateEngine(), appProperties(true));

        boolean sent = service.sendOrderConfirmation("to@example.com", "João", UUID.randomUUID(),
                LocalDate.now(), new BigDecimal("55.00"), List.of(Map.of(
                        "productName", "Pão", "quantity", "10", "unitMeasure", "UN")));

        assertThat(sent).isTrue();
        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void sendDailyReportMailEnabled() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage((Session) null));
        EmailNotificationService service = new EmailNotificationService(mailSender, templateEngine(), appProperties(true));

        vgandolfi.dev.mana_paes.application.dto.response.DailyProductionReportResponse production =
                new vgandolfi.dev.mana_paes.application.dto.response.DailyProductionReportResponse(LocalDate.now(),
                        List.of(new vgandolfi.dev.mana_paes.application.dto.response.DailyReportItemResponse(
                                UUID.randomUUID(), "Pão",
                                vgandolfi.dev.mana_paes.domain.model.enums.UnitMeasure.UN,
                                new BigDecimal("10"), new BigDecimal("5.00"))),
                        new BigDecimal("5.00"));
        vgandolfi.dev.mana_paes.application.dto.response.DailyFinancialReportResponse financial =
                new vgandolfi.dev.mana_paes.application.dto.response.DailyFinancialReportResponse(
                        LocalDate.now(), new BigDecimal("5.00"), 1);

        boolean sent = service.sendDailyReport("admin@example.com", production, financial);

        assertThat(sent).isTrue();
        verify(mailSender).send(any(MimeMessage.class));
    }
}