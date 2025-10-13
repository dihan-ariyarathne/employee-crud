# Spring Boot + MongoDB Atlas Employee CRUD App — Requirements & Blueprint
*Last updated:* 2025-10-12 03:13 UTC  
*Target audience:* Full‑stack developer using VS Code + ChatGPT extension  
*Goal:* Ship a **dockerized, resource‑efficient** CRUD web app for an Employee database hosted on **MongoDB Atlas**, with a UI and **auto‑schema discovery** from the connected collection. Include CI/CD via GitHub Actions for build, test, containerize, and deploy.

---

## 1) Product Overview

### 1.1 Objectives
- Connect to an existing MongoDB Atlas cluster and **auto‑detect the fields** present in the `employees` collection (schema inference).
- Provide a modern **web UI** to view, search, create, update, and delete employees with **dynamic forms** generated from inferred fields.
- Optimize for **low resource usage** and **fast startup**.
- Ship as Docker images (frontend + backend) and support **one‑command deploy** on another host (Docker Compose).
- Provide **CI/CD** to build, test, lint, containerize, push images to a registry, and optionally auto‑deploy.

### 1.2 Tech Stack
- **Backend**: Java 21, Spring Boot 3 (WebFlux, Spring Data MongoDB Reactive), Validation, Actuator, SpringDoc OpenAPI.
- **Frontend**: React + Vite + TypeScript, React Query, React Hook Form, TailwindCSS.
- **Database**: MongoDB Atlas.
- **CI/CD**: GitHub Actions, GHCR (or Docker Hub).
- **Container**: Multi‑stage Dockerfiles; distroless or Alpine JRE; Java layered jars; Node 20 LTS for UI build.

---

## 2) High‑Level Architecture

```
[Browser UI]  <—HTTP/JSON—>  [Spring Boot (WebFlux) API]  <—Reactive Driver—>  [MongoDB Atlas]
     |                                |                                  |
     +— Static assets (Nginx or Vite preview prod build)                 |
```

- Reactive stack to minimize threads and memory under load.
- **Schema Discovery Service** inspects a sample of documents (configurable) to infer fields & types; result cached + refreshable.

---

## 3) Functional Requirements

1. **Connect to Mongo Atlas**
   - Provide secure configuration via environment variables.
   - Validate connectivity on startup; expose `/actuator/health` with Mongo indicator.

2. **Auto‑Schema Discovery**
   - Endpoint: `GET /api/schema?collection=employees&sampleSize=500`
   - Merge keys from sampled documents; infer basic types (`string`, `number`, `boolean`, `date`, `array`, `object`) and optionality.
   - Cache schema in memory; TTL configurable; manual refresh: `POST /api/schema/refresh`.

3. **Employee CRUD**
   - Endpoints (pluralized `/api/employees`):
     - `GET /api/employees` with pagination, sorting, search (by any field), filter by field equality/contains.
     - `GET /api/employees/{id}`
     - `POST /api/employees`
     - `PUT /api/employees/{id}`
     - `PATCH /api/employees/{id}`
     - `DELETE /api/employees/{id}`
   - Support **dynamic fields** beyond a fixed POJO; store as `Map<String, Object>` plus metadata.
   - Soft delete optional via `deleted` flag.

4. **UI**
   - **Dynamic form generation** from `/api/schema` result.
   - Employee list with column chooser from inferred fields.
   - Search box + filter builder.
   - Add/Edit modal with validation rules inferred (e.g., type, required if always present).
   - Toast notifications, optimistic updates (React Query).
   - Basic auth/login optional (stretch).

5. **Indexes**
   - Ensure indexes on `_id`, frequently searched fields (e.g., `email`, `lastName`), and any text index for “quick search”.
   - Startup index creator with idempotent checks.

6. **Observability**
   - `/actuator` metrics; Prometheus format optional.
   - Structured JSON logs.

---

## 4) Non‑Functional Requirements

- **Performance**: Cold start < 2s on a typical VM, low idle memory (< 200MB) target.
- **Scalability**: Stateless backend; replica‑ready.
- **Security**: Use Atlas SRV connection string; credentials via secrets; CORS restricted list; input validation.
- **Portability**: Docker images; compose file for single‑host deploy.
- **Maintainability**: Clean module boundaries; typed API via OpenAPI.

