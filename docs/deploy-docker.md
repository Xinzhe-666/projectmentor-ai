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
- `KNIFE4J_ENABLED`：控制 Knife4j API 文档开关，生产环境保持 `false`；本地开发需要时可设为 `true`。
- 注册邮箱验证码上线时，配置 `EMAIL_VERIFICATION_ENABLED=true`，并填写 `MAIL_HOST`、`MAIL_PORT`、`MAIL_USERNAME`、`MAIL_PASSWORD`、`MAIL_FROM` 等 SMTP 信息。
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

该命令保留用于本地或资源充足环境的完整构建启动。2 核 2G 轻量服务器可以运行项目，但不适合每次在服务器上构建前端或后端；不建议在服务器执行 `docker compose build frontend`、`docker compose up -d --build`、`npm run build` 或 `mvn clean package`。低配服务器上的前端更新推荐使用“本地构建 dist 后上传覆盖”的轻量流程，后端更新推荐使用“本地构建 jar 后上传”的快速流程。

首次启动时 MySQL 初始化会比较慢。Compose 已加入 MySQL healthcheck，后端会等待 MySQL 和 Redis 健康后启动；如果本机或 Docker Desktop 较慢导致后端失败，可以等待 MySQL healthy 后重启后端：

```bash
docker compose restart backend
```

## 低配服务器部署建议

- 2 核 2G 服务器可以运行 ProjectMentor AI 试用环境，但前端 Docker 构建和服务器本机 `npm run build` 都容易占用过多内存。
- 日常前端更新不要在服务器执行 `docker compose build frontend` 或 `docker compose up -d --build`。
- 前端推荐在本地 Windows 机器执行 `npm.cmd run build`，再把 `dist.zip` 上传到服务器覆盖静态文件。
- 日常后端更新不要在服务器执行 `mvn clean package`；推荐本地执行 `mvn clean package -DskipTests` 后上传 jar。
- Docker Compose 的正常启动能力仍然保留；本地开发、完整验证或资源充足服务器仍可使用完整 Compose 构建流程。

## 注册邮箱验证码部署检查

生产环境启用邮箱验证码时，服务器 `.env` 至少需要配置：

```env
EMAIL_VERIFICATION_ENABLED=true
MAIL_HOST=smtp.qq.com
MAIL_PORT=587
MAIL_USERNAME=你的发信邮箱
MAIL_PASSWORD=你的邮箱授权码
MAIL_FROM=你的发信邮箱
MAIL_SMTP_AUTH=true
MAIL_SMTP_STARTTLS_ENABLE=true
```

`docker-compose.yml` 的 backend 服务通过 `env_file: .env` 读取这些变量。启动或重启 backend 后，先确认变量已经进入容器：

```bash
docker compose exec backend printenv | grep -E "EMAIL_VERIFICATION|MAIL_" | sed -E 's/(MAIL_PASSWORD=).*/\1******/'
```

再用线上接口发一封测试验证码：

```bash
curl -i -X POST https://projectmentorai.com/api/auth/email-code \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"你的测试邮箱@example.com\"}"
```

不要把真实 `.env`、SMTP 授权码或邮箱密码提交到 Git；仓库中只保留 `.env.example` 示例。

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

低配服务器日常后端更新推荐使用快速方案：先在本地构建 jar，再上传服务器，服务器只用 `Dockerfile.fast` 打包运行镜像。

本地 Windows PowerShell：

```powershell
cd C:\Users\LiXin\Desktop\projectmentor-ai\backend\projectmentor-server
mvn clean package -DskipTests

$jar = Get-ChildItem .\target\*.jar | Where-Object { $_.Name -notlike "*sources*" -and $_.Name -notlike "*javadoc*" -and $_.Name -notlike "*.original" } | Select-Object -First 1
Copy-Item $jar.FullName .\target\projectmentor-server.jar -Force
scp target\projectmentor-server.jar root@服务器IP:/opt/projectmentor-ai/backend/projectmentor-server/target/projectmentor-server.jar
```

服务器：

```bash
cd /opt/projectmentor-ai
git pull
mkdir -p backend/projectmentor-server/target
ls -lh backend/projectmentor-server/target/projectmentor-server.jar
docker compose -f docker-compose.yml -f docker-compose.fast.yml build backend
docker compose up -d backend
docker compose logs --tail=120 backend
docker compose ps
```

