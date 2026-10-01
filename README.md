# WorkSync

A self-contained Spring Boot task and time-management application.

## What you need

- Java 17 or newer

## Run locally

1. Start the application:

   ```bash
   ./mvnw spring-boot:run
   ```

2. Open http://localhost:8080.

Sign in with either built-in local account:

- `admin@worksync.local` / `admin123`
- `employee@worksync.local` / `employee123`

Tasks and users are stored in memory, so they reset whenever the application
stops. This keeps the project self-contained while a persistent database is
not configured.

## Verify the project

```bash
./mvnw test
```

No external services or credentials are required for tests.