---

## 5) Configuration & Secrets

Environment variables (back + front):

**Backend (.env)**
```
SPRING_PROFILES_ACTIVE=prod
MONGODB_URI=mongodb+srv://<user>:<pass>@<cluster>/<db>?retryWrites=true&w=majority
MONGODB_DB=<dbName>
MONGODB_EMP_COLLECTION=employees
SCHEMA_SAMPLE_SIZE=500
SCHEMA_CACHE_TTL_SEC=300
SERVER_PORT=8080
CORS_ALLOWED_ORIGINS=https://your-ui-host, http://localhost:5173
LOG_LEVEL=INFO
```

**Frontend (.env)**
```
VITE_API_BASE_URL=http://localhost:8080
```

Provide `.env.sample` files and never commit real credentials.

---

## 6) Repository & Folder Structure (Monorepo)

```
employee-crud/
├─ backend/
│  ├─ src/main/java/com/acme/employee/
│  │  ├─ EmployeeCrudApplication.java
│  │  ├─ config/
│  │  │  ├─ MongoConfig.java
│  │  │  └─ CorsConfig.java
│  │  ├─ controller/
│  │  │  ├─ EmployeeController.java
│  │  │  └─ SchemaController.java
│  │  ├─ domain/
│  │  │  └─ EmployeeDocument.java
│  │  ├─ dto/
│  │  │  └─ QueryRequest.java
│  │  ├─ repository/
│  │  │  └─ EmployeeRepository.java
│  │  ├─ service/
│  │  │  ├─ EmployeeService.java
│  │  │  └─ SchemaDiscoveryService.java
│  │  ├─ util/
│  │  │  └─ TypeInference.java
│  │  └─ advice/
│  │     └─ GlobalExceptionHandler.java
│  ├─ src/main/resources/
│  │  ├─ application.yml
│  │  └─ logback.xml
│  ├─ src/test/java/...
│  ├─ pom.xml
│  ├─ Dockerfile
│  └─ Makefile
├─ frontend/
│  ├─ src/
│  │  ├─ main.tsx
│  │  ├─ App.tsx
│  │  ├─ api/client.ts
│  │  ├─ pages/EmployeesPage.tsx
│  │  ├─ components/
│  │  │  ├─ DataTable.tsx
│  │  │  ├─ DynamicForm.tsx
│  │  │  └─ Toasts.tsx
│  │  └─ hooks/useSchema.ts
│  ├─ public/
│  ├─ index.html
│  ├─ vite.config.ts
│  ├─ package.json
│  ├─ tsconfig.json
│  ├─ tailwind.config.js
│  ├─ postcss.config.js
│  ├─ Dockerfile
│  └─ Makefile
├─ deploy/
│  ├─ docker-compose.yml
│  └─ nginx/
│     └─ default.conf
├─ .github/workflows/
│  ├─ ci.yml
│  └─ release.yml
├─ .env.sample
├─ README.md
└─ LICENSE
```

---

## 7) Backend Design Details

### 7.1 Data Model (Flexible)
```java
// domain/EmployeeDocument.java
@Document(collection = "#{@employeeCollectionName}")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class EmployeeDocument {
  @Id private String id;
  private Map<String, Object> fields;  // dynamic attributes
  private Instant createdAt;
  private Instant updatedAt;
  private Boolean deleted;
}
```
- `@employeeCollectionName` bean pulls from `MONGODB_EMP_COLLECTION`.

### 7.2 Reactive Repository
```java
public interface EmployeeRepository extends ReactiveMongoRepository<EmployeeDocument, String> {
  // Additional query methods if needed
}
```

### 7.3 Schema Discovery (Core Logic)
- Sample `N` docs: `mongoTemplate.find(new Query().limit(sampleSize))`.
- Union all keys in `fields` + top-level metadata.
- For each key, infer type by scanning values:
  - If mixed: report `mixed`, include counts per type.
  - Optionality: `presenceRatio` in [0..1].
