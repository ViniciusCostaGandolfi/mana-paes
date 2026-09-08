package vgandolfi.dev.mana_paes.domain.exception;

/**
 * Conflito de recurso (ex.: bootstrap já realizado), mapeado para HTTP 409.
 * Herda de {@link BusinessException} — é tratada de forma específica pelo
 * {@code GlobalExceptionHandler} (409 em vez de 400).
 */
public class ConflictException extends BusinessException {

    public ConflictException(String message) {
        super(message);
    }
}