package vgandolfi.dev.mana_paes;

import com.jayway.jsonpath.JsonPath;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import vgandolfi.dev.mana_paes.domain.model.PasswordResetToken;
import vgandolfi.dev.mana_paes.domain.repository.PasswordResetTokenRepository;

import java.nio.charset.StandardCharsets;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Helpers de apoio aos testes de integração (fluxo de autenticação).
 */
final class TestSupport {

    private TestSupport() {
    }

    /**
     * Registra um novo admin (auto-cria tenant) e retorna o access token.
     */
    static String registerAndGetToken(MockMvc mockMvc, String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Teste\",\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(StandardCharsets.UTF_8), "$.accessToken");
    }

    /**
     * Admin cria um usuário ROLE_REQUESTER no mesmo tenant, define uma senha
     * conhecida via fluxo forgot/reset-password e retorna o access token dele.
     */
    static String createRequesterAndGetToken(MockMvc mockMvc, PasswordResetTokenRepository tokenRepository,
                                             String adminToken, String name, String email, String password)
            throws Exception {
        mockMvc.perform(post("/api/v1/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\",\"email\":\"" + email + "\",\"role\":\"ROLE_REQUESTER\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\"}"))
                .andExpect(status().isOk());

        PasswordResetToken resetToken = tokenRepository
                .findFirstByUser_EmailAndUsedFalseOrderByCreatedAtDesc(email)
                .orElseThrow();

        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + resetToken.getToken() + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk());

        MvcResult login = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.read(login.getResponse().getContentAsString(StandardCharsets.UTF_8), "$.accessToken");
    }
}