package server.components.errors;

public final class EmailSendFailedException extends DomainException {
    public EmailSendFailedException() {
        super("email_send_failed", "The email could not be sent.");
    }
}
