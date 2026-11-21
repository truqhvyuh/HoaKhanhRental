package server.components.control;

import io.javalin.Javalin;
import server.components.session.PasswordHasher;
import server.components.session.SmtpSettings;
import server.components.session.SmtpEmailSender;
import server.components.session.SessionService;
import server.controller.AuthFilter;
import server.controller.ErrorHandler;
import server.controller.SessionController;
import server.controller.ListingController;
import server.controller.SearchController;
import server.controller.TeamController;
import server.controller.OrganizationController;
import server.controller.ApplicationController;
import server.controller.NotificationController;
import server.controller.AdminController;
import server.data.Database;
import server.components.functions.ListingService;
import server.components.functions.ApplicationService;
import server.components.functions.NotificationService;
import server.components.administration.AdminAnnouncementService;
import server.components.user.TeamService;
import server.components.user.OrganizationService;
import server.components.misc.SwingUi;
import server.controller.OpenApiDocument;

import javax.swing.*;
import java.awt.GraphicsEnvironment;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Clock;
import java.time.Instant;
import java.util.*;

public final class ServerLifecycle {
    private static volatile Javalin app;
    private static volatile Database database;
    private static volatile String host = "127.0.0.1";
    private static volatile int port = 7070;

    private ServerLifecycle() {}

    public static void main(String[] args) {
        Map<String, String> options = parseOptions(args);
        if (args.length == 1 && "--help".equals(args[0])) {
            printHelp();
            return;
        }
        host = validateHost(options.getOrDefault("--host", "127.0.0.1"));
        port = validatePort(Integer.parseInt(options.getOrDefault("--port", "7070")));
        Path dataDir = Path.of(options.getOrDefault("--data-dir", "database")).toAbsolutePath();
        boolean swaggerDisabled = options.containsKey("--disable-swagger");
        SmtpSettings smtpSettings = SmtpSettings.from(options);

        database = new Database(dataDir);
        try {
            database.open();
            database.migrate();
            PasswordHasher passwordHasher = new PasswordHasher();
            String seedCredentials = ensureSeedAdmin(database, passwordHasher);
            if (seedCredentials != null) {
                System.out.println("Seed admin credentials: " + seedCredentials);
                showSeedDialog(seedCredentials);
            }

            SessionService sessions = new SessionService(database, passwordHasher, Clock.systemUTC(),
                    new SmtpEmailSender(smtpSettings), smtpSettings);
            app = Javalin.create(config -> {
                config.http.defaultContentType = "application/json";
                config.jetty.defaultHost = host;
                config.jetty.defaultPort = port;
            });
            app.before(ctx -> {
                ctx.header("Access-Control-Allow-Origin", "*");
                ctx.header("Access-Control-Allow-Headers", "Authorization, Content-Type");
                ctx.header("Access-Control-Allow-Methods", "GET,POST,PUT,PATCH,DELETE,OPTIONS");
                ctx.header("Cache-Control", "no-store");
            });
            app.options("/*", ctx -> ctx.status(204));
            ErrorHandler.install(app);
            new AuthFilter(sessions).install(app);
            new OpenApiDocument(swaggerDisabled).register(app);
            new SessionController(sessions).register(app);
            ListingService listings = new ListingService(database, Clock.systemUTC());
            new ListingController(listings).register(app);
            new SearchController(listings).register(app);
            TeamService teams = new TeamService(database, Clock.systemUTC());
            new TeamController(teams).register(app);
            OrganizationService organizations = new OrganizationService(database, Clock.systemUTC());
            new OrganizationController(organizations).register(app);
            new ApplicationController(new ApplicationService(database, Clock.systemUTC())).register(app);
            new NotificationController(new NotificationService(database)).register(app);
            new AdminController(new AdminAnnouncementService(database, Clock.systemUTC())).register(app);

            app.get("/api/status", ctx -> ctx.json(Map.of(
                    "state", "running",
                    "host", host,
                    "port", port,
                    "email", smtpSettings.isConfigured() ? "configured" : "not configured",
                    "swagger", swaggerDisabled ? false : true)));

            app.start(port);
            System.out.println("Room rental server running at http://" + host + ":" + port);
            if (!swaggerDisabled) System.out.println("Swagger is enabled at /swagger");
            SwingUi.show(smtpSettings, "Running");

            Runtime.getRuntime().addShutdownHook(new Thread(ServerLifecycle::stop));
        } catch (Exception exception) {
            System.err.println("Failed to start server: " + exception.getMessage());
            if (database != null) database.close();
            System.exit(1);
        }
    }

