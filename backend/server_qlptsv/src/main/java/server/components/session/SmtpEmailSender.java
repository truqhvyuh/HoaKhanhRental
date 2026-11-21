package server.components.session;

import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.util.Properties;

public final class SmtpEmailSender implements EmailSender {
    private final SmtpSettings settings;

    public SmtpEmailSender(SmtpSettings settings) {
        this.settings = settings;
    }

    @Override
    public void send(String recipient, String subject, String body) throws EmailSendException {
        if (!settings.isConfigured()) {
            throw new EmailSendException("SMTP is not configured", null);
        }
        Properties properties = new Properties();
        properties.put("mail.transport.protocol", "smtp");
        properties.put("mail.smtp.host", settings.host());
        properties.put("mail.smtp.port", Integer.toString(settings.port()));
        properties.put("mail.smtp.auth", !settings.username().isBlank() || !settings.password().isBlank());
        properties.put("mail.smtp.connectiontimeout", "10000");
        properties.put("mail.smtp.timeout", "10000");
        properties.put("mail.smtp.writetimeout", "10000");
        if (settings.port() == 465) {
            properties.put("mail.smtp.ssl.enable", "true");
        } else if (settings.port() == 587) {
            properties.put("mail.smtp.starttls.enable", "true");
        }

        Session session = Session.getInstance(properties, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(settings.username(), settings.password());
            }
        });
        try {
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(settings.from(),
                    settings.fromName() == null ? "" : settings.fromName()));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipient));
            message.setSubject(subject);
            message.setText(body);
            Transport.send(message);
        } catch (Exception exception) {
            throw new EmailSendException("SMTP send failed", exception);
        }
    }
}
