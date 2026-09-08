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
import vgandolfi.dev.mana_paes.domain.model.enums.NotificationType;
import vgandolfi.dev.mana_paes.domain.repository.NotificationLogRepository;

import java.nio.charset.StandardCharsets;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Fluxo de lembretes agendados: ADMIN cria/liste/atualiza/exclui lembretes do
 * tenant e dispara o envio de teste para os vendedores ativos; sem token o
 * recurso retorna 401.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ScheduledReminderFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private NotificationLogRepository logRepository;

    @Test
    void adminCrudAndTestReminderWithVendorRecipient() throws Exception {
        String adminToken = TestSupport.adminToken(mockMvc, "dona.lembretes@example.com", "senha123");
        TestSupport.createVendor(mockMvc, adminToken, "Everton", "5511988111002");

        // cria lembrete
        MvcResult createResult = mockMvc.perform(post("/api/v1/reminders")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Lembrete de pedidos","message":"Faça seus pedidos","reminderTime":"09:00","whatsappEnabled":true,"emailEnabled":false,"deliveryDayOffset":1}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Lembrete de pedidos"))
                .andExpect(jsonPath("$.reminderTime").value("09:00:00"))
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.whatsappEnabled").value(true))
                .andExpect(jsonPath("$.deliveryDayOffset").value(1))
                .andReturn();
        String reminderId = JsonPath.read(
                createResult.getResponse().getContentAsString(StandardCharsets.UTF_8), "$.id");

        // lista do tenant contém o lembrete
        mockMvc.perform(get("/api/v1/reminders")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].id").value(reminderId));

        // GET por id
        mockMvc.perform(get("/api/v1/reminders/" + reminderId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(reminderId));

        // PUT atualiza
        mockMvc.perform(put("/api/v1/reminders/" + reminderId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Lembrete vespertino","message":"Faça seus pedidos para amanhã","reminderTime":"16:00","enabled":true,"whatsappEnabled":true,"emailEnabled":false,"deliveryDayOffset":2}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Lembrete vespertino"))
                .andExpect(jsonPath("$.deliveryDayOffset").value(2));

        // disparo imediato: 1 vendedor ativo recebe (canais falham graciosamente sem infra em test)
        mockMvc.perform(post("/api/v1/reminders/" + reminderId + "/test")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recipients").value(1));

        // NotificationLog do tipo REMINDER registrado
        org.assertj.core.api.Assertions.assertThat(logRepository.findAll())
                .anyMatch(entry -> entry.getType() == NotificationType.REMINDER);

        // DELETE remove
        mockMvc.perform(delete("/api/v1/reminders/" + reminderId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/reminders")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty());
    }

    @Test
    void unauthenticatedCannotAccessReminders() throws Exception {
        mockMvc.perform(get("/api/v1/reminders"))
                .andExpect(status().isUnauthorized());
    }
}