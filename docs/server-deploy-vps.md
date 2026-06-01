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
AI_API_KEY=可留空；留空时使用规则版 fallback
```

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

## 访问

```text
http://服务器IP
```

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

如果修改 Java 后端代码，低配服务器可以构建但会较慢：

```bash
git pull
docker compose build backend
docker compose up -d backend
```

如果服务器构建明显卡顿，后续可升级为 GitHub Actions 构建镜像，再由服务器拉取镜像部署。

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

V4.2-1 新增项目问答记录表 `pm_project_qa_record`，V4.3-3 新增反馈表 `pm_feedback`。全新部署会在 MySQL 初始化时执行 `init.sql`；已有数据库不会自动重放初始化脚本，需要手动执行下面的 SQL：

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

### 上传 ZIP 超过 200MB

当前普通项目 ZIP 最大支持 200MB，nginx 请求体限制为 220MB。建议删除 `target`、`node_modules`、`dist`、`build` 等目录后重新压缩。

### 上传大 ZIP 超时

当前 nginx 已为大文件上传设置 `client_body_timeout 900s`、`send_timeout 900s`、`proxy_send_timeout 900s`、`proxy_read_timeout 900s`，并对 `/api/` 关闭 `proxy_request_buffering`。如果网络较慢，大文件上传可能需要数分钟，请不要刷新页面；建议删除 `node_modules`、`target`、`.git` 后再压缩，可显著提升速度。

### 前端刷新 404

当前 nginx 配置使用 `try_files $uri $uri/ /index.html;` 支持 Vue Router history 模式。如果刷新仍 404，确认挂载的 `deploy/nginx/nginx.conf` 是否生效，并重启 nginx 容器。

## 安全提醒

- 不要提交 `.env`。
- MySQL 和 Redis 不映射公网端口，只在 Docker Compose 内部网络中被后端通过 `mysql`、`redis` 服务名访问。
- 生产环境只开放 `22`、`80`、`443`；云防火墙不应开放 `3306`、`6379`。
- `JWT_SECRET` 必须换成长随机字符串，长度至少 32 个字符。
- `ADMIN_EMAILS` 只配置管理员邮箱白名单，不要把个人 `.env` 提交到仓库。
- `AI_API_KEY` 只放在后端 `.env`，不要写进前端代码或公开文档。
- `AI_API_KEY`、`JWT_SECRET`、`MYSQL_ROOT_PASSWORD` 必须通过环境变量配置，不要写入代码或公开文档。
- 试用版不承诺商业级稳定性。
- 不要公开服务器 SSH 密码，也不要把服务器登录信息发到聊天记录、公开材料或仓库中。
