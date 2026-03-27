# Architecture — Movie Service

## Overview

The Movie Service is a stateless Spring Boot service responsible for:

- Movie catalog management (CRUD)
- Paginated listing and multi-filter search
- JWT-secured write access (admin only)
- Exposing `averageRating` updates for the rating-service

It is one of five services that make up the Neo4flix backend, exposed to clients exclusively through the API Gateway.

---

## Technology Stack

| Layer | Technology |
|-------|-----------|
| Runtime | Java 25 |
| Framework | Spring Boot 4.0.4 · Spring MVC (servlet stack) |
| Security | Spring Security 7 · JJWT 0.12.5 (HS256) |
| Persistence | Spring Data Neo4j · Neo4j 5+ |
| Validation | Jakarta Bean Validation 3 · Custom `@MaxCurrentYearPlus` constraint |
| Build | Maven 3.9+ |

---

## Position in the System

```
                        ┌──────────────────────────────────────────────┐
                        │                 Neo4flix Backend             │
                        │                                              │
  Client (Angular) ───► │  API Gateway :8080                          │
                        │       │                                      │
                        │       ├──► User Microservice    :8082        │
                        │       ├──► Movie Service        :8083  ◄─── │
                        │       ├──► Rating Service       :8084        │
                        │       └──► Recommendation Svc   :8085        │
                        └──────────────────────────────────────────────┘
                                           │
                                       Neo4j :7687
                                    (neo4j database)
```

The API Gateway:
- Validates JWT signatures on protected routes before forwarding
- Routes `/api/movies/**` to this service
- `GET` requests are forwarded without a token requirement

This service is called by the **user-microservice** to validate that a `movieId` exists before adding it to a watchlist (`GET /api/movies/{id}`). The **rating-service** calls `updateAverageRating()` on this service whenever a new rating is submitted.

---

## Package Structure

```
io.github.johneliud.movie_service/
│
├── config/
│   └── SecurityConfig.java              # Filter chain, method security
│
├── controller/
│   └── MovieController.java             # GET|POST|PUT|DELETE /api/movies/**
│
├── dto/
│   ├── MovieRequest.java                # Create/update input (record)
│   ├── MovieResponse.java               # API response (record)
│   └── PagedResponse.java               # Generic paginated wrapper (record)
│
├── entity/
│   └── Movie.java                       # @Node — Neo4j graph node
│
├── exception/
│   └── GlobalExceptionHandler.java      # @RestControllerAdvice → RFC 9457 ProblemDetail
│
├── repository/
│   └── MovieRepository.java             # Neo4jRepository + custom @Query search
│
├── security/
│   └── JwtAuthenticationFilter.java     # OncePerRequestFilter — validates Bearer tokens
│
├── service/
│   └── MovieService.java                # CRUD + search + averageRating update
│
├── util/
│   └── JwtUtil.java                     # Token validation and claim extraction
│
└── validation/
    ├── MaxCurrentYearPlus.java           # Custom constraint annotation
    └── MaxCurrentYearPlusValidator.java  # Validates releaseYear ≤ currentYear + N
```

---

## Layered Architecture

```
  HTTP Request
       │
       ▼
  ┌─────────────────────────────────┐
  │  Spring Security Filter Chain   │
  │  JwtAuthenticationFilter        │  Validates Bearer token, populates SecurityContext
  └─────────────────────────────────┘
       │
       ▼
  ┌─────────────────────────────────┐
  │  Controller Layer               │  Maps HTTP ↔ DTO, delegates to service
  └─────────────────────────────────┘
       │
       ▼
  ┌─────────────────────────────────┐
  │  Service Layer                  │  Business logic, entity mapping, blank-to-null
  └─────────────────────────────────┘
       │
       ▼
  ┌─────────────────────────────────┐
  │  Repository Layer               │  Spring Data Neo4j — CRUD + custom Cypher queries
  └─────────────────────────────────┘
       │
       ▼
  ┌─────────────────────────────────┐
  │  Neo4j                          │  Graph database — Movie nodes
  └─────────────────────────────────┘
```

Controllers are thin — they parse query parameters, build `Pageable`, and delegate immediately to the service. No business logic lives in controllers.

---

## Security Architecture

### JWT Validation

Tokens are **issued by the user-microservice** and only **validated** here. `JwtUtil` uses the shared `jwt.secret` to verify the signature and extract claims.

Token structure (HS256):
```
{
  "sub": "<user UUID>",
  "role": "ROLE_USER" | "ROLE_ADMIN",
  "iat": <unix timestamp>,
  "exp": <unix timestamp>
}
```

