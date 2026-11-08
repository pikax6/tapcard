# GitHub 上传说明 — tapcard

> 本文件是「逐项标注说明」：每个文件是否入库、体积、风险，以及推送方式。
> 仓库已在本机初始化并提交，推送由你**在自己电脑**上执行（沙箱无法弹登录框，自动推送会失败）。

## 一、本仓库要上传什么（最终入库内容）

| 路径 | 类型 | 入库？ | 说明 |
|---|---|---|---|
| `backend/` | Spring Boot 后端（com.zxy.tapcard） | ✅ | `src/`（Java 源码）、`pom.xml`、`NAMING.md` |
| `frontend/` | Vue3 + Vite 前端 | ✅ | `src/`、`public/`、`index.html`、`package*.json`、`vite.config.js`、`.env.development` |
| `sql/` | 数据库脚本 | ✅ | `ykt_schema.sql`（表结构）、`ykt_seed.sql`（种子数据）、`ykt_er_only.sql` |
| `README.md` | 仓库说明 | ✅ | 已存在，保留 |
| `.gitignore` | 忽略规则 | ✅ | 已存在并本次补充（vite 临时产物、`.workbuddy/`、env 安全项） |
| `backend/target/` | Maven 编译产物 | ❌ 排除 | 30M，`target/` 已忽略 |
| `backend/.idea/` | IDE 配置 | ❌ 排除 | `.idea/` 已忽略 |
| `frontend/node_modules/` | 前端依赖 | ❌ 排除 | `node_modules/` 已忽略 |
| `frontend/dist/` | 前端构建产物 | ❌ 排除 | `dist/` 已忽略 |
| `frontend/vite.config.js.timestamp-*.mjs` | Vite 临时产物 | ❌ 排除 | 本次新增忽略规则 |

## 二、安全 / 隐私检查

- ✅ 后端 `application.yml` 使用**占位符**：`password: your_pwd`、`secret: change-this-to-your-own-32-bytes-secret-please`——均为非真实密钥，公开仓库安全。
- ✅ 前端 `.env.development` 仅含非敏感配置（`VITE_APP_TITLE`、`VITE_API_BASE=/api`），无密钥，已入库。
- ✅ 未发现任何硬编码真实密码 / API Key / 私钥。
- 若你之后填入真实数据库密码或 JWT secret，请改用环境变量注入，不要写回仓库。

## 三、你在本机推送到 GitHub 的命令

```powershell
# 1) 进入项目目录（注意中文路径，用引号）
cd "D:/27543/A-软件设计模式/期末考察/tapcard"

# 2) 推送（远程已设好 origin，无需再 add remote）
git push -u origin main
```

- 若提示输入**密码**：GitHub 已不支持账号密码，请填 **Personal Access Token (PAT)**，用户名 `pikax6`。
- **先在 GitHub 建好空仓库 `tapcard`**，并**不要勾** "Add README / .gitignore / license"（否则初始 commit 会和本地冲突导致 push 被拒）。
- 推完验证：`git ls-remote origin` 能看到 `main` 即成功。

## 四、若推送报 502（网关/代理错误，非密码问题）

```powershell
curl.exe -I https://github.com        # 若也 502 → 本机网络/代理/VPN 问题
git config --global --get http.proxy  # 若有旧代理 → git config --global --unset http.proxy 后重试
git push -u origin main
```