- Cache result in `Caffeine` or simple `ConcurrentHashMap` + TTL.
- Expose `GET /api/schema`, `POST /api/schema/refresh`.

**Example JSON**
```json
{
  "collection":"employees",
  "sampleSize":500,
  "fields":{
    "firstName": {"type":"string","presenceRatio":1.0},
    "lastName": {"type":"string","presenceRatio":0.98},
    "email": {"type":"string","presenceRatio":0.95,"uniqueHint":true},
    "age": {"type":"number","presenceRatio":0.75},
    "hireDate": {"type":"date","presenceRatio":0.88},
    "skills": {"type":"array","itemType":"string","presenceRatio":0.6}
  }
}
```

### 7.4 Query API
`POST /api/employees/query`
```json
{
  "page": 0,
  "size": 20,
  "sort": [{"field":"lastName","dir":"asc"}],
  "filters": [{"field":"skills","op":"contains","value":"java"}],
  "search": "doe"
}
```
- Translate to reactive `Criteria` (text index for `search` optional).

### 7.5 Validation
- Server infers minimal rules from schema (e.g., Date parsable, Email regex if key is `email`).
- Additional declarative validations configurable.

### 7.6 Indexer
- On startup, ensure indexes:
  - `{ email: 1, unique: true }` if email present in majority and unique in sample.
  - Compound or text index when configured.

### 7.7 OpenAPI
- SpringDoc at `/v3/api-docs` and Swagger UI at `/swagger-ui.html`.

---

## 8) Frontend Design Details

- Fetch schema from `/api/schema` on load.
- Render **DataTable** with columns from schema (select visible columns).
- DynamicForm:
  - Map types to widgets: `string -> input`, `number -> number`, `date -> date`, `boolean -> switch`, `array<string> -> tags`.
  - Basic required/optional hints from `presenceRatio`.
- React Query for data fetching/caching; optimistic updates on create/update/delete.
- Reusable FilterBuilder.
- Error boundaries & toasts.

---

## 9) Example Config Files

### 9.1 `backend/src/main/resources/application.yml`
```yaml
spring:
  application:
    name: employee-crud
  data:
    mongodb:
      uri: ${MONGODB_URI}
      database: ${MONGODB_DB}
server:
  port: ${SERVER_PORT:8080}
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
cors:
  allowed-origins: ${CORS_ALLOWED_ORIGINS:http://localhost:5173}
schema:
  sample-size: ${SCHEMA_SAMPLE_SIZE:500}
  cache-ttl-sec: ${SCHEMA_CACHE_TTL_SEC:300}
logging:
  level:
    root: ${LOG_LEVEL:INFO}
```

### 9.2 Backend Dockerfile (multi-stage)
```dockerfile
# Build stage
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -q -DskipTests package

# Run stage (small JRE)
FROM eclipse-temurin:21-jre-alpine
ENV JAVA_OPTS="-XX:+UseZGC -XX:MaxRAMPercentage=70 -XX:+AlwaysPreTouch"
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar /app/app.jar"]
```

### 9.3 Frontend Dockerfile
```dockerfile
# Build stage
FROM node:20-alpine AS build
WORKDIR /ui
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

# Serve stage
FROM nginx:1.27-alpine
COPY --from=build /ui/dist /usr/share/nginx/html
COPY deploy/nginx/default.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
```

### 9.4 `deploy/nginx/default.conf`
```nginx
server {
  listen 80;
  server_name _;
  root /usr/share/nginx/html;
  index index.html;

  location /api/ {
    proxy_pass http://backend:8080/;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
  }

  location / {
    try_files $uri /index.html;
  }
}
```

### 9.5 `deploy/docker-compose.yml`
```yaml
version: "3.9"
services:
  backend:
    image: ghcr.io/OWNER/employee-crud-backend:${TAG-latest}
    env_file:
      - ../.env
    ports:
      - "8080:8080"
    restart: unless-stopped

  frontend:
    image: ghcr.io/OWNER/employee-crud-frontend:${TAG-latest}
    environment:
      - VITE_API_BASE_URL=http://localhost:8080
    ports:
      - "5173:80"
    depends_on: [backend]
    restart: unless-stopped
```

---

