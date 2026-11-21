package server.components.errors;
public final class InvalidInputException extends DomainException {
    public InvalidInputException(String message) { super("INVALID_INPUT", message); }
    public InvalidInputException(String code, String message) { super(code, message); }
    public InvalidInputException(String code, String message, Object details) { super(code, message, details, null); }
}
