package server.controller;

import io.javalin.Javalin;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.*;

class OpenApiDocumentTest {
    @Test
    void servesOpenApiAndSwagger() throws Exception {
        Javalin app = Javalin.create();
        new OpenApiDocument(false).register(app);
        app.start(0);
        int port = app.port();
        HttpClient client = HttpClient.newHttpClient();

        HttpResponse<String> spec = client.send(
                HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/openapi.json")).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(200, spec.statusCode());
        assertTrue(spec.body().contains("\"openapi\":\"3.0.3\""));
        assertTrue(spec.body().contains("bearerAuth"));
        assertTrue(spec.body().contains("email_not_configured"));
        assertTrue(spec.body().contains("RegisterRequest"));
        assertTrue(spec.body().contains("ListingCreateRequest"));
        assertTrue(spec.body().contains("ApplicationCreateRequest"));

        HttpResponse<String> swagger = client.send(
                HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/swagger")).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(200, swagger.statusCode());
        assertTrue(swagger.body().contains("SwaggerUIBundle"));
        app.stop();
    }
}
