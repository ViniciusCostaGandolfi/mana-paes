package vgandolfi.dev.mana_paes.api.controller;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vgandolfi.dev.mana_paes.application.dto.request.NotificationConfigRequest;
import vgandolfi.dev.mana_paes.application.dto.response.DailyReportDispatchResponse;
import vgandolfi.dev.mana_paes.application.dto.response.MessageResponse;
import vgandolfi.dev.mana_paes.application.dto.response.NotificationConfigResponse;
import vgandolfi.dev.mana_paes.application.dto.response.NotificationLogResponse;
import vgandolfi.dev.mana_paes.domain.model.enums.NotificationChannel;
import vgandolfi.dev.mana_paes.domain.model.enums.NotificationStatus;
import vgandolfi.dev.mana_paes.domain.service.DailyReportDispatcher;
import vgandolfi.dev.mana_paes.domain.service.NotificationConfigService;
import vgandolfi.dev.mana_paes.infrastructure.security.AuthenticatedUser;

import java.time.LocalDate;

/**
 * Configuração de notificações, consulta de logs e disparo manual do relatório
 * diário (somente ADMIN).
 */
@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationConfigService configService;
    private final DailyReportDispatcher dailyReportDispatcher;

    public NotificationController(NotificationConfigService configService,
                                  DailyReportDispatcher dailyReportDispatcher) {
        this.configService = configService;
        this.dailyReportDispatcher = dailyReportDispatcher;
    }

    @GetMapping("/config")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<NotificationConfigResponse> getConfig(
            @AuthenticationPrincipal AuthenticatedUser principal) {
        return ResponseEntity.ok(configService.getConfig(principal.tenantId()));
    }

    @PutMapping("/config")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<NotificationConfigResponse> updateConfig(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody NotificationConfigRequest request) {
        return ResponseEntity.ok(configService.updateConfig(principal.tenantId(), request));
    }

    @PostMapping("/whatsapp/test")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MessageResponse> sendTest(@AuthenticationPrincipal AuthenticatedUser principal) {
        return ResponseEntity.ok(configService.sendTestWhatsApp(principal.tenantId()));
    }

    @GetMapping("/logs")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<NotificationLogResponse>> listLogs(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @RequestParam(required = false) NotificationStatus status,
            @RequestParam(required = false) NotificationChannel channel,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(configService.listLogs(principal.tenantId(), status, channel, pageable));
    }

    @PostMapping("/reports/daily/send")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DailyReportDispatchResponse> sendDailyReport(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        LocalDate reportDate = date != null ? date : LocalDate.now();
        return ResponseEntity.ok(dailyReportDispatcher.dispatchForTenant(principal.tenantId(), reportDate));
    }
}