package vgandolfi.dev.mana_paes.infrastructure.web.controller;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vgandolfi.dev.mana_paes.application.dto.request.ScheduledReminderRequest;
import vgandolfi.dev.mana_paes.application.dto.response.ScheduledReminderResponse;
import vgandolfi.dev.mana_paes.application.service.NotificationService;
import vgandolfi.dev.mana_paes.application.service.ScheduledReminderService;
import vgandolfi.dev.mana_paes.infrastructure.security.AuthenticatedUser;

import java.util.UUID;

/**
 * CRUD de lembretes agendados para vendedores (tenant-scoped). O disparo
 * recorrente é feito pelo {@code VendorReminderScheduler}; o endpoint
 * {@code POST /{id}/test} dispara imediatamente para validação.
 */
@RestController
@RequestMapping("/api/v1/reminders")
@PreAuthorize("hasAnyRole('SYS_ADMIN', 'ADMIN')")
public class ScheduledReminderController {

    private final ScheduledReminderService reminderService;

    public ScheduledReminderController(ScheduledReminderService reminderService) {
        this.reminderService = reminderService;
    }

    @GetMapping
    public ResponseEntity<Page<ScheduledReminderResponse>> list(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PageableDefault(size = 20, sort = "reminderTime") Pageable pageable) {
        return ResponseEntity.ok(reminderService.list(principal.tenantId(), pageable));
    }

    @PostMapping
    public ResponseEntity<ScheduledReminderResponse> create(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody ScheduledReminderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reminderService.create(principal.tenantId(), principal.userId(), request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ScheduledReminderResponse> getById(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable UUID id) {
        return ResponseEntity.ok(reminderService.getById(principal.tenantId(), id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ScheduledReminderResponse> update(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable UUID id,
            @Valid @RequestBody ScheduledReminderRequest request) {
        return ResponseEntity.ok(reminderService.update(principal.tenantId(), id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal AuthenticatedUser principal,
                                       @PathVariable UUID id) {
        reminderService.delete(principal.tenantId(), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/test")
    public ResponseEntity<NotificationService.VendorReminderResult> sendTest(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable UUID id) {
        return ResponseEntity.ok(reminderService.sendTest(principal.tenantId(), id));
    }
}