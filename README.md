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

Always cut branches from `develop`. Format: `<type>/<short-description>`

| Type | When to use | Example |
|---|---|---|
| `feat/` | New feature or endpoint | `feat/paddle-payments` |
| `fix/` | Bug fix | `fix/auth-token-expiry` |
| `hotfix/` | Urgent production fix (cut from `main`) | `hotfix/invoice-crash` |
| `chore/` | Maintenance, deps, config | `chore/upgrade-spring-boot` |
| `refactor/` | Code restructure, no behaviour change | `refactor/job-service-n-plus-one` |
| `test/` | Adding or fixing tests | `test/worker-invitation-coverage` |
| `docs/` | Documentation only | `docs/api-readme-update` |
| `ci/` | CI/CD pipeline changes | `ci/add-coverage-gate` |
| `release/` | Release preparation | `release/v1.2.0` |

```bash
git checkout develop && git pull origin develop
git checkout -b feat/your-feature-name
```

### Commit Message Convention

All commits must follow [Conventional Commits](https://www.conventionalcommits.org/):

```
<type>(<scope>): <subject>
```

- **type** — required, lowercase
- **scope** — optional, e.g. `auth`, `worker`, `job`, `workflow`, `invoice`, `company`, `db`, `ci`
- **subject** — imperative, lowercase, no trailing period, max 72 chars

| Type | When to use |
|---|---|
| `feat` | New feature or endpoint |
| `fix` | Bug fix |
| `refactor` | Code change with no behaviour change |
| `test` | Adding or updating tests |
| `chore` | Maintenance, deps, config, migrations |
| `ci` | CI/CD pipeline changes |
| `docs` | Documentation only |
| `perf` | Performance improvement |
| `revert` | Reverts a previous commit |

**Examples:**
```bash
feat(auth): add Google Sign-In via frontend token verification
fix(workflow): add missing workflow ID in response
refactor(service): eliminate N+1 queries in job service
test(worker): add integration tests for invitation flow
chore(db): add Flyway migration V11 for subscription table
```

**Rules:**
- Imperative mood: `add` not `added`, `fix` not `fixed`
- Merge and revert commits are exempt from validation

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