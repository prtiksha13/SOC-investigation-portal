# SOC Sentinel — Full Stack Project

This is a Spring Boot + MariaDB implementation of the AI-Powered SOC Alert Prioritization and Investigation Portal from the project documentation.

## What is stored in MariaDB

- Every alert: reference code, title, asset, description, severity, status, AI priority score, and timestamps
- Every investigation note: alert link, note text, analyst name, and timestamp
- Six sample alerts are inserted the first time the app starts; they are then real database records and can be changed from the website.

## VS Code extensions to install

1. **Extension Pack for Java** (Microsoft) — Java, Maven, debugger, test support
2. **Spring Boot Extension Pack** (VMware) — Spring Boot Dashboard and project support
3. **Database Client JDBC** (cweijan) or **SQLTools** — optional MariaDB database viewer

## How to run

1. Install Java 21, Maven, and XAMPP/MariaDB.
2. Start **MySQL** from the XAMPP Control Panel.
3. In `src/main/resources/application.properties`, set `spring.datasource.username` and `spring.datasource.password` to your MariaDB credentials. The included default is XAMPP's usual `root` user with a blank password.
4. Open this folder in VS Code.
5. Open the terminal in the project root and run:

   ```powershell
   ./run.ps1
   ```

   Or, if Maven is already on your PATH:

   ```powershell
   mvn spring-boot:run
   ```

6. Visit [http://localhost:8081](http://localhost:8081) unless you override the `PORT` environment variable.

The front end is served by Spring Boot, so do **not** use the Live Server extension for this version.

## API routes

- `GET`, `POST /api/alerts`
- `PATCH /api/alerts/{id}/status`
- `GET`, `POST /api/notes`
- `GET /api/metrics`
- `POST /api/alerts/{id}/ai-analysis`

The AI route is a safe local demonstration. To use Gemini, add a server-side Gemini client and keep its API key in an environment variable — never in `app.js`.
