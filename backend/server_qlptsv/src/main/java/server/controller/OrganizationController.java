package server.controller;

import io.javalin.Javalin;
import server.components.control.Registrable;
import server.components.user.OrganizationService;

import java.util.Map;

public final class OrganizationController implements Registrable {
    private final OrganizationService organizations;

    public OrganizationController(OrganizationService organizations) {
        this.organizations = organizations;
    }

    @Override
    public void register(Javalin app) {
        app.post("/api/organizations", ctx -> {
            long id = organizations.create(ctx.attribute("callerId"), JsonRequestBody.readMap(ctx.body()));
            ctx.status(201).json(Map.of("id", id));
        });
        app.get("/api/organizations/{id}", ctx ->
                ctx.json(organizations.get(ctx.pathParamAsClass("id", Long.class).get())));
    }
}
