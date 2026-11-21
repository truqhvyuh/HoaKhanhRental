package server.components.session;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SmtpSettingsTest {
    @Test
    void requiresHostPortAndFrom() {
        assertFalse(new SmtpSettings("", 587, "", "", "", "").isConfigured());
        assertFalse(new SmtpSettings("smtp.example.test", 0, "", "", "", "").isConfigured());
        assertFalse(new SmtpSettings("smtp.example.test", 587, "", "", "", "").isConfigured());
        assertTrue(new SmtpSettings("smtp.example.test", 587, "", "", "noreply@example.test", "").isConfigured());
    }

    @Test
    void flagHostAndPortOverrideEmptyEnvironmentValues() {
        SmtpSettings settings = SmtpSettings.from(Map.of(
                "--smtp-host", "127.0.0.1",
                "--smtp-port", "2525",
                "--smtp-from", "sender@example.test"));
        assertEquals("127.0.0.1", settings.host());
        assertEquals(2525, settings.port());
        assertEquals("sender@example.test", settings.from());
    }
}
