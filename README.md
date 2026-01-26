# WorkFlow Management Application

A Spring Boot backend service for managing job postings, applications, and users with JWT authentication and role-based access control.

## Getting Started

### 1. Clone the repository

```bash
git clone <repository-url>
cd Backend
```

### 2. Configure environment variables

Copy the example environment file and update with your configuration:

```bash
cp .env.example .env.local
```

Edit `.env.local` with your database credentials and JWT secret:

```env
DB_URL=localhost
DB_PORT=3306
DB_NAME=workflow_db
DB_USERNAME=root
DB_PASSWORD=mysql
JWT_SECRET=your-secret-key-here
SPRING_PROFILES_ACTIVE=dev
HIBERNATE_DDL_AUTO=none
FLYWAY_ENABLED=true
```

### 3. Start MySQL using Docker Compose

```bash
docker-compose up -d mysql
```

### 4. Build and run the application

Using Maven wrapper:

```bash
# Build the project
./mvnw clean package -DskipTests

# Run the application
./mvnw spring-boot:run
```

The application will start on `http://localhost:8080`

## API Documentation

- **Swagger UI:** http://localhost:8080/swagger-ui.html
- **Postman Collection:** `docs/Work-Flow-App-API.postman_collection.json`

### Updating Postman Collection

```bash
# Edit modules in docs/postman/modules/, then build:
npm run postman:build
```

See `docs/postman/README.md` for details.

## Running with Docker

```bash
# Build and run with Docker Compose
docker-compose up -d
```

## Development

### Testing

```bash
# Run all tests
./mvnw test

# Run specific test class
./mvnw test -Dtest=JobServiceTest

# Run with coverage report
./mvnw clean test jacoco:report
```

### Database Migrations

Migration files are in `src/main/resources/db/migration/`. To add a migration:

1. Create file: `V{version}__{description}.sql`
2. Restart the application - Flyway applies it automatically

## Contributing

### Branch Naming Convention

Always create branches from `develop` using the following naming patterns:

| Prefix | Usage | Example |
|--------|-------|---------|
| `feat/` | New features or functionality | `feat/user-authentication` |
| `fix/` | Bug fixes | `fix/login-validation-error` |
| `update/` | Enhancements to existing features | `update/improve-job-search` |
| `chore/` | Maintenance tasks, refactoring, dependencies | `chore/upgrade-spring-boot` |

**Examples:**
```bash
git checkout develop
git pull origin develop
git checkout -b feat/workflow-management
```

### Commit Message Convention

All commit messages must follow this format:

```
<type>: <description>
```

| Type | When to Use |
|------|-------------|
| `feat:` | Adding new functionality, new endpoints, new features |
| `fix:` | Fixing bugs, resolving issues, correcting behavior |
| `update:` | Improving existing features, enhancing performance, updating logic |
| `chore:` | Code cleanup, dependency updates, configuration changes, refactoring without behavior change |

**Examples:**
```bash
# New feature
git commit -m "feat: add workflow assignment to job creation"

# Bug fix
git commit -m "fix: resolve null pointer in worker validation"

# Enhancement
git commit -m "update: improve job search performance with pagination"

# Maintenance
git commit -m "chore: upgrade Spring Boot to 3.5.5"
```

**Guidelines:**
- Keep the description concise but descriptive (50-72 characters)
- Use imperative mood ("add" not "added", "fix" not "fixed")
- Reference issue numbers when applicable: `fix: resolve login error (#123)`

### Pull Request Process

1. Create a feature branch from `develop`
   ```bash
   git checkout develop
   git pull origin develop
   git checkout -b feat/your-feature-name
   ```

2. Make your changes and commit with proper messages

3. Write/update tests for your changes

4. Update the Postman collection if you modified API endpoints
   ```bash
   # Edit the relevant module in docs/postman/modules/
   npm run postman:build
   ```

5. Push your branch and create a pull request to `develop`
   ```bash
   git push origin feat/your-feature-name
   ```

6. Ensure CI pipeline passes before requesting review