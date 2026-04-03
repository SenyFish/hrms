# 人力资源管理系统（HRMS）

前后端分离项目：**Spring Boot 3 + Maven + MySQL**，前端 **Vue 3 + Vite + Element Plus + ECharts**，数据库可用 **Navicat 17** 连接管理。

## 功能模块（与思维导图对应）

| 模块 | 说明 |
|------|------|
| **首页** | ECharts 考勤趋势、统计卡片、个人信息摘要 |
| **系统管理** | 公告事务、文件上传下载、员工管理（含角色分配）、部门管理（考勤时间、请假说明、罚款、加班倍数） |
| **权限管理** | 角色管理（菜单分配）、菜单管理 |
| **薪资管理** | 五险一金与社保基数、参保城市维护、工资月报 **Excel 导出** |
| **考勤管理** | 上下班打卡、请假申请与审批、考勤查询、**CSV 导入**与 **Excel 导出** |

## 环境要求

- JDK 17+
- Maven 3.8+
- Node.js 18+
- MySQL 8.x

## 数据库（Navicat）

1. 打开 Navicat，新建 MySQL 连接（主机、端口、用户名、密码）。
2. 执行 `database/init-hrms.sql` 中的语句创建库 `hrms`，或手动新建数据库 `hrms`（字符集 **utf8mb4**）。
3. 修改后端配置 `backend/src/main/resources/application.yml` 中的 `spring.datasource.url`、`username`、`password`。

首次启动后端时，`spring.jpa.hibernate.ddl-auto=update` 会自动建表；之后可在 Navicat 中查看与维护数据。

## 启动后端

```bash
cd backend
mvn spring-boot:run
```

默认端口：**8080**。首次启动会初始化演示数据（若库中尚无用户）。

演示账号：

| 用户名 | 密码 | 角色 |
|--------|------|------|
| admin | admin123 | 管理员 |
| hr | hr123 | 人事 |
| emp | emp123 | 员工 |

## 启动前端

```bash
cd frontend
npm install
npm run dev
```

浏览器访问：**http://localhost:5173**（已配置代理将 `/api` 转发到 `http://localhost:8080`）。

生产构建：`npm run build`，静态资源在 `frontend/dist`，可部署到 Nginx 等，并反向代理 `/api` 到后端。

## 项目结构

```
hrms-spring-vue/
├── backend/                 # Maven Spring Boot
│   ├── pom.xml
│   └── src/main/java/com/hrms/
├── frontend/                # Vue 3 + Vite
│   ├── package.json
│   └── src/
├── database/init-hrms.sql   # 建库脚本（可选）
└── README.md
```

## 说明

- JWT 存放在浏览器 `localStorage`，请求头 `Authorization: Bearer <token>`。
- 文件上传目录由 `hrms.upload-dir` 配置，默认用户目录下 `hrms-uploads`。
- 生产环境请修改 `application.yml` 中的 `hrms.jwt.secret` 与数据库密码，并关闭 `ddl-auto` 或改为 `validate`/`none`，配合正式迁移脚本使用。
