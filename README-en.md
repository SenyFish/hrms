# <img src="frontend/public/favicon.svg" alt="HRMS logo" width="28" /> HRMS

Human Resource Management System built with a separated frontend and backend architecture.

This repository combines a Spring Boot 3 API, a Vue 3 admin client, and a MySQL database to cover common HR workflows such as employee management, attendance, payroll, recruitment, training, employee relations, and care planning.

<!-- README-I18N:START -->

**English** | [中文](./README.zh.md)

<!-- README-I18N:END -->

## Overview

- Backend: Spring Boot 3, Spring Security, Spring Data JPA, JWT, MySQL, Apache POI
- Frontend: Vue 3, Vite, TypeScript, Pinia, Vue Router, Nuxt UI, ECharts
- Database: MySQL 8
- Deployment: local development or Docker Compose

## Features

| Area | What it covers |
| --- | --- |
| Dashboard | Attendance trend charts, summary cards, user profile, care reminders |
| System management | Employees, departments, notices, file upload and download |
| Permissions | Role management, menu assignment, route-level access control |
| Payroll | Salary records, insured cities, performance data, Excel export |
| Attendance | Leave requests, attendance records, business trips, approvals |
| Recruitment | Requirements, positions, candidates |
| Employee relations | Contracts and disputes |
| Training | Training sessions and promotion planning |
| Employee care | Care plans, reminders, statistics |

## Architecture

```text
hrms/
|-- backend/                 Spring Boot API
|-- frontend/                Vue 3 + Vite admin app
|-- database/                SQL bootstrap script
|-- docker-compose.yml       Containerized local deployment
|-- DEPLOY-DOCKER.md         Docker deployment notes
`-- .env.example             Compose environment template
```

## Quick Start

### Prerequisites

- JDK 17+
- Maven 3.8+
- Node.js 18+
- MySQL 8.x

### 1. Create the database

Create a MySQL database named `hrms` with `utf8mb4` encoding.

You can either:

- import [`database/init-hrms.sql`](database/init-hrms.sql), or
- create an empty database and let Hibernate update the schema on first run

The backend defaults are defined in [`backend/src/main/resources/application.yml`](backend/src/main/resources/application.yml):

- database URL: `jdbc:mysql://localhost:3306/hrms...`
- username: `root`
- password: `root`

> [!NOTE]
> On a fresh database, the backend seeds demo users, menus, departments, notices, salary data, and attendance samples automatically when no users exist.

### 2. Start the backend

```bash
cd backend
mvn spring-boot:run
```

The API starts on `http://localhost:8080`.

### 3. Start the frontend

```bash
cd frontend
npm install
npm run dev
```

The frontend starts on `http://localhost:5173`.

Vite proxies `/api` requests to `http://localhost:8080`, so the frontend and backend work together without extra local proxy setup.

## Demo Accounts

The application seeds these accounts on first startup:

| Username | Password | Role |
| --- | --- | --- |
| `admin` | `admin123` | Administrator |
| `hr` | `hr123` | HR |
| `emp` | `emp123` | Employee |

## Environment Notes

- JWT secret and expiration are configurable through environment variables.
- Uploaded files are stored under `${user.home}/hrms-uploads` by default.
- CORS defaults allow local development on `localhost` and `127.0.0.1`.

> [!IMPORTANT]
> Replace the default JWT secret and database credentials before using this project outside local development.

## Docker Compose

This repository includes Dockerfiles for the frontend and backend plus a `docker-compose.yml` for MySQL, API, and web containers.

```bash
cp .env.example .env
docker compose up -d --build
```

Default container endpoints:

- frontend: `http://localhost:5173`
- backend: `http://localhost:8080`
- mysql: `localhost:3306`

If you want MySQL to import SQL automatically on first container start, place SQL files under `deploy/sql/` as described in [`DEPLOY-DOCKER.md`](DEPLOY-DOCKER.md). If you prefer the existing bootstrap script in this repo, copy [`database/init-hrms.sql`](database/init-hrms.sql) into that directory before running Compose.

## API and Auth

- API base path: `/api`
- Authentication: JWT bearer token
- Frontend token storage: `localStorage`
- Auth endpoints: `/api/auth/login`, `/api/auth/logout`, `/api/auth/me`

## Development Notes

- Frontend routes are defined in [`frontend/src/router/routes.ts`](frontend/src/router/routes.ts).
- Backend controllers live under [`backend/src/main/java/com/hrms/controller`](backend/src/main/java/com/hrms/controller).
- Excel export is implemented with Apache POI in [`backend/src/main/java/com/hrms/util/ExcelExportUtil.java`](backend/src/main/java/com/hrms/util/ExcelExportUtil.java).

## Screens and Modules

The current route map includes:

- `/home`
- `/system/files`, `/system/employees`, `/system/departments`
- `/permission/roles`, `/permission/menus`
- `/salary/records`, `/salary/cities`, `/salary/performance`
- `/attendance/leaves`, `/attendance/records`, `/attendance/trips`
- `/recruitment/requirements`, `/recruitment/positions`, `/recruitment/candidates`
- `/relations/contracts`, `/relations/disputes`
- `/training/sessions`, `/training/promotions`
- `/care/plans`, `/care/stats`
