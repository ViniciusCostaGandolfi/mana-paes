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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pedido público via link mágico: sem autenticação, o vendedor é resolvido
 * pelo telefone; o total considera turnos e opções com preço adicional. O
 * pedido criado aparece na listagem do ADMIN do tenant.
 */
@SpringBootTest
@AutoConfigureMockMvc
class PublicOrderFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void publicOrderCreatesOrderWithVendorShiftsAndOptions() throws Exception {
        String adminToken = TestSupport.adminToken(mockMvc, "dona.public@example.com", "senha123");
        TestSupport.createVendor(mockMvc, adminToken, "Everton", "5511988111001");

        // produto com grupo de opções (Grande +0,20)
        MvcResult productResult = mockMvc.perform(post("/api/v1/products")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Pão de sal","unitPrice":0.55,"unitMeasure":"UN","optionGroups":[
                                  {"name":"Tamanho","options":[
                                    {"name":"Pequeno","priceAdjustment":0},
                                    {"name":"Grande","priceAdjustment":0.20}
                                  ]}
                                ]}
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        String productJson = productResult.getResponse().getContentAsString(StandardCharsets.UTF_8);
        String productId = JsonPath.read(productJson, "$.id");
        String groupId = JsonPath.read(productJson, "$.optionGroups[0].id");
        String grandeId = JsonPath.read(productJson, "$.optionGroups[0].options[1].id");

        // pedido público (sem token): 10 manhã x (0,55 + 0,20) = 7,50
        String deliveryDate = LocalDate.now().plusDays(1).toString();
        mockMvc.perform(post("/api/v1/public/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"vendorPhone":"5511988111001","vendorName":"Everton","deliveryDate":"%s","items":[
                                  {"productId":"%s","quantidadeManha":10,"quantidadeTarde":0,"quantidadeNoite":0,
                                   "selections":[{"groupId":"%s","optionId":"%s"}]}
                                ]}
                                """.formatted(deliveryDate, productId, groupId, grandeId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.vendorName").value("Everton"))
                .andExpect(jsonPath("$.totalAmount").value(7.5))
                .andExpect(jsonPath("$.items[0].quantidadeManha").value(10.0))
                .andExpect(jsonPath("$.items[0].unitPrice").value(0.75))
                .andExpect(jsonPath("$.items[0].options[0].optionName").value("Grande"))
                .andReturn();

        // admin vê o pedido do vendedor na listagem do tenant
        mockMvc.perform(get("/api/v1/orders")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].vendorName").value("Everton"))
                .andExpect(jsonPath("$.content[0].totalAmount").value(7.5));
    }

@Test
    void publicOrderWithUnknownVendorReturns400() throws Exception {
        mockMvc.perform(post("/api/v1/public/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"vendorPhone":"5511999999999","vendorName":"X","deliveryDate":"2099-01-01","items":[
                                  {"productId":"00000000-0000-0000-0000-000000000000","quantidadeManha":1,"quantidadeTarde":0,"quantidadeNoite":0}
                                ]}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Vendedor não cadastrado"));
    }

    @Test
    void publicProductsListOnlyActiveProductsOfVendorTenant() throws Exception {
        // tenant 1 (vendedor do teste)
        String adminToken = TestSupport.adminToken(mockMvc, "dona.catalogo@example.com", "senha123");
        TestSupport.createVendor(mockMvc, adminToken, "Everton", "5511988111222");
        createProduct(adminToken, "Pão de sal", "0.55");
        createProduct(adminToken, "Bolo de Milho", "16.80");

        // tenant 2 (outro vendedor/tenant) — produtos NÃO devem vazar
        String otherAdminToken = TestSupport.adminToken(mockMvc, "dona.catalogo2@example.com", "senha123");
        TestSupport.createVendor(mockMvc, otherAdminToken, "Daiana", "5511988111333");
        createProduct(otherAdminToken, "Produto do Outro Tenant", "99.00");

        // catálogo público do Everton (sem token): só os produtos do tenant dele
        mockMvc.perform(get("/api/v1/public/products?phone=5511988111222"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[*].name").value(org.hamcrest.Matchers.hasItems("Pão de sal", "Bolo de Milho")))
                .andExpect(jsonPath("$[*].name").value(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.hasItem("Produto do Outro Tenant"))));
    }

    @Test
    void publicProductsIncludesOptionGroups() throws Exception {
        String adminToken = TestSupport.adminToken(mockMvc, "dona.catalogo.opt@example.com", "senha123");
        TestSupport.createVendor(mockMvc, adminToken, "Everton", "5511988111444");

        mockMvc.perform(post("/api/v1/products")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Pão de sal","unitPrice":0.55,"unitMeasure":"UN","optionGroups":[
                                  {"name":"Tamanho","options":[
                                    {"name":"Pequeno","priceAdjustment":0},
                                    {"name":"Grande","priceAdjustment":0.20}
                                  ]}
                                ]}
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/public/products?phone=5511988111444"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].optionGroups[0].name").value("Tamanho"))
                .andExpect(jsonPath("$[0].optionGroups[0].options[1].name").value("Grande"))
                .andExpect(jsonPath("$[0].optionGroups[0].options[1].priceAdjustment").value(0.20));
    }

    @Test
    void publicProductsWithUnknownVendorReturns400() throws Exception {
        mockMvc.perform(get("/api/v1/public/products?phone=5511999999999"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Vendedor não cadastrado"));
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
}