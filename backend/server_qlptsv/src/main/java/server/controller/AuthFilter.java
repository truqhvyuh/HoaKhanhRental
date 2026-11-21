package server.controller;

import io.javalin.Javalin;
import server.components.session.SessionService;
import server.components.errors.ForbiddenException;

public final class AuthFilter {
    private final SessionService sessions;

    public AuthFilter(SessionService sessions) {
        this.sessions = sessions;
    }

    public void install(Javalin app) {
        app.before(ctx -> {
            String authorization = ctx.header("Authorization");
            if (authorization == null || authorization.isBlank()) {
                ctx.attribute("callerId", null);
                return;
            }
            Long caller = sessions.callerForToken(authorization);
            ctx.attribute("callerId", caller);
            if (caller != null) {
                var account = sessions.me(caller);
                if (Boolean.TRUE.equals(account.get("mustChangePassword")) ||
                        Boolean.TRUE.equals(account.get("mustChangeEmail"))) {
                    String path = ctx.path();
                    if (!path.equals("/api/session/me") &&
                            !path.equals("/api/session/logout") &&
                            !path.equals("/api/session/password/change") &&
                            !path.equals("/api/session/email/change") &&
                            !path.equals("/api/session/email/verify")) {
                        throw new ForbiddenException("MUST_CHANGE_CREDENTIALS", "Password or email change is required.");
                    }
                }
            }
        });
    }
}
