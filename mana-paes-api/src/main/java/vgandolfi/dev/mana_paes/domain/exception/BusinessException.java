package vgandolfi.dev.mana_paes.domain.exception;

/**
 * Erro de regra de negócio mapeado para HTTP 400.
 */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }
}