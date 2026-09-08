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
 * Monta as mensagens de texto do WhatsApp (confirmação ao vendedor, alerta ao
 * admin, lembrete de pedido e resumo diário por turno). Formatação pt-BR,
 * simples, sem depender de infra.
 */
@Component
public class WhatsAppNotificationAdapter {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DecimalFormat MONEY = new DecimalFormat(
            "#,##0.00", DecimalFormatSymbols.getInstance(new Locale("pt", "BR")));

    public String orderConfirmation(OrderCreatedEvent event) {
        StringBuilder sb = new StringBuilder();
        sb.append("*Pedido confirmado!*\n");
        appendOrderSummary(sb, event);
        return sb.toString();
    }

    public String newOrderAdminAlert(OrderCreatedEvent event) {
        StringBuilder sb = new StringBuilder();
        sb.append("*Novo pedido recebido!*\n");
        appendOrderSummary(sb, event);
        return sb.toString();
    }

    public String testMessage() {
        return "Mensagem de teste do Mana Paes\n"
                + "Se voce recebeu esta mensagem, a integracao com a Evolution API esta funcionando.";
    }

    /**
     * Mensagem de lembrete de pedido enviada ao vendedor, com o link público.
     */
    public String vendorReminder(String name, String link) {
        return "Olá, " + name + "!\n"
                + "Faça o seu pedido pelo link:\n" + link;
    }

    /**
     * Resumo diário por turno (agregado por produto), com os nomes dos
     * vendedores e o link do sistema. Formato otimizado para WhatsApp.
     */
    public String dailyReport(DailyProductionReportResponse production, DailyFinancialReportResponse financial,
                              String appLink) {
        StringBuilder sb = new StringBuilder();
        sb.append("*Resumo dos pedidos - ").append(DATE_FORMAT.format(production.date())).append("*\n");
        String vendorLine = production.vendorNames().isEmpty()
                ? "Nenhum vendedor"
                : String.join(", ", production.vendorNames());
        sb.append(financial.totalOrders()).append(" pedido(s) de: ").append(vendorLine).append('\n');

        if (production.items().isEmpty()) {
            sb.append("Nenhum pedido no período.\n");
        } else {
            sb.append('\n');
            for (DailyReportItemResponse item : production.items()) {
                sb.append(item.productName())
                        .append(" — M:").append(quantity(item.quantidadeManha()))
                        .append(" T:").append(quantity(item.quantidadeTarde()))
                        .append(" N:").append(quantity(item.quantidadeNoite()))
                        .append(" — Total: R$ ").append(MONEY.format(item.totalAmount())).append('\n');
            }
        }
        sb.append('\n').append("TOTAL GERAL: R$ ").append(MONEY.format(financial.totalAmount()));
        if (appLink != null && !appLink.isBlank()) {
            sb.append("\n\nAcesse o sistema: ").append(appLink);
        }
        return sb.toString();
    }

    private void appendOrderSummary(StringBuilder sb, OrderCreatedEvent event) {
        sb.append("Pedido #").append(shortId(event.orderId())).append('\n');
        if (event.vendorName() != null) {
            sb.append("Vendedor: ").append(event.vendorName()).append('\n');
        }
        sb.append("Entrega: ").append(DATE_FORMAT.format(event.deliveryDate())).append('\n');
        sb.append("Itens:\n");
        for (OrderCreatedEvent.Item item : event.items()) {
            sb.append("- ").append(item.productName())
                    .append(" (")
                    .append(quantity(item.totalQuantity()))
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