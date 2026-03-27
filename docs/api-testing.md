# API Testing Guide — Movie Service

This guide covers testing every endpoint using **Postman** and **curl**.

Base URL: `http://localhost:8083`

---

## Postman Setup

### Environment variables

Create a Postman environment called **Neo4flix - Local** (or extend the existing one) with these variables:

| Variable | Initial Value | Description |
|----------|--------------|-------------|
| `movie_base_url` | `http://localhost:8083` | Movie service base URL |
| `access_token` | *(from user-microservice login)* | Admin JWT for write endpoints |
| `movie_id` | *(empty)* | Set after creating a movie |

### Authorization

For admin endpoints, set the **Authorization** tab:
- Type: `Bearer Token`
- Token: `{{access_token}}`

To obtain an admin token, log in via the user-microservice (`POST http://localhost:8082/api/auth/login`) with an account that has `ROLE_ADMIN`.

### Auto-capture movie ID

Add this **Tests** script to the `POST /api/movies` request:

```javascript
const body = pm.response.json();
if (body.id) {
    pm.environment.set("movie_id", body.id);
}
```

---

## Movies

### Create a movie

**POST** `{{movie_base_url}}/api/movies`

Headers:
- `Content-Type: application/json`
- `Authorization: Bearer {{access_token}}`

Body:
```json
{
  "title": "Inception",
  "genres": ["Sci-Fi", "Thriller"],
  "releaseYear": 2010,
  "description": "A thief who steals corporate secrets through dream-sharing technology.",
  "posterUrl": "https://example.com/inception.jpg"
}
```

Expected: `201 Created` with the full movie object.

curl:
```bash
curl -s -X POST http://localhost:8083/api/movies \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Inception",
    "genres": ["Sci-Fi", "Thriller"],
    "releaseYear": 2010,
    "description": "A thief who steals corporate secrets through dream-sharing technology.",
    "posterUrl": "https://example.com/inception.jpg"
  }' | jq .
```

---

### List all movies

**GET** `{{movie_base_url}}/api/movies`

No authentication required.

**With pagination and sorting:**

```
GET {{movie_base_url}}/api/movies?page=0&size=10&sort=releaseYear&direction=desc
```

Expected: `200 OK` with a paged response.

curl:
```bash
curl -s "http://localhost:8083/api/movies?page=0&size=10&sort=title&direction=asc" | jq .
```

---

### Search movies

**GET** `{{movie_base_url}}/api/movies/search`

No authentication required. All parameters are optional.

**By title:**
```
GET {{movie_base_url}}/api/movies/search?title=inception
```

**By genre:**
```
GET {{movie_base_url}}/api/movies/search?genre=Sci-Fi
```

**By release year range:**
```
GET {{movie_base_url}}/api/movies/search?releaseYearFrom=2000&releaseYearTo=2015
```

**Combined:**
```
GET {{movie_base_url}}/api/movies/search?genre=Thriller&releaseYearFrom=2010&releaseYearTo=2020
```

Expected: `200 OK` with filtered paged response.

curl:
```bash
curl -s "http://localhost:8083/api/movies/search?genre=Sci-Fi&releaseYearFrom=2000&releaseYearTo=2020" | jq .
```

---

### Get movie by ID

**GET** `{{movie_base_url}}/api/movies/{{movie_id}}`

No authentication required.

Expected: `200 OK` with a single movie object.

curl:
```bash
curl -s "http://localhost:8083/api/movies/$MOVIE_ID" | jq .
```

---

### Update a movie

**PUT** `{{movie_base_url}}/api/movies/{{movie_id}}`

Headers:
- `Content-Type: application/json`
- `Authorization: Bearer {{access_token}}`

Body (all fields replace the existing values):
```json
{
  "title": "Inception (Director's Cut)",
  "genres": ["Sci-Fi", "Thriller", "Action"],
  "releaseYear": 2010,
  "description": "Updated description.",
  "posterUrl": "https://example.com/inception-updated.jpg"
}
```

Expected: `200 OK` with the updated movie object.

curl:
```bash
curl -s -X PUT "http://localhost:8083/api/movies/$MOVIE_ID" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Inception (Director'\''s Cut)",
    "genres": ["Sci-Fi", "Thriller", "Action"],
    "releaseYear": 2010,
    "description": "Updated description.",
    "posterUrl": "https://example.com/inception-updated.jpg"
  }' | jq .
```

---

### Delete a movie

**DELETE** `{{movie_base_url}}/api/movies/{{movie_id}}`

Headers: `Authorization: Bearer {{access_token}}`

Expected: `204 No Content`.

curl:
```bash
curl -s -o /dev/null -w "%{http_code}" \
  -X DELETE "http://localhost:8083/api/movies/$MOVIE_ID" \
  -H "Authorization: Bearer $ACCESS_TOKEN"
```

---

## Validation errors

### Missing title

```bash
curl -s -X POST http://localhost:8083/api/movies \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"title": "", "releaseYear": 2010}' | jq .
```

Expected: `400 Bad Request`
```json
{
  "title": "Validation Error",
  "status": 400,
  "detail": "Validation failed",
  "errors": {
    "title": "Title is required"
  }
}
```

### Release year out of range

```bash
curl -s -X POST http://localhost:8083/api/movies \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"title": "Old Film", "releaseYear": 1800}' | jq .
```

Expected: `400 Bad Request` with `"releaseYear": "Release year must be 1990 or later"`.

---

## Common Error Responses

All errors follow RFC 9457 Problem Details format:

```json
{
  "type": "about:blank",
  "title": "Bad Request",
  "status": 400,
  "detail": "Movie not found with id: abc-123",
  "instance": "/api/movies/abc-123"
}
```

| Status | Meaning |
|--------|---------|
| 400 | Validation failure or movie not found |
| 401 | Missing or invalid JWT token |
| 403 | Authenticated but role is not `ROLE_ADMIN` |
| 500 | Unexpected server error |

---

## Postman Collection (import-ready)

Build the collection by:

1. Creating a new collection named **Neo4flix - Movie Service**
2. Adding a folder **Movies**
3. Setting collection-level variable `movie_base_url` to `http://localhost:8083`
4. Setting the collection authorization to `Bearer Token` with `{{access_token}}`
5. Adding the movie ID capture script from [Postman Setup](#postman-setup) to the `POST /api/movies` request