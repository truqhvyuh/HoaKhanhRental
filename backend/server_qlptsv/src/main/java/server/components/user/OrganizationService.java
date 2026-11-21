package server.components.user;

import server.components.errors.*;
import server.data.Database;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Clock;
import java.time.Instant;
import java.util.*;

public final class OrganizationService {
    private final Database database;
    private final Clock clock;

    public OrganizationService(Database database, Clock clock) {
        this.database = database;
        this.clock = clock;
    }

    public long create(Long caller, Map<String, Object> body) throws Exception {
        if (caller == null) throw new UnauthorizedException("Not logged in.");
        String name = Objects.toString(body.get("name"), "");
        if (name.length() < 3 || name.length() > 50) {
            throw new InvalidInputException("INVALID_ORGANIZATION_NAME", "Organization name must be 3 to 50 characters.");
        }
        try (PreparedStatement ps = database.connection().prepareStatement(
                "SELECT COUNT(*) FROM group_memberships WHERE user_id=?")) {
            ps.setLong(1, caller);
            try (ResultSet rs = ps.executeQuery()) { rs.next(); if (rs.getInt(1) > 0) throw new ConflictException("ALREADY_IN_GROUP", "You are already in a group."); }
        }
        database.connection().setAutoCommit(false);
        try {
            long id;
            String now = Instant.now(clock).toString();
            try (PreparedStatement ps = database.connection().prepareStatement(
                    "INSERT INTO organizations(name,description,owner_id,contact_email,phone,address,status,created_at) " +
                            "VALUES(?,?,?,?,?,?,'active',?)", PreparedStatement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, name);
                ps.setString(2, Objects.toString(body.get("description"), ""));
                ps.setLong(3, caller);
                ps.setString(4, Objects.toString(body.get("contactEmail"), ""));
                ps.setString(5, Objects.toString(body.get("phone"), ""));
                ps.setString(6, Objects.toString(body.get("address"), ""));
                ps.setString(7, now);
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) { keys.next(); id = keys.getLong(1); }
            }
            try (PreparedStatement ps = database.connection().prepareStatement(
                    "INSERT INTO group_memberships(user_id,group_type,group_id,joined_at) VALUES(?, 'organization', ?, ?)")) {
                ps.setLong(1, caller);
                ps.setLong(2, id);
                ps.setString(3, now);
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
        try (PreparedStatement ps = database.connection().prepareStatement("SELECT * FROM organizations WHERE id=?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new NotFoundException("Organization not found.");
                Map<String, Object> result = new LinkedHashMap<>();
                result.put("id", rs.getLong("id"));
                result.put("name", rs.getString("name"));
                result.put("description", rs.getString("description"));
                result.put("ownerId", rs.getObject("owner_id"));
                result.put("contactEmail", rs.getString("contact_email"));
                result.put("phone", rs.getString("phone"));
                result.put("address", rs.getString("address"));
                result.put("status", rs.getString("status"));
                return result;
            }
        }
    }
}
