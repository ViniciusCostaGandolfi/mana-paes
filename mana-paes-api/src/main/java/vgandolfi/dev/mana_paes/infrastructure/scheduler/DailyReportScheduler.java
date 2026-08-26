package vgandolfi.dev.mana_paes.infrastructure.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import vgandolfi.dev.mana_paes.domain.model.NotificationConfig;
import vgandolfi.dev.mana_paes.domain.model.Tenant;
import vgandolfi.dev.mana_paes.domain.repository.NotificationConfigRepository;
import vgandolfi.dev.mana_paes.domain.repository.TenantRepository;
import vgandolfi.dev.mana_paes.domain.service.DailyReportDispatcher;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Agendador do relatório diário (Fase 5): roda de hora em hora e, para cada
 * tenant ativo, dispara o relatório quando {@code dailyReportTime} foi
 * igualado/ultrapassado. A idempotência (relatório já enviado no dia) é
 * garantida pelo {@link DailyReportDispatcher}.
 *
 * <p>Condicional a {@code app.scheduler.enabled=true} (default em dev/test:
 * {@code false}) — em testes o bean não existe e nada dispara.</p>
 */
@Component
@ConditionalOnProperty(name = "app.scheduler.enabled", havingValue = "true")
public class DailyReportScheduler {

    private static final Logger log = LoggerFactory.getLogger(DailyReportScheduler.class);

    private final TenantRepository tenantRepository;
    private final NotificationConfigRepository configRepository;
    private final DailyReportDispatcher dispatcher;

    public DailyReportScheduler(TenantRepository tenantRepository,
                                NotificationConfigRepository configRepository,
                                DailyReportDispatcher dispatcher) {
        this.tenantRepository = tenantRepository;
        this.configRepository = configRepository;
        this.dispatcher = dispatcher;
    }

    @Scheduled(cron = "0 0 * * * *")
    public void runHourly() {
        runAt(LocalDate.now(), LocalTime.now());
    }

    /**
     * Núcleo do disparo, separado para permitir testes determinísticos
     * (injeção de data/hora).
     */
    void runAt(LocalDate date, LocalTime now) {
        log.info("daily_report_scheduler_run at={}", now);

        List<Tenant> tenants = tenantRepository.findByActiveTrue();
        for (Tenant tenant : tenants) {
            NotificationConfig config = configRepository.findByTenantId(tenant.getId()).orElse(null);
            if (config == null || config.getDailyReportTime() == null) {
                continue;
            }
            if (now.isBefore(config.getDailyReportTime())) {
                continue;
            }
            try {
                dispatcher.dispatchForTenant(tenant.getId(), date);
            } catch (Exception ex) {
                // falha de um tenant não impede os demais
                log.error("daily_report_dispatch_failed tenantId={} date={}", tenant.getId(), date, ex);
            }
        }
    }
}