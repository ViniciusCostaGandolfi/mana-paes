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
                "Everton", "5511900000001",
                new BigDecimal("55.00"), LocalDate.of(2026, 9, 1),
                List.of(new OrderCreatedEvent.Item(UUID.randomUUID(), "Pão Francês", new BigDecimal("10"), UnitMeasure.UN)));
    }

    @Test
    void orderConfirmationContainsVendorAndItems() {
        String message = adapter.orderConfirmation(event());

        assertThat(message).contains("Pedido confirmado");
        assertThat(message).contains("Everton");
        assertThat(message).contains("Pão Francês");
        assertThat(message).contains("R$ 55,00");
    }

    @Test
    void newOrderAdminAlertContainsVendorAndItems() {
        String message = adapter.newOrderAdminAlert(event());

        assertThat(message).contains("Novo pedido recebido");
        assertThat(message).contains("Everton");
        assertThat(message).contains("Pão Francês");
    }

    @Test
    void dailyReportContainsShiftsAndVendorNames() {
        DailyProductionReportResponse production = new DailyProductionReportResponse(LocalDate.of(2026, 9, 1),
                List.of(new DailyReportItemResponse(UUID.randomUUID(), "Pão de sal", UnitMeasure.UN,
                        new BigDecimal("50"), new BigDecimal("50"), new BigDecimal("0"),
                        new BigDecimal("100"), new BigDecimal("55.00"))),
                new BigDecimal("55.00"), List.of("Everton", "Daiana"));
        DailyFinancialReportResponse financial =
                new DailyFinancialReportResponse(LocalDate.of(2026, 9, 1), new BigDecimal("55.00"), 2);

        String message = adapter.dailyReport(production, financial, "http://localhost:3000");

        assertThat(message).contains("Resumo dos pedidos - 01/09/2026");
        assertThat(message).contains("2 pedido(s) de: Everton, Daiana");
        assertThat(message).contains("Pão de sal — M:50 T:50 N:0");
        assertThat(message).contains("TOTAL GERAL: R$ 55,00");
        assertThat(message).contains("Acesse o sistema: http://localhost:3000");
    }

    @Test
    void vendorReminderContainsNameAndLink() {
        String message = adapter.vendorReminder("Everton", "https://app.manapaes.com/pedido?telefone=5511900000001&dia=2026-09-02");

        assertThat(message).contains("Everton");
        assertThat(message).contains("https://app.manapaes.com/pedido?telefone=5511900000001&dia=2026-09-02");
    }

    @Test
    void testMessageIsNotEmpty() {
        assertThat(adapter.testMessage()).isNotBlank();
    }
}