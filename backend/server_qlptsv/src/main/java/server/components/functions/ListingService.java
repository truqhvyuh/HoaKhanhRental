package server.components.functions;

import server.components.errors.*;
import server.data.Database;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Clock;
import java.time.Instant;
import java.util.*;

public final class ListingService {
    private final Database database;
    private final Clock clock;

    public ListingService(Database database, Clock clock) {
        this.database = database;
        this.clock = clock;
    }

    public long create(Long caller, Map<String, Object> body) throws Exception {
        if (caller == null) throw new UnauthorizedException("Not logged in.");
        requireNotTimedOut(caller);
        String ownerType = Objects.toString(body.get("ownerType"), "user");
        long postedBy = caller;
        long organizationId = 0;
        if ("organization".equals(ownerType)) {
            organizationId = parseLong(body.get("organizationId"));
            if (!canPostOrganization(caller, organizationId)) {
                throw new ForbiddenException("NO_PERMISSION", "You cannot post for this organization.");
            }
        }
        checkListingRateLimit(caller, ownerType, organizationId);
        String now = Instant.now(clock).toString();
        try (PreparedStatement ps = database.connection().prepareStatement(
                "INSERT INTO listings(owner_type,owner_user_id,organization_id,posted_by,title,description,rent_type,rent_amount,address," +
                        "contact_email,contact_phone,contact_note,capacity_total,capacity_available,status,created_at,updated_at) " +
                        "VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)", PreparedStatement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, ownerType);
            ps.setObject(2, "user".equals(ownerType) ? caller : null);
            ps.setObject(3, "organization".equals(ownerType) ? organizationId : null);
            ps.setLong(4, postedBy);
            ps.setString(5, blank(body.get("title")));
            ps.setString(6, blank(body.get("description")));
            ps.setString(7, "fixed".equals(body.get("rentType")) ? "fixed" : "negotiable");
            ps.setObject(8, parseNullableLong(body.get("rentAmount")));
            ps.setString(9, blank(body.get("address")));
            ps.setString(10, blank(body.get("contactEmail")));
            ps.setString(11, blank(body.get("contactPhone")));
            ps.setString(12, blank(body.get("contactNote")));
            ps.setObject(13, parseNullableLong(body.get("capacityTotal")));
            ps.setObject(14, parseNullableLong(body.get("capacityAvailable")));
            ps.setString(15, "published".equals(body.get("status")) ? "published" : "draft");
            ps.setString(16, now);
            ps.setString(17, now);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getLong(1);
            }
        }
    }

    public Map<String, Object> get(long id) throws Exception {
        try (PreparedStatement ps = database.connection().prepareStatement("SELECT * FROM listings WHERE id=?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new NotFoundException("Listing not found.");
                if (!"published".equals(rs.getString("status")) && !hasAdmin(rs.getLong("posted_by"))) {
                    throw new NotFoundException("Listing not found.");
                }
                return listingMap(rs);
            }
        }
    }

    public List<Map<String, Object>> mine(Long caller) throws Exception {
        if (caller == null) throw new UnauthorizedException("Not logged in.");
        List<Map<String, Object>> result = new ArrayList<>();
        try (PreparedStatement ps = database.connection().prepareStatement(
                "SELECT * FROM listings WHERE owner_type='user' AND owner_user_id=? ORDER BY id DESC")) {
            ps.setLong(1, caller);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) result.add(listingMap(rs));
            }
        }
        return result;
    }

    public List<Map<String, Object>> search(Map<String, String> query) throws Exception {
        int page = Math.max(1, parseInt(query.get("page"), 1));
        int size = Math.min(50, Math.max(1, parseInt(query.get("size"), 20)));
        String keyword = query.getOrDefault("keyword", "").trim();
        String sort = query.getOrDefault("sort", "newest");
        String sql = "SELECT * FROM listings WHERE status='published'";
        if (!keyword.isEmpty()) sql += " AND (title LIKE ? OR description LIKE ? OR address LIKE ?)";
        sql += " ORDER BY " + switch (sort) {
            case "lowest_rent" -> "CASE WHEN rent_type='negotiable' THEN 1 ELSE 0 END, rent_amount ASC";
            case "highest_rent" -> "rent_amount DESC";
            case "most_available" -> "capacity_available DESC";
            default -> "id DESC";
        } + ", id DESC LIMIT ? OFFSET ?";
        List<Map<String, Object>> result = new ArrayList<>();
        try (PreparedStatement ps = database.connection().prepareStatement(sql)) {
            int index = 1;
            if (!keyword.isEmpty()) {
                String pattern = "%" + keyword + "%";
                ps.setString(index++, pattern);
                ps.setString(index++, pattern);
                ps.setString(index++, pattern);
            }
            ps.setInt(index++, size);
            ps.setInt(index, (page - 1) * size);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) result.add(listingMap(rs));
            }
        }
        return result;
    }

    public void setStatus(long id, Long caller, String status) throws Exception {
        requireOwnerOrEditor(id, caller);
        Map<String, Object> listing = getForOwner(id);
        if ("publish".equals(status)) validatePublish(listing);
        String currentStatus = Objects.toString(listing.get("status"), "draft");
        String newStatus = switch (status) {
            case "publish" -> "published";
            case "hide" -> "hidden";
            case "unhide" -> "hidden".equals(currentStatus) ? "draft" : currentStatus;
            default -> throw new InvalidInputException("INVALID_STATUS", "Status is invalid.");
        };
        try (PreparedStatement ps = database.connection().prepareStatement(
                "UPDATE listings SET status=?,updated_at=? WHERE id=?")) {
            ps.setString(1, newStatus);
            ps.setString(2, Instant.now(clock).toString());
            ps.setLong(3, id);
            ps.executeUpdate();
        }
    }

    public void delete(long id, Long caller) throws Exception {
        requireOwnerOrEditor(id, caller);
        try (PreparedStatement ps = database.connection().prepareStatement(
                "UPDATE listings SET status='deleted',updated_at=? WHERE id=?")) {
            ps.setString(1, Instant.now(clock).toString());
            ps.setLong(2, id);
            ps.executeUpdate();
        }
    }

    private void validatePublish(Map<String, Object> listing) {
        if (length(listing.get("title")) < 3 || length(listing.get("title")) > 100 ||
                length(listing.get("address")) == 0 ||
                listing.get("rentType") == null ||
                listing.get("capacityTotal") == null ||
                listing.get("capacityAvailable") == null) {
            throw new ConflictException("LISTING_INCOMPLETE", "Listing is missing required publishing fields.");
        }
        if (!listingHasReadyAttachment(((Number) listing.get("id")).longValue())) {
            throw new ConflictException("NO_READY_ATTACHMENT", "A ready attachment is required to publish.");
        }
    }

    private boolean listingHasReadyAttachment(long id) {
        try (PreparedStatement ps = database.connection().prepareStatement(
                "SELECT COUNT(*) FROM listing_attachments WHERE listing_id=? AND status='ready'")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        } catch (Exception exception) {
            return false;
        }
    }

    private Map<String, Object> listingMap(ResultSet rs) throws Exception {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", rs.getLong("id"));
        result.put("ownerType", rs.getString("owner_type"));
        result.put("ownerUserId", rs.getObject("owner_user_id"));
        result.put("organizationId", rs.getObject("organization_id"));
        result.put("postedBy", rs.getLong("posted_by"));
        result.put("title", rs.getString("title"));
        result.put("description", rs.getString("description"));
        result.put("rentType", rs.getString("rent_type"));
        result.put("rentAmount", rs.getObject("rent_amount"));
        result.put("address", rs.getString("address"));
        result.put("contact", Map.of(
                "email", Objects.toString(rs.getString("contact_email"), ""),
                "phone", Objects.toString(rs.getString("contact_phone"), ""),
                "note", Objects.toString(rs.getString("contact_note"), ""),
                "source", "listing"));
        result.put("capacityTotal", rs.getObject("capacity_total"));
        result.put("capacityAvailable", rs.getObject("capacity_available"));
        result.put("status", rs.getString("status"));
        result.put("createdAt", rs.getString("created_at"));
        result.put("updatedAt", rs.getString("updated_at"));
        return result;
    }

    private Map<String, Object> getForOwner(long id) throws Exception {
        try (PreparedStatement ps = database.connection().prepareStatement("SELECT * FROM listings WHERE id=?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new NotFoundException("Listing not found.");
                return listingMap(rs);
            }
        }
    }

    private void requireOwnerOrEditor(long id, Long caller) throws Exception {
        if (caller == null) throw new UnauthorizedException("Not logged in.");
        try (PreparedStatement ps = database.connection().prepareStatement(
                "SELECT owner_type,owner_user_id,organization_id,posted_by,hold_by FROM listings WHERE id=?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new NotFoundException("Listing not found.");
                if (rs.getObject("hold_by") != null) throw new ForbiddenException("MODERATION_HOLD", "Listing is under moderation hold.");
                if ("user".equals(rs.getString("owner_type")) && rs.getLong("owner_user_id") == caller) return;
                long organizationId = rs.getLong("organization_id");
                if ("organization".equals(rs.getString("owner_type")) &&
                        (rs.getLong("posted_by") == caller || isOrganizationOwner(caller, organizationId) ||
                                hasOrganizationPermission(caller, organizationId, "edit listing"))) return;
                throw new ForbiddenException("NOT_OWNER", "You cannot modify this listing.");
            }
        }
    }

    private boolean canPostOrganization(long caller, long organizationId) throws Exception {
        return isOrganizationOwner(caller, organizationId) ||
                hasOrganizationPermission(caller, organizationId, "post listing");
    }

    private boolean isOrganizationOwner(long caller, long organizationId) throws Exception {
        try (PreparedStatement ps = database.connection().prepareStatement(
                "SELECT COUNT(*) FROM organizations WHERE id=? AND owner_id=?")) {
            ps.setLong(1, organizationId);
            ps.setLong(2, caller);
            try (ResultSet rs = ps.executeQuery()) { rs.next(); return rs.getInt(1) == 1; }
        }
    }

    private boolean hasOrganizationPermission(long caller, long organizationId, String permission) throws Exception {
        try (PreparedStatement ps = database.connection().prepareStatement(
                "SELECT COUNT(*) FROM organization_member_permissions WHERE organization_id=? AND user_id=? AND permission=?")) {
            ps.setLong(1, organizationId);
            ps.setLong(2, caller);
            ps.setString(3, permission);
            try (ResultSet rs = ps.executeQuery()) { rs.next(); return rs.getInt(1) == 1; }
        }
    }

    private void requireNotTimedOut(long caller) throws Exception {
        try (PreparedStatement ps = database.connection().prepareStatement(
                "SELECT COUNT(*) FROM sanctions WHERE user_id=? AND type='timeout' AND revoked_time IS NULL AND (end_time IS NULL OR end_time>?)")) {
            ps.setLong(1, caller);
            ps.setString(2, Instant.now(clock).toString());
            try (ResultSet rs = ps.executeQuery()) { rs.next(); if (rs.getInt(1) > 0) throw new ForbiddenException("TIMED_OUT", "You are timed out."); }
        }
    }

    private void checkListingRateLimit(long caller, String ownerType, long organizationId) throws Exception {
        Instant cutoff = Instant.now(clock).minusSeconds(3600);
        String sql = "SELECT COUNT(*) FROM listings WHERE created_at>? AND ";
        if ("organization".equals(ownerType)) sql += "owner_type='organization' AND organization_id=?";
        else sql += "owner_type='user' AND posted_by=?";
        try (PreparedStatement ps = database.connection().prepareStatement(sql)) {
            ps.setString(1, cutoff.toString());
            ps.setLong(2, "organization".equals(ownerType) ? organizationId : caller);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                int limit = "organization".equals(ownerType) ? 30 : 10;
                if (rs.getInt(1) >= limit) throw new RateLimitException("Listing creation limit reached.", 3600);
            }
        }
    }

    private boolean hasAdmin(long caller) throws Exception {
        try (PreparedStatement ps = database.connection().prepareStatement(
                "SELECT COUNT(*) FROM admin_permissions WHERE user_id=?")) {
            ps.setLong(1, caller);
            try (ResultSet rs = ps.executeQuery()) { rs.next(); return rs.getInt(1) > 0; }
        }
    }

    private static int parseInt(String value, int fallback) {
        try { return Integer.parseInt(value); } catch (Exception ignored) { return fallback; }
    }
    private static long parseLong(Object value) {
        try { return Long.parseLong(Objects.toString(value)); } catch (Exception exception) { throw new InvalidInputException("INVALID_ID", "Invalid id."); }
    }
    private static Long parseNullableLong(Object value) {
        if (value == null) return null;
        try { return Long.parseLong(Objects.toString(value)); } catch (Exception exception) { return null; }
    }
    private static String blank(Object value) { return value == null ? "" : Objects.toString(value); }
    private static int length(Object value) { return value == null ? 0 : Objects.toString(value).length(); }
}