    public static synchronized void stop() {
        if (app != null) {
            app.stop();
            app = null;
        }
        if (database != null) {
            database.close();
            database = null;
        }
    }

    private static Map<String, String> parseOptions(String[] args) {
        Map<String, String> options = new HashMap<>();
        for (int i = 0; i < args.length; i++) {
            if (args[i].startsWith("--")) {
                if (i + 1 < args.length && !args[i + 1].startsWith("--")) {
                    options.put(args[i], args[++i]);
                } else {
                    options.put(args[i], "true");
                }
            }
        }
        return options;
    }

    private static void printHelp() {
        System.out.println("""
                Usage: room-rental-backend [options]
                  --host <127.x.x.x>          API host (default 127.0.0.1)
                  --port <1-65535>            API port (default 7070)
                  --data-dir <path>           Data directory
                  --smtp-host <host>          SMTP host
                  --smtp-port <port>          SMTP port
                  --smtp-user <name>          SMTP username
                  --smtp-password <password>  SMTP password (prefer SMTP_PASSWORD env; flags are visible in process lists)
                  --smtp-from <address>       Sender email address
                  --smtp-from-name <name>     Sender display name
                  --disable-swagger           Disable Swagger UI
                """);
    }

    private static String validateHost(String value) {
        if (!value.matches("^127(?:\\.[0-9]{1,3}){3}$")) {
            throw new IllegalArgumentException("Host must be inside 127.0.0.0/8.");
        }
        return value;
    }

    private static int validatePort(int value) {
        if (value < 1 || value > 65535) throw new IllegalArgumentException("Port must be 1-65535.");
        return value;
    }

    private static String ensureSeedAdmin(Database database, PasswordHasher hasher) throws Exception {
        try (PreparedStatement ps = database.connection().prepareStatement(
                "SELECT COUNT(*) FROM admin_permissions WHERE permission='edit_admin_permission'")) {
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                if (rs.getInt(1) > 0) return null;
            }
        }
        SecureRandom random = new SecureRandom();
        String login = "seed_" + Long.toUnsignedString(random.nextLong(), 36).replace("-", "").substring(0, 12);
        String password = "S" + Long.toUnsignedString(random.nextLong(), 36).replace("-", "") +
                "a!" + random.nextInt(90);
        if (password.length() > 72) password = password.substring(0, 72);

        database.connection().setAutoCommit(false);
        try {
            long userId;
            try (PreparedStatement ps = database.connection().prepareStatement(
                    "INSERT INTO users(login_name,email,email_verified,password_hash,is_seed,must_change_password,must_change_email,created_at) " +
                            "VALUES(?,?,1,?,1,1,1,?)", PreparedStatement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, login);
                ps.setNull(2, java.sql.Types.VARCHAR);
                ps.setString(3, hasher.hash(password));
                ps.setString(4, Instant.now(Clock.systemUTC()).toString());
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    keys.next();
                    userId = keys.getLong(1);
                }
            }
            try (PreparedStatement ps = database.connection().prepareStatement(
                    "INSERT INTO user_details(user_id,display_name) VALUES(?,?)")) {
                ps.setLong(1, userId);
                ps.setString(2, "Seed Administrator");
                ps.executeUpdate();
            }
            String[] permissions = {"edit_listing","hide_listing","delete_listing","edit_team","edit_organization",
                    "ban","timeout","edit_user_info","edit_user_password","edit_admin_permission"};
            try (PreparedStatement ps = database.connection().prepareStatement(
                    "INSERT INTO admin_permissions(user_id,permission) VALUES(?,?)")) {
                for (String permission : permissions) {
                    ps.setLong(1, userId);
                    ps.setString(2, permission);
                    ps.addBatch();
                }
                ps.executeBatch();
            }
            database.connection().commit();
            return login + " / " + password;
        } catch (Exception exception) {
            database.connection().rollback();
            throw exception;
        } finally {
            database.connection().setAutoCommit(true);
        }
    }

    private static void showSeedDialog(String credentials) {
        if (!GraphicsEnvironment.isHeadless()) {
            SwingUtilities.invokeLater(() ->
                    JOptionPane.showMessageDialog(null, "Seed admin credentials (shown once):\n" + credentials,
                            "Seed Administrator", JOptionPane.INFORMATION_MESSAGE));
        }
    }
}
