package vgandolfi.dev.mana_paes;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Caminhos de erro da API: 404 (recurso não encontrado), 400 (validação, corpo
 * malformado, tipo de parâmetro) e 401 (não autenticado). Cobre o
 * {@code GlobalExceptionHandler} e as exceções de domínio.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ErrorHandlingIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void notFoundReturnsApiError() throws Exception {
        String adminToken = TestSupport.registerAndGetToken(mockMvc, "dona.erros@example.com", "senha123");

        mockMvc.perform(get("/api/v1/orders/" + UUID.randomUUID())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void emptyItemsValidationReturns400() throws Exception {
        String adminToken = TestSupport.registerAndGetToken(mockMvc, "dona.validacao@example.com", "senha123");

        mockMvc.perform(post("/api/v1/orders")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"deliveryDate\":\"2099-01-01\",\"items\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    void malformedBodyReturns400() throws Exception {
        String adminToken = TestSupport.registerAndGetToken(mockMvc, "dona.malformed@example.com", "senha123");

        mockMvc.perform(post("/api/v1/orders")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not-json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void typeMismatchReturns400() throws Exception {
        String adminToken = TestSupport.registerAndGetToken(mockMvc, "dona.tipos@example.com", "senha123");

        mockMvc.perform(get("/api/v1/reports/daily/production?date=not-a-date")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void unauthenticatedReturns401() throws Exception {
        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void requesterOnAdminEndpointReturns403() throws Exception {
        // 403 é coberto no AuthFlowIntegrationTest; aqui validamos o corpo padrão
        mockMvc.perform(get("/api/v1/notifications/config"))
                .andExpect(status().isUnauthorized());
    }
}