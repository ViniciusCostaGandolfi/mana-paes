package vgandolfi.dev.mana_paes.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Lembrete agendado recorrente (diário): dispara notificações para os motoboys
 * ativos do tenant no horário configurado ({@code reminderTime}). A
 * idempotência é garantida por {@code lastSentDate} — no máximo um disparo por
 * dia por lembrete.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "scheduled_reminders", indexes = {
        @Index(name = "idx_scheduled_reminders_tenant_id", columnList = "tenant_id"),
        @Index(name = "idx_scheduled_reminders_enabled_time", columnList = "enabled, reminder_time")
})
public class ScheduledReminder extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "message", nullable = false, length = 500)
    private String message;

    @Column(name = "reminder_time", nullable = false)
    private LocalTime reminderTime;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    @Column(name = "whatsapp_enabled", nullable = false)
    private boolean whatsappEnabled = false;

    @Column(name = "email_enabled", nullable = false)
    private boolean emailEnabled = true;

    @Column(name = "last_sent_date")
    private LocalDate lastSentDate;

    /**
     * Deslocamento em dias da data de entrega informada no link do lembrete
     * (ex.: 1 = lembrete de hoje aponta para amanhã). Default 1.
     */
    @Column(name = "delivery_day_offset", nullable = false)
    private int deliveryDayOffset = 1;

    /**
     * Usuário (ADMIN/SYS_ADMIN) que criou o lembrete — auditoria. Opcional.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;
}