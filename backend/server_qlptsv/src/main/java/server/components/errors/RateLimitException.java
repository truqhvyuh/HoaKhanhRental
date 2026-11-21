package server.components.errors;
public final class RateLimitException extends DomainException {
    public RateLimitException(String message, int retryAfterSeconds) {
        super("RATE_LIMIT", message, null, retryAfterSeconds);
    }
}
