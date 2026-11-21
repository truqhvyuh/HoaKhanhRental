package server.components.functions;

import server.components.errors.UnauthorizedException;
import server.data.Database;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class NotificationService {
    private final Database database;

    public NotificationService(Database database) {
        this.database = database;
    }

    public List<Map<String, Object>> list(long caller) throws Exception {
        List<Map<String, Object>> result = new ArrayList<>();
        try (PreparedStatement ps = database.connection().prepareStatement(
                "SELECT id,type,notification_key,text_line,read_flag,created_at FROM notifications " +
                        "WHERE user_id=? ORDER BY id DESC LIMIT 50")) {
            ps.setLong(1, caller);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(Map.of("id", rs.getLong("id"), "type", rs.getString("type"),
                            "key", rs.getString("notification_key"), "text", rs.getString("text_line"),
                            "read", rs.getInt("read_flag") == 1, "createdAt", rs.getString("created_at")));
                }
            }
        }
        return result;
    }

    public void markRead(long caller, long id) throws Exception {
        try (PreparedStatement ps = database.connection().prepareStatement(
                "UPDATE notifications SET read_flag=1 WHERE id=? AND user_id=?")) {
            ps.setLong(1, id);
            ps.setLong(2, caller);
            ps.executeUpdate();
        }
    }

    public void requireLogin(Long caller) {
        if (caller == null) throw new UnauthorizedException("Not logged in.");
    }
}
