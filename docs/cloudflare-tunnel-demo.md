# Cloudflare Tunnel 临时试用

这个方案用于低成本临时分享 ProjectMentor AI，让同学点开 `trycloudflare.com` 链接即可试用本机运行的前后端。它不依赖 Docker，也不适合长期正式运营。

## 1. 启动后端

在新的 PowerShell 窗口中进入后端目录：

```powershell
cd C:\Users\LiXin\Desktop\projectmentor-ai\backend\projectmentor-server
```

按自己的本地数据库和 AI 服务配置环境变量。真实的 `DB_PASSWORD` 和 `AI_API_KEY` 请在 PowerShell 中自行输入，不要写入代码或提交到仓库。

```powershell
$env:DB_HOST="localhost"
$env:DB_PORT="3306"
$env:DB_NAME="projectmentor"
$env:DB_USERNAME="your_db_username"
$env:DB_PASSWORD="your_db_password"

$env:AI_ENABLED="true"
$env:AI_BASE_URL="https://your-ai-provider.example.com/v1"
$env:AI_API_KEY="your_ai_api_key"
$env:AI_MODEL="your_model_name"

mvn spring-boot:run
```

后端默认运行在：

```text
http://localhost:8080
```

## 2. 启动前端

在第二个 PowerShell 窗口中进入前端目录：

```powershell
cd C:\Users\LiXin\Desktop\projectmentor-ai\frontend\projectmentor-web
npm run dev
```

前端默认运行在：

```text
http://localhost:5173
```

开发环境下前端请求会访问同源 `/api`，再由 Vite 代理到本机后端 `http://localhost:8080`。

## 3. 启动 Cloudflare Tunnel

在第三个 PowerShell 窗口中运行：

```powershell
& "C:\Users\LiXin\AppData\Local\Microsoft\WinGet\Packages\Cloudflare.cloudflared_Microsoft.Winget.Source_8wekyb3d8bbwe\cloudflared.exe" tunnel --url http://localhost:5173
```

启动成功后，终端会输出一个 `https://*.trycloudflare.com` 临时公网链接。把这个链接发给测试同学即可。

## 4. 验证方式

1. 先在电脑浏览器打开本地前端 `http://localhost:5173`，确认页面可以加载。
2. 复制 cloudflared 输出的 `trycloudflare.com` 链接。
3. 用手机关闭 Wi-Fi，切到手机流量访问该链接。
4. 尝试注册、登录、创建项目、调用需要后端的功能。
5. 如果页面能打开且接口功能正常，说明外部访问链路已经通过。

## 5. 注意事项

- 后端 PowerShell 窗口不能关。
- 前端 PowerShell 窗口不能关。
- cloudflared PowerShell 窗口不能关。
- 电脑不能关机，也不能断网。
- 这是临时试用链接，适合发给同学测试，不适合长期正式运营。
- `trycloudflare.com` 链接每次启动可能变化，需要以 cloudflared 当前输出为准。
- 不要把真实数据库密码、AI Key 或其他敏感信息写入代码、文档或提交记录。
