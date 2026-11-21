package server.components.control;

import io.javalin.Javalin;

public interface Registrable {
    void register(Javalin app);
}
