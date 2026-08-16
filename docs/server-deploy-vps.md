# ProjectMentor AI VPS 试用版部署

这份文档适用于低成本海外 VPS 试用版部署，不需要国内备案，适合早期同学试用和面试演示。当前部署方式定位为试用和演示，不承诺商业级稳定性。

## 前置条件

- 一台 Ubuntu 服务器。
- 已安装 Git。
- 已安装 Docker 和 Docker Compose。
- 服务器防火墙开放 `22`、`80` 和 `443`。
- 不要在云防火墙开放 `3306`、`6379`。

## 低配服务器部署建议

2 核 2G 服务器可以运行 ProjectMentor AI 试用环境，但不适合每次在服务器上构建前端或后端。服务器上不建议执行 `docker compose build frontend`，不建议执行 `docker compose up -d --build`，也不建议执行 `npm run build` 或 `mvn clean package`。前端更新推荐在本地构建 `dist`，压缩为 `dist.zip` 后上传服务器覆盖；后端更新推荐在本地构建 jar 后上传服务器。

Docker Compose 的正常启动能力仍然保留；本地开发、完整验证或资源充足服务器仍可使用完整构建流程。低配服务器日常启动或重启优先使用：

```bash
docker compose up -d
```

## 部署步骤

```bash
git clone https://github.com/Xinzhe-666/projectmentor-ai.git
cd projectmentor-ai
cp .env.example .env
```

编辑 `.env`，至少填写：

```env
MYSQL_ROOT_PASSWORD=请换成数据库 root 密码
JWT_SECRET=请换成长随机字符串
ADMIN_EMAILS=your-admin@example.com
KNIFE4J_ENABLED=false
FLYWAY_ENABLED=true
FLYWAY_BASELINE_ON_MIGRATE=false
AI_API_KEY=可留空；留空时使用规则版 fallback
```

生产环境应保持 `KNIFE4J_ENABLED=false`，避免公开 Knife4j API 文档。本地开发如需调试接口，可通过 `KNIFE4J_ENABLED=true` 开启。

`FLYWAY_BASELINE_ON_MIGRATE` 默认和日常运行必须保持 `false`。只有已经存在业务表、尚无 `flyway_schema_history` 且已确认 schema 与 V1 等价的 legacy 数据库，才在首次接入时临时设为 `true`；成功后应立即恢复为 `false`。

注册邮箱验证码上线时，还需要在服务器 `.env` 中配置 SMTP 并开启邮箱验证码：

```env
EMAIL_VERIFICATION_ENABLED=true
MAIL_HOST=smtp.qq.com
MAIL_PORT=587
MAIL_USERNAME=你的发信邮箱
MAIL_PASSWORD=你的邮箱授权码
MAIL_FROM=你的发信邮箱
MAIL_SMTP_AUTH=true
MAIL_SMTP_STARTTLS_ENABLE=true
EMAIL_VERIFICATION_SUBJECT=ProjectMentor AI 邮箱验证码
```

真实 `.env` 不提交到 Git；`.env.example` 只保留示例值，不写入真实 SMTP 授权码、邮箱密码、AI Key 或 `JWT_SECRET`。

启动服务：

```bash
docker compose up -d
```

全新环境中，MySQL 只创建空 `projectmentor_ai` database；backend 等待 MySQL healthy 后由 Flyway 依次执行 V1、V2，migration 成功后才完成启动。已有非空数据库不要直接按 fresh 流程启动，先执行本文“Flyway 数据库迁移与 V4.9-1 preflight”章节。

## 查看状态

```bash
docker compose ps
docker compose logs -f backend
docker compose logs -f nginx
```

也可以输出一份适合复制给排查问题使用的线上状态报告：

```bash
cd /opt/projectmentor-ai
bash scripts/check-prod.sh
```

## 注册邮箱验证码线上验收

`docker-compose.yml` 的 backend 服务通过 `env_file: .env` 读取邮箱验证码和 SMTP 配置。更新 `.env` 后重启 backend，并确认变量已经进入容器：

