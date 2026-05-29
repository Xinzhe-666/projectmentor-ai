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

## 停止服务

```bash
docker compose down
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
- `AI_API_KEY` 只放在后端 `.env`，不要写进前端代码或公开文档。
- `AI_API_KEY`、`JWT_SECRET`、`MYSQL_ROOT_PASSWORD` 必须通过环境变量配置，不要写入代码或公开文档。
- 试用版不承诺商业级稳定性。
- 不要公开服务器 SSH 密码，也不要把服务器登录信息发到聊天记录、公开材料或仓库中。