`docker-compose.fast.yml` 只覆盖 backend 的 build 配置，其他服务仍使用原 `docker-compose.yml`。`Dockerfile.fast` 只复制 `target/projectmentor-server.jar`，不会在服务器内执行 Maven 或下载 Maven 依赖。

如果没有本地 jar，或在本地 / 资源充足服务器上需要完整 Docker 构建，仍可使用原方案：

```bash
docker compose build backend
docker compose up -d backend
```

原方案会在 Docker build 内执行 Maven 构建；低配服务器可能很慢。不要提交 `target/` 或 jar 文件到 Git。

## 查看日志

```bash
docker compose logs -f backend
```

也可以查看所有服务日志：

```bash
docker compose logs -f
```

## 备份、恢复与线上检查

备份 MySQL：

```bash
cd /opt/projectmentor-ai
bash scripts/backup-mysql.sh
```

备份文件输出到 `backups/mysql/`，文件名形如 `pmai_mysql_20260603_223000.sql`。脚本通过 `docker exec projectmentor-mysql` 在容器内执行 `mysqldump`，并从容器环境变量读取 `MYSQL_ROOT_PASSWORD` 和 `MYSQL_DATABASE`。

恢复 MySQL：

```bash
cd /opt/projectmentor-ai
bash scripts/restore-mysql.sh backups/mysql/xxx.sql
```

恢复脚本需要输入 `YES` 才会继续。恢复前请先备份当前数据库。

线上状态检查：

```bash
cd /opt/projectmentor-ai
bash scripts/check-prod.sh
```

该脚本只读输出当前时间、当前 Git commit、`docker compose ps`、backend / frontend / mysql / redis / nginx 容器状态、磁盘空间、内存、`docker stats --no-stream`、backend 最近 80 行日志、nginx 最近 50 行日志和常用排查命令。

## Nginx 敏感路径拦截

V4.4-1.1 在 Nginx 层拦截明显敏感路径，用于减少公网扫描噪音和误访问风险，不替代后端鉴权。

当前会直接返回 404 或 403 的典型请求：

- `.env`、`.env.local`、`.env.production.local`、`env.json`、`runtime-env.json`、`assets/env.json`、`static/env.json`、`static/config.json`。
- `.sql`、`.dump`、`.bak`、`.backup`、`.tar`、`.tar.gz`、`.tgz`、`.gz`、`.zip`、`.7z`、`.rar` 等备份或压缩文件请求。
- `/@fs/`、`/__better_errors`、`/_debug`、`/trace.axd`、`/rails/info/routes`、`/swagger`、`/swagger-ui`、`/v2/api-docs`、`/v3/api-docs`、`/actuator`、`/graphql`、`/v2/graphql`、`/api/install`。
- 包含 `/shell`、`wget`、`chmod`、`rm+-rf`、`GponForm`、`geoserver` 的明显扫描请求。

该配置不影响 `/api/**` 正常代理，不影响前端路由 fallback，不影响 `/share/**`、`/assets/**` 和 `/donate/**`。

部署前先检查 Nginx 配置：

```bash
cd /opt/projectmentor-ai
docker compose exec nginx nginx -t
```

如果 `nginx -t` 失败，不要 reload / restart。检查通过后重启：

```bash
docker compose restart nginx
```

测试命令：

```bash
curl -I http://127.0.0.1/.env
curl -I http://127.0.0.1/backup.sql
curl -I 'http://127.0.0.1/@fs/etc/passwd?raw'
curl -I http://127.0.0.1/
curl -I http://127.0.0.1/api/auth/me
```

期望 `.env`、`backup.sql`、`/@fs/...` 返回 404 或 403，首页返回 200，`/api/auth/me` 返回 401 或正常业务响应。

## 访问

- 前端入口：http://localhost
- 后端健康检查：http://localhost/api/health

生产构建中前端 API 使用同源 `/api`，由 Nginx 反向代理到后端 `backend:8080`，不会把 `AI_API_KEY` 放进前端。

## 域名、HTTPS 与反向代理