```bash
docker compose up -d backend
docker compose exec backend printenv | grep -E "EMAIL_VERIFICATION|MAIL_" | sed -E 's/(MAIL_PASSWORD=).*/\1******/'
```

再用线上接口发一封测试验证码：

```bash
curl -i -X POST https://projectmentorai.com/api/auth/email-code \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"你的测试邮箱@example.com\"}"
```

Foxmail / QQ SMTP 推荐使用邮箱授权码作为 `MAIL_PASSWORD`，不要使用网页登录密码，也不要把授权码写入公开文档或提交到仓库。

## 访问

```text
https://projectmentorai.com
https://www.projectmentorai.com
```

## 正式域名与 HTTPS

当前正式域名为 `projectmentorai.com` 和 `www.projectmentorai.com`。`deploy/nginx/nginx.conf` 已配置两个域名、HTTP 到 HTTPS 跳转、HTTP/2、TLS 1.2 / 1.3 和 ACME challenge；不要修改正式证书路径，也不要把证书或私钥提交到仓库。

当前 HTTPS 配置继续保留：

- `/api` 转发到 `backend:8080`；
- `try_files $uri $uri/ /index.html;` 前端 history fallback；
- `client_max_body_size 820m` 和现有上传超时设置；
- `X-Forwarded-Proto $scheme` 等代理请求头。

当前正式域名已经确定；如后续增加 sitemap，应使用正式 HTTPS 绝对 URL，不要使用服务器 IP 或占位域名。

## 数据库备份与恢复

备份线上 MySQL：

```bash
cd /opt/projectmentor-ai
bash scripts/backup-mysql.sh
```

备份文件会写入 `backups/mysql/`，文件名形如 `pmai_mysql_20260603_223000.sql`。脚本从 `projectmentor-mysql` 容器环境变量读取 `MYSQL_ROOT_PASSWORD` 和 `MYSQL_DATABASE`，不会在脚本中写死真实密码。

从指定 SQL 文件恢复前先停止 backend，避免恢复期间继续写入或触发 migration：

```bash
cd /opt/projectmentor-ai
docker compose stop backend
bash scripts/restore-mysql.sh backups/mysql/xxx.sql
```

恢复脚本会提示“恢复操作会覆盖或影响当前数据库，请确认已备份当前数据。”，并要求输入 `YES` 后才继续。恢复前请务必先备份当前数据库。

Flyway 接入后创建的完整备份会自然包含 `flyway_schema_history`，恢复后保持 `FLYWAY_BASELINE_ON_MIGRATE=false` 再启动 backend，Flyway 会从已记录版本继续验证和迁移。旧备份如果没有 history 表，必须先确认业务 schema 与 V1 等价、执行 V2 duplicate preflight，再建立 V1 baseline 并执行 V2；baseline 不会补缺表、缺列或修复旧字段类型。

## Nginx 敏感路径拦截

V4.8-3 在正式 HTTPS 配置中恢复并整理 Nginx 层敏感路径拦截，用于减少公网扫描噪音和误访问风险，不替代后端鉴权或完整 WAF。

当前会拦截：

- `.env`、`.env.*`、`.env-*`、`env.json`、`runtime-env.json`、`assets/env.json`、`static/env.json`、`static/config.json`。
- `.sql`、`.dump`、`.bak`、`.backup`、`.tar`、`.tar.gz`、`.tgz`、`.gz`、`.zip`、`.7z`、`.rar` 等备份或压缩文件请求。
- `/@fs/`、`/__better_errors`、`/_debug`、`/trace.axd`、`/rails/info/routes`、`/swagger`、`/swagger-ui`、`/swagger-ui.html`、`/doc.html`、`/webjars/`、`/v2/api-docs`、`/v3/api-docs`、`/actuator`、`/graphql`、`/v2/graphql`、`/api/install`。
- 包含 `/shell`、`wget`、`chmod`、`rm+-rf`、`rm%20-rf`、`rm%2b-rf`、`GponForm`、`geoserver` 的明显扫描请求。

该配置保留 ACME challenge、`/api/**` 正常代理、前端路由 fallback、`/share/**`、`/assets/**` 和 `/donate/**` 正常访问。`/api/projects/{projectId}/upload-zip` 不以 `.zip` 结尾，继续正常代理。

