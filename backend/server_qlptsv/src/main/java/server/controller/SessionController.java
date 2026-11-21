package server.controller;

import io.javalin.Javalin;
import server.components.control.Registrable;
import server.components.errors.UnauthorizedException;
import server.components.session.SessionService;

import java.util.Map;

public final class SessionController implements Registrable {
    private final SessionService sessions;

    public SessionController(SessionService sessions) {
        this.sessions = sessions;
    }

    @Override
    public void register(Javalin app) {
        app.post("/api/session/register", ctx -> {
            sessions.requireEmailConfigured();
            Map<String, Object> body = JsonRequestBody.readMap(ctx.body());
            String otp = sessions.register(body);
            ctx.status(202).json(Map.of("code", "VERIFICATION_SENT"));
        });
        app.post("/api/session/register/verify", ctx -> {
            Map<String, Object> body = JsonRequestBody.readMap(ctx.body());
            sessions.verifyRegistration(String.valueOf(body.get("email")), String.valueOf(body.get("otp")));
            ctx.status(201).json(Map.of("code", "REGISTERED"));
        });
        app.post("/api/session/login", ctx -> {
            Map<String, Object> body = JsonRequestBody.readMap(ctx.body());
            var result = sessions.login(String.valueOf(body.get("identifier")), String.valueOf(body.get("password")), ctx.userAgent());
            ctx.status(200).json(Map.of("token", result.token(), "account", result.account()));
        });
        app.post("/api/session/logout", ctx -> {
            sessions.logout(ctx.header("Authorization"));
            ctx.status(204).json("");
        });
        app.get("/api/session/me", ctx -> {
            Long caller = ctx.attribute("callerId");
            if (caller == null) throw new UnauthorizedException("Not logged in.");
            ctx.json(sessions.me(caller));
        });
        app.post("/api/session/password/change", ctx -> {
            Long caller = ctx.attribute("callerId");
            if (caller == null) throw new UnauthorizedException("Not logged in.");
            Map<String, Object> body = JsonRequestBody.readMap(ctx.body());
            sessions.changePassword(caller, String.valueOf(body.get("currentPassword")),
                    String.valueOf(body.get("newPassword")), ctx.header("Authorization"));
            ctx.status(204).json("");
        });
        app.post("/api/session/password/forgot", ctx -> {
            sessions.requireEmailConfigured();
            Map<String, Object> body = JsonRequestBody.readMap(ctx.body());
            sessions.forgotPassword(String.valueOf(body.getOrDefault("email", "")).trim());
            ctx.status(202).json(Map.of("code", "IF_ACCOUNT_EXISTS"));
        });
        app.post("/api/session/email/change", ctx -> {
            Long caller = ctx.attribute("callerId");
            if (caller == null) throw new UnauthorizedException("Not logged in.");
            sessions.requireEmailConfigured();
            Map<String, Object> body = JsonRequestBody.readMap(ctx.body());
            sessions.changeEmail(caller, String.valueOf(body.getOrDefault("email", "")).trim());
            ctx.status(202).json(Map.of("code", "VERIFICATION_SENT"));
        });
        app.post("/api/session/email/verify", ctx -> {
            Long caller = ctx.attribute("callerId");
            if (caller == null) throw new UnauthorizedException("Not logged in.");
            Map<String, Object> body = JsonRequestBody.readMap(ctx.body());
            sessions.verifyEmailChange(caller, String.valueOf(body.getOrDefault("otp", "")));
            ctx.status(204).json("");
        });
        app.post("/api/session/password/reset", ctx -> {
            sessions.requireEmailConfigured();
            Map<String, Object> body = JsonRequestBody.readMap(ctx.body());
            sessions.resetPassword(String.valueOf(body.getOrDefault("email", "")).trim(),
                    String.valueOf(body.getOrDefault("otp", "")),
                    String.valueOf(body.getOrDefault("newPassword", "")));
            ctx.status(204).json("");
        });
        app.post("/api/users/{id}/password-reset", ctx -> {
            Long caller = ctx.attribute("callerId");
            if (caller == null) throw new UnauthorizedException("Not logged in.");
            sessions.adminPasswordReset(ctx.pathParamAsClass("id", Long.class).get());
            ctx.status(204).json("");
        });
    }
}
