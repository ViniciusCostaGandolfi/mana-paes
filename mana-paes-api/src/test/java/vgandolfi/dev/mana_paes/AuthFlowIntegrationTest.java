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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Smoke test de integração do fluxo de autenticação e autorização:
 * login do SYS_ADMIN (criado pelo SystemAdminInitializer via env), acesso
 * autenticado a /api/v1/users, 401 sem token, e RBAC de criação de usuários
 * (apenas SYS_ADMIN cria; ADMIN recebe 400 no service).
 */
@SpringBootTest
@AutoConfigureMockMvc
class AuthFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void sysAdminLoginAndListUsers() throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"vinicius@vgandolfi.dev\",\"password\":\"admin123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.user.email").value("vinicius@vgandolfi.dev"))
                .andExpect(jsonPath("$.user.role").value("ROLE_SYS_ADMIN"))
                .andReturn();
        String accessToken = JsonPath.read(
                loginResult.getResponse().getContentAsString(StandardCharsets.UTF_8), "$.accessToken");

        mockMvc.perform(get("/api/v1/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());

        // sem token, endpoint protegido retorna 401
        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginWithWrongPasswordReturns401() throws Exception {
        // garante que o SYS_ADMIN de env existe e cria um ADMIN para o teste
        String adminToken = TestSupport.adminToken(mockMvc, "maria@example.com", "senha123");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"maria@example.com\",\"password\":\"senha-errada\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Credenciais inválidas"));

        // admin logado consegue acessar /users do próprio tenant
        mockMvc.perform(get("/api/v1/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    void adminCannotCreateUsers() throws Exception {
        // ADMIN autenticado tenta criar um usuário -> 400 (apenas SYS_ADMIN cria)
        String adminToken = TestSupport.adminToken(mockMvc, "dona@example.com", "senha123");

        mockMvc.perform(post("/api/v1/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Outro\",\"email\":\"outro.admin@example.com\",\"password\":\"senha123\",\"role\":\"ROLE_ADMIN\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Papel não autorizado a criar usuários"));
    }
}