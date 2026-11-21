package server.controller;

import io.javalin.Javalin;
import server.components.errors.DomainException;
import server.components.errors.RateLimitException;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ErrorHandler {
    public static void install(Javalin app) {
        app.exception(server.components.errors.EmailNotConfiguredException.class, (exception, ctx) -> {
            ctx.status(502).json(Map.of(
                    "code", "email_not_configured",
                    "message", exception.getMessage()));
        });
        app.exception(server.components.errors.EmailSendFailedException.class, (exception, ctx) -> {
            ctx.status(502).json(Map.of(
                    "code", "email_send_failed",
                    "message", exception.getMessage()));
        });
        app.exception(DomainException.class, (exception, ctx) -> {
            ctx.status(statusFor(exception));
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("code", exception.code());
            body.put("message", exception.getMessage());
            if (exception.details() != null) body.put("details", exception.details());
            if (exception.retryAfterSeconds() != null) body.put("retryAfterSeconds", exception.retryAfterSeconds());
            ctx.json(body);
        });
        app.exception(Exception.class, (exception, ctx) -> {
            ctx.status(500);
            ctx.json(Map.of("code", "INTERNAL_ERROR", "message", "Unexpected server error."));
        });
    }

    private static int statusFor(DomainException exception) {
        if (exception instanceof server.components.errors.UnauthorizedException) return 401;
        if (exception instanceof server.components.errors.ForbiddenException) return 403;
        if (exception instanceof server.components.errors.NotFoundException) return 404;
        if (exception instanceof server.components.errors.ConflictException) return 409;
        if (exception instanceof server.components.errors.TooLargeException) return 413;
        if (exception instanceof RateLimitException) return 429;
        if (exception instanceof server.components.errors.EmailNotConfiguredException) return 502;
        if (exception instanceof server.components.errors.EmailSendFailedException) return 502;
        return 400;
    }
}
