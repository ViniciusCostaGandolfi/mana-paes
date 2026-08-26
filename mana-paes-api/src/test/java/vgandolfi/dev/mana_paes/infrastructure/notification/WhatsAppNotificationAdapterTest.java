package vgandolfi.dev.mana_paes.infrastructure.notification;

import org.junit.jupiter.api.Test;
import vgandolfi.dev.mana_paes.application.dto.response.DailyFinancialReportResponse;
import vgandolfi.dev.mana_paes.application.dto.response.DailyProductionReportResponse;
import vgandolfi.dev.mana_paes.application.dto.response.DailyReportItemResponse;
import vgandolfi.dev.mana_paes.application.event.OrderCreatedEvent;
import vgandolfi.dev.mana_paes.domain.model.enums.UnitMeasure;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class WhatsAppNotificationAdapterTest {

    private final WhatsAppNotificationAdapter adapter = new WhatsAppNotificationAdapter();

    private OrderCreatedEvent event() {
        return new OrderCreatedEvent(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                new BigDecimal("55.00"), LocalDate.of(2026, 9, 1),
                List.of(new OrderCreatedEvent.Item(UUID.randomUUID(), "Pão Francês", new BigDecimal("10"), UnitMeasure.UN)));
    }

    @Test
    void orderConfirmationContainsOrderData() {
        String message = adapter.orderConfirmation(event(), "João");

        assertThat(message).contains("Pedido confirmado");
        assertThat(message).contains("João");
        assertThat(message).contains("Pão Francês");
        assertThat(message).contains("R$ 55,00");
    }

    @Test
    void newOrderAdminAlertContainsRequesterAndItems() {
        String message = adapter.newOrderAdminAlert(event(), "João");

        assertThat(message).contains("Novo pedido recebido");
        assertThat(message).contains("João");
        assertThat(message).contains("Pão Francês");
    }

    @Test
    void dailyReportContainsTotalsAndItems() {
        DailyProductionReportResponse production = new DailyProductionReportResponse(LocalDate.of(2026, 9, 1),
                List.of(new DailyReportItemResponse(UUID.randomUUID(), "Pão Francês", UnitMeasure.UN,
                        new BigDecimal("10"), new BigDecimal("5.00"))),
                new BigDecimal("5.00"));
        DailyFinancialReportResponse financial =
                new DailyFinancialReportResponse(LocalDate.of(2026, 9, 1), new BigDecimal("5.00"), 2);

        String message = adapter.dailyReport(production, financial);

        assertThat(message).contains("Relatório diário");
        assertThat(message).contains("01/09/2026");
        assertThat(message).contains("Pedidos: 2");
        assertThat(message).contains("Pão Francês");
        assertThat(message).contains("R$ 5,00");
    }

    @Test
    void testMessageIsNotEmpty() {
        assertThat(adapter.testMessage()).isNotBlank();
    }
}