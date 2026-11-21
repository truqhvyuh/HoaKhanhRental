package server.components.functions;

import server.components.errors.*;
import server.data.Database;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Clock;
import java.time.Instant;
import java.util.*;

public final class ApplicationService {
    private final Database database;
    private final Clock clock;

    public ApplicationService(Database database, Clock clock) {
        this.database = database;
        this.clock = clock;
    }

    public long create(Long caller, Map<String, Object> body) throws Exception {
        if (caller == null) throw new UnauthorizedException("Not logged in.");
        long listingId = Long.parseLong(String.valueOf(body.get("listingId")));
        boolean asTeam = Boolean.TRUE.equals(body.get("asTeam"));
        try (PreparedStatement ps = database.connection().prepareStatement(
                "SELECT status,capacity_available,owner_type,owner_user_id,organization_id,posted_by FROM listings WHERE id=?")) {
            ps.setLong(1, listingId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new NotFoundException("Listing not found.");
                if (!"published".equals(rs.getString("status"))) throw new ConflictException("LISTING_NOT_PUBLISHED", "Listing is not published.");
                if (rs.getInt("capacity_available") <= 0) throw new ConflictException("LISTING_FULL", "Listing is full.");
                if ("user".equals(rs.getString("owner_type")) && rs.getLong("owner_user_id") == caller) {
                    throw new ConflictException("OWN_LISTING", "You cannot apply to your own listing.");
                }
            }
        }
        long applicantId = caller;
        String applicantType = "user";
        if (asTeam) {
            try (PreparedStatement ps = database.connection().prepareStatement(
                    "SELECT t.id,t.leader_id FROM group_memberships m JOIN teams t ON t.id=m.group_id " +
                            "WHERE m.user_id=? AND m.group_type='team' AND t.status='active'")) {
                ps.setLong(1, caller);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) throw new ConflictException("NO_TEAM", "You are not in a team.");
                    if (rs.getLong("leader_id") != caller) throw new ForbiddenException("NOT_TEAM_LEADER", "Only the team leader can apply.");
                    applicantId = rs.getLong("id");
                    applicantType = "team";
                }
            }
        }
        String now = Instant.now(clock).toString();
        try (PreparedStatement ps = database.connection().prepareStatement(
                "INSERT INTO applications(listing_id,applicant_type,applicant_id,status,note,party_size,capacity_adjusted,created_at,updated_at) " +
                        "VALUES(?,?,'pending',?,1,0,?,?)", PreparedStatement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, listingId);
            ps.setString(2, applicantType);
            ps.setLong(3, applicantId);
            ps.setString(4, Objects.toString(body.get("note"), ""));
            ps.setString(5, now);
            ps.setString(6, now);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) { keys.next(); return keys.getLong(1); }
        }
    }

    public List<Map<String, Object>> mine(Long caller) throws Exception {
        if (caller == null) throw new UnauthorizedException("Not logged in.");
        List<Map<String, Object>> result = new ArrayList<>();
        try (PreparedStatement ps = database.connection().prepareStatement(
                "SELECT * FROM applications WHERE applicant_type='user' AND applicant_id=? ORDER BY id DESC")) {
            ps.setLong(1, caller);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(Map.of("id", rs.getLong("id"), "listingId", rs.getLong("listing_id"),
                            "status", rs.getString("status"), "note", Objects.toString(rs.getString("note"), ""),
                            "partySize", rs.getInt("party_size"), "createdAt", rs.getString("created_at")));
                }
            }
        }
        return result;
    }

    public void withdraw(long id, Long caller) throws Exception {
        if (caller == null) throw new UnauthorizedException("Not logged in.");
        try (PreparedStatement ps = database.connection().prepareStatement(
                "UPDATE applications SET status='withdrawn',ended_at=?,updated_at=? " +
                        "WHERE id=? AND applicant_type='user' AND applicant_id=? AND status IN ('pending','accepted')")) {
            ps.setString(1, Instant.now(clock).toString());
            ps.setString(2, Instant.now(clock).toString());
            ps.setLong(3, id);
            ps.setLong(4, caller);
            if (ps.executeUpdate() == 0) throw new NotFoundException("Application not found.");
        }
    }
}