部署前先检查 Nginx 配置：

```bash
cd /opt/projectmentor-ai
docker compose exec nginx nginx -t
```

如果 `nginx -t` 失败，不要 reload / restart。检查通过后重启 Nginx 并执行线上回归：

```bash
docker compose restart nginx
bash scripts/check-nginx-security.sh https://projectmentorai.com
```

回归脚本要求正常页面返回 200，敏感路径返回 403 或 404 且不能落入 Vue 首页，明显扫描特征返回 403；任一检查失败时退出码非 0。

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

## 后端快速部署流程

2 核 2G 服务器日常更新后端时，推荐使用“本地构建 jar + 上传服务器 + 服务器快速构建运行镜像”。这样服务器只执行 Docker 镜像打包，不在服务器内运行 Maven，也不会在服务器内下载 Maven 依赖。

如果本次版本包含新的 `db/migration/V*.sql`，部署 backend 前必须先备份数据库并审查 migration。backend 启动会自动应用尚未执行的 migration；不得通过关闭 Flyway、修改已应用 migration 或直接编辑 `flyway_schema_history` 绕过错误。

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

快速方案使用 `backend/projectmentor-server/Dockerfile.fast`，只复制 `target/projectmentor-server.jar`，不执行 `mvn clean package`。`docker-compose.fast.yml` 只覆盖 backend 的 build 配置，MySQL、Redis、frontend 和 nginx 仍沿用原 `docker-compose.yml`。

如果没有本地 jar，或需要在资源充足环境里完整验证 Docker 构建链路，仍可使用原方案：

```bash
git status
git pull --ff-only
docker compose build backend
docker compose up -d backend
```

原方案会在 Docker build 内执行 Maven 构建，低配服务器可能非常慢；日常部署推荐优先使用快速方案。不要提交 `target/` 或 jar 文件到 Git。

## 服务器迁移与恢复流程

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

旧服务器：

```bash
cd /opt/projectmentor-ai
bash scripts/backup-mysql.sh
ls -lh backups/mysql/
```

然后下载或 `scp` SQL 备份，保存 `.env`，并保存 `frontend/projectmentor-web/public/donate/wechat.png` 和 `frontend/projectmentor-web/public/donate/alipay.png`。不要把 `.env` 或 SQL 备份提交到 Git。

新服务器：

```bash
# 1. 安装 Docker / Docker Compose
# 2. 克隆仓库
git clone https://github.com/Xinzhe-666/projectmentor-ai.git /opt/projectmentor-ai
cd /opt/projectmentor-ai

# 3. 复制 .env、donate 二维码和 SQL 备份到新服务器
mkdir -p backups/mysql frontend/projectmentor-web/public/donate

# 4. 先启动 MySQL 和 Redis
docker compose up -d mysql redis

# 5. 保持 backend 停止并恢复数据库
bash scripts/restore-mysql.sh backups/mysql/xxx.sql
```

恢复后先判断备份是否包含 `flyway_schema_history`。包含 history 的当前备份保持 `FLYWAY_BASELINE_ON_MIGRATE=false`；没有 history 的旧备份必须按下一节完成 schema 核对、V2 duplicate preflight 与一次性 V1 baseline。确认后再按现有轻量部署方式继续：

```bash
# 本地重新构建 backend jar 后上传到：
# /opt/projectmentor-ai/backend/projectmentor-server/target/projectmentor-server.jar
docker compose -f docker-compose.yml -f docker-compose.fast.yml build backend
docker compose up -d backend

# 本地重新 build frontend dist.zip 后上传并覆盖静态文件
# 覆盖完成后启动 frontend 和 nginx
docker compose up -d frontend nginx

# 检查状态
bash scripts/check-prod.sh
```

迁移完成后重点测试：登录、项目、报告、问答、反馈、额度和管理员后台。

## 管理员后台配置

V4.3-1 新增只读管理员后台。生产环境需要在 `.env` 中配置管理员邮箱：

```env
ADMIN_EMAILS=your-admin@example.com
```

说明：

