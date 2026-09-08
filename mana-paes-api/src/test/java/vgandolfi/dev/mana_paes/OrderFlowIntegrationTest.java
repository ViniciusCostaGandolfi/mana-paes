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

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Ciclo de vida completo do pedido: criação (ADMIN), cálculo de total,
 * transições de status, histórico, RBAC e relatórios diários.
 */
@SpringBootTest
@AutoConfigureMockMvc
class OrderFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void fullOrderLifecycleWithReports() throws Exception {
        String adminToken = TestSupport.adminToken(mockMvc, "dona.pedidos@example.com", "senha123");

        String paoId = createProduct(adminToken, "Pão Francês", "0.50");
        String boloId = createProduct(adminToken, "Bolo de Chocolate", "25.00");

        LocalDate deliveryDate = LocalDate.now().plusDays(3);
        String dateStr = deliveryDate.toString();

        // admin cria pedido: 10 x pão (5,00) + 2 x bolo (50,00) = 55,00
        String orderBody = """
                {"deliveryDate":"%s","items":[
                  {"productId":"%s","quantidadeManha":10,"quantidadeTarde":0,"quantidadeNoite":0},
                  {"productId":"%s","quantidadeManha":2,"quantidadeTarde":0,"quantidadeNoite":0}
                ]}
                """.formatted(dateStr, paoId, boloId);

        MvcResult orderResult = mockMvc.perform(post("/api/v1/orders")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderBody))
                .andExpect(status().isCreated())
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

        // transições válidas PENDING -> ACCEPTED (CANCELLED é terminal)
        updateStatus(adminToken, orderId, "ACCEPTED");

        // transição inválida a partir de ACCEPTED (não pode voltar para PENDING)
        mockMvc.perform(patch("/api/v1/orders/" + orderId + "/status")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"PENDING\"}"))
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
    }

    @Test
    void acceptAllAcceptsPendingOrdersOfTenant() throws Exception {
        String adminToken = TestSupport.adminToken(mockMvc, "dona.lote@example.com", "senha123");

        String productId = createProduct(adminToken, "Pão do Lote", "1.00");
        String deliveryDate = LocalDate.now().plusDays(2).toString();

        createOrder(adminToken, deliveryDate, productId);
        createOrder(adminToken, deliveryDate, productId);

        mockMvc.perform(post("/api/v1/orders/accept-all")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("2 pedido(s) aceito(s)"));

        mockMvc.perform(get("/api/v1/orders")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[*].status").value(
                        org.hamcrest.Matchers.containsInAnyOrder("ACCEPTED", "ACCEPTED")));

        // idempotência: novo accept-all não encontra PENDING restantes
        mockMvc.perform(post("/api/v1/orders/accept-all")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("0 pedido(s) aceito(s)"));
    }

    @Test
    void acceptAllDoesNotTouchOrdersOfOtherTenants() throws Exception {
        String adminToken = TestSupport.adminToken(mockMvc, "dona.lote2@example.com", "senha123");
        String otherAdminToken = TestSupport.adminToken(mockMvc, "dona.lote3@example.com", "senha123");

        String productId = createProduct(adminToken, "Pão Lote A", "1.00");
        String otherProductId = createProduct(otherAdminToken, "Pão Lote B", "1.00");
        String deliveryDate = LocalDate.now().plusDays(2).toString();

        createOrder(adminToken, deliveryDate, productId);
        createOrder(otherAdminToken, deliveryDate, otherProductId);

        mockMvc.perform(post("/api/v1/orders/accept-all")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("1 pedido(s) aceito(s)"));

        // o pedido do outro tenant segue PENDING
        mockMvc.perform(get("/api/v1/orders")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + otherAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].status").value("PENDING"));
    }

    @Test
    void acceptAllRequiresAdminRole() throws Exception {
        String sysAdminToken = TestSupport.sysAdminToken(mockMvc);

        mockMvc.perform(post("/api/v1/orders/accept-all")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + sysAdminToken))
                .andExpect(status().isForbidden());
    }

    private String createOrder(String adminToken, String deliveryDate, String productId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/orders")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"deliveryDate\":\"" + deliveryDate
                                + "\",\"items\":[{\"productId\":\"" + productId
                                + "\",\"quantidadeManha\":1,\"quantidadeTarde\":0,\"quantidadeNoite\":0}]}"))
                .andExpect(status().isCreated())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(StandardCharsets.UTF_8), "$.id");
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