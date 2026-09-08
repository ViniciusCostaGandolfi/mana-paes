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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Fluxo de produtos: admin cria, lista, desativa e verifica o filtro ativo.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ProductFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void adminCreatesListsAndDeactivatesProduct() throws Exception {
        String adminToken = TestSupport.adminToken(mockMvc, "dona.produtos@example.com", "senha123");

        // admin cria produto
        MvcResult createResult = mockMvc.perform(post("/api/v1/products")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Pão Francês\",\"description\":\"Pão de 500g\",\"unitPrice\":0.50,\"unitMeasure\":\"UN\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Pão Francês"))
                .andExpect(jsonPath("$.unitPrice").value(0.5))
                .andExpect(jsonPath("$.unitMeasure").value("UN"))
                .andExpect(jsonPath("$.active").value(true))
                .andReturn();
        String productId = JsonPath.read(
                createResult.getResponse().getContentAsString(StandardCharsets.UTF_8), "$.id");

        // lista produtos do tenant
        mockMvc.perform(get("/api/v1/products")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].name").value("Pão Francês"));

        // admin desativa produto
        mockMvc.perform(patch("/api/v1/products/" + productId + "/active")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        // ?active=true não retorna o produto desativado
        mockMvc.perform(get("/api/v1/products?active=true")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty());

        // sem token, endpoint protegido retorna 401
        mockMvc.perform(get("/api/v1/products"))
                .andExpect(status().isUnauthorized());
    }
}