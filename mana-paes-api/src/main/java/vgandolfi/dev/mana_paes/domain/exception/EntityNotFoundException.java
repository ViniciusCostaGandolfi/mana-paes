package vgandolfi.dev.mana_paes.domain.exception;

public class EntityNotFoundException extends RuntimeException {

    public EntityNotFoundException(String message) {
        super(message);
    }

    public static EntityNotFoundException of(String entity, Object id) {
        return new EntityNotFoundException(entity + " não encontrado(a): " + id);
    }
}