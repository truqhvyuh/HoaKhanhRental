package server.components.errors;
public final class TooLargeException extends DomainException {
    public TooLargeException(String message) { super("TOO_LARGE", message); }
}
