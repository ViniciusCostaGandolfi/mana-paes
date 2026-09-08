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
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * Vendedor (sem login): faz pedidos pelo link público (identificado pelo
 * telefone). O telefone é o identificador do link de pedido e o destino das
 * notificações WhatsApp.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "vendors",
        uniqueConstraints = @UniqueConstraint(name = "uk_vendors_tenant_phone", columnNames = {"tenant_id", "phone"}),
        indexes = {
                @Index(name = "idx_vendors_tenant_id", columnList = "tenant_id"),
                @Index(name = "idx_vendors_phone", columnList = "phone")
        })
public class Vendor extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "phone", nullable = false, length = 20)
    private String phone;

    @Column(name = "whatsapp_number", length = 20)
    private String whatsappNumber;

    @Column(name = "active", nullable = false)
    private boolean active = true;
}