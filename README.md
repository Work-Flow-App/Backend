# WorkFlow Management Application

A Spring Boot backend service for managing job postings, applications, and users with JWT authentication and role-based access control.

## Features

- **JWT-based authentication** - Login/signup with token refresh and multi-device support
- **Role-based access control** - ADMIN, COMPANY, WORKER roles with authorization
- **Company management** - Profile management and company dashboard with statistics
- **Worker management** - Create, update, and manage company workers with user accounts
- **Client management** - Store and manage client information
- **Job Templates** - Create dynamic job templates with customizable fields (TEXT, NUMBER, DATE, BOOLEAN, DROPDOWN)
- **Job Management** - Create job instances from templates, track status, and manage field values
- **MySQL database** - With Flyway migrations for schema management
- **RESTful API** - Complete API with OpenAPI/Swagger documentation
- **Docker containerization** - Multi-stage builds with Docker Compose
- **CI/CD pipeline** - GitHub Actions with AWS ECR integration

## Tech Stack

- **Java 21**
- **Spring Boot 3.5.5**
  - Spring Data JPA
  - Spring Security
  - Spring Web
- **MySQL 8.3.0**
- **Flyway** - Database migrations
- **JWT** - Token-based authentication
- **Lombok** - Reduce boilerplate code
- **SpringDoc OpenAPI** - API documentation
- **Docker & Docker Compose**

## Prerequisites

- Java 21
- Maven 3.x (or use included Maven wrapper)
- Docker & Docker Compose
- MySQL 8.x (or use Docker Compose)

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

### Swagger UI
Once the application is running, access the Swagger UI at:
```
http://localhost:8080/swagger-ui.html
```

### API Docs JSON
API documentation JSON is available at:
```
http://localhost:8080/api-docs
```

### Postman Collection
A comprehensive Postman collection is included in the repository:

**File:** `docs/Work-Flow-App-API.postman_collection.json`

**Features:**
- Complete API requests with example payloads
- Pre-configured authentication with token management
- Environment variables for dev/staging/prod
- Test scripts that automatically capture tokens
- Multiple response examples for each endpoint
- Detailed descriptions of all parameters

**Setup:**
1. Import the collection in Postman: **File** → **Import** → Select `docs/Work-Flow-App-API.postman_collection.json`
2. Create a new environment or use the embedded variables
3. Set `base_url` to your environment (default: `https://api.dev.workfloow.app`)
4. Login first to capture JWT token
5. Use the token for subsequent authenticated requests

**Environment Variables:**
- `{{base_url}}` - API base URL (change per environment)
- `{{jwt_token}}` - JWT access token (auto-set after login)
- `{{refresh_token}}` - Refresh token (auto-set after login)

## API Endpoints

### Authentication (Public)

- `POST /api/v1/auth/signup` - Register a new user account
- `POST /api/v1/auth/login` - Login and receive JWT token
- `POST /api/v1/auth/refresh` - Refresh access token using refresh token
- `POST /api/v1/auth/logout` - Logout from current device
- `POST /api/v1/auth/logout-all` - Logout from all devices
- `POST /api/v1/auth/forgot-password` - Request password reset code
- `POST /api/v1/auth/reset-password` - Reset password with verification code

### Company Management (Requires Authentication)

- `GET /api/v1/companies/profile` - Get company profile
- `POST /api/v1/companies/profile` - Update company profile
- `GET /api/v1/companies/dashboard` - Get company dashboard with statistics

### Worker Management (Requires COMPANY Role)

- `POST /api/v1/workers` - Create a new worker
- `GET /api/v1/workers` - Get all workers
- `GET /api/v1/workers/{id}` - Get worker by ID
- `PUT /api/v1/workers/{id}` - Update worker
- `DELETE /api/v1/workers/{id}` - Soft delete worker
- `POST /api/v1/workers/{id}/invite` - Send invitation to worker

### Client Management (Requires COMPANY Role)

- `POST /api/v1/clients` - Create a new client
- `GET /api/v1/clients` - Get all clients
- `GET /api/v1/clients/{id}` - Get client by ID
- `PUT /api/v1/clients/{id}` - Update client
- `DELETE /api/v1/clients/{id}` - Soft delete client

### Job Templates (Requires COMPANY Role)

- `POST /api/v1/job-templates` - Create a new job template
- `GET /api/v1/job-templates` - Get all job templates
- `POST /api/v1/job-templates/fields` - Add field to job template

**Field Types Supported:**
- `TEXT` - Free text input
- `NUMBER` - Numeric input
- `DATE` - Date picker
- `BOOLEAN` - Checkbox/toggle
- `DROPDOWN` - Select from predefined options (requires `options` JSON array)

### Jobs (Requires COMPANY Role)

