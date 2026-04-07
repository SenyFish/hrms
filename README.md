# <img src="frontend/public/favicon.svg" alt="HRMS logo" width="28" /> HRMS

人力资源管理系统，采用前后端分离架构，覆盖员工、考勤、薪酬、招聘、培训、员工关系与员工关怀等常见 HR 场景。

<!-- README-I18N:START -->

[English](./README-en.md) | **中文**

<!-- README-I18N:END -->

## 概览

- 后端：Spring Boot 3、Spring Security、Spring Data JPA、JWT、MySQL、Apache POI
- 前端：Vue 3、Vite、TypeScript、Pinia、Vue Router、Nuxt UI、ECharts
- 数据库：MySQL 8
- 交付方式：本地开发、Docker Compose、GitHub Actions + GHCR

## 功能模块

| 模块 | 说明 |
| --- | --- |
| 仪表盘 | 统计卡片、考勤趋势、个人资料、关怀提醒 |
| 系统管理 | 员工、部门、公告、文件上传下载 |
| 权限管理 | 角色管理、菜单分配、路由级访问控制 |
| 薪酬管理 | 薪资记录、参保城市、绩效数据、Excel 导出 |
| 考勤管理 | 请假、考勤记录、出差、审批流程 |
| 招聘管理 | 招聘需求、岗位、候选人 |
| 员工关系 | 合同与劳动争议 |
| 培训管理 | 培训场次、晋升规划 |
| 员工关怀 | 关怀计划、提醒、统计 |

## 项目结构

```text
hrms/
|-- backend/                 Spring Boot API
|-- frontend/                Vue 3 + Vite 管理端
|-- database/                MySQL 初始化脚本与数据库镜像 Dockerfile
|-- docker-compose.yml       基于 GHCR 镜像的容器编排
|-- DEPLOY-DOCKER.md         Docker 部署说明
`-- .github/workflows/       CI/CD 流水线
```

## 快速开始

### 环境要求

- JDK 17+
- Maven 3.8+
- Node.js 24+
- MySQL 8.x

### 1. 初始化数据库

创建一个名为 `hrms` 的 MySQL 数据库，使用 `utf8mb4` 编码。

你可以选择：

- 导入 [`database/init-hrms.sql`](./database/init-hrms.sql)
- 或者只创建空库，让 Hibernate 在首次启动时自动更新表结构

默认后端配置位于 [application.yml](./backend/src/main/resources/application.yml)：

- 数据库地址：`jdbc:mysql://localhost:3306/hrms...`
- 用户名：`root`
- 密码：`root`

> [!NOTE]
> 如果当前数据库中没有用户，后端首次启动时会自动初始化演示账号、菜单、部门、公告、薪资和考勤示例数据。

### 2. 启动后端

```bash
cd backend
mvn spring-boot:run
```

默认地址：`http://localhost:8080`

### 3. 启动前端

```bash
cd frontend
npm install
npm run dev
```

默认地址：`http://localhost:5173`

前端开发环境会将 `/api` 请求代理到 `http://localhost:8080`。

## 演示账号

首次初始化后可使用以下账号登录：

| 用户名 | 密码 | 角色 |
| --- | --- | --- |
| `admin` | `admin123` | 管理员 |
| `hr` | `hr123` | 人事 |
| `emp` | `emp123` | 员工 |

## Docker 部署

当前仓库默认通过 GHCR 镜像部署，不再要求本地构建。

```bash
docker compose pull
docker compose up -d
```

默认容器访问地址：

- frontend: `http://localhost:5173`
- backend: `http://localhost:8080`
- mysql: `localhost:3306`

当前 Compose 使用的镜像：

- `ghcr.io/senyfish/hrms-mysql:dev`
- `ghcr.io/senyfish/hrms-backend:dev`
- `ghcr.io/senyfish/hrms-frontend:dev`

其中 MySQL 镜像已经内置 [`database/init-hrms.sql`](./database/init-hrms.sql)，在 MySQL 数据卷为空的首次启动时会自动执行初始化脚本。

> [!IMPORTANT]
> 当前 `docker-compose.yml` 中的数据库密码、JWT 密钥和镜像标签都是明文固定值，适合当前部署流程演示。用于生产环境前，请务必替换为你自己的安全配置。

## CI/CD

仓库已内置两条 GitHub Actions 流程：

- `CI`
  - 对 `dev` / `main` 的 `push` 和 `pull_request` 生效
  - 后端执行 `mvn -B test`
  - 前端执行 `npm ci --registry=https://registry.npmjs.org --no-audit --no-fund` 和 `npm run build`
- `Docker Publish`
  - 对 `dev` / `main` 的 `push` 与 `v*` tag 生效
  - 自动构建并推送 MySQL、后端、前端镜像到 GHCR

默认标签规则：

- `dev` 分支：`dev`、`dev-<sha>`、`sha-<sha>`
- `main` 分支：`latest`、`main-<sha>`、`sha-<sha>`
- `v*` 标签：对应版本标签

## 运行配置

后端支持通过环境变量覆盖以下关键项：

- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `JWT_SECRET`
- `JWT_EXPIRE_HOURS`
- `UPLOAD_DIR`
- `CORS_ALLOWED_ORIGIN_PATTERNS`

默认上传目录是 `/app/uploads`；在当前 Compose 中，它被挂载到 Docker 命名卷 `hrms_uploads`。

## API 与认证

- API 前缀：`/api`
- 认证方式：JWT Bearer Token
- 前端令牌存储：`localStorage`
- 常用认证接口：
  - `/api/auth/login`
  - `/api/auth/logout`
  - `/api/auth/me`

## 主要页面

当前前端路由覆盖：

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

## 开发说明

- 后端入口配置见 [application.yml](./backend/src/main/resources/application.yml)
- 前端路由见 [routes.ts](./frontend/src/router/routes.ts)
- Excel 导出能力位于 [ExcelExportUtil.java](./backend/src/main/java/com/hrms/util/ExcelExportUtil.java)
- Docker 部署补充说明见 [DEPLOY-DOCKER.md](./DEPLOY-DOCKER.md)
