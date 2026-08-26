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
import org.springframework.transaction.annotation.Transactional;
import vgandolfi.dev.mana_paes.domain.model.PasswordResetToken;
import vgandolfi.dev.mana_paes.domain.repository.PasswordResetTokenRepository;

import java.nio.charset.StandardCharsets;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Smoke test de integração do fluxo de autenticação e autorização:
 * register → login → acesso autenticado a /api/v1/users, 401 sem token,
 * 403 para REQUESTER em endpoint de ADMIN, e fluxo forgot/reset de senha.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AuthFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Test
    void registerLoginAndListUsers() throws Exception {
        MvcResult registerResult = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"João da Padaria","email":"joao@example.com","password":"senha123","phone":"11988887777","whatsappNumber":"11988887777"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.user.email").value("joao@example.com"))
                .andExpect(jsonPath("$.user.role").value("ROLE_ADMIN"))
                .andReturn();

        String accessToken = JsonPath.read(
                registerResult.getResponse().getContentAsString(StandardCharsets.UTF_8), "$.accessToken");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"joao@example.com\",\"password\":\"senha123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.user.email").value("joao@example.com"));

        mockMvc.perform(get("/api/v1/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].email").value("joao@example.com"));

        // sem token, endpoint protegido retorna 401
        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginWithWrongPasswordReturns401() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Maria Oliveira\",\"email\":\"maria@example.com\",\"password\":\"senha123\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"maria@example.com\",\"password\":\"senha-errada\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Credenciais inválidas"));
    }

    @Test
    @Transactional
    void requesterGetsForbiddenOnAdminEndpoint() throws Exception {
        // 1. admin registra (auto-cria tenant) e cria um ROLE_REQUESTER no mesmo tenant
        MvcResult adminResult = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Dona Padaria\",\"email\":\"dona@example.com\",\"password\":\"senha123\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        String adminToken = JsonPath.read(
                adminResult.getResponse().getContentAsString(StandardCharsets.UTF_8), "$.accessToken");

        mockMvc.perform(post("/api/v1/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Funcionario\",\"email\":\"funcionario@example.com\",\"role\":\"ROLE_REQUESTER\"}"))
                .andExpect(status().isCreated());

        // 2. admin dispara recuperação de senha do REQUESTER (email desabilitado em dev: token fica no banco)
        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"funcionario@example.com\"}"))
                .andExpect(status().isOk());

        PasswordResetToken resetToken = passwordResetTokenRepository.findAll().stream()
                .filter(t -> "funcionario@example.com".equals(t.getUser().getEmail()) && !t.isUsed())
                .findFirst()
                .orElseThrow();

        // 3. define senha conhecida para o requester
        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + resetToken.getToken() + "\",\"password\":\"nova123\"}"))
                .andExpect(status().isOk());

        // 4. login como REQUESTER e tenta listar usuários -> 403
        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"funcionario@example.com\",\"password\":\"nova123\"}"))
                .andExpect(status().isOk())
                .andReturn();
        String requesterToken = JsonPath.read(
                loginResult.getResponse().getContentAsString(StandardCharsets.UTF_8), "$.accessToken");

        mockMvc.perform(get("/api/v1/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + requesterToken))
                .andExpect(status().isForbidden());
    }
}