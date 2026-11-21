# Room Rental Server Operations

This note explains how to build, start, inspect, and stop the local room-rental server. It is intended for both people and coding agents.

## 1. What the server does

- Runs a local REST API backed by SQLite.
- Binds to a loopback address (`127.x.x.x`); it is not exposed to the network.
- Supports front-end pages opened with `file://`. CORS allows those pages to call the API.
- Starts from one executable JAR. Java 21 is required.

The API details are documented in the server's OpenAPI/Swagger page; this operations note is not a replacement for that reference.

## 2. Build and start

From the project directory, build the executable JAR:

```powershell
mvn package
```

Start it with the default settings:

```powershell
java -jar "target\room-rental-backend.jar"
```

The defaults are host `127.0.0.1`, port `7070`, and data directory `database` under the current working directory. To override them:

```powershell
java -jar "target\room-rental-backend.jar" `
  --host 127.0.0.1 `
  --port 7070 `
  --data-dir "D:\room-rental-data"
```

Use `java -jar "target\room-rental-backend.jar" --help` to see the supported startup options.

On startup, the server creates the data folders, opens SQLite, runs migrations, and starts the API. If there is no active account with the top admin permission, it creates a seed admin and prints its credentials once. Save those credentials; the password cannot be recovered from the database.

The Swing window is informational. Use the terminal to start or stop the server. Stop it with **Ctrl+C** in the terminal running the JAR.

## 3. API and OpenAPI/Swagger

With the default settings:

- API base URL: `http://127.0.0.1:7070/api`
- Swagger UI: `http://127.0.0.1:7070/swagger`
- OpenAPI JSON: `http://127.0.0.1:7070/openapi.json`
- Server status: `http://127.0.0.1:7070/api/status`

Use Swagger UI to browse the documented operations, parameters, request bodies, responses, and bearer-token authorization. Agents can read `/openapi.json` directly. The document describes the endpoints currently implemented in the server; it does not imply that every feature in the design blueprint is implemented.

Swagger UI loads its interface assets from a CDN, so the browser needs internet access to render the page. The OpenAPI JSON endpoint itself is served by the local server.

Swagger UI is enabled by default. Start with `--disable-swagger` to disable the UI; `/openapi.json` remains available.

## 4. Configuration

Settings are provided at startup and are not saved to a configuration file.

| Option | Default | Description |
|---|---|---|
| `--host` | `127.0.0.1` | API bind address; must be in `127.0.0.0/8` |
| `--port` | `7070` | API port, from 1 to 65535 |
| `--data-dir` | `database` | Directory for database, uploaded files, logs, and supporting files |
| `--disable-swagger` | off | Disable the Swagger UI page |

Email is optional. Configure SMTP with these environment variables or matching startup flags:

| Environment variable | Startup flag | Purpose |
|---|---|---|
| `SMTP_HOST` | `--smtp-host` | SMTP server |
| `SMTP_PORT` | `--smtp-port` | SMTP port |
| `SMTP_USER` | `--smtp-user` | SMTP username |
| `SMTP_PASSWORD` | `--smtp-password` | SMTP password |
| `SMTP_FROM` | `--smtp-from` | Sender email address |
| `SMTP_FROM_NAME` | `--smtp-from-name` | Sender display name |

Flags override environment variables. Prefer `SMTP_PASSWORD` to `--smtp-password`, because command-line arguments can be visible in process listings. Email is considered configured only when the SMTP host, port, and sender address are set. Username and password may be empty if the relay allows it.

For example, in PowerShell:

```powershell
$env:SMTP_HOST = "smtp.example.com"
$env:SMTP_PORT = "587"
$env:SMTP_USER = "mailer@example.com"
$env:SMTP_PASSWORD = "<your-mail-password>"
$env:SMTP_FROM = "mailer@example.com"
$env:SMTP_FROM_NAME = "Room Rental"
java -jar "target\room-rental-backend.jar"
```

Without SMTP configuration, email-dependent operations return `502 email_not_configured`; other server functions, including seed-admin login, remain available. Never put real credentials in source files or commit them.

## 5. Data and files

The data directory contains:

```text
database/
  room-rental.db    SQLite database
  resources/        Uploaded images and videos
  logs/             Log files
  bin/              ffmpeg tools, when installed
```

The same folders are placed under the path supplied by `--data-dir`. Back up the entire data directory, not only the database file. SQLite may also create `-wal` and `-shm` sidecar files while the server is running.

The server needs write access to its data directory. This matters when running from an installed application directory such as `Program Files`; use a writable location with `--data-dir`.

## 6. Basic API usage

- Log in using `POST /api/session/login`. The response contains a bearer token.
- Send the token in the `Authorization: Bearer <token>` header for authenticated requests. Do not put tokens in URLs.
- Requests without a token are guests and can use public browsing and search endpoints.
- Errors use a JSON object with `code` and `message`, and may include `details` or `retryAfterSeconds`.
- The OpenAPI document lists the implemented routes and their request/response shapes.

## 7. Seed administrator

- On startup, a seed admin is created if no active user holds `edit_admin_permission`.
- The seed login name and password are printed to the terminal and shown once in the window. Keep them safe; the password is stored as a hash and cannot be displayed again.
- The seed account starts with no email address and requires both its password and email to be changed. Email change requires SMTP configuration.
- The seed account becomes a regular admin after both required changes are complete.

The current JAR supports seed creation during startup. Standalone `seed-admin` and `reset-seed-admin` terminal commands are not implemented.

## 8. Video processing

Video uploads require ffmpeg to be ready and return `503` while it is unavailable. The server may use tools bundled with the installer or download the configured build when available. Image and other API operations do not require ffmpeg.

## 9. Troubleshooting

| Symptom | What to check |
|---|---|
| JAR does not start | Confirm Java 21 is installed and run `java -jar "target\room-rental-backend.jar" --help` |
| Port is already in use | Choose another port with `--port` |
| API is unreachable | Check the startup output and visit `/api/status` on the configured host and port |
| Browser blocks requests | Confirm the server is running and the front end uses the same host and port |
| API returns `401` | Send a valid, unexpired bearer token |
| API returns `502 email_not_configured` | Set SMTP host, port, and sender address, then restart |
| Video upload returns `503` | Check whether ffmpeg is available |
| Data folders cannot be created | Choose a writable location with `--data-dir` |
| Seed email/password flow is unavailable | SMTP must be configured; seed credentials are printed only when the seed is created |

## 10. Safety notes

- The API binds only to loopback, but other software running on the same computer can access it.
- Contact information returned by public profile endpoints may be visible to guests.
- Never log or share passwords, bearer tokens, OTPs, SMTP credentials, or temporary passwords.
- Timestamps are stored in UTC.
