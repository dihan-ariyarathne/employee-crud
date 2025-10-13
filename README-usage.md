# Employee CRUD – Usage & Deployment Guide

This guide explains how to build, test, run, and deploy the app using GitHub Actions (CI/CD) and Docker. Use it as a quick reference for contributors and operators.

**Components**
- `backend/` – Spring Boot 3 WebFlux (Java 21)
- `frontend/` – Vite + React + TypeScript
- `.github/workflows/ci.yml` – CI on pull requests (build/test)
- `.github/workflows/cd.yml` – CD on main/tags (build/push Docker images)
- `docker-compose.yml` – Local dev stack
- `docker-compose.prod.yml` – Image-based compose for production-like runs

## Prerequisites
- Java 21, Maven 3.9+
- Node.js 20+
- Docker Desktop (or Docker Engine + Compose)
- MongoDB (local or Atlas)

## Quick Start (Local Dev)
- Backend
  - `cd backend && cp .env.sample .env` (set values)
  - `mvn spring-boot:run` → http://localhost:8080
- Frontend
  - `cd frontend && cp .env.sample .env` (ensure VITE_API_BASE_URL=http://localhost:8080)
  - `npm install && npm run dev` → http://localhost:5173
- Tests
  - `cd backend && mvn test`
  - `cd frontend && npm run lint && npm run typecheck`

## Docker (Local Dev)
Use the dev compose to build and run both services:

```
docker compose up --build
```

- Backend: http://localhost:8080
- Frontend: http://localhost:5173

Note: For production images, the frontend’s API URL is baked at build time via `VITE_API_BASE_URL` (see CD).

## CI (GitHub Actions)
File: `.github/workflows/ci.yml`
- Trigger: `pull_request`
- Backend: Java 21, Maven cache, `mvn -B -ntp -f backend/pom.xml verify`
- Frontend: Node 20, npm cache, `npm ci`, `npm run lint`, `npm run typecheck`, `npm run build`
- Purpose: fast feedback before merging; no secrets required

## CD (GitHub Actions → GHCR)
File: `.github/workflows/cd.yml`
- Triggers: push to `main`, tags (`v*`, `release-*`), or manual dispatch
- Registry: GitHub Container Registry (GHCR) using `GITHUB_TOKEN`
- Builds & pushes images:
  - Backend → `ghcr.io/<owner>/employee-crud-backend:{latest,sha-…}`
  - Frontend → `ghcr.io/<owner>/employee-crud-frontend:{latest,sha-…}`
- Required secret:
  - `VITE_API_BASE_URL` – frontend API URL to bake at build time

Frontend Docker build args (file: `frontend/Dockerfile`)
- Accepts `ARG VITE_API_BASE_URL` and sets `ENV VITE_API_BASE_URL=$VITE_API_BASE_URL` before `npm run build`.

## Run Published Images (Laptop or Server)
File: `docker-compose.prod.yml`

1) Replace `<owner>` with your GitHub username/org in image names.

2) Create `.env.prod` next to the compose file:
- `MONGODB_URI=mongodb://admin:pass@host.docker.internal:27017` (for local Mongo)
- `MONGODB_DB=employeesdb`
- `MONGODB_EMP_COLLECTION=employees`
- `CORS_ALLOWED_ORIGINS=http://localhost`
- `SPRING_PROFILES_ACTIVE=prod`
- `LOG_LEVEL=INFO`

3) Login to GHCR and run:
```
echo <PAT_with_read:packages> | docker login ghcr.io -u <owner> --password-stdin
docker compose -f docker-compose.prod.yml --env-file .env.prod pull
docker compose -f docker-compose.prod.yml --env-file .env.prod up -d --remove-orphans
```

Access: frontend http://localhost (80), backend http://localhost:8080.

## Configuration Reference
- Backend env
  - `MONGODB_URI`, `MONGODB_DB`, `MONGODB_EMP_COLLECTION`
  - `SERVER_PORT` (default 8080)
  - `CORS_ALLOWED_ORIGINS` (comma-separated)
  - `SPRING_PROFILES_ACTIVE` (`local`/`prod`)
- Frontend env
  - Dev: `frontend/.env`
  - Prod: `VITE_API_BASE_URL` build arg from secret in CD

## Security
- Do not commit secrets. `.gitignore` excludes `.env` files.
- Keep any service account JSON (e.g., Firebase) out of git. Mount it at runtime and set `GOOGLE_APPLICATION_CREDENTIALS` to the mounted path, or inject via a secret store.

## Update & Rollback
- Update: push to `main` → CI/CD builds `latest` → on host run `pull` + `up -d`.
- Rollback: pin images to `sha-<commit>` tags in `docker-compose.prod.yml` and `up -d`.

## Common Issues
- CORS blocked: set `CORS_ALLOWED_ORIGINS` to your frontend origin.
- Frontend calling wrong URL: ensure `VITE_API_BASE_URL` secret is correct and images rebuilt.
- GHCR access denied: ensure PAT has `read:packages` and `docker login ghcr.io` succeeded.