## 10) CI/CD (GitHub Actions)

### 10.1 `.github/workflows/ci.yml`
- Trigger: PRs and pushes to `main`.
- Jobs:
  1. **backend**: setup JDK 21, cache Maven, run tests, build jar.
  2. **frontend**: setup Node 20, `npm ci`, lint, unit tests, `npm run build`.
  3. **docker**: build multi-arch images and push to GHCR with `GITHUB_TOKEN`.

```yaml
name: CI
on:
  push:
    branches: [ "main" ]
  pull_request:
    branches: [ "main" ]

permissions:
  contents: read
  packages: write

jobs:
  backend:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: 21
      - name: Cache Maven
        uses: actions/cache@v4
        with:
          path: ~/.m2/repository
          key: ${{ runner.os }}-m2-${{ hashFiles('backend/pom.xml') }}
      - name: Build & Test
        working-directory: backend
        run: mvn -B -DskipITs verify

  frontend:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with:
          node-version: 20
          cache: npm
          cache-dependency-path: frontend/package-lock.json
      - name: Install
        working-directory: frontend
        run: npm ci
      - name: Lint & Test & Build
        working-directory: frontend
        run: |
          npm run lint --if-present
          npm test --if-present -- --ci
          npm run build

  docker:
    needs: [backend, frontend]
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Login to GHCR
        uses: docker/login-action@v3
        with:
          registry: ghcr.io
          username: ${{ github.actor }}
          password: ${{ secrets.GITHUB_TOKEN }}
      - name: Extract metadata (tags, labels) backend
        id: meta_back
        uses: docker/metadata-action@v5
        with:
          images: ghcr.io/${{ github.repository_owner }}/employee-crud-backend
      - name: Build & push backend
        uses: docker/build-push-action@v6
        with:
          context: ./backend
          push: true
          tags: ${{ steps.meta_back.outputs.tags }}
          labels: ${{ steps.meta_back.outputs.labels }}
      - name: Extract metadata (tags, labels) frontend
        id: meta_front
        uses: docker/metadata-action@v5
        with:
          images: ghcr.io/${{ github.repository_owner }}/employee-crud-frontend
      - name: Build & push frontend
        uses: docker/build-push-action@v6
        with:
          context: ./frontend
          push: true
          tags: ${{ steps.meta_front.outputs.tags }}
          labels: ${{ steps.meta_front.outputs.labels }}
```

### 10.2 `.github/workflows/release.yml`
- Trigger on git tag like `v*`.
- Publishes images with version tag and creates a GitHub Release.
- Optional job to deploy via SSH to a target host and run `docker compose pull && up -d`.

```yaml
name: Release
on:
  push:
    tags:
      - "v*"

permissions:
  contents: write
  packages: write

jobs:
  build-and-push:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: docker/login-action@v3
        with:
          registry: ghcr.io
          username: ${{ github.actor }}
          password: ${{ secrets.GITHUB_TOKEN }}
      - uses: docker/metadata-action@v5
        id: meta_back
        with:
          images: ghcr.io/${{ github.repository_owner }}/employee-crud-backend
          tags: |
            type=raw,value=${{ github.ref_name }}
            type=raw,value=latest
      - uses: docker/build-push-action@v6
        with:
          context: ./backend
          push: true
          tags: ${{ steps.meta_back.outputs.tags }}
      - uses: docker/metadata-action@v5
        id: meta_front
        with:
          images: ghcr.io/${{ github.repository_owner }}/employee-crud-frontend
          tags: |
            type=raw,value=${{ github.ref_name }}
            type=raw,value=latest
      - uses: docker/build-push-action@v6
        with:
          context: ./frontend
          push: true
          tags: ${{ steps.meta_front.outputs.tags }}

  create-release:
    needs: build-and-push
    runs-on: ubuntu-latest
    steps:
      - uses: actions/create-release@v1
        env:
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
        with:
          tag_name: ${{ github.ref_name }}
          release_name: Employee CRUD ${{ github.ref_name }}
          draft: false
          prerelease: false
```

---

## 11) Developer Workflows (VS Code + ChatGPT Extension)

