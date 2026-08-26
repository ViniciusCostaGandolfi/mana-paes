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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Fluxo de notificações (Fase 4): config por tenant, envio de teste WhatsApp
 * (falha graciosa sem Evolution API em test) com NotificationLog, e webhook da
 * Evolution API respondendo 200.
 *
 * <p>Roda com {@code app.notifications.enabled=false} (default do perfil test):
 * sem RabbitMQ — o endpoint de teste e o webhook não dependem da fila.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
class NotificationFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void configTestWhatsAppAndLogs() throws Exception {
        String adminToken = TestSupport.registerAndGetToken(mockMvc, "dona.notificacoes@example.com", "senha123");

        // GET config cria a config padrão quando ausente
        mockMvc.perform(get("/api/v1/notifications/config")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.whatsappEnabled").value(false))
                .andExpect(jsonPath("$.emailEnabled").value(true))
                .andExpect(jsonPath("$.dailyReportTime").value("18:00:00"));

        // PUT config atualiza (parcial)
        mockMvc.perform(put("/api/v1/notifications/config")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"adminWhatsappNumber":"5511999999999","adminEmail":"dona@example.com","whatsappEnabled":true}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.adminWhatsappNumber").value("5511999999999"))
                .andExpect(jsonPath("$.whatsappEnabled").value(true))
                .andExpect(jsonPath("$.evolutionApiKeyConfigured").value(false));

        // POST whatsapp/test: sem Evolution API configurada em test -> falha
        // graciosa (200 + mensagem), NotificationLog registrado como FAILED
        mockMvc.perform(post("/api/v1/notifications/whatsapp/test")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").isNotEmpty());

        // GET logs com filtro por status mostra o registro do teste
        mockMvc.perform(get("/api/v1/notifications/logs?status=FAILED")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].channel").value("WHATSAPP"))
                .andExpect(jsonPath("$.content[0].type").value("TEST"))
                .andExpect(jsonPath("$.content[0].status").value("FAILED"))
                .andExpect(jsonPath("$.content[0].recipient").value("5511999999999"))
                .andExpect(jsonPath("$.content[0].errorMessage").isNotEmpty());

        // requester (não-ADMIN) não pode ver config
        // (token de um requester exige fluxo extra; aqui validamos via endpoint protegido)
        mockMvc.perform(get("/api/v1/notifications/config"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void evolutionWebhookAlwaysAnswers200() throws Exception {
        mockMvc.perform(post("/api/v1/webhooks/evolution-api")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"event":"CONNECTION_UPDATE","instance":"inst-1","data":{"state":"open"}}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/webhooks/evolution-api")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"event":"QRCODE_UPDATED","instance":"inst-1","data":{"qrcode":"base64..."}}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/webhooks/evolution-api")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"event":"MESSAGES_UPSERT","instance":"inst-1","data":{}}
                                """))
                .andExpect(status().isOk());

        // payload sem "event" ainda responde 200 (evita retry do Evolution)
        mockMvc.perform(post("/api/v1/webhooks/evolution-api")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"instance":"inst-1"}
                                """))
                .andExpect(status().isOk());
    }

    @Test
    void manualDailyReportSendIsIdempotent() throws Exception {
        String adminToken = TestSupport.registerAndGetToken(mockMvc, "dona.relatorio@example.com", "senha123");

        // config com canais ativos (em test os envios falham graciosamente)
        mockMvc.perform(put("/api/v1/notifications/config")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"adminWhatsappNumber":"5511999999999","adminEmail":"admin@example.com","whatsappEnabled":true}
                                """))
                .andExpect(status().isOk());

        // produto + pedido com entrega hoje (o relatório agrega por deliveryDate)
        String productId = createProduct(adminToken, "Pão do Relatório", "1.00");
        String today = LocalDate.now().toString();
        mockMvc.perform(post("/api/v1/orders")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"deliveryDate\":\"" + today + "\",\"items\":[{\"productId\":\"" + productId + "\",\"quantity\":5}]}"))
                .andExpect(status().isCreated());

        // primeiro disparo: dispatched=true
        mockMvc.perform(post("/api/v1/notifications/reports/daily/send?date=" + today)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.date").value(today))
                .andExpect(jsonPath("$.dispatched").value(true));

        // segundo disparo: idempotência — não reenvia no mesmo dia
        mockMvc.perform(post("/api/v1/notifications/reports/daily/send?date=" + today)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dispatched").value(false));

        // logs de DAILY_REPORT registrados (em test sem Evolution/SMTP ficam FAILED)
        mockMvc.perform(get("/api/v1/notifications/logs?status=FAILED")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].type").value("DAILY_REPORT"))
                .andExpect(jsonPath("$.content[0].recipient").isNotEmpty());
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