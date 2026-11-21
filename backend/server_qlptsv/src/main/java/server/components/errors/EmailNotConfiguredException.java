package server.components.errors;

public final class EmailNotConfiguredException extends DomainException {
    public EmailNotConfiguredException() {
        super("email_not_configured", "Email is not configured. SMTP settings are required.");
    }
}
