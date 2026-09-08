package vgandolfi.dev.mana_paes.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Habilita o agendamento ({@code @Scheduled}). A infraestrutura em si é
 * inofensiva em dev/test (nenhum método {@code @Scheduled} existe com o bean
 * condicional desabilitado); o agendador real (
 * {@code DailyReportScheduler}) só é criado com
 * {@code app.scheduler.enabled=true}.
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}