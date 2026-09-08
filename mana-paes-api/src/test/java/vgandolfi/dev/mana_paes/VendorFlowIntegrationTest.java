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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Fluxo de vendedores: ADMIN cria/liste/atualiza/desativa; sem token o recurso
 * retorna 401.
 */
@SpringBootTest
@AutoConfigureMockMvc
class VendorFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void adminCrudVendor() throws Exception {
        String adminToken = TestSupport.adminToken(mockMvc, "dona.vendors@example.com", "senha123");

        // cria vendedor
        MvcResult createResult = mockMvc.perform(post("/api/v1/vendors")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Everton\",\"phone\":\"5511988111001\",\"whatsappNumber\":\"5511988111001\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Everton"))
                .andExpect(jsonPath("$.phone").value("5511988111001"))
                .andExpect(jsonPath("$.active").value(true))
                .andReturn();
        String vendorId = JsonPath.read(
                createResult.getResponse().getContentAsString(StandardCharsets.UTF_8), "$.id");

        // lista
        mockMvc.perform(get("/api/v1/vendors")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(vendorId));

        // getById
        mockMvc.perform(get("/api/v1/vendors/" + vendorId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Everton"));

        // update
        mockMvc.perform(put("/api/v1/vendors/" + vendorId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Everton Souza\",\"phone\":\"5511988111001\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Everton Souza"));

        // desativa
        mockMvc.perform(patch("/api/v1/vendors/" + vendorId + "/status")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        // telefone duplicado no tenant -> 400
        mockMvc.perform(post("/api/v1/vendors")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Outro\",\"phone\":\"5511988111001\"}"))
                .andExpect(status().isBadRequest());

        // sem token -> 401
        mockMvc.perform(get("/api/v1/vendors"))
                .andExpect(status().isUnauthorized());
    }
}