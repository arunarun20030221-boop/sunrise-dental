# Sunrise Dental Clinic Management System

A distributed appointment and patient management system for a dental clinic.

The project is a Maven multi-module build:

| Module   | What it is                                                                 |
|----------|----------------------------------------------------------------------------|
| `server` | REST web services plus the JSP staff web UI, packaged as a WAR for Tomcat   |
| `client` | A separate menu-driven console process that consumes those services over HTTP |

The two run as independent processes and talk over HTTP, which is what makes the
application genuinely distributed rather than a single deployable pretending to be one.

Architecture is strict three-tier — presentation, service, data — and the layering is
enforced by `verify-layers.sh`, which reads the real `import` statements and fails the
build if any layer reaches past its neighbour.

---

## Running on Windows

These steps assume a clean Windows 10 or 11 machine with nothing installed yet.

### 1. Install the prerequisites

| Software     | Version                | Notes                                                          |
|--------------|------------------------|----------------------------------------------------------------|
| JDK          | **17** exactly         | Spring Framework 6.1 does not support the newest JDKs           |
| Apache Maven | 3.9 or newer           | Needs `JAVA_HOME` pointing at the JDK 17 install                |
| PostgreSQL   | 14 or newer            | Remember the password you set for the `postgres` superuser      |
| Apache Tomcat| **10.1 or 11**         | Tomcat 9 and earlier **will not work** — see the warning below  |

> **Tomcat 10.1 or newer is required.** This project builds against
> `jakarta.servlet-api` 6.0. Tomcat 9 implements the older `javax.servlet` API, and
> deploying the WAR there fails with a 404 or a `ClassNotFoundException` rather than an
> obvious error message. If the app deploys but every URL 404s, check the Tomcat version
> first.

Verify the toolchain from a Command Prompt:

```bat
java -version
mvn -version
```

Both must report **17**. If `mvn -version` reports a different JDK, set `JAVA_HOME`:

```bat
setx JAVA_HOME "C:\Program Files\Java\jdk-17"
```

Then open a **new** Command Prompt so the change takes effect.

### 2. Create the database

Only an empty database is needed. The application creates its own tables, sequence,
stored functions and trigger on first start, and seeds the reference data. Every script
is idempotent, so restarting is safe.

Open **SQL Shell (psql)** from the Start menu, or use `psql` directly:

```bat
psql -U postgres -c "CREATE DATABASE sunrise_dental;"
```

### 3. Give the application the database credentials

The application reads `DB_URL`, `DB_USERNAME` and `DB_PASSWORD` from the environment
first, and only falls back to the values in
`server/src/main/resources/application.properties`. Using the environment keeps your real
password out of the repository.

Tomcat runs as its own process, so the variables have to be set for **Tomcat**, not for
your shell. Create the file `%CATALINA_HOME%\bin\setenv.bat` — Tomcat's startup script
picks it up automatically:

```bat
set "DB_URL=jdbc:postgresql://localhost:5432/sunrise_dental"
set "DB_USERNAME=postgres"
set "DB_PASSWORD=your_postgres_password"
```

If you would rather not create that file, edit `db.username` and `db.password` in
`application.properties` instead — but do not commit the password.

### 4. Build the WAR

From the project root:

```bat
mvn -pl server clean package -DskipTests
```

This produces `server\target\sunrise-dental.war`.

Use `clean` rather than an incremental build. `javac` skips recompilation when only the
POM has changed, which silently drops compiler-flag changes such as `-parameters` — and
without that flag every `@RequestParam` handler fails at runtime.

### 5. Deploy to Tomcat

```bat
copy server\target\sunrise-dental.war "%CATALINA_HOME%\webapps\"
"%CATALINA_HOME%\bin\startup.bat"
```

Tomcat expands the WAR and serves it under the `/sunrise-dental` context path.

To redeploy after a change, stop Tomcat, delete both the old WAR and its expanded folder,
then copy the new one in:

```bat
"%CATALINA_HOME%\bin\shutdown.bat"
rmdir /s /q "%CATALINA_HOME%\webapps\sunrise-dental"
del "%CATALINA_HOME%\webapps\sunrise-dental.war"
copy server\target\sunrise-dental.war "%CATALINA_HOME%\webapps\"
"%CATALINA_HOME%\bin\startup.bat"
```

Deleting the expanded folder matters — Tomcat will otherwise keep serving the stale one.

### 6. Log in

Open <http://localhost:8080/sunrise-dental/login>

The seeded development accounts are:

| Username     | Password       | Role         |
|--------------|----------------|--------------|
| `admin`      | `admin123`     | Administrator|
| `reception1` | `reception123` | Receptionist |

