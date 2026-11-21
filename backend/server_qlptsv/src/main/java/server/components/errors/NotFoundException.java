package server.components.errors;
public final class NotFoundException extends DomainException {
    public NotFoundException(String message) { super("NOT_FOUND", message); }
}
