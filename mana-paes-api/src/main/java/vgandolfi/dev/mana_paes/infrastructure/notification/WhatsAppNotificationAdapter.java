package vgandolfi.dev.mana_paes.infrastructure.notification;

import org.springframework.stereotype.Component;
import vgandolfi.dev.mana_paes.application.dto.response.DailyFinancialReportResponse;
import vgandolfi.dev.mana_paes.application.dto.response.DailyProductionReportResponse;
import vgandolfi.dev.mana_paes.application.dto.response.DailyReportItemResponse;
import vgandolfi.dev.mana_paes.application.event.OrderCreatedEvent;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Monta as mensagens de texto do WhatsApp (confirmação ao solicitante, alerta
 * ao admin, mensagem de teste). Formatação simples, sem depender de infra.
 */
@Component
public class WhatsAppNotificationAdapter {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DecimalFormat MONEY = new DecimalFormat(
            "#,##0.00", DecimalFormatSymbols.getInstance(new Locale("pt", "BR")));

    public String orderConfirmation(OrderCreatedEvent event, String requesterName) {
        StringBuilder sb = new StringBuilder();
        sb.append("*Pedido confirmado!*\n");
        appendOrderSummary(sb, event, requesterName);
        return sb.toString();
    }

    public String newOrderAdminAlert(OrderCreatedEvent event, String requesterName) {
        StringBuilder sb = new StringBuilder();
        sb.append("*Novo pedido recebido!*\n");
        appendOrderSummary(sb, event, requesterName);
        return sb.toString();
    }

    public String testMessage() {
        return "Mensagem de teste do Mana Paes\n"
                + "Se voce recebeu esta mensagem, a integracao com a Evolution API esta funcionando.";
    }

    public String dailyReport(DailyProductionReportResponse production, DailyFinancialReportResponse financial) {
        StringBuilder sb = new StringBuilder();
        sb.append("*Relatório diário* ").append(DATE_FORMAT.format(production.date())).append('\n');
        sb.append("Pedidos: ").append(financial.totalOrders()).append('\n');
        if (production.items().isEmpty()) {
            sb.append("Nenhum pedido no período.\n");
        } else {
            sb.append("Itens:\n");
            for (DailyReportItemResponse item : production.items()) {
                sb.append("- ").append(item.productName())
                        .append(" (qtd ").append(quantity(item.totalQuantity()))
                        .append(' ').append(item.unitMeasure()).append(")\n");
            }
        }
        sb.append("Total: R$ ").append(MONEY.format(financial.totalAmount()));
        return sb.toString();
    }

    private void appendOrderSummary(StringBuilder sb, OrderCreatedEvent event, String requesterName) {
        sb.append("Pedido #").append(shortId(event.orderId())).append('\n');
        sb.append("Cliente: ").append(requesterName).append('\n');
        sb.append("Entrega: ").append(DATE_FORMAT.format(event.deliveryDate())).append('\n');
        sb.append("Itens:\n");
        for (OrderCreatedEvent.Item item : event.items()) {
            sb.append("- ").append(item.productName())
                    .append(" (")
                    .append(quantity(item.quantity()))
                    .append(' ')
                    .append(item.unitMeasure())
                    .append(")\n");
        }
        sb.append("Total: R$ ").append(MONEY.format(event.totalAmount()));
    }

    private String quantity(BigDecimal quantity) {
        return quantity.stripTrailingZeros().toPlainString();
    }

    private String shortId(java.util.UUID id) {
        return id.toString().substring(0, 8);
    }
}