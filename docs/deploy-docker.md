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
- `FLYWAY_ENABLED=true`：默认启用数据库 migration。
- `FLYWAY_BASELINE_ON_MIGRATE=false`：全新数据库和完成接入后的环境保持 `false`；只允许 legacy 非空数据库首次接入时临时设为 `true`。
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

全新 Compose 环境中，MySQL 先创建空 `projectmentor_ai` database；backend 等待 MySQL 和 Redis 健康后，由 Flyway 依次执行 V1、V2，migration 成功后 backend 才完成启动。Compose 不再通过 MySQL entrypoint 执行业务 `init.sql`。如果本机或 Docker Desktop 较慢导致后端启动失败，可以等待 MySQL healthy 后重启 backend：

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

## V4.9-1 数据库升级 preflight

部署包含 `V2__schema_integrity_constraints.sql` 的 backend 前，必须先执行 `bash scripts/backup-mysql.sh` 并确认备份可用，再在目标数据库执行：

```sql
SELECT email, COUNT(*) AS cnt
FROM pm_user
GROUP BY email
HAVING COUNT(*) > 1;

SELECT project_id, file_path, COUNT(*) AS cnt
FROM pm_project_file
GROUP BY project_id, file_path
HAVING COUNT(*) > 1;
```

两条查询都必须返回 0 行。任一查询有返回时立即 **STOP**，不要启动 V4.9-1 backend，不要自动删除、合并或选择保留历史数据；先人工核对并制定经过审查的显式修复方案。

- 已经部署 V4.9-0、history 中已有 version 1 的环境：保持 `FLYWAY_BASELINE_ON_MIGRATE=false`，preflight 通过后启动新 backend，预期新增成功的 `V2 SQL`。
- 尚无 `flyway_schema_history` 的 legacy 环境：先确认 schema 与 V1 等价并完成 preflight，仅首次临时设为 `true`；预期 history 为 `V1 BASELINE`、`V2 SQL`。验证业务数据后立即恢复为 `false` 并强制重建 backend。

完整检查与验证步骤见 [数据库迁移说明](database-migrations.md)。不要运行 Flyway clean、修改 V1/V2 或直接编辑 history。

## 后端更新流程

低配服务器日常后端更新推荐使用快速方案：先在本地构建 jar，再上传服务器，服务器只用 `Dockerfile.fast` 打包运行镜像。

