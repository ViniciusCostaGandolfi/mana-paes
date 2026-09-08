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

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Opção escolhida de um item de pedido (snapshot do nome e do ajuste de preço
 * no momento do pedido).
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "order_item_options", indexes = {
        @Index(name = "idx_order_item_options_order_item_id", columnList = "order_item_id")
})
public class OrderItemOption extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_item_id", nullable = false)
    private OrderItem orderItem;

    @Column(name = "option_name", nullable = false, length = 100)
    private String optionName;

    @Column(name = "price_adjustment", nullable = false, precision = 19, scale = 2)
    private BigDecimal priceAdjustment = BigDecimal.ZERO;

    @Column(name = "option_id")
    private UUID optionId;
}