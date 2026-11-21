package server.controller;

import io.javalin.Javalin;
import server.components.control.Registrable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class OpenApiDocument implements Registrable {
    private final boolean swaggerDisabled;

    public OpenApiDocument(boolean swaggerDisabled) {
        this.swaggerDisabled = swaggerDisabled;
    }

    @Override
    public void register(Javalin app) {
        app.get("/openapi.json", ctx -> ctx.json(document()));
        if (!swaggerDisabled) {
            app.get("/swagger", ctx -> {
                ctx.html("""
                        <!doctype html>
                        <html lang="en">
                        <head>
                          <meta charset="utf-8">
                          <title>Room Rental API</title>
                          <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/swagger-ui-dist@5.18.2/swagger-ui.css">
                        </head>
                        <body>
                          <div id="swagger-ui"></div>
                          <script src="https://cdn.jsdelivr.net/npm/swagger-ui-dist@5.18.2/swagger-ui-bundle.js"></script>
                          <script>
                            SwaggerUIBundle({
                              url: '/openapi.json',
                              dom_id: '#swagger-ui',
                              deepLinking: true
                            });
                          </script>
                        </body>
                        </html>
                        """);
            });
        }
    }

    private Map<String, Object> document() {
        Map<String, Object> document = new LinkedHashMap<>();
        document.put("openapi", "3.0.3");
        document.put("info", Map.of(
                "title", "Room Rental API",
                "version", "1.0.0",
                "description", "Local-only room rental backend"));
        document.put("servers", List.of(Map.of("url", "/")));
        Map<String, Object> schemas = new LinkedHashMap<>();
        schemas.put("Error", Map.of(
                "type", "object",
                "required", List.of("code", "message"),
                "properties", Map.of(
                        "code", Map.of("type", "string"),
                        "message", Map.of("type", "string"),
                        "details", Map.of("description", "Optional validation details"),
                        "retryAfterSeconds", Map.of("type", "integer"))));
        schemas.put("RegisterRequest", requestSchema(
                Map.of("email", "string", "loginName", "string", "password", "string"),
                List.of("email", "loginName", "password")));
        schemas.put("VerifyRegistrationRequest", requestSchema(
                Map.of("email", "string", "otp", "string"), List.of("email", "otp")));
        schemas.put("LoginRequest", requestSchema(
                Map.of("identifier", "string", "password", "string"), List.of("identifier", "password")));
        schemas.put("PasswordChangeRequest", requestSchema(
                Map.of("currentPassword", "string", "newPassword", "string"),
                List.of("currentPassword", "newPassword")));
        schemas.put("EmailRequest", requestSchema(Map.of("email", "string"), List.of("email")));
        schemas.put("EmailOtpRequest", requestSchema(Map.of("otp", "string"), List.of("otp")));
        schemas.put("PasswordResetRequest", requestSchema(
                Map.of("email", "string", "otp", "string", "newPassword", "string"),
                List.of("email", "otp", "newPassword")));
        schemas.put("ListingCreateRequest", requestSchema(fields(
                        "ownerType", "string",
                        "organizationId", "integer",
                        "title", "string",
                        "description", "string",
                        "rentType", "string",
                        "rentAmount", "integer",
                        "address", "string",
                        "contactEmail", "string",
                        "contactPhone", "string",
                        "contactNote", "string",
                        "capacityTotal", "integer",
                        "capacityAvailable", "integer",
                        "status", "string"),
                List.of()));
        schemas.put("ListingStatusRequest", requestSchema(
                Map.of("status", "string"), List.of("status")));
        schemas.put("TeamCreateRequest", requestSchema(Map.of("name", "string"), List.of("name")));
        schemas.put("OrganizationCreateRequest", requestSchema(Map.of(
                        "name", "string",
                        "description", "string",
                        "contactEmail", "string",
                        "phone", "string",
                        "address", "string"),
                List.of("name")));
        schemas.put("ApplicationCreateRequest", requestSchema(Map.of(
                        "listingId", "integer",
                        "asTeam", "boolean",
                        "note", "string"),
                List.of("listingId")));
        schemas.put("AnnouncementCreateRequest", requestSchema(Map.of(
                        "title", "string",
                        "body", "string",
                        "pinned", "boolean"),
                List.of("title", "body")));
        schemas.put("IdResponse", Map.of(
                "type", "object",
                "required", List.of("id"),
                "properties", Map.of("id", Map.of("type", "integer", "format", "int64"))));
        schemas.put("LoginResponse", Map.of(
                "type", "object",
                "required", List.of("token", "account"),
                "properties", Map.of(
                        "token", Map.of("type", "string", "description", "Bearer token, returned once"),
                        "account", Map.of("type", "object", "description", "Account, roles and permission details"))));
        document.put("components", Map.of(
                "securitySchemes", Map.of(
                        "bearerAuth", Map.of("type", "http", "scheme", "bearer")),
                "schemas", schemas));

        Map<String, Object> paths = new LinkedHashMap<>();
        addSessionPaths(paths);
        addListingPaths(paths);
        addSearchAndGroupPaths(paths);
        addApplicationNotificationAndAdminPaths(paths);
        document.put("paths", paths);
        return document;
    }

    private void addSessionPaths(Map<String, Object> paths) {
        addRequest(paths, "/api/session/register", "POST", "Session",
                "Register a user. Nothing is created until the OTP is verified. Registration returns 502 until SMTP is configured.",
                false, "RegisterRequest", List.of("202", "400", "409", "502"));
        addRequest(paths, "/api/session/register/verify", "POST", "Session",
                "Verify a six-digit registration OTP within ten minutes.",
                false, "VerifyRegistrationRequest", List.of("201", "400", "401", "409", "410"));
        addRequest(paths, "/api/session/login", "POST", "Session",
                "Authenticate by email or login name and receive a bearer token. Ten failures lock the identifier for fifteen minutes.",
                false, "LoginRequest", List.of("200", "401", "403", "429"));
        paths.put("/api/session/logout", operation("POST", "Session",
                "End the current session.", true, true, List.of("204", "401")));
        paths.put("/api/session/me", operation("GET", "Session",
                "Get the current account and derived roles.", true, false, List.of("200", "401")));
        addRequest(paths, "/api/session/password/change", "POST", "Session",
                "Change password and end other sessions while keeping the current bearer session.",
                true, "PasswordChangeRequest", List.of("204", "400", "401", "403", "409"));
        addRequest(paths, "/api/session/password/forgot", "POST", "Session",
                "Request password-reset OTP. The 202 response is identical for existing and unknown addresses.",
                false, "EmailRequest", List.of("202", "502"));
        addRequest(paths, "/api/session/password/reset", "POST", "Session",
                "Reset password using OTP. This ends all user sessions.",
                false, "PasswordResetRequest", List.of("204", "400", "401", "409", "410", "502"));
        addRequest(paths, "/api/session/email/change", "POST", "Session",
                "Send email-change OTP to a new address. The old address is not changed until verification.",
                true, "EmailRequest", List.of("202", "400", "401", "409", "502"));
        addRequest(paths, "/api/session/email/verify", "POST", "Session",
                "Apply a verified email change and clear the must-change-email flag.",
                true, "EmailOtpRequest", List.of("204", "400", "401", "409", "410"));
        addRequest(paths, "/api/users/{id}/password-reset", "POST", "User",
                "Administrator temporary-password reset. Flagged seed accounts are blocked.",
                true, null, List.of("204", "401", "403", "404", "502"));
    }

    private void addListingPaths(Map<String, Object> paths) {
        addRequest(paths, "/api/listings", "POST", "Listing",
                "Create a draft or published listing. Drafts allow incomplete fields; published listings require title, address, rent, capacity, and a ready attachment.",
                true, "ListingCreateRequest", List.of("201", "400", "401", "403", "409", "429"));
        putOperation(paths, "/api/listings/mine", "GET", "Listing",
                "List the caller's solo listings.", true, false, List.of("200", "401"));
        putOperationWithParameters(paths, "/api/listings/{id}", "GET", "Listing",
                "Get a listing by integer id.", false, false, List.of("200", "404"),
                List.of(pathParameter("id", "Listing id")));
        addRequest(paths, "/api/listings/{id}/status", "PUT", "Listing",
                "Set status to publish, hide, or unhide. Hide is blocked by a moderation hold.",
                true, "ListingStatusRequest", List.of("204", "400", "401", "403", "409"));
        addParametersToMethod(paths, "/api/listings/{id}/status", "put",
                List.of(pathParameter("id", "Listing id")));
        addMethod(paths, "/api/listings/{id}", "DELETE", operation("DELETE", "Listing",
                "Soft-delete a listing.", true, false, List.of("204", "401", "403", "404")));
        addParametersToMethod(paths, "/api/listings/{id}", "delete",
                List.of(pathParameter("id", "Listing id")));
    }

    private void addSearchAndGroupPaths(Map<String, Object> paths) {
        putOperation(paths, "/api/search/listings", "GET", "Search",
                "Search published listings.", false, false, List.of("200"));
        addParametersToMethod(paths, "/api/search/listings", "get", List.of(
                queryParameter("keyword", "string", "Keyword over title, description or address"),
                queryParameter("page", "integer", "Page number; default 1"),
                queryParameter("size", "integer", "Page size; default 20, maximum 50"),
                queryParameter("sort", "string", "newest, lowest_rent, highest_rent, or most_available")));
        addRequest(paths, "/api/teams", "POST", "Team",
                "Create an active team. The caller becomes leader and must not belong to another group.",
                true, "TeamCreateRequest", List.of("201", "400", "401", "409"));
        putOperationWithParameters(paths, "/api/teams/{id}", "GET", "Team",
                "Get team details, leader and members.", false, false, List.of("200", "404"),
                List.of(pathParameter("id", "Team id")));
        putOperationWithParameters(paths, "/api/teams/{id}/disband", "POST", "Team",
                "Disband a team. Only the leader or an authorized team editor may do this.",
                true, false, List.of("204", "401", "403", "404", "409"),
                List.of(pathParameter("id", "Team id")));
        addRequest(paths, "/api/organizations", "POST", "Organization",
                "Create an organization. The caller becomes owner and sole member.",
                true, "OrganizationCreateRequest", List.of("201", "400", "401", "409"));
        putOperationWithParameters(paths, "/api/organizations/{id}", "GET", "Organization",
                "Get organization details.", false, false, List.of("200", "404"),
                List.of(pathParameter("id", "Organization id")));
    }

    private void addApplicationNotificationAndAdminPaths(Map<String, Object> paths) {
        addRequest(paths, "/api/applications", "POST", "Application",
                "Submit a solo or team application. Team applications require the caller to be the team leader.",
                true, "ApplicationCreateRequest", List.of("201", "400", "401", "403", "409", "429"));
        putOperation(paths, "/api/applications/mine", "GET", "Application",
                "Get the caller's applications.", true, false, List.of("200", "401"));
        putOperationWithParameters(paths, "/api/applications/{id}", "DELETE", "Application",
                "Withdraw a pending or accepted application.", true, false, List.of("204", "401", "404"),
                List.of(pathParameter("id", "Application id")));
        putOperation(paths, "/api/notifications", "GET", "Notification",
                "List the caller's newest notifications.", true, false, List.of("200", "401"));
        putOperationWithParameters(paths, "/api/notifications/{id}/read", "POST", "Notification",
                "Mark one notification read. The notification must belong to the caller.",
                true, false, List.of("204", "401", "404"),
                List.of(pathParameter("id", "Notification id")));
        putOperation(paths, "/api/announcements", "GET", "Admin",
                "Public announcements.", false, false, List.of("200"));
        addRequest(paths, "/api/admin/announcements", "POST", "Admin",
                "Post an announcement. The caller must hold at least one moderator permission.",
                true, "AnnouncementCreateRequest", List.of("201", "400", "401", "403"));
        putOperation(paths, "/api/status", "GET", "Status",
                "Server status, including email configuration.", false, false, List.of("200"));
    }

    private void putOperation(Map<String, Object> paths, String path, String method,
                              String tag, String summary, boolean secured,
                              boolean hasBody, List<String> statuses) {
        paths.put(path, Map.of(method.toLowerCase(), operationDefinition(
                method, tag, summary, secured, hasBody, statuses)));
    }

    private void addRequest(Map<String, Object> paths, String path, String method,
                            String tag, String summary, boolean secured,
                            String schemaName, List<String> statuses) {
        Map<String, Object> operation = new LinkedHashMap<>(
                operation(method, tag, summary, secured, true, statuses));
        if (schemaName != null) {
            Map<String, Object> operationDefinition = new LinkedHashMap<>(
                    (Map<String, Object>) operation.get(method.toLowerCase()));
            operationDefinition.put("requestBody", Map.of(
                    "required", true,
                    "content", Map.of("application/json", Map.of(
                            "schema", Map.of("$ref", "#/components/schemas/" + schemaName)))));
            if ("LoginRequest".equals(schemaName)) {
                Map<String, Object> responses = (Map<String, Object>) operationDefinition.get("responses");
                Map<String, Object> success = new LinkedHashMap<>((Map<String, Object>) responses.get("200"));
                success.put("content", Map.of("application/json", Map.of(
                        "schema", Map.of("$ref", "#/components/schemas/LoginResponse"))));
                responses.put("200", success);
            }
            if (statuses.contains("201")) {
                Map<String, Object> responses = (Map<String, Object>) operationDefinition.get("responses");
                Map<String, Object> success = new LinkedHashMap<>((Map<String, Object>) responses.get("201"));
                success.put("content", Map.of("application/json", Map.of(
                        "schema", Map.of("$ref", "#/components/schemas/IdResponse"))));
                responses.put("201", success);
            }
            operation.put(method.toLowerCase(), operationDefinition);
        }
        paths.put(path, operation);
    }

    private void putOperationWithParameters(Map<String, Object> paths, String path, String method,
                                            String tag, String summary, boolean secured,
                                            boolean hasBody, List<String> statuses,
                                            List<Map<String, Object>> parameters) {
        Map<String, Object> operationDefinition = operationDefinition(
                method, tag, summary, secured, hasBody, statuses);
        operationDefinition.put("parameters", parameters);
        paths.put(path, Map.of(method.toLowerCase(), operationDefinition));
    }

    private void addParametersToMethod(Map<String, Object> paths, String path, String method,
                                       List<Map<String, Object>> parameters) {
        Map<String, Object> existingPath = new LinkedHashMap<>(
                paths.containsKey(path) ? (Map<String, Object>) paths.get(path) : Map.of());
        Map<String, Object> operation = new LinkedHashMap<>(
                existingPath.containsKey(method) ? (Map<String, Object>) existingPath.get(method) : Map.of());
        operation.put("parameters", parameters);
        existingPath.put(method, operation);
        paths.put(path, existingPath);
    }

    private static Map<String, Object> pathParameter(String name, String description) {
        return Map.of(
                "name", name,
                "in", "path",
                "required", true,
                "description", description,
                "schema", Map.of("type", "integer", "format", "int64"));
    }

    private static Map<String, Object> queryParameter(String name, String type, String description) {
        return Map.of(
                "name", name,
                "in", "query",
                "required", false,
                "description", description,
                "schema", Map.of("type", type));
    }

    private static Map<String, String> fields(String... nameAndType) {
        Map<String, String> fields = new LinkedHashMap<>();
        for (int i = 0; i + 1 < nameAndType.length; i += 2) {
            fields.put(nameAndType[i], nameAndType[i + 1]);
        }
        return fields;
    }

    private static Map<String, Object> requestSchema(Map<String, String> fields,
                                                     List<String> required) {
        Map<String, Object> properties = new LinkedHashMap<>();
        for (Map.Entry<String, String> field : fields.entrySet()) {
            Map<String, Object> property = new LinkedHashMap<>();
            String type = field.getValue();
            property.put("type", type);
            if ("integer".equals(type)) property.put("format", "int64");
            properties.put(field.getKey(), property);
        }
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        if (!required.isEmpty()) schema.put("required", required);
        schema.put("properties", properties);
        schema.put("additionalProperties", false);
        return schema;
    }

    private void addMethod(Map<String, Object> paths, String path, String method,
                           Map<String, Object> operation) {
        Map<String, Object> existing = new LinkedHashMap<>(paths.containsKey(path)
                ? (Map<String, Object>) paths.get(path)
                : Map.of());
        existing.putAll(operation);
        paths.put(path, existing);
    }

    private Map<String, Object> operation(String method, String tag, String summary,
                                          boolean secured, boolean hasBody, List<String> statuses) {
        return Map.of(method.toLowerCase(), operationDefinition(
                method, tag, summary, secured, hasBody, statuses));
    }

    private Map<String, Object> operationDefinition(String method, String tag, String summary,
                                                    boolean secured, boolean hasBody,
                                                    List<String> statuses) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("tags", List.of(tag));
        value.put("summary", summary);
        value.put("operationId", method.toLowerCase() + "-" + tag.toLowerCase() + "-" + Integer.toHexString(summary.hashCode()));
        if (secured) value.put("security", List.of(Map.of("bearerAuth", List.of())));
        if (hasBody) value.put("requestBody", Map.of(
                "required", true,
                "content", Map.of("application/json", Map.of("schema", Map.of("type", "object")))));
        Map<String, Object> responses = new LinkedHashMap<>();
        for (String status : statuses) {
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("description", responseDescription(status));
            if ("500".equals(status) || status.charAt(0) == '4' || status.charAt(0) == '5') {
                response.put("content", Map.of("application/json", Map.of(
                        "schema", Map.of("$ref", "#/components/schemas/Error"))));
            }
            if ("502".equals(status)) {
                response.put("x-error-codes", List.of("email_not_configured", "email_send_failed"));
            }
            responses.put(status, response);
        }
        value.put("responses", responses);
        return value;
    }

    private static String responseDescription(String status) {
        return switch (status) {
            case "200" -> "Success";
            case "201" -> "Created";
            case "202" -> "Accepted";
            case "204" -> "No content";
            case "400" -> "Invalid input";
            case "401" -> "Not logged in or invalid token";
            case "403" -> "Forbidden";
            case "404" -> "Not found";
            case "409" -> "Conflict";
            case "410" -> "Expired";
            case "429" -> "Rate limited";
            case "502" -> "Email not configured or SMTP send failed";
            default -> "Response";
        };
    }
}
