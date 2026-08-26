package vgandolfi.dev.mana_paes.infrastructure.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

/**
 * Resposta 401 em JSON para requisições não autenticadas em endpoints protegidos.
 */
@Component
public class RestAuthenticationEntryPoint extends AbstractRestErrorHandler implements AuthenticationEntryPoint {

    public RestAuthenticationEntryPoint(ObjectMapper objectMapper) {
        super(objectMapper);
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
            throws IOException {
        writeError(response, HttpStatus.UNAUTHORIZED.value(), "Não autenticado");
    }
}