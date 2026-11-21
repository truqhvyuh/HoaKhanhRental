package server.data;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.PreparedStatement;

import static org.junit.jupiter.api.Assertions.*;

class DatabaseTest {
    @Test
    void migrationAllowsNullEmailAndUniqueNonNullEmail() throws Exception {
        Path directory = Files.createTempDirectory("room-rental-db");
        try (Database database = new Database(directory)) {
            database.open();
            database.migrate();
            try (PreparedStatement first = database.connection().prepareStatement(
                    "INSERT INTO users(login_name,email,email_verified,password_hash,is_seed,must_change_password,must_change_email,created_at) " +
                            "VALUES('seedOne',NULL,0,'hash',1,1,1,'2026-01-01T00:00:00Z')");
                 PreparedStatement second = database.connection().prepareStatement(
                         "INSERT INTO users(login_name,email,email_verified,password_hash,is_seed,must_change_password,must_change_email,created_at) " +
                                 "VALUES('seedTwo',NULL,0,'hash',1,1,1,'2026-01-01T00:00:00Z')");
                 PreparedStatement duplicate = database.connection().prepareStatement(
                         "INSERT INTO users(login_name,email,email_verified,password_hash,is_seed,must_change_password,must_change_email,created_at) " +
                                 "VALUES('otherOne','User@example.test',1,'hash',0,0,0,'2026-01-01T00:00:00Z')");
                 PreparedStatement duplicateTwo = database.connection().prepareStatement(
                         "INSERT INTO users(login_name,email,email_verified,password_hash,is_seed,must_change_password,must_change_email,created_at) " +
                                 "VALUES('otherTwo','user@example.TEST',1,'hash',0,0,0,'2026-01-01T00:00:00Z')")) {
                first.executeUpdate();
                second.executeUpdate();
                duplicate.executeUpdate();
                assertThrows(java.sql.SQLException.class, duplicateTwo::executeUpdate);
            }
        }
    }
}
