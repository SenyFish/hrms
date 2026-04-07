# HRMS Docker 部署说明

## 目录

- `docker-compose.yml`：整体编排
- `database/Dockerfile`：MySQL 初始化镜像
- `backend/Dockerfile`：Spring Boot 镜像
- `frontend/Dockerfile`：前端静态站点镜像
- `frontend/nginx.conf`：前端路由与 `/api` 反向代理
- `docker-data/hrms-uploads/`：容器部署时默认的宿主机附件目录

## 首次部署

1. MySQL 镜像已内置 `database/init-hrms.sql`，在数据卷为空的首次启动时会自动初始化数据库。

2. 拉取并启动容器：

```bash
docker compose pull
docker compose up -d
```

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
