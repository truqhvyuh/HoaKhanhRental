package server.components.session;

import server.data.Database;
import server.components.errors.*;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.security.SecureRandom;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

public final class SessionService {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    private final Database database;
    private final PasswordHasher passwordHasher;
    private final EmailSender emailSender;
    private final SmtpSettings smtpSettings;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();
    private final Map<String, LoginLock> loginLocks = new HashMap<>();

    public record LoginResult(long userId, String loginName, String token, Map<String, Object> account) {}

    private record LoginLock(int failures, Instant lockedUntil) {}

    public SessionService(Database database, PasswordHasher passwordHasher, Clock clock) {
        this(database, passwordHasher, clock, new EmailSender() {
            @Override
            public void send(String recipient, String subject, String body) throws EmailSendException {
                throw new EmailSendException("Email sender not configured", null);
            }
        }, new SmtpSettings("", 0, "", "", "", ""));
    }

    public SessionService(Database database, PasswordHasher passwordHasher, Clock clock,
                          EmailSender emailSender, SmtpSettings smtpSettings) {
        this.database = database;
        this.passwordHasher = passwordHasher;
        this.clock = clock;
        this.emailSender = emailSender;
        this.smtpSettings = smtpSettings;
    }

    public void requireEmailConfigured() {
        if (!smtpSettings.isConfigured()) throw new EmailNotConfiguredException();
    }