域名不是当前部署的必选项。已有域名时，可添加 `A` 记录指向服务器公网 IP，并把 `deploy/nginx/nginx.conf` 的 `server_name _;` 替换为实际域名。

HTTPS 推荐使用 Certbot / Let's Encrypt 或云厂商证书。证书文件路径和私钥由部署者自行配置，不应提交到 Git。启用 HTTPS 时要继续保留：

- `/api` 反向代理到 `backend:8080`；
- `try_files $uri $uri/ /index.html;`，支持 Vue Router history 刷新；
- 820MB 请求体限制及现有大文件上传超时；
- `Host`、`X-Real-IP`、`X-Forwarded-For`、`X-Forwarded-Proto` 请求头。

现有 Nginx 配置增加以下安全头：

- `X-Frame-Options: SAMEORIGIN`
- `X-Content-Type-Options: nosniff`
- `Referrer-Policy: strict-origin-when-cross-origin`
- `Permissions-Policy: geolocation=(), microphone=(), camera=()`

Content-Security-Policy 暂不强制写入默认配置。正式启用前应先核对前端脚本、样式、图片、API 和第三方来源，避免策略过严破坏现有页面。

当前没有 canonical 公共域名，因此不生成 `sitemap.xml`。正式域名确定后再使用绝对 HTTPS URL 添加 sitemap，不要写服务器 IP 或占位域名。

## 迁移资料清单

迁移服务器时至少需要带走：

- MySQL SQL 备份。
- `.env`。
- `frontend/projectmentor-web/public/donate/wechat.png`。
- `frontend/projectmentor-web/public/donate/alipay.png`。
- `docker-compose.yml`。
- `docker-compose.fast.yml`。
- `deploy/nginx/nginx.conf`。
- 后端 jar 可重新本地构建。
- 前端 `dist.zip` 可重新本地构建。

推荐迁移流程见 [server-deploy-vps.md](server-deploy-vps.md)：旧服务器先执行 `bash scripts/backup-mysql.sh`，下载 SQL 备份并保存 `.env` 与 donate 二维码；新服务器安装 Docker / Docker Compose、clone 仓库、复制 `.env` 和二维码、启动 `mysql redis`、执行恢复脚本，再按快速方案构建 backend、覆盖 frontend dist 并启动 nginx。

## 安全说明

- MySQL 和 Redis 不映射公网端口，只在 Docker Compose 内部网络中被后端通过 `mysql`、`redis` 服务名访问。
- 生产环境只开放 `22`、`80`、`443`；云防火墙不应开放 `3306`、`6379`。
- `.env` 不提交到 Git。
- `AI_API_KEY`、`JWT_SECRET`、`MYSQL_ROOT_PASSWORD` 必须通过环境变量配置，不要写入代码、镜像或公开文档。
- Nginx 敏感路径拦截用于减少公网扫描噪音，不替代后端鉴权和接口权限校验。

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
client_max_body_size 820m;
client_body_timeout 1200s;
client_header_timeout 120s;
send_timeout 1200s;
proxy_connect_timeout 60s;
proxy_send_timeout 1200s;
proxy_read_timeout 1200s;
proxy_request_buffering off;
```

后端 Spring Boot multipart 文件和请求限制均为 820MB，业务上传上限为 800MB。ZIP 上传会自动过滤常见依赖、构建、缓存和 IDE 目录；单个文本文件最多解析 2MB，最多保存 8000 个有效文件，累计有效处理大小最多 1GB。大文件上传可能需要数分钟，请不要刷新页面。仍建议删除 `node_modules`、`target`、`dist`、`.git`、`logs`、`coverage` 后再压缩，或直接制作源码核心包，可显著提升上传速度。

如果线上 `pm_project` 字段仍是旧长度，可执行：

```sql
ALTER TABLE pm_project MODIFY COLUMN description TEXT NULL COMMENT '项目描述';
ALTER TABLE pm_project MODIFY COLUMN tech_stack TEXT NULL COMMENT '技术栈';
```

V4.5-1 新增 Claim-Evidence 主张证据矩阵。已有数据库需要执行：

```sql
ALTER TABLE pm_analysis_report
ADD COLUMN claim_evidence LONGTEXT NULL COMMENT '主张证据矩阵JSON';
```
