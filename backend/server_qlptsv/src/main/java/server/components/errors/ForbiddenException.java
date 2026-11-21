package server.components.errors;
public final class ForbiddenException extends DomainException {
    public ForbiddenException(String code, String message) { super(code, message); }
}