- `POST /api/v1/jobs` - Create a new job instance
- `GET /api/v1/jobs` - Get all jobs
- `GET /api/v1/jobs/{id}` - Get job by ID
- `PUT /api/v1/jobs/{id}` - Update job
- `DELETE /api/v1/jobs/{id}` - Soft delete job

**Job Status Values:**
- `NEW` - Newly created job
- `IN_PROGRESS` - Job in progress
- `COMPLETED` - Job completed
- `ON_HOLD` - Job on hold
- `CANCELLED` - Job cancelled

### Authentication Header

All protected endpoints require JWT token in Authorization header:

```
Authorization: Bearer <your-jwt-token>
```

**Example:**
```bash
curl -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  http://localhost:8080/api/v1/workers
```

## Running with Docker

### Build Docker image

```bash
docker build -t work-flow-backend .
```

### Run with Docker Compose

```bash
docker-compose up -d
```

This starts both MySQL and the backend application.

## Development

### Testing

The project includes comprehensive test coverage with both unit tests and integration tests.

#### Run Tests

```bash
# Run all tests (unit + integration)
./mvnw test

# Run only unit tests
./mvnw test -Dtest="*ServiceTest"

# Run only integration tests
./mvnw test -Dtest="*IntegrationTest"

# Run a specific test class
./mvnw test -Dtest=JwtServiceTest

# Run with coverage report
./mvnw clean test jacoco:report
```

#### Test Structure

**Unit Tests** (`src/test/java/com/workflow/service/`)
- **JwtServiceTest** (13 tests) - JWT token generation, validation, expiration, signature verification
- **AuthenticationServiceTest** (12 tests) - User authentication, token generation, error handling
- **UserServiceTest** (17 tests) - User creation, validation, password encoding, role assignment

**Integration Tests** (`src/test/java/com/workflow/controller/`)
- **AuthControllerIntegrationTest** (20 tests) - Full API endpoint testing with Spring context
  - Signup endpoint validation
  - Login endpoint validation
  - Request/response validation
  - Security testing
  - Edge cases and error handling

**Test Coverage:**
- **42 Unit Tests** covering service layer business logic
- **20 Integration Tests** covering API endpoints with full Spring Boot context
- **Total: 62 Tests** ✅

#### Test Configuration

Tests use H2 in-memory database configured in `src/test/resources/application-test.yml`:
- No need for running MySQL during tests
- Tests are isolated and repeatable
- Fast execution with in-memory database

#### Writing New Tests

**Unit Test Example:**
```java
@ExtendWith(MockitoExtension.class)
class MyServiceTest {
    @Mock
    private MyRepository repository;

    @InjectMocks
    private MyService service;

    @Test
    void shouldDoSomething() {
        // Given
        when(repository.findById(1L)).thenReturn(Optional.of(entity));

        // When
        Result result = service.doSomething(1L);

        // Then
        assertNotNull(result);
        verify(repository).findById(1L);
    }
}
```

**Integration Test Example:**
```java
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class MyControllerIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldReturnSuccess() throws Exception {
        mockMvc.perform(post("/api/v1/endpoint")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"key\":\"value\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result").exists());
    }
}
```

### Database Migrations

Database schema changes are managed using Flyway. Migration files are located in:

```
src/main/resources/db/migration/
```

To create a new migration:

1. Create a new file: `V{version}__{description}.sql`
2. Write your SQL migration
3. Restart the application - Flyway will automatically apply it

Example: `V3__add_job_postings_table.sql`

### Project Structure

```
src/main/java/com/workflow/
├── common/              # Constants, validators, exceptions
├── config/              # Security, OpenAPI, and bean configurations
├── controller/          # REST API endpoints
├── dto/                 # Data Transfer Objects
├── entity/              # JPA entities
├── repository/          # Spring Data JPA repositories
└── service/             # Business logic layer
```

## User Roles

- **ADMIN** - Full system access
- **COMPANY** - Post jobs and manage applications
- **WORKER** - Apply for jobs

## Environment Profiles

- **Development**: `.env.dev` or `.env.local`
- **Production**: `.env.prod`

## CI/CD

The project uses GitHub Actions for CI/CD:

- **CI Pipeline** (`ci.yml`): Builds Docker image and pushes to AWS ECR on push to `develop` branch
- **CD Pipeline** (`cd.yml`): Deployment automation

## Security

- Passwords are encrypted using BCrypt
- JWT tokens expire after a configured time period
- Stateless authentication (no server-side sessions)
- CSRF protection disabled (stateless API)
- All endpoints require authentication except login, signup, and Swagger UI

## Contributing

1. Create a feature branch from `develop`
2. Make your changes
3. Write/update tests
4. Submit a pull request to `develop`