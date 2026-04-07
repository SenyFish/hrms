# <img src="frontend/public/favicon.svg" alt="HRMS logo" width="28" /> HRMS

Human Resource Management System built with a separated frontend and backend architecture, covering common HR scenarios such as employees, attendance, payroll, recruitment, training, employee relations, and employee care.

<!-- README-I18N:START -->

**English** | [中文](./README.md)

<!-- README-I18N:END -->

## Overview

- Backend: Spring Boot 3, Spring Security, Spring Data JPA, JWT, MySQL, Apache POI
- Frontend: Vue 3, Vite, TypeScript, Pinia, Vue Router, Nuxt UI, ECharts
- Database: MySQL 8
- Delivery options: local development, Docker Compose, GitHub Actions + GHCR

## Modules

| Module | Description |
| --- | --- |
| Dashboard | Summary cards, attendance trends, profile info, care reminders |
| System Management | Employees, departments, notices, file upload and download |
| Permission Management | Roles, menus, and route-level access control |
| Payroll | Salary records, insured cities, performance data, Excel export |
| Attendance | Leave requests, attendance records, business trips, approval flows |
| Recruitment | Hiring requirements, positions, and candidates |
| Employee Relations | Contracts and labor disputes |
| Training | Training sessions and promotion planning |
| Employee Care | Care plans, reminders, and statistics |

## Project Structure

```text
hrms/
|-- backend/                 Spring Boot API
|-- frontend/                Vue 3 + Vite admin app
|-- database/                MySQL bootstrap script and database image Dockerfile
|-- docker-compose.yml       Compose stack based on GHCR images
|-- DEPLOY-DOCKER.md         Docker deployment notes
`-- .github/workflows/       CI/CD workflows
```

## Quick Start

### Requirements

- JDK 17+
- Maven 3.8+
- Node.js 24+
- MySQL 8.x

### 1. Initialize the database

Create a MySQL database named `hrms` using `utf8mb4`.

You can either:

- import [`database/init-hrms.sql`](./database/init-hrms.sql)
- or create an empty database and let Hibernate update the schema on first startup

Default backend settings are defined in [application.yml](./backend/src/main/resources/application.yml):

- Database URL: `jdbc:mysql://localhost:3306/hrms...`
- Username: `root`
- Password: `root`

> [!NOTE]
> If the database has no users yet, the backend seeds demo accounts, menus, departments, notices, payroll data, and attendance sample data automatically on first startup.

### 2. Start the backend

```bash
cd backend
mvn spring-boot:run
```

Default address: `http://localhost:8080`

### 3. Start the frontend

```bash
cd frontend
npm install
npm run dev
```

Default address: `http://localhost:5173`

In development, the frontend proxies `/api` requests to `http://localhost:8080`.

## Demo Accounts

The following accounts are available after first-time initialization:

| Username | Password | Role |
| --- | --- | --- |
| `admin` | `admin123` | Administrator |
| `hr` | `hr123` | HR |
| `emp` | `emp123` | Employee |

## Docker Deployment

The repository now deploys by pulling prebuilt images from GHCR instead of building locally.

```bash
docker compose pull
docker compose up -d
```

Default container endpoints:

- frontend: `http://localhost:5173`
- backend: `http://localhost:8080`
- mysql: `localhost:3306`

Current Compose images:

- `ghcr.io/senyfish/hrms-mysql:dev`
- `ghcr.io/senyfish/hrms-backend:dev`
- `ghcr.io/senyfish/hrms-frontend:dev`

The MySQL image already includes [`database/init-hrms.sql`](./database/init-hrms.sql), so the bootstrap script runs automatically the first time the MySQL data volume starts with an empty data directory.

> [!IMPORTANT]
> The current `docker-compose.yml` keeps the database password, JWT secret, and image tags as fixed plain-text values for the current deployment flow. Replace them with your own secure values before using this setup in production.

## CI/CD

The repository includes two GitHub Actions workflows:

- `CI`
  - Runs on `push` and `pull_request` targeting `dev` and `main`
  - Backend runs `mvn -B test`
  - Frontend runs `npm ci --registry=https://registry.npmjs.org --no-audit --no-fund` and `npm run build`
- `Docker Publish`
  - Runs on pushes to `dev` and `main`, plus `v*` tags
  - Builds and publishes MySQL, backend, and frontend images to GHCR

Default tag rules:

- `dev` branch: `dev`, `dev-<sha>`, `sha-<sha>`
- `main` branch: `latest`, `main-<sha>`, `sha-<sha>`
- `v*` tags: matching version tags

## Runtime Configuration

The backend supports overriding these key settings through environment variables:

- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `JWT_SECRET`
- `JWT_EXPIRE_HOURS`
- `UPLOAD_DIR`
- `CORS_ALLOWED_ORIGIN_PATTERNS`

The default upload directory is `/app/uploads`; in the current Compose setup it is backed by the Docker named volume `hrms_uploads`.

## API and Authentication

- API prefix: `/api`
- Authentication: JWT Bearer Token
- Frontend token storage: `localStorage`
- Common auth endpoints:
  - `/api/auth/login`
  - `/api/auth/logout`
  - `/api/auth/me`

## Main Pages

Current frontend routes include:

- `/home`
- `/system/files`
- `/system/employees`
- `/system/departments`
- `/permission/roles`
- `/permission/menus`
- `/salary/records`
- `/salary/cities`
- `/salary/performance`
- `/attendance/leaves`
- `/attendance/records`
- `/attendance/trips`
- `/recruitment/requirements`
- `/recruitment/positions`
- `/recruitment/candidates`
- `/relations/contracts`
- `/relations/disputes`
- `/training/sessions`
- `/training/promotions`
- `/care/plans`
- `/care/stats`

## Development Notes

- Backend entry configuration: [application.yml](./backend/src/main/resources/application.yml)
- Frontend routes: [routes.ts](./frontend/src/router/routes.ts)
- Excel export support: [ExcelExportUtil.java](./backend/src/main/java/com/hrms/util/ExcelExportUtil.java)
- Docker deployment details: [DEPLOY-DOCKER.md](./DEPLOY-DOCKER.md)
