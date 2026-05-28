# ProjectMentor AI VPS 试用版部署

这份文档适用于低成本海外 VPS 试用版部署，不需要国内备案，适合早期同学试用和面试演示。当前部署方式定位为试用和演示，不承诺商业级稳定性。

## 前置条件

- 一台 Ubuntu 服务器。
- 已安装 Git。
- 已安装 Docker 和 Docker Compose。
- 服务器防火墙开放 80 端口。
- 可选开放 443 端口，用于后续配置 HTTPS。

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
docker compose up -d --build
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

## 更新项目

```bash
git pull
docker compose up -d --build
```

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

### 前端刷新 404

当前 nginx 配置使用 `try_files $uri $uri/ /index.html;` 支持 Vue Router history 模式。如果刷新仍 404，确认挂载的 `deploy/nginx/nginx.conf` 是否生效，并重启 nginx 容器。

## 安全提醒

- 不要提交 `.env`。
- `JWT_SECRET` 必须换成长随机字符串，长度至少 32 个字符。
- `AI_API_KEY` 只放在后端 `.env`，不要写进前端代码或公开文档。
- 试用版不承诺商业级稳定性。
- 不要公开服务器 SSH 密码，也不要把服务器登录信息发到聊天记录、公开材料或仓库中。
