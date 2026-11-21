package server.controller;

import io.javalin.Javalin;
import server.components.control.Registrable;
import server.components.errors.UnauthorizedException;
import server.components.functions.ListingService;

import java.util.Map;

public final class ListingController implements Registrable {
    private final ListingService listings;

    public ListingController(ListingService listings) {
        this.listings = listings;
    }

    @Override
    public void register(Javalin app) {
        app.post("/api/listings", ctx -> {
            Long caller = ctx.attribute("callerId");
            long id = listings.create(caller, JsonRequestBody.readMap(ctx.body()));
            ctx.status(201).json(Map.of("id", id));
        });
        app.get("/api/listings/mine", ctx -> {
            Long caller = ctx.attribute("callerId");
            if (caller == null) throw new UnauthorizedException("Not logged in.");
            ctx.json(listings.mine(caller));
        });
        app.get("/api/listings/{id}", ctx -> ctx.json(listings.get(ctx.pathParamAsClass("id", Long.class).get())));
        app.put("/api/listings/{id}/status", ctx -> {
            Map<String, Object> body = JsonRequestBody.readMap(ctx.body());
            listings.setStatus(ctx.pathParamAsClass("id", Long.class).get(), ctx.attribute("callerId"),
                    String.valueOf(body.get("status")));
            ctx.status(204).json("");
        });
        app.delete("/api/listings/{id}", ctx -> {
            listings.delete(ctx.pathParamAsClass("id", Long.class).get(), ctx.attribute("callerId"));
            ctx.status(204).json("");
        });
    }
}
