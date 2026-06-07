# ProjectMentor AI VPS 试用版部署

这份文档适用于低成本海外 VPS 试用版部署，不需要国内备案，适合早期同学试用和面试演示。当前部署方式定位为试用和演示，不承诺商业级稳定性。

## 前置条件

- 一台 Ubuntu 服务器。
- 已安装 Git。
- 已安装 Docker 和 Docker Compose。
- 服务器防火墙开放 `22` 和 `80`。
- 可选开放 `443`，用于后续配置 HTTPS。
- 不要在云防火墙开放 `3306`、`6379`。

## 低配服务器部署建议

2 核 2G 服务器可以运行 ProjectMentor AI 试用环境，但不适合每次在服务器上构建前端。服务器上不建议执行 `docker compose build frontend`，不建议执行 `docker compose up -d --build`，也不建议执行 `npm run build`。前端更新推荐在本地构建 `dist`，压缩为 `dist.zip` 后上传服务器覆盖。

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
AI_API_KEY=可留空；留空时使用规则版 fallback
```

生产环境应保持 `KNIFE4J_ENABLED=false`，避免公开 Knife4j API 文档。本地开发如需调试接口，可通过 `KNIFE4J_ENABLED=true` 开启。

启动服务：

```bash
docker compose up -d
```

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

## 访问

```text
http://服务器IP
```

## 数据库备份与恢复

备份线上 MySQL：

```bash
cd /opt/projectmentor-ai
bash scripts/backup-mysql.sh
```

备份文件会写入 `backups/mysql/`，文件名形如 `pmai_mysql_20260603_223000.sql`。脚本从 `projectmentor-mysql` 容器环境变量读取 `MYSQL_ROOT_PASSWORD` 和 `MYSQL_DATABASE`，不会在脚本中写死真实密码。

从指定 SQL 文件恢复：

```bash
cd /opt/projectmentor-ai
bash scripts/restore-mysql.sh backups/mysql/xxx.sql
```

恢复脚本会提示“恢复操作会覆盖或影响当前数据库，请确认已备份当前数据。”，并要求输入 `YES` 后才继续。恢复前请务必先备份当前数据库。

## Nginx 敏感路径拦截

V4.4-1.1 在 `deploy/nginx/nginx.conf` 增加 Nginx 层敏感路径拦截，用于减少公网扫描噪音和误访问风险，不替代后端鉴权。

当前会拦截：

- `.env`、`.env.local`、`.env.production`、`.env.production.local`、`.env.development`、`.env.development.local`、`env.json`、`runtime-env.json`、`assets/env.json`、`static/env.json`、`static/config.json`。
- `.sql`、`.dump`、`.bak`、`.backup`、`.tar`、`.tar.gz`、`.tgz`、`.gz`、`.zip`、`.7z`、`.rar` 等备份或压缩文件请求。
- `/@fs/`、`/__better_errors`、`/_debug`、`/trace.axd`、`/rails/info/routes`、`/swagger`、`/swagger-ui`、`/v2/api-docs`、`/v3/api-docs`、`/actuator`、`/graphql`、`/v2/graphql`、`/api/install`。
- 包含 `/shell`、`wget`、`chmod`、`rm+-rf`、`GponForm`、`geoserver` 的明显扫描请求。

该配置保留 `/api/**` 正常代理、前端路由 fallback、`/share/**`、`/assets/**` 和 `/donate/**` 正常访问。

部署前先检查 Nginx 配置：

```bash
cd /opt/projectmentor-ai
docker compose exec nginx nginx -t
```

如果 `nginx -t` 失败，不要 reload / restart。检查通过后重启 Nginx：

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

期望：

- `/.env` 返回 404 或 403。
- `/backup.sql` 返回 404 或 403。
- `/@fs/etc/passwd?raw` 返回 404 或 403。
- `/` 返回 200。
- `/api/auth/me` 返回 401 或正常业务响应，不能被 Nginx 误拦截。

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

本地 Windows PowerShell：

```powershell
cd C:\Users\LiXin\Desktop\projectmentor-ai\backend\projectmentor-server
mvn clean package -DskipTests

$jar = Get-ChildItem .\target\*.jar | Where-Object { $_.Name -notlike "*sources*" -and $_.Name -notlike "*javadoc*" -and $_.Name -notlike "*.original" } | Select-Object -First 1
Copy-Item $jar.FullName .\target\projectmentor-server.jar -Force
scp target\projectmentor-server.jar root@8.218.121.30:/opt/projectmentor-ai/backend/projectmentor-server/target/projectmentor-server.jar
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

快速方案使用 `backend/projectmentor-server/Dockerfile.fast`，只复制 `target/projectmentor-server.jar`，不执行 `mvn clean package`。`docker-compose.fast.yml` 只覆盖 backend 的 build 配置，MySQL、Redis、frontend 和 nginx 仍沿用原 `docker-compose.yml`。

如果没有本地 jar，或需要在资源充足环境里完整验证 Docker 构建链路，仍可使用原方案：

```bash
git pull
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

# 5. 恢复数据库
bash scripts/restore-mysql.sh backups/mysql/xxx.sql
```

恢复数据库后，按现有轻量部署方式继续：

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

## 数据库变更

V4.2-1 新增项目问答记录表 `pm_project_qa_record`，V4.3-3 新增反馈表 `pm_feedback`，V4.5-1 为审计报告新增 `claim_evidence` 字段。全新部署会在 MySQL 初始化时执行 `init.sql`；已有数据库不会自动重放初始化脚本，需要手动执行下面的 SQL：

```sql
ALTER TABLE pm_analysis_report
ADD COLUMN claim_evidence LONGTEXT NULL COMMENT '主张证据矩阵JSON';
```

```sql
CREATE TABLE IF NOT EXISTS pm_project_qa_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    project_id BIGINT NOT NULL COMMENT '项目ID',
    question VARCHAR(1000) NOT NULL COMMENT '问题',
    answer TEXT NULL COMMENT '回答',
    ai_used TINYINT NOT NULL DEFAULT 0 COMMENT '是否使用AI',
    evidence_json TEXT NULL COMMENT '证据JSON',
    suggested_follow_ups_json TEXT NULL COMMENT '建议追问JSON',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    INDEX idx_user_project (user_id, project_id),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目问答记录表';
```

```sql
CREATE TABLE IF NOT EXISTS pm_feedback (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '反馈ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    contact VARCHAR(255) NULL COMMENT '联系方式',
    type VARCHAR(50) NOT NULL COMMENT '反馈类型',
    content TEXT NOT NULL COMMENT '反馈内容',
    page_url VARCHAR(500) NULL COMMENT '反馈来源页面',
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING' COMMENT '状态',
    admin_note TEXT NULL COMMENT '管理员备注',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    INDEX idx_user_id (user_id),
    INDEX idx_status (status),
    INDEX idx_type (type),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户反馈表';
```

## 重置数据库

```bash
docker compose down -v
```

注意：这会删除 Docker volume 中的 MySQL 和 Redis 数据，数据库内容会被清空。

## 常见问题

### Docker Hub 拉镜像失败

可能是服务器网络访问 Docker Hub 不稳定。可以稍后重试，或按服务器供应商建议配置 Docker 镜像源。

### 80 端口被占用

先检查占用情况：

```bash
sudo lsof -i :80
```

如果已有 Nginx、Apache 或其他服务占用 80 端口，需要先停止冲突服务，或调整 `docker-compose.yml` 中 nginx 的端口映射。

### MySQL 首次启动慢

首次启动会初始化数据库并执行 `init.sql`，可能需要几十秒。可以查看状态：

```bash
docker compose ps
docker compose logs -f mysql
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

### 项目字段长度不足

V4.4-2 将项目描述上限调整为 10000 字符、技术栈上限调整为 5000 字符。`init.sql` 中 `pm_project.description` 和 `pm_project.tech_stack` 均为 `TEXT`。如果线上已有库仍使用旧字段长度，可执行：

```sql
ALTER TABLE pm_project MODIFY COLUMN description TEXT NULL COMMENT '项目描述';
ALTER TABLE pm_project MODIFY COLUMN tech_stack TEXT NULL COMMENT '技术栈';
```

### 前端刷新 404

当前 nginx 配置使用 `try_files $uri $uri/ /index.html;` 支持 Vue Router history 模式。如果刷新仍 404，确认挂载的 `deploy/nginx/nginx.conf` 是否生效，并重启 nginx 容器。

## 安全提醒

- 不要提交 `.env`。
- MySQL 和 Redis 不映射公网端口，只在 Docker Compose 内部网络中被后端通过 `mysql`、`redis` 服务名访问。
- 生产环境只开放 `22`、`80`、`443`；云防火墙不应开放 `3306`、`6379`。
- Nginx 会拦截常见敏感文件、备份文件、调试入口和扫描路径，避免进入 SPA fallback；这不替代后端鉴权。
- `JWT_SECRET` 必须换成长随机字符串，长度至少 32 个字符。
- `ADMIN_EMAILS` 只配置管理员邮箱白名单，不要把个人 `.env` 提交到仓库。
- `AI_API_KEY` 只放在后端 `.env`，不要写进前端代码或公开文档。
- `AI_API_KEY`、`JWT_SECRET`、`MYSQL_ROOT_PASSWORD` 必须通过环境变量配置，不要写入代码或公开文档。
- 试用版不承诺商业级稳定性。
- 不要公开服务器 SSH 密码，也不要把服务器登录信息发到聊天记录、公开材料或仓库中。