These are development seed accounts only. See
[Changing the seeded passwords](#changing-the-seeded-passwords) before any real use.

### 7. Run the console client

The client is a **second process**. Leave Tomcat running and open a new Command Prompt.

The client jar declares a main class but does not bundle its Jackson dependency, so
`java -jar` on its own fails with `NoClassDefFoundError`. Copy the dependencies next to
it first, then run it with an explicit classpath:

```bat
mvn -pl client clean package -DskipTests
mvn -pl client dependency:copy-dependencies
java -cp "client\target\classes;client\target\dependency\*" com.sunrise.dental.client.ConsoleClient http://localhost:8080/sunrise-dental
```

The base URL argument is required here. The client defaults to port **9090**, which is
what the original development machine used; a standard Tomcat install on Windows listens
on **8080**. Passing the URL explicitly avoids a connection-refused error.

---

## Running the tests

The test suite runs against a **real PostgreSQL**, not an in-memory substitute. The
schema uses a sequence, PL/pgSQL stored functions and a trigger, none of which H2 can
execute — an H2-backed run would silently skip the database features the system depends
on.

With PostgreSQL running and `sunrise_dental` created:

```bat
set "DB_URL=jdbc:postgresql://localhost:5432/sunrise_dental"
set "DB_USERNAME=postgres"
set "DB_PASSWORD=your_postgres_password"
mvn verify
```

In PowerShell, set the variables with `$env:DB_URL = "..."` instead.

`verify-layers.sh` is a Bash script. It runs in CI and on macOS or Linux; on Windows run
it from **Git Bash** if you want the layering check locally:

```bash
./verify-layers.sh
```

---

## Configuration reference

All settings live in `server/src/main/resources/application.properties`. The three
database keys are overridden by environment variables where present.

| Key                        | Environment override | Default                                          |
|----------------------------|----------------------|--------------------------------------------------|
| `db.url`                   | `DB_URL`             | `jdbc:postgresql://localhost:5432/sunrise_dental` |
| `db.username`              | `DB_USERNAME`        | `postgres`                                       |
| `db.password`              | `DB_PASSWORD`        | *(empty)*                                        |
| `clinic.opening-time`      | —                    | `08:00`                                          |
| `clinic.closing-time`      | —                    | `18:00`                                          |
| `clinic.consultation-fee`  | —                    | `2000.00`                                        |
| `mail.host`                | —                    | *(empty — sending disabled)*                     |

Bookings outside the opening and closing times are rejected by
`AppointmentSchedulingRules`.

Leaving `mail.host` empty disables email sending: `NotificationService` logs the message
it would have sent instead, so the application runs without SMTP credentials.

### Changing the seeded passwords

The passwords in `server/src/main/resources/db/data.sql` are stored as BCrypt hashes, and
the seed accounts exist for development only. To replace one, generate a new hash with the
same encoder the application authenticates with:

```java
new BCryptPasswordEncoder().encode("your-new-password")
```

Replace the corresponding `password_hash` value in `data.sql`. Because the inserts are
`ON CONFLICT (username) DO NOTHING`, an existing row is **not** overwritten on restart —
update the row directly, or drop it first:

```sql
UPDATE staff SET password_hash = '<new-hash>' WHERE username = 'admin';
```

---

## Troubleshooting

| Symptom                                              | Cause and fix                                                                                     |
|------------------------------------------------------|---------------------------------------------------------------------------------------------------|
| Every URL returns 404 after a successful deploy       | Tomcat 9 or older. Install Tomcat 10.1 or 11.                                                      |
| `PSQLException: Connection refused`                   | PostgreSQL is not running, or `DB_URL` points at the wrong port.                                   |
| `PSQLException: password authentication failed`       | `DB_PASSWORD` is wrong, or `setenv.bat` was created after Tomcat had already started.              |
| `FATAL: database "sunrise_dental" does not exist`     | Step 2 was skipped.                                                                                |
| Client exits with `NoClassDefFoundError`              | Run it with the classpath from step 7, not `java -jar`.                                            |
| Client reports connection refused                     | The base URL argument was omitted, so it tried port 9090. Pass the 8080 URL explicitly.            |
| Handler fails with "parameter name information not available" | Incremental build dropped the `-parameters` flag. Rebuild with `clean`.                    |
| Changes do not appear after redeploy                  | The expanded `webapps\sunrise-dental` folder was not deleted.                                      |

Tomcat's log is `%CATALINA_HOME%\logs\catalina.out` (or the dated `catalina.*.log` files
on Windows). Startup failures usually name their root cause on a `Caused by:` line.

---

## Building on macOS or Linux

`redeploy.sh` in the project root does the full stop, rebuild, redeploy and health-check
cycle against a local Homebrew Tomcat. It pins `JAVA_HOME` to JDK 17 and uses port 9090.
