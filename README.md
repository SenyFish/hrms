# <img src="frontend/public/favicon.svg" alt="HRMS logo" width="28" /> HRMS

人力资源管理系统，采用前后端分离架构。

该仓库结合了 Spring Boot 3 API、Vue 3 管理端和 MySQL 数据库，用于覆盖常见的人力资源流程，包括员工管理、考勤、薪酬、招聘、培训、员工关系和关怀计划。

<!-- README-I18N:START -->

[English](./README-en.md) | **中文**

<!-- README-I18N:END -->

## 概览

- 后端：Spring Boot 3、Spring Security、Spring Data JPA、JWT、MySQL、Apache POI
- 前端：Vue 3、Vite、TypeScript、Pinia、Vue Router、Nuxt UI、ECharts
- 数据库：MySQL 8
- 部署方式：本地开发或 Docker Compose

## 功能特性

| 模块 | 说明 |
| --- | --- |
| 仪表盘 | 考勤趋势图、统计卡片、个人资料、关怀提醒 |
| 系统管理 | 员工、部门、公告、文件上传下载 |
| 权限管理 | 角色管理、菜单分配、基于路由的访问控制 |
| 薪酬管理 | 薪资记录、参保城市、绩效数据、Excel 导出 |
| 考勤管理 | 请假申请、考勤记录、出差申请、审批流程 |
| 招聘管理 | 招聘需求、岗位、候选人 |
| 员工关系 | 合同与纠纷管理 |
| 培训管理 | 培训场次与晋升规划 |
| 员工关怀 | 关怀计划、提醒、统计 |

## 架构

```text
hrms/
|-- backend/                 Spring Boot API
|-- frontend/                Vue 3 + Vite admin app
|-- database/                SQL bootstrap script
|-- docker-compose.yml       Containerized local deployment
|-- DEPLOY-DOCKER.md         Docker deployment notes
`-- README-en.md             English documentation
```

## 快速开始

### 前置要求

- JDK 17+
- Maven 3.8+
- Node.js 18+
- MySQL 8.x

### 1. 创建数据库

创建一个名为 `hrms` 的 MySQL 数据库，并使用 `utf8mb4` 编码。

你可以选择：

- 导入 [`database/init-hrms.sql`](database/init-hrms.sql)，或者
- 创建一个空数据库，并让 Hibernate 在首次运行时自动更新表结构

后端默认配置位于 [`backend/src/main/resources/application.yml`](backend/src/main/resources/application.yml)：

- 数据库地址：`jdbc:mysql://localhost:3306/hrms...`
- 用户名：`root`
- 密码：`root`

> [!NOTE]
> 对于全新数据库，如果系统检测到当前没有用户，后端会自动初始化演示账号、菜单、部门、公告、薪资数据和考勤示例数据。

### 2. 启动后端

```bash
cd backend
mvn spring-boot:run
```

API 默认启动在 `http://localhost:8080`。

### 3. 启动前端

```bash
cd frontend
npm install
npm run dev
```

前端默认启动在 `http://localhost:5173`。

Vite 已将 `/api` 请求代理到 `http://localhost:8080`，本地联调无需额外配置代理。

## 演示账号

应用首次启动时会自动初始化以下账号：

| 用户名 | 密码 | 角色 |
| --- | --- | --- |
| `admin` | `admin123` | 管理员 |
| `hr` | `hr123` | 人事 |
| `emp` | `emp123` | 员工 |

## 环境说明

- JWT 密钥和过期时间可通过环境变量配置。
- 上传文件默认存储在 `${user.home}/hrms-uploads`。
- CORS 默认允许 `localhost` 和 `127.0.0.1` 下的本地开发来源。

> [!IMPORTANT]
> 如果不是本地开发环境，请务必替换默认 JWT 密钥和数据库凭据。

## Docker Compose

仓库中已包含前端、后端的 Dockerfile，以及用于启动 MySQL、API 和 Web 容器的 `docker-compose.yml`。

```bash
docker compose pull
docker compose up -d
```

容器默认访问地址：

- frontend: `http://localhost:5173`
- backend: `http://localhost:8080`
- mysql: `localhost:3306`

当前 MySQL 镜像已经内置 [`database/init-hrms.sql`](database/init-hrms.sql)，因此在 MySQL 数据卷为空的首次启动时会自动执行该初始化脚本。

## API 与认证

- API 基础路径：`/api`
- 认证方式：JWT Bearer Token
- 前端令牌存储位置：`localStorage`
- 认证接口：`/api/auth/login`、`/api/auth/logout`、`/api/auth/me`

## 开发说明

- 前端路由定义位于 [`frontend/src/router/routes.ts`](frontend/src/router/routes.ts)。
- 后端控制器位于 [`backend/src/main/java/com/hrms/controller`](backend/src/main/java/com/hrms/controller)。
- Excel 导出能力由 [`backend/src/main/java/com/hrms/util/ExcelExportUtil.java`](backend/src/main/java/com/hrms/util/ExcelExportUtil.java) 中的 Apache POI 实现。

## 页面与模块

当前路由包括：

- `/home`
- `/system/files`, `/system/employees`, `/system/departments`
- `/permission/roles`, `/permission/menus`
- `/salary/records`, `/salary/cities`, `/salary/performance`
- `/attendance/leaves`, `/attendance/records`, `/attendance/trips`
- `/recruitment/requirements`, `/recruitment/positions`, `/recruitment/candidates`
- `/relations/contracts`, `/relations/disputes`
- `/training/sessions`, `/training/promotions`
- `/care/plans`, `/care/stats`
