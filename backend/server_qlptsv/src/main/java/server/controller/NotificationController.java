package server.controller;

import io.javalin.Javalin;
import server.components.control.Registrable;
import server.components.functions.NotificationService;

public final class NotificationController implements Registrable {
    private final NotificationService notifications;

    public NotificationController(NotificationService notifications) {
        this.notifications = notifications;
    }

    @Override
    public void register(Javalin app) {
        app.get("/api/notifications", ctx -> {
            Long caller = ctx.attribute("callerId");
            notifications.requireLogin(caller);
            ctx.json(notifications.list(caller));
        });
        app.post("/api/notifications/{id}/read", ctx -> {
            Long caller = ctx.attribute("callerId");
            notifications.requireLogin(caller);
            notifications.markRead(caller, ctx.pathParamAsClass("id", Long.class).get());
            ctx.status(204).json("");
        });
    }
}
