package server.controller;

import io.javalin.Javalin;
import server.components.control.Registrable;
import server.components.administration.AdminAnnouncementService;

import java.util.Map;

public final class AdminController implements Registrable {
    private final AdminAnnouncementService announcements;

    public AdminController(AdminAnnouncementService announcements) {
        this.announcements = announcements;
    }

    @Override
    public void register(Javalin app) {
        app.get("/api/announcements", ctx -> ctx.json(announcements.publicList()));
        app.post("/api/admin/announcements", ctx -> {
            long id = announcements.create(ctx.attribute("callerId"), JsonRequestBody.readMap(ctx.body()));
            ctx.status(201).json(Map.of("id", id));
        });
    }
}
