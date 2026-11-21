package server.controller;

import io.javalin.Javalin;
import server.components.control.Registrable;
import server.components.functions.ApplicationService;

import java.util.Map;

public final class ApplicationController implements Registrable {
    private final ApplicationService applications;

    public ApplicationController(ApplicationService applications) {
        this.applications = applications;
    }

    @Override
    public void register(Javalin app) {
        app.post("/api/applications", ctx -> {
            long id = applications.create(ctx.attribute("callerId"), JsonRequestBody.readMap(ctx.body()));
            ctx.status(201).json(Map.of("id", id));
        });
        app.get("/api/applications/mine", ctx -> ctx.json(applications.mine(ctx.attribute("callerId"))));
        app.delete("/api/applications/{id}", ctx -> {
            applications.withdraw(ctx.pathParamAsClass("id", Long.class).get(), ctx.attribute("callerId"));
            ctx.status(204).json("");
        });
    }
}