### 11.1 Prompts to Kickstart Backend
- “Create a Spring Boot 3 WebFlux project with reactive MongoDB and a dynamic `EmployeeDocument` as `Map<String,Object>`. Add controllers for `/api/employees` and `/api/schema` with the methods listed in this document. Implement `SchemaDiscoveryService` to sample N documents and infer types. Include unit tests for inference.”

### 11.2 Prompts to Kickstart Frontend
- “Scaffold a Vite React TS app with React Query and Tailwind. Build `useSchema()` hook that calls `/api/schema`, `DynamicForm` that maps types to inputs, and `EmployeesPage` with table, search, filters, and CRUD dialogs.”

### 11.3 Local Dev
```bash
# Backend
cd backend
cp ../.env.sample .env  # set MONGODB_URI etc.
mvn spring-boot:run

# Frontend
cd ../frontend
cp ../.env.sample .env
npm ci && npm run dev
```

---

## 12) Testing Strategy

- **Backend Unit**: type inference, repository, controller validation.
- **Contract Tests**: OpenAPI schema snapshots.
- **Integration (optional)**: Testcontainers + MongoDB for CI (service containers).
- **Frontend**: component tests (React Testing Library), E2E (Playwright) optional.

---

## 13) Security & Hardening

- Use MongoDB Atlas **least‑privilege** user.
- Validate and sanitize all inputs; restrict allowed query operators.
- CORS: restrict to known origins.
- HTTP headers via Nginx: `X-Frame-Options`, `X-Content-Type-Options`, `Content-Security-Policy`.
- Dependabot alerts enabled.

---

## 14) Performance Notes

- Reactive stack + small thread pools.
- JVM flags tuned for container memory (`MaxRAMPercentage`).
- Enable **Spring Boot layered jars** for faster Docker rebuilds.
- Consider **GraalVM native** build as stretch goal (profile `native`).

---

## 15) Acceptance Criteria

- Connects to Atlas with given URI and lists inferred fields via `/api/schema` within 1s on a sample of 500 docs.
- UI loads, renders table and dynamic forms; CRUD works end‑to‑end.
- Docker images build successfully and run with `docker compose up -d`.
- CI passes on PR; Release build pushes versioned images.
- Documentation present (`README.md`) with setup steps.

---

## 16) Appendix — Code Sketches

### 16.1 `SchemaDiscoveryService` (sketch)
```java
@Service
@RequiredArgsConstructor
public class SchemaDiscoveryService {
  private final ReactiveMongoTemplate template;
  private final Cache<String, SchemaResult> cache = Caffeine.newBuilder()
      .expireAfterWrite(Duration.ofSeconds(ttl))
      .maximumSize(10)
      .build();

  public Mono<SchemaResult> infer(String collection, int sample) {
    var key = collection + ":" + sample;
    var cached = cache.getIfPresent(key);
    if (cached != null) return Mono.just(cached);

    return template.find(new Query().limit(sample), EmployeeDocument.class, collection)
      .flatMap(doc -> Flux.fromIterable(doc.getFields().entrySet()))
      .groupBy(Map.Entry::getKey)
      .flatMap(group -> group.map(Map.Entry::getValue).collectList()
        .map(values -> inferField(group.key(), values)))
      .collectMap(FieldSpec::name, f -> f)
      .map(map -> new SchemaResult(collection, sample, map))
      .doOnNext(r -> cache.put(key, r));
  }
}
```

### 16.2 `DynamicForm` mapping (sketch)
```tsx
switch (field.type) {
  case "string": return <input type="text" ... />;
  case "number": return <input type="number" ... />;
  case "boolean": return <input type="checkbox" ... />;
  case "date": return <input type="date" ... />;
  case "array": return <TagInput ... />;
}
```

---

## 17) License
Choose MIT or Apache‑2.0 for straightforward reuse.

---

## 18) Quick Start Checklist
- [ ] Set Atlas URI & DB in `.env`.
- [ ] Run backend locally.
- [ ] Run frontend locally.
- [ ] Commit and push; CI is green.
- [ ] Create tag `v1.0.0`; images published.
- [ ] On target host: `docker login ghcr.io`, `docker compose pull`, `up -d`.

---

*End of document.*