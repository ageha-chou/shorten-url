# ShortenURL
A production-ready URL shortening service built with Java 21 and Spring Boot. 
It supports secure user management, short code generation and resolution, rich analytics, and robust, Redis-backed sliding window rate limiting. 
The project includes comprehensive unit, integration, and stress tests and ships with OpenAPI/Swagger documentation.

## Features
- Short URL creation and redirection
- User registration and management
- Visit tracking and URL analytics
- Sliding-window rate limiting powered by Redis + Lua
- Secure password hashing and authentication via Spring Security
- OpenAPI/Swagger documentation
- Async processing where appropriate
- Extensive test suite (unit, integration, stress)

## Tech Stack
- Java 21, Maven
- Spring Boot (Web, Security, Data JPA, Validation)
- JPA/Hibernate (MySQL)
- Redis (rate limiting, caching)
- Lombok
- OpenAPI/Swagger

## Architecture Overview
- Controller layer: HTTP endpoints for auth, URL management, resolution, and information queries
- Service layer: Business logic for URL generation, resolution, analytics, and users
- Repository layer: Spring Data JPA repositories for persistence
- Security: Spring Security with password encoding and request filtering
- Rate Limiting: Sliding window algorithm implemented with a Redis Lua script
- Configuration: Dedicated configuration for Security, Jackson, Redis, Swagger, and Async

## Prerequisites
- Java 21
- Maven 3.9+
- A relational database (e.g., PostgreSQL, MySQL) with a JDBC driver on the classpath
- Redis 6+ (recommended 7+)

## API Documentation
- Swagger UI: [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)
- OpenAPI JSON: [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

These endpoints might differ if you run on a non-default port or behind a reverse proxy.
## Rate Limiting
- Algorithm: Sliding window
- Backend: Redis with a Lua script for atomic operations
- Scope: Typically per IP and/or per user
- Configuration: Limit values and window sizes are configurable via application properties or environment variables

Ensure Redis is reachable from the application and properly configured. The Lua script used by the limiter is bundled with the application and loaded at runtime.
## Security
- Passwords are securely hashed
- Endpoints are protected by Spring Security
- Public endpoints (e.g., resolving short URLs) and secured endpoints (e.g., management, analytics) are separated by configuration

You may need to configure CORS and authentication/authorization details to match your deployment needs.
## Project Structure (high level)
- src/main/java/.../config: Application configuration (Security, Redis, Swagger, Jackson, Async, etc.)
- src/main/java/.../controller: REST controllers
- src/main/java/.../service: Business services
- src/main/java/.../repository: JPA repositories
- src/main/java/.../entity: Persistence entities
- src/main/resources: Application properties, static/templates (if any), Redis scripts, SQL
- src/test/java: Unit, integration, and stress tests
