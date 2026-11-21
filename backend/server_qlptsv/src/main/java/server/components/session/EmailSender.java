package server.components.session;

public interface EmailSender {
    void send(String recipient, String subject, String body) throws EmailSendException;

    final class EmailSendException extends Exception {
        public EmailSendException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
