package server.controller;

import io.javalin.Javalin;
import server.components.control.Registrable;
import server.components.functions.ListingService;

import java.util.Map;
import java.util.stream.Collectors;

public final class SearchController implements Registrable {
    private final ListingService listings;

    public SearchController(ListingService listings) {
        this.listings = listings;
    }

    @Override
    public void register(Javalin app) {
        app.get("/api/search/listings", ctx -> {
            Map<String, String> query = ctx.queryParamMap().entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().get(0)));
            ctx.json(listings.search(query));
        });
    }
}
