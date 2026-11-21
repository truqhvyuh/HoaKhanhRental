package server.components.user;

import server.components.errors.*;
import server.data.Database;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Clock;
import java.time.Instant;
import java.util.*;

public final class TeamService {
    private final Database database;
    private final Clock clock;

    public TeamService(Database database, Clock clock) {
        this.database = database;
        this.clock = clock;
    }

    public long create(Long caller, String name) throws Exception {
        if (caller == null) throw new UnauthorizedException("Not logged in.");
        validateName(name);
        ensureNoGroup(caller);
        database.connection().setAutoCommit(false);
        try {
            long id;
            String now = Instant.now(clock).toString();
            try (PreparedStatement ps = database.connection().prepareStatement(
                    "INSERT INTO teams(name,leader_id,status,created_at) VALUES(?,?, 'active',?)",
                    PreparedStatement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, name);
                ps.setLong(2, caller);
                ps.setString(3, now);
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) { keys.next(); id = keys.getLong(1); }
            }
            try (PreparedStatement ps = database.connection().prepareStatement(
                    "INSERT INTO group_memberships(user_id,group_type,group_id,joined_at) VALUES(?,?,?,?)")) {
                ps.setLong(1, caller);
                ps.setString(2, "team");
                ps.setLong(3, id);
                ps.setString(4, now);
                ps.executeUpdate();
            }
            database.connection().commit();
            return id;
        } catch (Exception exception) {
            database.connection().rollback();
            throw exception;
        } finally {
            database.connection().setAutoCommit(true);
        }
    }

    public Map<String, Object> get(long id) throws Exception {
        try (PreparedStatement ps = database.connection().prepareStatement(
                "SELECT t.id,t.name,t.leader_id,t.status,u.login_name FROM teams t JOIN users u ON u.id=t.leader_id WHERE t.id=?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new NotFoundException("Team not found.");
                Map<String, Object> result = new LinkedHashMap<>();
                result.put("id", rs.getLong("id"));
                result.put("name", rs.getString("name"));
                result.put("leaderId", rs.getLong("leader_id"));
                result.put("leaderName", rs.getString("login_name"));
                result.put("status", rs.getString("status"));
                result.put("members", members(id));
                return result;
            }
        }
    }

    public void disband(long id, Long caller) throws Exception {
        requireLeader(id, caller);
        database.connection().setAutoCommit(false);
        try {
            try (PreparedStatement ps = database.connection().prepareStatement(
                    "UPDATE teams SET status='disbanded' WHERE id=?")) {
                ps.setLong(1, id);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = database.connection().prepareStatement(
                    "DELETE FROM group_memberships WHERE group_type='team' AND group_id=?")) {
                ps.setLong(1, id);
                ps.executeUpdate();
            }
            database.connection().commit();
        } catch (Exception exception) {
            database.connection().rollback();
            throw exception;
        } finally {
            database.connection().setAutoCommit(true);
        }
    }

    private List<Map<String, Object>> members(long teamId) throws Exception {
        List<Map<String, Object>> members = new ArrayList<>();
        try (PreparedStatement ps = database.connection().prepareStatement(
                "SELECT u.id,u.login_name,m.joined_at FROM group_memberships m JOIN users u ON u.id=m.user_id " +
                        "WHERE m.group_type='team' AND m.group_id=? ORDER BY m.joined_at,u.id")) {
            ps.setLong(1, teamId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) members.add(Map.of("id", rs.getLong("id"), "loginName", rs.getString("login_name")));
            }
        }
        return members;
    }

    private void requireLeader(long id, Long caller) throws Exception {
        if (caller == null) throw new UnauthorizedException("Not logged in.");
        try (PreparedStatement ps = database.connection().prepareStatement(
                "SELECT leader_id,status FROM teams WHERE id=?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new NotFoundException("Team not found.");
                if (rs.getLong("leader_id") != caller) throw new ForbiddenException("NOT_LEADER", "Only the team leader can do this.");
                if (!"active".equals(rs.getString("status"))) throw new ConflictException("TEAM_DISBANDED", "Team is disbanded.");
            }
        }
    }

    private void ensureNoGroup(long caller) throws Exception {
        try (PreparedStatement ps = database.connection().prepareStatement(
                "SELECT COUNT(*) FROM group_memberships WHERE user_id=?")) {
            ps.setLong(1, caller);
            try (ResultSet rs = ps.executeQuery()) { rs.next(); if (rs.getInt(1) > 0) throw new ConflictException("ALREADY_IN_GROUP", "You are already in a group."); }
        }
    }

    private void validateName(String name) {
        if (name == null || name.length() < 3 || name.length() > 50) {
            throw new InvalidInputException("INVALID_TEAM_NAME", "Team name must be 3 to 50 characters.");
        }
    }
}
