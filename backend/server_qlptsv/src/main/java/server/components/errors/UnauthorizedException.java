package server.components.errors;
public final class UnauthorizedException extends DomainException {
    public UnauthorizedException(String message) { super("UNAUTHORIZED", message); }
}
