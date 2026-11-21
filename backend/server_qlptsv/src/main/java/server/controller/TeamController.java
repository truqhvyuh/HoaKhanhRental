package server.controller;

import io.javalin.Javalin;
import server.components.control.Registrable;
import server.components.user.TeamService;

import java.util.Map;

public final class TeamController implements Registrable {
    private final TeamService teams;

    public TeamController(TeamService teams) {
        this.teams = teams;
    }

    @Override
    public void register(Javalin app) {
        app.post("/api/teams", ctx -> {
            Map<String, Object> body = JsonRequestBody.readMap(ctx.body());
            long id = teams.create(ctx.attribute("callerId"), String.valueOf(body.get("name")));
            ctx.status(201).json(Map.of("id", id));
        });
        app.get("/api/teams/{id}", ctx ->
                ctx.json(teams.get(ctx.pathParamAsClass("id", Long.class).get())));
        app.post("/api/teams/{id}/disband", ctx -> {
            teams.disband(ctx.pathParamAsClass("id", Long.class).get(), ctx.attribute("callerId"));
            ctx.status(204).json("");
        });
    }
}
