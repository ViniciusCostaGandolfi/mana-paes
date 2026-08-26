package vgandolfi.dev.mana_paes;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Fluxo completo da conexão WhatsApp com o MOCK ativo (default em test:
 * {@code app.evolution.url} em branco): connect → CONNECTING + QR fake →
 * simulate-scan → OPEN com número → test → disconnect → CLOSE.
 *
 * <p>O delay de auto-transição do mock é elevado (600s) para que o teste
 * controle a abertura via {@code simulate-scan} de forma determinística.</p>
 */
@SpringBootTest(properties = "app.evolution.mock-connect-delay-ms=600000")
@AutoConfigureMockMvc
class WhatsAppFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void connectSimulateTestAndDisconnect() throws Exception {
        String adminToken = TestSupport.registerAndGetToken(mockMvc, "dona.whatsapp@example.com", "senha123");

        // connect -> CONNECTING com QR fake (data URI SVG)
        mockMvc.perform(post("/api/v1/whatsapp/connect")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("CONNECTING"))
                .andExpect(jsonPath("$.qrCodeBase64").value(org.hamcrest.Matchers.startsWith("data:image/svg+xml;base64,")));

        // status segue CONNECTING
        mockMvc.perform(get("/api/v1/whatsapp/status")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("CONNECTING"))
                .andExpect(jsonPath("$.connectedNumber").isEmpty());

        // simulate-scan (exclusivo do mock) -> OPEN com número fixo
        mockMvc.perform(post("/api/v1/whatsapp/simulate-scan")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("OPEN"))
                .andExpect(jsonPath("$.connectedNumber").value("5511999999999"));

        // status agora OPEN
        mockMvc.perform(get("/api/v1/whatsapp/status")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("OPEN"))
                .andExpect(jsonPath("$.connectedNumber").value("5511999999999"));

        // test -> mensagem enviada (mock, sem chamada externa)
        mockMvc.perform(post("/api/v1/whatsapp/test")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").isNotEmpty());

        // disconnect -> CLOSE
        mockMvc.perform(post("/api/v1/whatsapp/disconnect")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").isNotEmpty());

        mockMvc.perform(get("/api/v1/whatsapp/status")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("CLOSE"))
                .andExpect(jsonPath("$.connectedNumber").isEmpty());
    }

    @Test
    void whatsappEndpointsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/whatsapp/status"))
                .andExpect(status().isUnauthorized());
    }
}