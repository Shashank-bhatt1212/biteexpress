# BiteExpress

BiteExpress is a restaurant discovery REST API built on Spring Boot 3.4.1 and
MongoDB. It exposes a small set of endpoints for creating restaurants, fetching
them by id, and listing/searching them with structured filters, pagination,
geo-aware distance search, and a dependency-free natural-language query parser.

On first boot against an empty database, the app seeds itself from the bundled
`zomato.csv` (packaged on the classpath inside the jar). Seeding is guarded by a
`count() == 0` check, so restarts do not re-seed.

## Prerequisites

- JDK 21
- The bundled Maven wrapper (`mvnw.cmd` on Windows / `mvnw` on Unix) — no local
  Maven install required
- Docker — required to run the app via Docker Compose, and required for the
  integration tests (they use Testcontainers, which needs a running Docker
  daemon)

## Configuration

The MongoDB connection is read from a single property with an environment
override:

```yaml
spring:
  data:
    mongodb:
      uri: ${SPRING_DATA_MONGODB_URI:mongodb://localhost:27017/biteexpress}
```

Set `SPRING_DATA_MONGODB_URI` to point at any MongoDB instance. If unset, the
app defaults to `mongodb://localhost:27017/biteexpress`. Docker Compose sets this
to `mongodb://mongo:27017/biteexpress` so the app reaches the `mongo` service.

## Run locally

Start a local MongoDB listening on `mongodb://localhost:27017/biteexpress`, then
run the app with the Maven wrapper:

```powershell
.\mvnw.cmd spring-boot:run
```

The API comes up on `http://localhost:8080`.

## Run with Docker Compose

This builds the app image (multi-stage Dockerfile) and starts MongoDB plus the
app. The app waits for MongoDB to report healthy before starting.

```powershell
docker compose up --build
```

- MongoDB is published on `localhost:27017` (named volume `mongo-data` persists
  the data).
- The app is published on `localhost:8080`.

To override the database URI used by the app, set `SPRING_DATA_MONGODB_URI` in
the `app` service environment (the compose file already wires it to the `mongo`
service).

Stop and remove the containers with:

```powershell
docker compose down
```

## Endpoints

Base path: `/v1/restaurants`. Distances are expressed in **meters**.

### 1. Create a restaurant

`POST /v1/restaurants` — returns `201 Created` with a `Location` header pointing
at the new resource and the created restaurant in the body.

```powershell
curl -i -X POST http://localhost:8080/v1/restaurants `
  -H "Content-Type: application/json" `
  -d '{
    "restaurantId": 9000001,
    "name": "Spice Garden",
    "countryCode": 1,
    "city": "Gurgaon",
    "address": "DLF Phase 3, Gurgaon",
    "latitude": 28.4941,
    "longitude": 77.0916,
    "cuisines": ["South Indian", "Chinese"],
    "averageCostForTwo": 800,
    "currency": "Indian Rupees(Rs.)",
    "priceRange": 2,
    "aggregateRating": 4.3,
    "ratingText": "Very Good",
    "votes": 320
  }'
```

Example response (`201 Created`, `Location: /v1/restaurants/<id>`):

```json
{
  "id": "66b0f0c2e4a1b2c3d4e5f607",
  "restaurantId": 9000001,
  "Name": "Spice Garden",
  "Address": "DLF Phase 3, Gurgaon",
  "City": "Gurgaon",
  "Country": "India",
  "Cuisines": ["South Indian", "Chinese"],
  "Average Rating": 4.3,
  "ratingText": "Very Good",
  "averageCostForTwo": 800,
  "currency": "Indian Rupees(Rs.)",
  "priceRange": 2
}
```

### 2. Fetch a restaurant by id

`GET /v1/restaurants/{id}` — the `{id}` is the Mongo `_id` string returned at
creation. Returns `200 OK`, or `404 Not Found` with an error body if no match.

```powershell
curl http://localhost:8080/v1/restaurants/66b0f0c2e4a1b2c3d4e5f607
```

### 3. List restaurants (filter / sort / paginate)

`GET /v1/restaurants` — all query parameters are optional:

| Param | Meaning |
|-------|---------|
| `name` | case-insensitive substring match on name |
| `city` | case-insensitive exact match on city |
| `minRating` | keep restaurants with rating `>=` this value (0.0–5.0) |
| `cuisines` | one or more cuisines; matches if any overlaps (repeat the param) |
| `maxCostForTwo` | keep restaurants with average cost for two `<=` this value |
| `lat`, `lng` | reference point; supply both to enable distance search |
| `radius` | max distance in meters (only meaningful with `lat`+`lng`) |
| `sort` | one of `name`, `rating`, `distance` |
| `direction` | `asc` or `desc` |
| `page` | 0-based page index (default `0`) |
| `size` | page size (default `20`, capped at `100`) |

Structured example — best-rated South Indian places in Gurgaon:

```powershell
curl "http://localhost:8080/v1/restaurants?city=Gurgaon&cuisines=South%20Indian&minRating=4&sort=rating&direction=desc&size=1"
```

Geo example — restaurants within 5 km of a point, nearest first (note each item
carries a `Distance` field in meters):

```powershell
curl "http://localhost:8080/v1/restaurants?lat=28.4941&lng=77.0916&radius=5000&sort=distance&direction=asc&size=1"
```

Example paged response:

```json
{
  "content": [
    {
      "id": "66b0f0c2e4a1b2c3d4e5f607",
      "restaurantId": 9000001,
      "Name": "Spice Garden",
      "Address": "DLF Phase 3, Gurgaon",
      "City": "Gurgaon",
      "Country": "India",
      "Cuisines": ["South Indian", "Chinese"],
      "Average Rating": 4.3,
      "ratingText": "Very Good",
      "averageCostForTwo": 800,
      "currency": "Indian Rupees(Rs.)",
      "priceRange": 2,
      "Distance": 0.0
    }
  ],
  "page": 0,
  "size": 1,
  "totalElements": 1,
  "totalPages": 1
}
```

The `Distance` field appears only when `lat` and `lng` are supplied; it is
omitted otherwise.

### 4. Search with natural language

`GET /v1/restaurants/search` — accepts everything the listing endpoint accepts
plus a required `q` parameter containing a natural-language query. The query is
parsed locally (no external service) for city, cuisines, a price cap
(`under Rs. N`), and nearest intent, then merged with any explicit structured
params (explicit params win).

```powershell
curl "http://localhost:8080/v1/restaurants/search?q=Best%20restaurants%20in%20Gurgaon%20with%20South%20Indian%20Food"
```

More examples the parser understands:

```powershell
curl "http://localhost:8080/v1/restaurants/search?q=Restaurants%20in%20Gurgaon%20under%20Rs.%201000%20for%20two"
curl "http://localhost:8080/v1/restaurants/search?q=Nearest%20place%20that%20serves%20Vada%20Pav"
```

If a query expresses nearest intent but no `lat`/`lng` is supplied, the search
falls back to rating sort while still applying the other parsed filters.

The search endpoint returns the same `PagedResponse` shape as the listing
endpoint.

## Run the tests

The test suite includes integration tests that use Testcontainers, so a running
Docker daemon is required.

```powershell
.\mvnw.cmd test
```

To build the jar without running tests:

```powershell
.\mvnw.cmd -q -DskipTests package
```