如果本次后端版本包含新的 `db/migration/V*.sql`，部署前必须先执行数据库备份并审查 migration。backend 启动时会自动执行尚未应用的 migration；不要通过关闭 Flyway、修改旧 migration 或直接编辑 `flyway_schema_history` 绕过失败。

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
git status
git pull --ff-only
mkdir -p backend/projectmentor-server/target
ls -lh backend/projectmentor-server/target/projectmentor-server.jar
docker compose -f docker-compose.yml -f docker-compose.fast.yml build backend
docker compose up -d backend
docker compose logs --tail=120 backend
docker compose ps
```

如果 `git status` 显示服务器仓库存在未提交修改，先执行 `git diff` 核对并停止拉取，不要直接覆盖。

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

恢复 MySQL 前先停止 backend，避免恢复期间继续写入或触发 migration：

```bash
cd /opt/projectmentor-ai
docker compose stop backend
bash scripts/restore-mysql.sh backups/mysql/xxx.sql
```

恢复脚本需要输入 `YES` 才会继续。恢复前请先备份当前数据库。Flyway 接入后生成的完整备份会自然包含 `flyway_schema_history`；恢复后保持 `FLYWAY_BASELINE_ON_MIGRATE=false`，启动 backend 时 Flyway 会从已记录版本继续验证并执行后续 migration。

旧备份如果没有 `flyway_schema_history`，不能直接启动 backend 或假定 V1 会补齐旧结构。先确认恢复后的业务 schema 与 V1 等价并执行两条 V2 duplicate preflight，再按 [数据库迁移说明](database-migrations.md) 建立 V1 baseline 并执行 V2；不等价或查询发现重复时应停止并制定显式修复方案。

线上状态检查：

```bash
cd /opt/projectmentor-ai
bash scripts/check-prod.sh
```

该脚本只读输出当前时间、当前 Git commit、`docker compose ps`、backend / frontend / mysql / redis / nginx 容器状态、磁盘空间、内存、`docker stats --no-stream`、backend 最近 80 行日志、nginx 最近 50 行日志和常用排查命令。

## Nginx 敏感路径拦截

V4.8-3 在正式 HTTPS 配置中恢复并整理 Nginx 敏感路径规则，用于减少公网扫描噪音和误访问风险，不替代后端鉴权或完整 WAF。

当前会直接返回 404 或 403 的典型请求：

- `.env`、`.env.*`、`.env-*`、`env.json`、`runtime-env.json`、`assets/env.json`、`static/env.json`、`static/config.json`。
- `.sql`、`.dump`、`.bak`、`.backup`、`.tar`、`.tar.gz`、`.tgz`、`.gz`、`.zip`、`.7z`、`.rar` 等备份或压缩文件请求。
- `/@fs/`、`/__better_errors`、`/_debug`、`/trace.axd`、`/rails/info/routes`、`/swagger`、`/swagger-ui`、`/swagger-ui.html`、`/doc.html`、`/webjars/`、`/v2/api-docs`、`/v3/api-docs`、`/actuator`、`/graphql`、`/v2/graphql`、`/api/install`。
- 包含 `/shell`、`wget`、`chmod`、`rm+-rf`、`rm%20-rf`、`rm%2b-rf`、`GponForm`、`geoserver` 的明显扫描请求。

该配置保留 ACME challenge、`/api/**` 正常代理、前端路由 fallback、`/share/**`、`/assets/**` 和 `/donate/**`。`/api/projects/{projectId}/upload-zip` 不以 `.zip` 结尾，继续正常代理。

部署前先检查 Nginx 配置：

```bash
cd /opt/projectmentor-ai
docker compose exec nginx nginx -t
```

如果 `nginx -t` 失败，不要 reload / restart。检查通过后重启并执行线上回归：

```bash
docker compose restart nginx
bash scripts/check-nginx-security.sh https://projectmentorai.com
```

回归脚本要求 `/`、`/login`、`/register` 返回 200；敏感路径返回 403 或 404，且不能回退到 Vue 首页；明显扫描特征返回 403。脚本不发送 Cookie、Token 或密码，任一检查失败时退出码非 0。

## 访问

- 正式前端入口：https://projectmentorai.com
- 正式前端入口：https://www.projectmentorai.com
- 后端健康检查：https://projectmentorai.com/api/health

当前仓库中的 Nginx 配置面向正式 HTTPS 部署，启动时需要服务器已有正式证书文件。生产构建中前端 API 使用同源 `/api`，由 Nginx 反向代理到后端 `backend:8080`，不会把 `AI_API_KEY` 放进前端。不要为本地测试提交临时证书或私钥。

## 域名、HTTPS 与反向代理

当前正式域名为 `https://projectmentorai.com` 和 `https://www.projectmentorai.com`。`deploy/nginx/nginx.conf` 已配置这两个 `server_name`、HTTP 到 HTTPS 跳转、HTTP/2、TLS 1.2 / 1.3 和 ACME challenge。证书继续使用服务器上的正式路径，不提交证书或私钥。

当前 HTTPS 配置继续保留：

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

当前正式域名已经确定；如后续增加 `sitemap.xml`，应使用正式 HTTPS 绝对 URL，不要写服务器 IP 或占位域名。

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

推荐迁移流程见 [server-deploy-vps.md](server-deploy-vps.md)：旧服务器先执行 `bash scripts/backup-mysql.sh`，下载 SQL 备份并保存 `.env` 与 donate 二维码；新服务器安装 Docker / Docker Compose、clone 仓库、复制 `.env` 和二维码，只启动 `mysql redis` 后执行恢复。带 `flyway_schema_history` 的备份保持 baseline-on-migrate 为 `false`；无 history 的旧备份必须先完成 legacy schema 核对、V2 duplicate preflight 与一次性 V1 baseline，再启动 backend 执行 V2。

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

再次启动全新环境时，MySQL 会重新创建空 database，Flyway 会重新执行 V1、V2。该流程不可恢复已删除数据，执行前必须确认不需要保留现有 volume。

## 常见问题

### MySQL 启动慢导致后端连接失败

Compose 已配置 MySQL healthcheck，backend 会等待 MySQL healthy 后执行 Flyway migration。首次拉取镜像、创建 database 或执行 V1、V2 可能较慢；先查看 backend 日志中的 Flyway 错误，不要通过关闭 Flyway 绕过，再按情况重启：

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

### 旧数据库字段或表与 V1 不一致

历史版本曾通过手工 ALTER 或建表 SQL 补齐 `pm_project` 字段、`pm_project_qa_record`、`pm_feedback` 和 `pm_analysis_report.claim_evidence`。V4.9-0 的 legacy baseline 不会执行 V1，也不会比较或修复这些差异。

首次 baseline 前应将实际 schema 与 `V1__baseline_schema.sql` 逐表核对，并执行 V2 duplicate preflight。如果缺表、缺列、字段类型、默认值、索引、字符集或排序规则不一致，或发现重复邮箱 / 项目文件，停止 baseline，先制定经过审查的显式修复方案。V4.9-0 后不要继续复制历史手工 SQL 作为常规升级方式，详见 [数据库迁移说明](database-migrations.md)。
