package vgandolfi.dev.mana_paes.infrastructure.notification;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.spring6.SpringTemplateEngine;
import vgandolfi.dev.mana_paes.infrastructure.config.AppProperties;
import vgandolfi.dev.mana_paes.domain.model.User;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * O e-mail é usado apenas para recuperação de senha no modelo atual.
 */
class EmailNotificationServiceTest {

    private AppProperties appProperties(boolean mailEnabled) {
        return new AppProperties(
                new AppProperties.Jwt("test-secret-test-secret-test-secret-test-secret-1234", 3600000L, 86400000L),
                new AppProperties.Encryption("mana-paes-test-master-key-32chars!"),
                new AppProperties.Evolution("", "", 0L, "mana-paes", false),
                new AppProperties.Backend("http://localhost:8080"),
                new AppProperties.Frontend("http://localhost"),
                new AppProperties.Mail(mailEnabled),
                new AppProperties.Notifications(false, 2),
                new AppProperties.Scheduler(false),
                new AppProperties.SystemAdmin("vinicius@vgandolfi.dev", "admin123"));
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

    private User user() {
        User user = new User();
        user.setName("João");
        user.setEmail("joao@example.com");
        return user;
    }

    @Test
    void mailDisabledReturnsFalseWithoutSending() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        EmailNotificationService service = new EmailNotificationService(mailSender, null, appProperties(false));

        service.sendForgotPasswordEmail(user(), "token", Instant.now().plusSeconds(3600));

        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void mailEnabledRendersTemplateAndSends() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage((Session) null));
        EmailNotificationService service = new EmailNotificationService(mailSender, templateEngine(), appProperties(true));

        service.sendForgotPasswordEmail(user(), "token-abc", Instant.now().plusSeconds(3600));

        verify(mailSender).send(any(MimeMessage.class));
    }
}