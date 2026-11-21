package server.components.session;

import org.junit.jupiter.api.Test;
import server.components.errors.InvalidInputException;

import static org.junit.jupiter.api.Assertions.*;

class PasswordHasherTest {
    @Test
    void hashesAndVerifiesPasswords() {
        PasswordHasher hasher = new PasswordHasher();
        String hash = hasher.hash("Strong!Password1");
        assertTrue(hasher.matches("Strong!Password1", hash));
        assertFalse(hasher.matches("Wrong!Password1", hash));
    }

    @Test
    void rejectsPolicyViolationsWithDetails() {
        PasswordHasher hasher = new PasswordHasher();
        InvalidInputException exception = assertThrows(InvalidInputException.class,
                () -> hasher.validate("lowercase", "valid_login", "test@example.com"));
        assertNotNull(exception.details());
    }
}