### Filter Chain

```
Incoming request
       │
       ├── GET /api/movies/**  ──► permitAll (no filter processing required)
       │
       └── POST | PUT | DELETE /api/movies/** ──► JwtAuthenticationFilter
                                                        │
                                              extract Authorization: Bearer <token>
                                                        │
                                              JwtUtil.isTokenValid(token)
                                                        │
                                              ┌─────────┴─────────┐
                                            valid               invalid
                                              │                   │
                                      set SecurityContext    clear SecurityContext
                                      (userId, role)        (Spring Security → 401)
                                              │
                                      @PreAuthorize("hasAuthority('ROLE_ADMIN')")
                                              │
                                      controller method
```

### Role-Based Access

| Endpoint pattern | Required role |
|-----------------|--------------|
| `GET /api/movies/**` | Public |
| `POST /api/movies` | `ROLE_ADMIN` |
| `PUT /api/movies/{id}` | `ROLE_ADMIN` |
| `DELETE /api/movies/{id}` | `ROLE_ADMIN` |

Admin authorization is enforced by `@PreAuthorize("hasAuthority('ROLE_ADMIN')")` on controller methods, backed by `@EnableMethodSecurity` in `SecurityConfig`.

---

## Neo4j Data Model

```
┌──────────────────────────────────────┐
│          Movie (:Movie)              │
├──────────────────────────────────────┤
│ id            String (UUID)  PK      │
│ title         String                 │
│ genres        List<String>           │
│ releaseYear   Integer                │
│ description   String                 │
│ posterUrl     String                 │
│ averageRating Double                 │
│ createdAt     LocalDateTime          │
└──────────────────────────────────────┘
```

The `id` is a UUID string generated by `UUIDStringGenerator` on first save. `averageRating` starts as `null` and is updated by the rating-service via `MovieService.updateAverageRating()`.

No relationships between nodes are defined at this layer — the recommendation-service and rating-service manage those separately.

---

## Search Implementation

The `MovieRepository.search()` method uses a custom Cypher query with optional filters:

```cypher
MATCH (m:Movie)
WHERE ($title IS NULL OR toLower(m.title) CONTAINS toLower($title))
  AND ($genre IS NULL OR $genre IN m.genres)
  AND ($releaseYearFrom IS NULL OR m.releaseYear >= $releaseYearFrom)
  AND ($releaseYearTo IS NULL OR m.releaseYear <= $releaseYearTo)
RETURN m
ORDER BY m.title ASC
SKIP $skip LIMIT $limit
```

- Each filter is independently optional — passing `NULL` skips that condition
- `MovieService.blankToNull()` converts empty/blank strings to `null` before passing to the query
- A matching `countQuery` powers `Page.getTotalElements()` and `Page.getTotalPages()`

---

## Custom Validation

`@MaxCurrentYearPlus` is a custom Bean Validation constraint on `MovieRequest.releaseYear`:

```
@MaxCurrentYearPlus(years = 10)
```

At runtime, `MaxCurrentYearPlusValidator` computes `LocalDate.now().getYear() + 10` — the ceiling advances automatically each year without code changes. `null` values pass validation (the field is optional).

---

## Error Handling

`GlobalExceptionHandler` (`@RestControllerAdvice`) maps exceptions to RFC 9457 `ProblemDetail` responses:

| Exception | HTTP Status |
|-----------|------------|
| `MethodArgumentNotValidException` | 400 — field-level validation errors with `errors` map |
| `IllegalArgumentException` | 400 — resource not found or business rule violation |
| `IllegalStateException` | 422 — upstream service unavailable |
| `Exception` (catch-all) | 500 — unexpected server error |

---

## Configuration

All sensitive values are externalized via environment variables in production and via `application-secrets.properties` locally. No credentials are committed to source control.

| Property | Env variable | Description |
|----------|-------------|-------------|
| `jwt.secret` | `JWT_SECRET` | HS256 signing key — must match user-microservice |
| `spring.neo4j.uri` | `NEO4J_URI` | Neo4j Bolt URI (e.g. `bolt://localhost:7687`) |
| `spring.neo4j.authentication.username` | `NEO4J_USERNAME` | Neo4j username |
| `spring.neo4j.authentication.password` | `NEO4J_PASSWORD` | Neo4j password |
| `spring.data.neo4j.database` | `NEO4J_DATABASE` | Database name (default `neo4j`) |
| `server.port` | `SERVER_PORT` | Default 8083 |