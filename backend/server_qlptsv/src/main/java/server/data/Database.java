package server.data;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public final class Database implements AutoCloseable {
    private final Path databaseFile;
    private Connection connection;

    public Database(Path dataDirectory) {
        databaseFile = dataDirectory.resolve("room-rental.db");
    }

    public synchronized void open() throws Exception {
        Files.createDirectories(databaseFile.getParent());
        Files.createDirectories(databaseFile.getParent().resolve("resources"));
        Files.createDirectories(databaseFile.getParent().resolve("logs"));
        Files.createDirectories(databaseFile.getParent().resolve("bin"));
        connection = DriverManager.getConnection("jdbc:sqlite:" + databaseFile);
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
            statement.execute("PRAGMA journal_mode = WAL");
        }
    }

    public synchronized Connection connection() {
        if (connection == null) {
            throw new IllegalStateException("database is closed");
        }
        return connection;
    }

    public synchronized void migrate() throws Exception {
        try (var statements = connection.createStatement()) {
            statements.execute("CREATE TABLE IF NOT EXISTS schema_version (version INTEGER NOT NULL)");
            int version = 0;
            try (var rs = statements.executeQuery("SELECT COALESCE(MAX(version), 0) FROM schema_version")) {
                if (rs.next()) version = rs.getInt(1);
            }
            if (version == 0) {
                applyMigration(statements, "/migrations/V1__initial.sql");
                version = 1;
            }
            if (version < 2) {
                applyMigration(statements, "/migrations/V2__nullable_user_email.sql");
                statements.execute("INSERT INTO schema_version(version) VALUES (2)");
            }
        }
    }

    private void applyMigration(Statement statements, String resource) throws Exception {
        try (var stream = Database.class.getResourceAsStream(resource)) {
            if (stream == null) throw new IllegalStateException("Missing migration: " + resource);
            String sql = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            for (String statement : sql.split(";")) {
                String trimmed = statement.trim();
                if (!trimmed.isEmpty()) statements.execute(trimmed);
            }
        }
    }

    public Path databaseFile() {
        return databaseFile;
    }

    @Override
    public synchronized void close() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException ignored) {
            }
            connection = null;
        }
    }
}
