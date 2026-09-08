package vgandolfi.dev.mana_paes.domain.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vgandolfi.dev.mana_paes.application.dto.response.DailyFinancialReportResponse;
import vgandolfi.dev.mana_paes.application.dto.response.DailyProductionReportResponse;
import vgandolfi.dev.mana_paes.application.dto.response.DailyReportDispatchResponse;
import vgandolfi.dev.mana_paes.application.dto.response.DailyReportItemResponse;
import vgandolfi.dev.mana_paes.domain.model.DailyReport;
import vgandolfi.dev.mana_paes.domain.model.DailyReportItem;
import vgandolfi.dev.mana_paes.domain.model.NotificationConfig;
import vgandolfi.dev.mana_paes.domain.model.Tenant;
import vgandolfi.dev.mana_paes.domain.repository.DailyReportRepository;
import vgandolfi.dev.mana_paes.domain.repository.NotificationConfigRepository;
import vgandolfi.dev.mana_paes.domain.repository.ProductRepository;
import vgandolfi.dev.mana_paes.domain.repository.TenantRepository;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Dispara o relatório diário de um tenant (geração + persistência em
 * {@code DailyReport} + envio de notificações ao admin). Compartilhado entre o
 * scheduler ({@code DailyReportScheduler}) e o endpoint manual
 * ({@code POST /api/v1/notifications/reports/daily/send}).
 *
 * <p>Idempotência: se já existir um {@code DailyReport} com {@code sent=true}
 * para a data, o disparo é ignorado (não reenvia no mesmo dia). Se existir com
 * {@code sent=false} (disparo anterior incompleto), os itens são regenerados e
 * o envio é concluído.</p>
 */
@Service
public class DailyReportDispatcher {

    private static final Logger log = LoggerFactory.getLogger(DailyReportDispatcher.class);

    private final TenantRepository tenantRepository;
    private final NotificationConfigRepository configRepository;
    private final DailyReportRepository dailyReportRepository;
    private final ProductRepository productRepository;
    private final ReportService reportService;
    private final NotificationService notificationService;

    public DailyReportDispatcher(TenantRepository tenantRepository,
                                 NotificationConfigRepository configRepository,
                                 DailyReportRepository dailyReportRepository,
                                 ProductRepository productRepository,
                                 ReportService reportService,
                                 NotificationService notificationService) {
        this.tenantRepository = tenantRepository;
        this.configRepository = configRepository;
        this.dailyReportRepository = dailyReportRepository;
        this.productRepository = productRepository;
        this.reportService = reportService;
        this.notificationService = notificationService;
    }

    /**
     * Dispara (ou confirma idempotência) do relatório diário de um tenant.
     */
    @Transactional
    public DailyReportDispatchResponse dispatchForTenant(UUID tenantId, LocalDate date) {
        DailyReport existing = dailyReportRepository.findByTenantIdAndReportDate(tenantId, date).orElse(null);
        if (existing != null && existing.isSent()) {
            log.info("daily_report_already_sent tenantId={} date={}", tenantId, date);
            return new DailyReportDispatchResponse(date, false, false, false,
                    "relatório já enviado", "relatório já enviado");
        }

        DailyFinancialReportResponse financial = reportService.dailyFinancial(tenantId, date);
        DailyProductionReportResponse production = reportService.dailyProduction(tenantId, date);

        DailyReport report = existing != null ? existing : new DailyReport();
        report.getItems().clear();
        for (DailyReportItemResponse itemResp : production.items()) {
            DailyReportItem item = new DailyReportItem();
            item.setProduct(productRepository.getReferenceById(itemResp.productId()));
            item.setTotalQuantity(itemResp.totalQuantity());
            item.setTotalAmount(itemResp.totalAmount());
            report.addItem(item);
        }
        report.setTenant(tenantRepository.getReferenceById(tenantId));
        report.setReportDate(date);
        report.setTotalAmount(financial.totalAmount());
        report.setTotalOrders(financial.totalOrders());
        report.setGeneratedAt(Instant.now());
        report.setSent(false);
        dailyReportRepository.save(report);

        NotificationService.DailyReportSendResult sendResult =
                notificationService.sendDailyReportNotifications(tenantId, production, financial);

        report.setSent(true);
        dailyReportRepository.save(report);

        log.info("daily_report_dispatched tenantId={} date={} totalOrders={} totalAmount={} whatsapp={} email={}",
                tenantId, date, financial.totalOrders(), financial.totalAmount(),
                sendResult.whatsappSent(), sendResult.emailSent());
        return new DailyReportDispatchResponse(date, true,
                sendResult.whatsappSent(), sendResult.emailSent(),
                sendResult.whatsappMessage(), sendResult.emailMessage());
    }

    /**
     * Para o scheduler: dispara o relatório de hoje de todos os tenants ativos
     * cujo {@code dailyReportTime} já foi igualado/ultrapassado em {@code now}.
     */
    @Transactional
    public List<DailyReportDispatchResponse> dispatchDueTenants(LocalDate date, LocalTime now) {
        List<DailyReportDispatchResponse> results = new ArrayList<>();
        for (Tenant tenant : tenantRepository.findByActiveTrue()) {
            NotificationConfig config = configRepository.findByTenantId(tenant.getId()).orElse(null);
            if (config == null || config.getDailyReportTime() == null) {
                continue;
            }
            if (now.isBefore(config.getDailyReportTime())) {
                continue;
            }
            results.add(dispatchForTenant(tenant.getId(), date));
        }
        return results;
    }
}