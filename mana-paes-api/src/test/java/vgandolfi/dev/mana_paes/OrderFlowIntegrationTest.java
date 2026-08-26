package vgandolfi.dev.mana_paes;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import vgandolfi.dev.mana_paes.domain.repository.PasswordResetTokenRepository;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Ciclo de vida completo do pedido: criação com cálculo de total, transições de
 * status, histórico, RBAC e relatórios diários (produção e financeiro).
 */
@SpringBootTest
@AutoConfigureMockMvc
class OrderFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PasswordResetTokenRepository tokenRepository;

    @Test
    void fullOrderLifecycleWithReports() throws Exception {
        String adminToken = TestSupport.registerAndGetToken(mockMvc, "dona.pedidos@example.com", "senha123");
        String requesterToken = TestSupport.createRequesterAndGetToken(mockMvc, tokenRepository, adminToken,
                "Solicitante", "func.pedidos@example.com", "senha123");

        String paoId = createProduct(adminToken, "Pão Francês", "0.50");
        String boloId = createProduct(adminToken, "Bolo de Chocolate", "25.00");

        LocalDate deliveryDate = LocalDate.now().plusDays(3);
        String dateStr = deliveryDate.toString();

        // requester cria pedido: 10 x pão (5,00) + 2 x bolo (50,00) = 55,00
        String orderBody = """
                {"deliveryDate":"%s","items":[
                  {"productId":"%s","quantity":10},
                  {"productId":"%s","quantity":2}
                ]}
                """.formatted(dateStr, paoId, boloId);

        MvcResult orderResult = mockMvc.perform(post("/api/v1/orders")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + requesterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.requesterName").value("Solicitante"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.totalAmount").value(55.0))
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items[0].productName").value("Pão Francês"))
                .andExpect(jsonPath("$.items[0].subtotal").value(5.0))
                .andExpect(jsonPath("$.items[1].productName").value("Bolo de Chocolate"))
                .andExpect(jsonPath("$.items[1].subtotal").value(50.0))
                .andReturn();
        String orderId = JsonPath.read(
                orderResult.getResponse().getContentAsString(StandardCharsets.UTF_8), "$.id");

        // requester lista os próprios pedidos
        mockMvc.perform(get("/api/v1/orders")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + requesterToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(orderId));

        // transições válidas PENDING -> IN_PRODUCTION -> READY -> DELIVERED
        updateStatus(adminToken, orderId, "IN_PRODUCTION");
        updateStatus(adminToken, orderId, "READY");
        updateStatus(adminToken, orderId, "DELIVERED");

        // transição inválida a partir de status terminal
        mockMvc.perform(patch("/api/v1/orders/" + orderId + "/status")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"READY\"}"))
                .andExpect(status().isBadRequest());

        // relatório de produção do dia (agregado por produto, exclui cancelados)
        mockMvc.perform(get("/api/v1/reports/daily/production?date=" + dateStr)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.date").value(dateStr))
                .andExpect(jsonPath("$.totalAmount").value(55.0))
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items[0].productName").value("Bolo de Chocolate"))
                .andExpect(jsonPath("$.items[0].totalQuantity").value(2.0))
                .andExpect(jsonPath("$.items[1].productName").value("Pão Francês"))
                .andExpect(jsonPath("$.items[1].totalQuantity").value(10.0));

        // relatório financeiro (admin)
        mockMvc.perform(get("/api/v1/reports/daily/financial?date=" + dateStr)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAmount").value(55.0))
                .andExpect(jsonPath("$.totalOrders").value(1));

        // requester NÃO pode ver relatório financeiro (RBAC)
        mockMvc.perform(get("/api/v1/reports/daily/financial?date=" + dateStr)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + requesterToken))
                .andExpect(status().isForbidden());
    }

    private String createProduct(String adminToken, String name, String price) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/products")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\",\"unitPrice\":" + price + ",\"unitMeasure\":\"UN\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(StandardCharsets.UTF_8), "$.id");
    }

    private void updateStatus(String token, String orderId, String status) throws Exception {
        mockMvc.perform(patch("/api/v1/orders/" + orderId + "/status")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"" + status + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(status));
    }
}