- 多个管理员邮箱使用英文逗号分隔。
- 后端会去除空格并忽略大小写。
- 修改 `.env` 后需要重启 backend 才会读取新环境变量：

```bash
docker compose up -d backend
```

- 管理员后台用于查看用户、项目、报告、分享、问答和反馈运行情况。
- 后台不会返回密码、密钥、数据库密码、完整项目源码或完整问答证据 JSON。
- V4.3-2 增加管理员手动发放额度：只能增加额度，不能扣减额度；每次发放都会写入额度流水，并记录发放原因、adminId 和 adminEmail。
- V4.3-3 增加反馈管理：管理员可筛选反馈、查看详情并更新状态，不做邮件通知、客服聊天或复杂工单系统。
- 当前没有接入真实支付、支付回调或批量发放能力。

## 停止服务

```bash
docker compose down
```

## Flyway 数据库迁移与 V4.9-1 preflight

V4.9-0 起，`db/migration/V*.sql` 是业务 schema 的唯一真源。V1 已不可变；V4.9-1 新增 V2，为 email 与项目内文件路径增加 UNIQUE、将新 plan 的 credits default 对齐为 10，并增加 QA project 索引。V2 不更新既有余额，也不自动清理历史重复数据。

生产启动包含 V2 的 backend 前，先执行 `bash scripts/backup-mysql.sh` 并确认备份已另行妥善保存，再在目标数据库运行：

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

两条查询都必须返回 0 行。只要有任何返回就应 **STOP**：不要部署 V2，不要自动 `DELETE`、合并用户或选择 `MIN(id)` / `MAX(id)`；先人工检查并制定经过审查的显式修复方案。查询不需要、也不应输出 password、文件 content、token 或 secret。

生产状态 A——已经部署 V4.9-0，history 中已有成功的 version 1：

1. 完成备份与上述 duplicate preflight。
2. 保持 `FLYWAY_ENABLED=true`、`FLYWAY_BASELINE_ON_MIGRATE=false`。
3. 启动 V4.9-1 backend，检查日志与 history，确认新增成功的 `V2 SQL`。
4. 验证 13 张业务表、V2 索引/default、既有余额和关键业务数据，再检查 `/api/health`。

生产状态 B——仍是无 `flyway_schema_history` 的 legacy database：

1. 完成备份，确认实际表、列、null/default、索引、字符集与排序规则均与 V1 等价，再完成上述 duplicate preflight。
2. 如果缺表、缺列、旧字段类型或历史重复，立即停止；baseline 不会检查或修复这些差异。
3. 仅本次临时设置 `FLYWAY_ENABLED=true`、`FLYWAY_BASELINE_ON_MIGRATE=true`，启动已构建并验证的 V4.9-1 backend。
4. 检查 history，预期为成功的 `V1 BASELINE` 与 `V2 SQL`；V1 被跳过，V2 实际执行。
5. 验证原有用户、项目、文件和余额均保留，并检查登录、项目、报告、问答、反馈、额度和管理员后台。
6. 立即恢复 `FLYWAY_BASELINE_ON_MIGRATE=false`，执行 `docker compose up -d --force-recreate backend`，再次检查日志、history 与健康状态。

如果 history 已经存在，不要再次 baseline。生产不要长期保持 baseline 为 `true`；不要运行 `flyway clean`、修改已应用的 V1/V2 或直接编辑 history。完整规范见 [数据库迁移说明](database-migrations.md)。

## 重置数据库

```bash
docker compose down -v
```

注意：这会删除 Docker volume 中的 MySQL 和 Redis 数据，数据库内容会被清空。

再次启动全新环境时，MySQL 会重新创建空 database，Flyway 会重新执行 V1、V2；该操作不能恢复已删除的数据。

## 常见问题

### Docker Hub 拉镜像失败

可能是服务器网络访问 Docker Hub 不稳定。可以稍后重试，或按服务器供应商建议配置 Docker 镜像源。

### 80 端口被占用

先检查占用情况：

```bash
sudo lsof -i :80
```

如果已有 Nginx、Apache 或其他服务占用 80 端口，需要先停止冲突服务，或调整 `docker-compose.yml` 中 nginx 的端口映射。

