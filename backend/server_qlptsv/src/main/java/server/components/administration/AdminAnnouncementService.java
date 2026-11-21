package server.components.administration;

import server.components.errors.*;
import server.data.Database;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Clock;
import java.time.Instant;
import java.util.*;

public final class AdminAnnouncementService {
    private final Database database;
    private final Clock clock;

    public AdminAnnouncementService(Database database, Clock clock) {
        this.database = database;
        this.clock = clock;
    }

    public List<Map<String, Object>> publicList() throws Exception {
        List<Map<String, Object>> result = new ArrayList<>();
        try (PreparedStatement ps = database.connection().prepareStatement(
                "SELECT id,title,body,pinned,created_at FROM announcements ORDER BY pinned DESC, id DESC LIMIT 50")) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) result.add(Map.of("id", rs.getLong("id"), "title", rs.getString("title"),
                        "body", rs.getString("body"), "pinned", rs.getInt("pinned") == 1,
                        "createdAt", rs.getString("created_at")));
            }
        }
        return result;
    }

    public long create(Long caller, Map<String, Object> body) throws Exception {
        requireModerator(caller);
        String title = Objects.toString(body.get("title"), "");
        String text = Objects.toString(body.get("body"), "");
        if (title.isBlank() || title.length() > 100 || text.isBlank() || text.length() > 5000) {
            throw new InvalidInputException("INVALID_ANNOUNCEMENT", "Announcement title or body is invalid.");
        }
        String now = Instant.now(clock).toString();
        try (PreparedStatement ps = database.connection().prepareStatement(
                "INSERT INTO announcements(author_id,title,body,pinned,created_at,updated_at) VALUES(?,?,?,?,?,?)",
                PreparedStatement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, caller);
            ps.setString(2, title);
            ps.setString(3, text);
            ps.setInt(4, Boolean.TRUE.equals(body.get("pinned")) ? 1 : 0);
            ps.setString(5, now);
            ps.setString(6, now);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) { keys.next(); return keys.getLong(1); }
        }
    }

    private void requireModerator(Long caller) throws Exception {
        if (caller == null) throw new UnauthorizedException("Not logged in.");
        try (PreparedStatement ps = database.connection().prepareStatement(
                "SELECT COUNT(*) FROM admin_permissions WHERE user_id=?")) {
            ps.setLong(1, caller);
            try (ResultSet rs = ps.executeQuery()) { rs.next(); if (rs.getInt(1) == 0) throw new ForbiddenException("NOT_MODERATOR", "Moderator permission required."); }
        }
    }
}
