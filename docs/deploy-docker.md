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

该命令保留用于本地或资源充足环境的完整构建启动。2 核 2G 轻量服务器可以运行项目，但不适合每次在服务器上构建前端；不建议在服务器执行 `docker compose build frontend`、`docker compose up -d --build`，也不建议在服务器执行 `npm run build`。低配服务器上的前端更新推荐使用“本地构建 dist 后上传覆盖”的轻量流程。

首次启动时 MySQL 初始化会比较慢。Compose 已加入 MySQL healthcheck，后端会等待 MySQL 和 Redis 健康后启动；如果本机或 Docker Desktop 较慢导致后端失败，可以等待 MySQL healthy 后重启后端：

```bash
docker compose restart backend
```

## 低配服务器部署建议

- 2 核 2G 服务器可以运行 ProjectMentor AI 试用环境，但前端 Docker 构建和服务器本机 `npm run build` 都容易占用过多内存。
- 日常前端更新不要在服务器执行 `docker compose build frontend` 或 `docker compose up -d --build`。
- 前端推荐在本地 Windows 机器执行 `npm.cmd run build`，再把 `dist.zip` 上传到服务器覆盖静态文件。
- Docker Compose 的正常启动能力仍然保留；本地开发、完整验证或资源充足服务器仍可使用完整 Compose 构建流程。

## 前端轻量更新流程

本地 Windows PowerShell：

```powershell
cd C:\Users\LiXin\Desktop\projectmentor-ai\frontend\projectmentor-web
npm.cmd run build
Remove-Item -Force dist.zip -ErrorAction SilentlyContinue
Compress-Archive -Path dist* -DestinationPath dist.zip -Force
scp dist.zip root@服务器IP:/opt/projectmentor-ai/frontend-dist.zip
```

服务器：

```bash
cd /opt/projectmentor-ai
rm -rf /tmp/projectmentor-frontend-dist
mkdir -p /tmp/projectmentor-frontend-dist
unzip -o frontend-dist.zip -d /tmp/projectmentor-frontend-dist
docker exec projectmentor-frontend-assets sh -c "rm -rf /usr/share/nginx/html/*"
docker cp /tmp/projectmentor-frontend-dist/. projectmentor-frontend-assets:/usr/share/nginx/html/
docker compose restart frontend nginx
docker compose ps
```

## 后端更新流程

后端如果修改 Java 代码，低配服务器可以构建但会较慢：

```bash
docker compose build backend
docker compose up -d backend
```

如果服务器构建明显卡顿，后续可升级为 GitHub Actions 构建镜像，再由服务器拉取镜像部署。

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

## 安全说明

- MySQL 和 Redis 不映射公网端口，只在 Docker Compose 内部网络中被后端通过 `mysql`、`redis` 服务名访问。
- 生产环境只开放 `22`、`80`、`443`；云防火墙不应开放 `3306`、`6379`。
- `.env` 不提交到 Git。
- `AI_API_KEY`、`JWT_SECRET`、`MYSQL_ROOT_PASSWORD` 必须通过环境变量配置，不要写入代码、镜像或公开文档。

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

### 端口 80 被占用，或数据库端口访问不到

默认只有 Nginx 暴露 `80`。MySQL 的 `3306` 和 Redis 的 `6379` 不映射到宿主机公网端口，外部不能通过服务器 IP 直接访问它们；后端仍通过 Compose 内部服务名 `mysql`、`redis` 连接。如果 `80` 被占用，可以停止占用进程，或修改 `docker-compose.yml` 中 nginx 的宿主机端口映射。

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
