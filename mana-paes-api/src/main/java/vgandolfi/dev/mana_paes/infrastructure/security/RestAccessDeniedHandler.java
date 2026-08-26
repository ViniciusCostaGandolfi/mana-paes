package vgandolfi.dev.mana_paes.infrastructure.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

/**
 * Resposta 403 em JSON para usuários autenticados sem permissão.
 */
@Component
public class RestAccessDeniedHandler extends AbstractRestErrorHandler implements AccessDeniedHandler {

    public RestAccessDeniedHandler(ObjectMapper objectMapper) {
        super(objectMapper);
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException)
            throws IOException {
        writeError(response, HttpStatus.FORBIDDEN.value(), "Acesso negado");
    }
}