# HRMS Docker 部署说明

## 目录

- `docker-compose.yml`：整体编排
- `backend/Dockerfile`：Spring Boot 镜像
- `frontend/Dockerfile`：前端静态站点镜像
- `frontend/nginx.conf`：前端路由与 `/api` 反向代理
- `.env.example`：环境变量模板
- `deploy/sql/`：首次启动 MySQL 时自动导入的 SQL
- `docker-data/hrms-uploads/`：容器部署时默认的宿主机附件目录

## 首次部署

1. 复制环境变量模板：

```bash
cp .env.example .env
```

2. 如果需要初始化数据库，把备份文件放到 `deploy/sql/` 下。

例如：

- `deploy/sql/hrms-8.0.sql`

3. 启动容器：

```bash
docker compose up -d --build
```

当前示例默认使用 `docker.1ms.run` 作为镜像代理前缀，以降低直连 Docker Hub 失败的概率。

## 访问地址

- 前端：`http://localhost:5173`
- 后端：`http://localhost:8080`

前端会通过 Nginx 自动把 `/api` 转发到后端容器。

## 附件持久化

容器内附件目录为：

- `/app/uploads`

默认由宿主机目录 `docker-data/hrms-uploads/` 挂载到容器内 `/app/uploads`。

如果你要迁移历史附件，建议把原附件目录内容复制到：

- `docker-data/hrms-uploads/`

## 常用命令

查看状态：

```bash
docker compose ps
```

查看日志：

```bash
docker compose logs -f
```

停止服务：

```bash
docker compose down
```

停止并删除卷：

```bash
docker compose down -v
```