### MySQL / Flyway 首次启动慢

首次启动需要拉取镜像、创建空 database，并由 backend 执行 Flyway V1、V2，可能需要几十秒。可以查看状态和 migration 日志：

```bash
docker compose ps
docker compose logs -f mysql
docker compose logs -f backend
```

### 后端连接数据库失败

检查 `.env` 中 `MYSQL_ROOT_PASSWORD` 是否和 compose 中 MySQL 容器一致，然后查看后端日志：

```bash
docker compose logs -f backend
```

如果刚启动就失败，可能是 MySQL 仍在初始化，等待健康检查完成后再观察。

### AI_API_KEY 未配置时 fallback

`AI_API_KEY` 可以留空。留空或调用失败时，系统会使用规则版报告，不会把这部分包装成完整 AI 审计能力。

### 分享链接打不开

确认前端访问地址是服务器 IP 或域名，且 nginx 正在运行：

```bash
docker compose ps
docker compose logs -f nginx
```

分享接口只开放 `/api/share/reports/**` 的只读公开访问，普通报告详情仍需要登录。

### 上传 ZIP 超过 800MB

当前普通项目 ZIP 最大支持 800MB，nginx 请求体限制为 820m，Spring Multipart 文件和请求限制均为 820MB。建议删除 `target`、`node_modules`、`dist`、`build`、`.git`、`logs`、`coverage` 等目录后重新压缩，或先制作源码核心包。

### 上传大 ZIP 超时

当前 nginx 已为大文件上传设置 `client_body_timeout 1200s`、`send_timeout 1200s`、`proxy_send_timeout 1200s`、`proxy_read_timeout 1200s`，并对 `/api/` 关闭 `proxy_request_buffering`。如果网络较慢，大文件上传可能需要数分钟，请不要刷新页面；建议删除 `node_modules`、`target`、`dist`、`.git` 后再压缩，可显著提升速度。

### 旧数据库字段、表或索引与 V1 不一致

历史版本曾依靠手工 SQL 补齐字段和表。legacy baseline 不会执行 V1，也不会检测 schema drift；如果实际数据库与 V1 不一致，不要直接 baseline 或复制旧 ALTER。即使与 V1 等价，也必须先执行 V2 duplicate preflight；发现结构偏差或重复数据都应停止并制定显式修复方案，详见 [数据库迁移说明](database-migrations.md)。

### 前端刷新 404

当前 nginx 配置使用 `try_files $uri $uri/ /index.html;` 支持 Vue Router history 模式。如果刷新仍 404，确认挂载的 `deploy/nginx/nginx.conf` 是否生效，并重启 nginx 容器。

## 安全提醒

- 不要提交 `.env`。
- MySQL 和 Redis 不映射公网端口，只在 Docker Compose 内部网络中被后端通过 `mysql`、`redis` 服务名访问。
- 生产环境只开放 `22`、`80`、`443`；云防火墙不应开放 `3306`、`6379`。
- Nginx 会拦截常见敏感文件、备份文件、调试入口和扫描路径，避免进入 SPA fallback；这不替代后端鉴权。
- Nginx 默认返回 `X-Frame-Options`、`X-Content-Type-Options`、`Referrer-Policy` 和 `Permissions-Policy` 安全头。
- Content-Security-Policy 建议在确认实际 API、字体、图片和脚本来源后逐步启用；不要直接复制过严策略导致 Vue 页面或本地资源失效。
- `JWT_SECRET` 必须换成长随机字符串，长度至少 32 个字符。
- `ADMIN_EMAILS` 只配置管理员邮箱白名单，不要把个人 `.env` 提交到仓库。
- `AI_API_KEY` 只放在后端 `.env`，不要写进前端代码或公开文档。
- `AI_API_KEY`、`JWT_SECRET`、`MYSQL_ROOT_PASSWORD` 必须通过环境变量配置，不要写入代码或公开文档。
- 试用版不承诺商业级稳定性。
- 不要公开服务器 SSH 密码，也不要把服务器登录信息发到聊天记录、公开材料或仓库中。
