package server.components.session;

import java.util.Map;

public record SmtpSettings(
        String host,
        int port,
        String username,
        String password,
        String from,
        String fromName) {

    public boolean isConfigured() {
        return host != null && !host.isBlank()
                && port > 0
                && from != null && !from.isBlank();
    }

    public String state() {
        return isConfigured() ? "email: configured" : "email: not configured";
    }

    public static SmtpSettings from(Map<String, String> options) {
        String host = precedence(options, "--smtp-host", "SMTP_HOST");
        String portText = precedence(options, "--smtp-port", "SMTP_PORT");
        int port;
        try {
            port = Integer.parseInt(portText);
        } catch (Exception exception) {
            port = 0;
        }
        String password = precedence(options, "--smtp-password", "SMTP_PASSWORD");
        return new SmtpSettings(
                host,
                port,
                precedence(options, "--smtp-user", "SMTP_USER"),
                password,
                precedence(options, "--smtp-from", "SMTP_FROM"),
                precedence(options, "--smtp-from-name", "SMTP_FROM_NAME"));
    }

    private static String precedence(Map<String, String> options, String flag, String environmentName) {
        if (options.containsKey(flag) && options.get(flag) != null && !options.get(flag).isBlank()
                && !"true".equals(options.get(flag))) {
            return options.get(flag).trim();
        }
        String environment = System.getenv(environmentName);
        return environment == null ? "" : environment.trim();
    }
}
