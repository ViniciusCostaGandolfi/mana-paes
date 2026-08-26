package vgandolfi.dev.mana_paes.domain.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vgandolfi.dev.mana_paes.domain.model.Order;
import vgandolfi.dev.mana_paes.domain.model.enums.OrderStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {

    List<Order> findByRequesterId(UUID requesterId);

    List<Order> findByTenantIdAndDeliveryDate(UUID tenantId, LocalDate deliveryDate);

    List<Order> findByTenantIdAndStatusAndDeliveryDate(UUID tenantId, OrderStatus status, LocalDate deliveryDate);

    Optional<Order> findByIdAndTenantId(UUID id, UUID tenantId);

    Page<Order> findByTenantId(UUID tenantId, Pageable pageable);

    Page<Order> findByRequesterIdAndTenantId(UUID requesterId, UUID tenantId, Pageable pageable);

    @Query("select o from Order o left join fetch o.items where o.id = :id")
    Optional<Order> findWithItems(@Param("id") UUID id);

    @Query("select o from Order o left join fetch o.items i left join fetch i.product where o.id = :id and o.tenant.id = :tenantId")
    Optional<Order> findWithItemsAndTenant(@Param("id") UUID id, @Param("tenantId") UUID tenantId);

    /**
     * Agregação de produção: quantidade e valor totais por produto para a
     * entrega na data informada, excluindo o status informado (CANCELLED).
     * Retorna linhas {productId, productName, unitMeasure, totalQuantity, totalAmount}.
     */
    @Query("""
            select oi.product.id, oi.product.name, oi.product.unitMeasure, sum(oi.quantity), sum(oi.subtotal)
            from OrderItem oi
            join oi.order o
            where o.tenant.id = :tenantId and o.deliveryDate = :deliveryDate and o.status <> :excludedStatus
            group by oi.product.id, oi.product.name, oi.product.unitMeasure
            order by oi.product.name
            """)
    List<Object[]> sumByProductForDate(@Param("tenantId") UUID tenantId,
                                       @Param("deliveryDate") LocalDate deliveryDate,
                                       @Param("excludedStatus") OrderStatus excludedStatus);

    /**
     * Agregação financeira: {totalOrders, totalAmount} para a entrega na data
     * informada, excluindo o status informado (CANCELLED). Sempre retorna uma
     * linha (count=0 e sum=null quando não há pedidos).
     */
    @Query("""
            select count(o), sum(o.totalAmount)
            from Order o
            where o.tenant.id = :tenantId and o.deliveryDate = :deliveryDate and o.status <> :excludedStatus
            """)
    List<Object[]> countAndSumForDate(@Param("tenantId") UUID tenantId,
                                      @Param("deliveryDate") LocalDate deliveryDate,
                                      @Param("excludedStatus") OrderStatus excludedStatus);
}