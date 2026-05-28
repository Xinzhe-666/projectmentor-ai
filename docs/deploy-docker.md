# Docker + Nginx 部署

## 前置要求

安装 Docker Desktop，并确认 Docker Compose 可用：

```bash
docker compose version
```

## 配置环境变量

复制环境变量示例文件：

```bash
cp .env.example .env
```

修改 `.env` 中的敏感配置：

- `MYSQL_ROOT_PASSWORD`：改成开发或部署环境自己的 MySQL root 密码。
- `JWT_SECRET`：改成足够长的随机字符串，建议不少于 32 位。
- `AI_API_KEY`：可选；未配置时，系统会降级使用规则报告。

不要把真实 `.env` 提交到 Git。

## 启动

启动前可以先检查 Compose 配置：

```bash
docker compose config
```

```bash
docker compose up -d --build
```

首次启动时 MySQL 初始化会比较慢。Compose 已加入 MySQL healthcheck，后端会等待 MySQL 和 Redis 健康后启动；如果本机或 Docker Desktop 较慢导致后端失败，可以等待 MySQL healthy 后重启后端：

```bash
docker compose restart backend
```

## 查看日志

```bash
docker compose logs -f backend
```

也可以查看所有服务日志：

```bash
docker compose logs -f
```

## 访问

- 前端入口：http://localhost
- 后端健康检查：http://localhost/api/health

生产构建中前端 API 使用同源 `/api`，由 Nginx 反向代理到后端 `backend:8080`，不会把 `AI_API_KEY` 放进前端。

## 停止

```bash
docker compose down
```

## 重置数据库

下面命令会删除 MySQL、Redis 和前端静态资源卷，数据库数据会被清空：

```bash
docker compose down -v
```

## 常见问题

### MySQL 启动慢导致后端连接失败

Compose 已配置 MySQL healthcheck，后端会等待 MySQL healthy。首次初始化仍可能因为机器性能或镜像拉取较慢而失败，等待一两分钟后执行：

```bash
docker compose restart backend
```

### 端口 80、3306、6379 被占用

默认 Nginx 暴露 `80`，MySQL 暴露 `3306`，Redis 暴露 `6379`。如果端口被占用，可以停止占用进程，或修改 `docker-compose.yml` 中对应的宿主机端口映射。

### AI_API_KEY 未配置

`AI_API_KEY` 可以留空。未配置时，系统会降级为规则报告，不会向前端暴露任何 AI Key。

### 前端刷新 404

`deploy/nginx/nginx.conf` 中已配置：

```nginx
try_files $uri $uri/ /index.html;
```

这会让 Vue history 路由在刷新页面时回退到 `index.html`。

### 上传 ZIP 失败

Nginx 已设置：

```nginx
client_max_body_size 220m;
client_body_timeout 900s;
client_header_timeout 60s;
send_timeout 900s;
proxy_connect_timeout 60s;
proxy_send_timeout 900s;
proxy_read_timeout 900s;
proxy_request_buffering off;
```

后端 Spring Boot multipart 文件限制为 200MB，请求限制为 220MB。ZIP 上传会自动过滤常见依赖、构建和 IDE 目录；大文件上传可能需要数分钟，请不要刷新页面。仍建议删除 `node_modules`、`target`、`.git` 后再压缩，可显著提升上传速度。
