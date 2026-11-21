package server.components.errors;

public abstract class DomainException extends RuntimeException {
    private final String code;
    private final Object details;
    private final Integer retryAfterSeconds;

    protected DomainException(String code, String message) {
        this(code, message, null, null);
    }

    protected DomainException(String code, String message, Object details, Integer retryAfterSeconds) {
        super(message);
        this.code = code;
        this.details = details;
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public String code() { return code; }
    public Object details() { return details; }
    public Integer retryAfterSeconds() { return retryAfterSeconds; }
}
