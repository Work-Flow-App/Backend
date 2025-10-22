# WorkFlow Management Application

A Spring Boot backend service for managing job postings, applications, and users with JWT authentication and role-based access control.

## Features

- JWT-based authentication (login/signup)
- Role-based access control (ADMIN, COMPANY, WORKER)
- MySQL database with Flyway migrations
- RESTful API with OpenAPI/Swagger documentation
- Docker containerization with multi-stage builds
- CI/CD pipeline with GitHub Actions and AWS ECR

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

Once the application is running, access the Swagger UI at:

```
http://localhost:8080/swagger-ui.html
```

API documentation JSON is available at:

```
http://localhost:8080/api-docs
```

## API Endpoints

### Authentication

- `POST /api/v1/auth/signup` - Create a new user account
- `POST /api/v1/auth/login` - Login and receive JWT token

### Protected Endpoints

Include the JWT token in the Authorization header:

```
Authorization: Bearer <your-jwt-token>
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

### Run tests

```bash
# Run all tests
./mvnw test

# Run a specific test class
./mvnw test -Dtest=WorkFlowManagementApplicationTests
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