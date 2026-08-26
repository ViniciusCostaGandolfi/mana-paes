package vgandolfi.dev.mana_paes.infrastructure.security;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import tools.jackson.databind.ObjectMapper;
import vgandolfi.dev.mana_paes.api.exception.ApiError;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Base para os handlers de erro de segurança: serializa {@link ApiError} como JSON.
 * Não é um bean — evita ambiguidade de tipos entre entry point e access denied handler.
 */
public abstract class AbstractRestErrorHandler {

    private final ObjectMapper objectMapper;

    protected AbstractRestErrorHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    protected void writeError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getWriter(), ApiError.of(status, message));
    }
}