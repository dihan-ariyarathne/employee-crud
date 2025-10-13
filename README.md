# Employee CRUD (Spring Boot + React)

This project implements a reactive Spring Boot API and a Vite + React UI for managing employees stored in MongoDB Atlas. The backend discovers the collection schema dynamically and the frontend renders forms, tables, and filters automatically from that schema.

## Project Layout

- `backend/` – Spring Boot 3 WebFlux service (Java 21)
- `frontend/` – Vite + React + TypeScript web client with TailwindCSS
- `docker-compose.yml` – Builds and runs the full stack locally (backend + frontend)

## Prerequisites

- Java 21 JDK
- Maven 3.9+
- Node.js 20+
- MongoDB Atlas credentials with access to the target database/collection

## Getting Started

1. Copy the environment samples and fill in real values:
   ```bash
   cp .env.sample .env
   cp backend/.env.sample backend/.env
   cp frontend/.env.sample frontend/.env
   ```
   Update `backend/.env` with your Atlas URI, database name, and collection.

2. Start the backend:
   ```bash
   cd backend
   mvn spring-boot:run
   ```
   The API listens on `http://localhost:8080` and exposes OpenAPI docs at `/api/swagger-ui`.

3. Start the frontend:
   ```bash
   cd frontend
   npm install
   npm run dev
   ```
   Open `http://localhost:5173` to interact with the UI. The dev server proxies `/api` calls to the backend.

4. (Optional) Run tests:
   ```bash
   cd backend && mvn test
   cd ../frontend && npm run lint && npm run typecheck
   ```

## Docker Workflow

Build and run both services with Docker Compose:
```bash
docker compose up --build
```
The frontend is served on `http://localhost:5173` by default; the backend remains on `8080`.

## CI/CD

GitHub Actions workflows (not yet committed) should run the Maven and npm pipelines, build the Docker images, and push them to your container registry. Adjust credentials and registry targets as needed.

## Next Steps

- Wire the backend to your MongoDB Atlas cluster by providing the connection details.
- Extend test coverage (service/controller layers, React component tests, and E2E flows).
- Harden security (authentication, rate limiting, input sanitization).
- Add GitHub Actions workflows per the requirements document.

---
Refer to `employee-crud-requirements.md` for the full specification and stretch goals (native images, observability tooling, etc.).