    public String register(Map<String, Object> body) throws Exception {
        if (!smtpSettings.isConfigured()) throw new EmailNotConfiguredException();
        String email = Objects.toString(body.get("email"), "").trim();
        String loginName = Objects.toString(body.get("loginName"), "");
        String password = Objects.toString(body.get("password"), "");
        validateLoginName(loginName);
        passwordHasher.validate(password, loginName, email);
        if (!email.matches("^[^@\\s]+@[^@\\s]+$")) throw new InvalidInputException("INVALID_EMAIL", "Email is invalid.");

        long count;
        try (PreparedStatement ps = database.connection().prepareStatement(
                "SELECT COUNT(*) FROM users WHERE email = ? COLLATE NOCASE")) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                count = rs.getLong(1);
            }
        }
        if (count > 0) throw new ConflictException("EMAIL_EXISTS", "Email is already registered.");

        String otp = String.format("%06d", random.nextInt(1_000_000));
        sendOrThrow(email, "Registration verification", "Your verification code is " + otp);
        try (PreparedStatement ps = database.connection().prepareStatement(
                "INSERT INTO pending_verifications(purpose,email,otp_hash,attempts,last_sent,expires_at,payload) VALUES(?,?,?,?,?,?,?)")) {
            ps.setString(1, "register");
            ps.setString(2, email);
            ps.setString(3, passwordHasher.hash(otp));
            ps.setInt(4, 0);
            ps.setString(5, Instant.now(clock).toString());
            ps.setString(6, Instant.now(clock).plus(10, ChronoUnit.MINUTES).toString());
            ps.setString(7, "{\"loginName\":\"" + loginName.replace("\"", "\\\"") + "\",\"passwordHash\":\"" +
                    passwordHasher.hash(password).replace("\"", "\\\"") + "\"}");
            ps.executeUpdate();
        }
        return otp;
    }

    public void forgotPassword(String email) throws Exception {
        if (!smtpSettings.isConfigured()) throw new EmailNotConfiguredException();
        boolean exists = false;
        boolean seed = false;
        try (PreparedStatement ps = database.connection().prepareStatement(
                "SELECT is_seed FROM users WHERE email=? COLLATE NOCASE")) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    exists = true;
                    seed = rs.getInt("is_seed") == 1;
                }
            }
        }
        if (!exists || seed) return;
        String otp = String.format("%06d", random.nextInt(1_000_000));
        sendOrThrow(email, "Password reset verification", "Your password reset code is " + otp);
        try (PreparedStatement ps = database.connection().prepareStatement(
                "INSERT INTO pending_verifications(purpose,email,otp_hash,attempts,last_sent,expires_at,payload) VALUES(?,?,?,?,?,?,?)")) {
            ps.setString(1, "password_reset");
            ps.setString(2, email);
            ps.setString(3, passwordHasher.hash(otp));
            ps.setInt(4, 0);
            ps.setString(5, Instant.now(clock).toString());
            ps.setString(6, Instant.now(clock).plus(10, ChronoUnit.MINUTES).toString());
            ps.setString(7, "");
            ps.executeUpdate();
        }
    }

    public void changeEmail(long userId, String newEmail) throws Exception {
        if (!smtpSettings.isConfigured()) throw new EmailNotConfiguredException();
        if (!newEmail.matches("^[^@\\s]+@[^@\\s]+$")) {
            throw new InvalidInputException("INVALID_EMAIL", "Email is invalid.");
        }
        try (PreparedStatement ps = database.connection().prepareStatement(
                "SELECT COUNT(*) FROM users WHERE email=? COLLATE NOCASE")) {
            ps.setString(1, newEmail);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                if (rs.getInt(1) > 0) throw new ConflictException("EMAIL_EXISTS", "Email is already registered.");
            }
        }
        String otp = String.format("%06d", random.nextInt(1_000_000));
        sendOrThrow(newEmail, "Email change verification", "Your email change code is " + otp);
        try (PreparedStatement ps = database.connection().prepareStatement(
                "INSERT INTO pending_verifications(purpose,email,otp_hash,attempts,last_sent,expires_at,payload) VALUES(?,?,?,?,?,?,?)")) {
            ps.setString(1, "email_change");
            ps.setString(2, newEmail);
            ps.setString(3, passwordHasher.hash(otp));
            ps.setInt(4, 0);
            ps.setString(5, Instant.now(clock).toString());
            ps.setString(6, Instant.now(clock).plus(10, ChronoUnit.MINUTES).toString());
            ps.setString(7, String.valueOf(userId));
            ps.executeUpdate();
        }
    }

    public void verifyEmailChange(long userId, String otp) throws Exception {
        long verificationId;
        String email;
        String otpHash;
        Instant expires;
        int attempts;
        try (PreparedStatement ps = database.connection().prepareStatement(
                "SELECT id,email,otp_hash,expires_at,attempts,payload FROM pending_verifications " +
                        "WHERE purpose='email_change' AND payload=? ORDER BY id DESC LIMIT 1")) {
            ps.setString(1, String.valueOf(userId));
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new NotFoundException("No email verification found.");
                verificationId = rs.getLong("id");
                email = rs.getString("email");
                otpHash = rs.getString("otp_hash");
                expires = Instant.parse(rs.getString("expires_at"));
                attempts = rs.getInt("attempts");
            }
        }
        if (expires.isBefore(Instant.now(clock))) throw new ConflictException("OTP_EXPIRED", "Verification code expired.");
        if (attempts >= 5) throw new ConflictException("OTP_KILLED", "Verification code has been invalidated.");
        if (!passwordHasher.matches(otp, otpHash)) {
            try (PreparedStatement ps = database.connection().prepareStatement(
                    "UPDATE pending_verifications SET attempts=attempts+1 WHERE id=?")) {
                ps.setLong(1, verificationId);
                ps.executeUpdate();
            }
            throw new UnauthorizedException("Invalid verification code.");
        }
        try (PreparedStatement ps = database.connection().prepareStatement(
                "UPDATE users SET email=?,email_verified=1,must_change_email=0 WHERE id=?")) {
            ps.setString(1, email);
            ps.setLong(2, userId);
            ps.executeUpdate();
        }
        try (PreparedStatement ps = database.connection().prepareStatement(
                "DELETE FROM pending_verifications WHERE id=?")) {
            ps.setLong(1, verificationId);
            ps.executeUpdate();
        }
    }

    public void resetPassword(String email, String otp, String newPassword) throws Exception {
        if (!smtpSettings.isConfigured()) throw new EmailNotConfiguredException();
        long verificationId;
        long userId;
        String otpHash;
        Instant expires;
        int attempts;
        try (PreparedStatement ps = database.connection().prepareStatement(
                "SELECT v.id,v.otp_hash,v.expires_at,v.attempts,u.id AS user_id " +
                        "FROM pending_verifications v JOIN users u ON u.email=v.email COLLATE NOCASE " +
                        "WHERE v.purpose='password_reset' AND v.email=? COLLATE NOCASE ORDER BY v.id DESC LIMIT 1")) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new NotFoundException("No password reset verification found.");
                verificationId = rs.getLong("id");
                otpHash = rs.getString("otp_hash");
                expires = Instant.parse(rs.getString("expires_at"));
                attempts = rs.getInt("attempts");
                userId = rs.getLong("user_id");
            }
        }

        if (expires.isBefore(Instant.now(clock))) throw new ConflictException("OTP_EXPIRED", "Verification code expired.");
        if (attempts >= 5) throw new ConflictException("OTP_KILLED", "Verification code has been invalidated.");
        if (!passwordHasher.matches(otp, otpHash)) {
            try (PreparedStatement ps = database.connection().prepareStatement(
                    "UPDATE pending_verifications SET attempts=attempts+1 WHERE id=?")) {
                ps.setLong(1, verificationId);
                ps.executeUpdate();
            }
            throw new UnauthorizedException("Invalid verification code.");
        }
        String loginName;
        String currentHash;
        try (PreparedStatement ps = database.connection().prepareStatement(
                "SELECT login_name,password_hash,email FROM users WHERE id=?")) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                loginName = rs.getString("login_name");
                currentHash = rs.getString("password_hash");
                email = rs.getString("email");
            }
        }
        passwordHasher.validate(newPassword, loginName, email);
        if (passwordHasher.matches(newPassword, currentHash)) {
            throw new ConflictException("PASSWORD_REUSED", "New password must differ from the current password.");
        }
        try (PreparedStatement ps = database.connection().prepareStatement(
                "UPDATE users SET password_hash=?,must_change_password=0 WHERE id=?")) {
            ps.setString(1, passwordHasher.hash(newPassword));
            ps.setLong(2, userId);
            ps.executeUpdate();
        }
        try (PreparedStatement ps = database.connection().prepareStatement("DELETE FROM sessions WHERE user_id=?")) {
            ps.setLong(1, userId);
            ps.executeUpdate();
        }
        try (PreparedStatement ps = database.connection().prepareStatement(
                "DELETE FROM pending_verifications WHERE id=?")) {
            ps.setLong(1, verificationId);
            ps.executeUpdate();
        }
    }

    public void adminPasswordReset(long targetId) throws Exception {
        if (!smtpSettings.isConfigured()) throw new EmailNotConfiguredException();
        String email;
        String loginName;
        int isSeed;
        try (PreparedStatement ps = database.connection().prepareStatement(
                "SELECT email,login_name,is_seed FROM users WHERE id=?")) {
            ps.setLong(1, targetId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new NotFoundException("User not found.");
                email = rs.getString("email");
                loginName = rs.getString("login_name");
                isSeed = rs.getInt("is_seed");
            }
        }
        if (isSeed == 1) throw new ForbiddenException("SEEDED_ACCOUNT_RESET_BLOCKED", "A flagged seed account cannot be reset by email.");
        if (email == null || email.isBlank()) throw new ConflictException("NO_EMAIL", "The target account has no email address.");
        String temporary = "T" + String.format("%06d", random.nextInt(1_000_000)) + "a!";
        passwordHasher.validate(temporary, loginName, email);
        sendOrThrow(email, "Temporary password", "Your temporary password is " + temporary);
        try (PreparedStatement ps = database.connection().prepareStatement(
                "UPDATE users SET password_hash=?,must_change_password=1 WHERE id=?")) {
            ps.setString(1, passwordHasher.hash(temporary));
            ps.setLong(2, targetId);
            ps.executeUpdate();
        }
        try (PreparedStatement ps = database.connection().prepareStatement(
                "DELETE FROM sessions WHERE user_id=?")) {
            ps.setLong(1, targetId);
            ps.executeUpdate();
        }
    }

    private void sendOrThrow(String recipient, String subject, String body) {
        try {
            emailSender.send(recipient, subject, body);
        } catch (EmailSender.EmailSendException exception) {
            throw new EmailSendFailedException();
        }
    }

    public void verifyRegistration(String email, String otp) throws Exception {
        long verificationId;
        String payload;
        String otpHash;
        Instant expires;
        int attempts;
        try (PreparedStatement ps = database.connection().prepareStatement(
                "SELECT id,payload,otp_hash,expires_at,attempts FROM pending_verifications " +
                        "WHERE purpose='register' AND email=? COLLATE NOCASE ORDER BY id DESC LIMIT 1")) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new NotFoundException("No registration verification found.");
                verificationId = rs.getLong("id");
                payload = rs.getString("payload");
                otpHash = rs.getString("otp_hash");
                expires = Instant.parse(rs.getString("expires_at"));
                attempts = rs.getInt("attempts");
            }
        }
        if (expires.isBefore(Instant.now(clock))) throw new ConflictException("OTP_EXPIRED", "Verification code expired.");
        if (attempts >= 5) throw new ConflictException("OTP_KILLED", "Verification code has been invalidated.");
        if (!passwordHasher.matches(otp, otpHash)) {
            try (PreparedStatement ps = database.connection().prepareStatement(
                    "UPDATE pending_verifications SET attempts=attempts+1 WHERE id=?")) {
                ps.setLong(1, verificationId);
                ps.executeUpdate();
            }
            throw new UnauthorizedException("Invalid verification code.");
        }

        try {
            Map<String, Object> data = OBJECT_MAPPER.readValue(payload, MAP_TYPE);
            String loginName = Objects.toString(data.get("loginName"), "");
            String passwordHash = Objects.toString(data.get("passwordHash"), "");
            String now = Instant.now(clock).toString();
            try (PreparedStatement ps = database.connection().prepareStatement(
                    "INSERT INTO users(login_name,email,email_verified,password_hash,is_seed,must_change_password,must_change_email,created_at) " +
                            "VALUES(?,?,?,?,0,0,0,?)", PreparedStatement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, loginName);
                ps.setString(2, email);
                ps.setInt(3, 1);
                ps.setString(4, passwordHash);
                ps.setString(5, now);
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        long userId = keys.getLong(1);
                        try (PreparedStatement details = database.connection().prepareStatement(
                                "INSERT INTO user_details(user_id,display_name,phone,address,profile_picture,bio) VALUES(?,?,?,?,?,?)")) {
                            details.setLong(1, userId);
                            details.setString(2, loginName);
                            details.setNull(3, java.sql.Types.VARCHAR);
                            details.setNull(4, java.sql.Types.VARCHAR);
                            details.setNull(5, java.sql.Types.VARCHAR);
                            details.setNull(6, java.sql.Types.VARCHAR);
                            details.executeUpdate();
                        }
                    }
                }
            }
            try (PreparedStatement ps = database.connection().prepareStatement(
                    "DELETE FROM pending_verifications WHERE id=?")) {
                ps.setLong(1, verificationId);
                ps.executeUpdate();
            }
        } catch (java.sql.SQLException exception) {
            if (exception.getMessage() != null && exception.getMessage().toLowerCase(Locale.ROOT).contains("unique")) {
                throw new ConflictException("USER_EXISTS", "Login name or email already exists.");
            }
            throw exception;
        }
    }

    public LoginResult login(String identifier, String password, String userAgent) throws Exception {
        LoginLock lock = loginLocks.get(identifier.toLowerCase(Locale.ROOT));
        if (lock != null && lock.lockedUntil.isAfter(Instant.now(clock))) {
            throw new RateLimitException("Too many failed login attempts.", (int) ChronoUnit.SECONDS.between(Instant.now(clock), lock.lockedUntil));
        }

        long userId = 0;
        String loginName = null;
        String hash = null;
        boolean banned = false;
        String bannedReason = null;
        try (PreparedStatement ps = database.connection().prepareStatement(
                (identifier.contains("@")
                        ? "SELECT u.id,u.login_name,u.password_hash,s.reason FROM users u LEFT JOIN sanctions s ON s.user_id=u.id AND s.type='ban' AND s.revoked_time IS NULL WHERE u.email=? COLLATE NOCASE"
                        : "SELECT u.id,u.login_name,u.password_hash,s.reason FROM users u LEFT JOIN sanctions s ON s.user_id=u.id AND s.type='ban' AND s.revoked_time IS NULL WHERE u.login_name=? COLLATE NOCASE"))) {
            ps.setString(1, identifier);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    userId = rs.getLong("id");
                    loginName = rs.getString("login_name");
                    hash = rs.getString("password_hash");
                    bannedReason = rs.getString("reason");
                    banned = bannedReason != null;
                }
            }
        }
        if (banned) throw new ForbiddenException("BANNED", "This account is banned. Reason: " + bannedReason);
        if (userId == 0 || hash == null || !passwordHasher.matches(password, hash)) {
            LoginLock previous = loginLocks.getOrDefault(identifier.toLowerCase(Locale.ROOT), new LoginLock(0, Instant.EPOCH));
            int failures = previous.failures + 1;
            Instant lockedUntil = failures >= 10 ? Instant.now(clock).plus(15, ChronoUnit.MINUTES) : Instant.EPOCH;
            loginLocks.put(identifier.toLowerCase(Locale.ROOT), new LoginLock(failures, lockedUntil));
            throw new UnauthorizedException("Invalid login credentials.");
        }
        loginLocks.remove(identifier.toLowerCase(Locale.ROOT));

        String token = randomToken();
        String tokenHash = passwordHasher.hash(token);
        Instant now = Instant.now(clock);
        try (PreparedStatement ps = database.connection().prepareStatement(
                "INSERT INTO sessions(token_hash,user_id,created_at,last_used,expires_at,user_agent) VALUES(?,?,?,?,?,?)")) {
            ps.setString(1, tokenHash);
            ps.setLong(2, userId);
            ps.setString(3, now.toString());
            ps.setString(4, now.toString());
            ps.setString(5, now.plus(7, ChronoUnit.DAYS).toString());
            ps.setString(6, userAgent);
            ps.executeUpdate();
        }
        Map<String, Object> account = me(userId);
        return new LoginResult(userId, loginName, token, account);
    }

    public Map<String, Object> me(long userId) throws Exception {
        Map<String, Object> result = new LinkedHashMap<>();
        try (PreparedStatement ps = database.connection().prepareStatement(
                "SELECT u.id,u.login_name,u.email,u.is_seed,u.must_change_password,u.must_change_email,d.display_name,d.phone,d.address,d.profile_picture,d.bio " +
                        "FROM users u LEFT JOIN user_details d ON d.user_id=u.id WHERE u.id=?")) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new NotFoundException("User not found.");
                result.put("id", rs.getLong("id"));
                result.put("loginName", rs.getString("login_name"));
                result.put("email", rs.getString("email"));
                result.put("displayName", rs.getString("display_name"));
                result.put("phone", rs.getString("phone"));
                result.put("address", rs.getString("address"));
                result.put("picture", rs.getString("profile_picture"));
                result.put("bio", rs.getString("bio"));
                result.put("isSeed", rs.getInt("is_seed") == 1);
                result.put("mustChangePassword", rs.getInt("must_change_password") == 1);
                result.put("mustChangeEmail", rs.getInt("must_change_email") == 1);
            }
        }
        result.put("roles", deriveRoles(userId));
        result.put("adminPermissions", adminPermissions(userId));
        return result;
    }

    public List<String> adminPermissions(long userId) throws Exception {
        List<String> permissions = new ArrayList<>();
        try (PreparedStatement ps = database.connection().prepareStatement("SELECT permission FROM admin_permissions WHERE user_id=?")) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) permissions.add(rs.getString(1));
            }
        }
        return permissions;
    }

    public List<String> deriveRoles(long userId) throws Exception {
        List<String> roles = new ArrayList<>();
        roles.add("renter");
        try (PreparedStatement ps = database.connection().prepareStatement(
                "SELECT EXISTS(SELECT 1 FROM listings WHERE owner_type='user' AND owner_user_id=? AND status!='deleted')")) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) { if (rs.next() && rs.getInt(1) == 1) roles.add("owner"); }
        }
        if (!adminPermissions(userId).isEmpty()) roles.add("moderator");
        return roles;
    }

    public Long callerForToken(String authorization) throws Exception {
        if (authorization == null || !authorization.startsWith("Bearer ")) return null;
        String token = authorization.substring(7).trim();
        if (token.isEmpty()) throw new UnauthorizedException("Bad or expired token.");
        try (PreparedStatement ps = database.connection().prepareStatement("SELECT user_id,token_hash,expires_at,last_used FROM sessions")) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    if (!passwordHasher.matches(token, rs.getString("token_hash"))) continue;
                    Instant expires = Instant.parse(rs.getString("expires_at"));
                    Instant idleDeadline = Instant.parse(rs.getString("last_used")).plus(24, ChronoUnit.HOURS);
                    if (expires.isBefore(Instant.now(clock)) || idleDeadline.isBefore(Instant.now(clock))) {
                        try (PreparedStatement delete = database.connection().prepareStatement("DELETE FROM sessions WHERE token_hash=?")) {
                            delete.setString(1, rs.getString("token_hash"));
                            delete.executeUpdate();
                        }
                        throw new UnauthorizedException("Bad or expired token.");
                    }
                    try (PreparedStatement update = database.connection().prepareStatement(
                            "UPDATE sessions SET last_used=? WHERE token_hash=?")) {
                        update.setString(1, Instant.now(clock).toString());
                        update.setString(2, rs.getString("token_hash"));
                        update.executeUpdate();
                    }
                    return rs.getLong("user_id");
                }
            }
        }
        throw new UnauthorizedException("Bad or expired token.");
    }

    public void logout(String authorization) throws Exception {
        Long caller = callerForToken(authorization);
        if (caller == null) throw new UnauthorizedException("Not logged in.");
        try (PreparedStatement ps = database.connection().prepareStatement(
                "DELETE FROM sessions WHERE user_id=?")) {
            ps.setLong(1, caller);
            ps.executeUpdate();
        }
    }

    public void validateLoginName(String loginName) {
        if (loginName == null || loginName.length() < 8 || loginName.length() > 50 ||
                loginName.contains("@") || loginName.chars().anyMatch(ch -> ch < 32 || ch > 126) ||
                loginName.startsWith(" ") || loginName.endsWith(" ") || loginName.contains("  ") ||
                loginName.chars().noneMatch(ch -> Character.isLetterOrDigit(ch))) {
            throw new InvalidInputException("INVALID_LOGIN_NAME", "Login name is invalid.");
        }
    }

    public String randomToken() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        StringBuilder result = new StringBuilder();
        for (byte value : bytes) result.append(String.format("%02x", value));
        return result.toString();
    }

    public void changePassword(long userId, String current, String next, String authorization) throws Exception {
        String hash;
        String loginName;
        String email;
        try (PreparedStatement ps = database.connection().prepareStatement(
                "SELECT u.login_name,u.email,u.password_hash FROM users u WHERE u.id=?")) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new NotFoundException("User not found.");
                loginName = rs.getString("login_name");
                email = rs.getString("email");
                hash = rs.getString("password_hash");
            }
        }
        if (!passwordHasher.matches(current, hash)) throw new ForbiddenException("PASSWORD_INCORRECT", "Current password is incorrect.");
        passwordHasher.validate(next, loginName, email);
        if (passwordHasher.matches(next, hash)) throw new ConflictException("PASSWORD_REUSED", "New password must differ from the current password.");
        try (PreparedStatement ps = database.connection().prepareStatement(
                "UPDATE users SET password_hash=?,must_change_password=0 WHERE id=?")) {
            ps.setString(1, passwordHasher.hash(next));
            ps.setLong(2, userId);
            ps.executeUpdate();
        }
        if (authorization != null && authorization.startsWith("Bearer ")) {
            String token = authorization.substring(7).trim();
            String currentHash = null;
            try (PreparedStatement ps = database.connection().prepareStatement(
                    "SELECT token_hash FROM sessions WHERE user_id=?")) {
                ps.setLong(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String candidate = rs.getString("token_hash");
                        if (passwordHasher.matches(token, candidate)) {
                            currentHash = candidate;
                            break;
                        }
                    }
                }
            }
            if (currentHash != null) {
                try (PreparedStatement ps = database.connection().prepareStatement(
                        "DELETE FROM sessions WHERE user_id=? AND token_hash<>?")) {
                    ps.setLong(1, userId);
                    ps.setString(2, currentHash);
                    ps.executeUpdate();
                }
            }
        }
    }
}
