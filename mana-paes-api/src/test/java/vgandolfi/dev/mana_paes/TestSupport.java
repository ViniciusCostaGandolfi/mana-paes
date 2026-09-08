package vgandolfi.dev.mana_paes;

import com.jayway.jsonpath.JsonPath;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Helpers de apoio aos testes de integração.
 *
 * <p>O primeiro {@code SYS_ADMIN} é criado pelo {@code SystemAdminInitializer}
 * a partir do env ({@code app.system-admin.email/password}), em todos os
 * perfis — nos testes usa as credenciais default do perfil dev. Admins são
 * criados pelo SYS_ADMIN em tenants novos; vendedores (sem login) são
 * cadastrados pelo ADMIN via {@code /api/v1/vendors}.</p>
 */
final class TestSupport {

    static final String SYS_ADMIN_EMAIL = "vinicius@vgandolfi.dev";
    static final String SYS_ADMIN_PASSWORD = "admin123";

    private TestSupport() {
    }

    /**
     * Login como o SYS_ADMIN criado pelo {@code SystemAdminInitializer}.
     */
    static String sysAdminToken(MockMvc mockMvc) throws Exception {
        MvcResult login = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + SYS_ADMIN_EMAIL + "\",\"password\":\""
                                + SYS_ADMIN_PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.read(login.getResponse().getContentAsString(StandardCharsets.UTF_8), "$.accessToken");
    }

    /**
     * SYS_ADMIN cria um ADMIN em um tenant novo e retorna o access token dele.
     */
    static String createAdminAndGetToken(MockMvc mockMvc, String sysAdminToken,
                                         String name, String email, String password) throws Exception {
        mockMvc.perform(post("/api/v1/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + sysAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\",\"email\":\"" + email
                                + "\",\"password\":\"" + password + "\",\"role\":\"ROLE_ADMIN\"}"))
                .andExpect(status().isCreated());

        MvcResult login = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.read(login.getResponse().getContentAsString(StandardCharsets.UTF_8), "$.accessToken");
    }

    /**
     * Login do SYS_ADMIN + ADMIN novo em tenant isolado (e-mail fixo por teste).
     */
    static String adminToken(MockMvc mockMvc, String email, String password) throws Exception {
        String sysAdminToken = sysAdminToken(mockMvc);
        return createAdminAndGetToken(mockMvc, sysAdminToken, "Admin " + email, email, password);
    }

    /**
     * ADMIN cadastra um vendedor e retorna o id dele.
     */
    static String createVendor(MockMvc mockMvc, String adminToken, String name, String phone) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/vendors")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\",\"phone\":\"" + phone + "\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(StandardCharsets.UTF_8), "$.id");
    }
}