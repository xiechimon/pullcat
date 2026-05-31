# pullcat

AI 驱动的 Pull Request Review 助手。输入 GitHub PR 链接，自动获取代码变更并进行多维度 AI 分析，审核后发布到 PR。

## 工作流程

```
1. 输入 PR 链接                2. AI 五维分析                  3. 审核并发布
   ┌──────────────────┐         ┌─────────────────────┐        ┌──────────────┐
   │ https://github.  │         │ ✓ 变更总结           │        │ 人工审核     │
   │ com/owner/repo/  │  ────▶  │ ✓ 风险检测           │ ────▶ │ 勾选问题     │
   │ pull/123         │         │ ✓ 代码质量           │        │ → 发布到 PR  │
   └──────────────────┘         │ ✓ 一致性分析         │        └──────────────┘
                                │ ✓ 测试覆盖           │
                                └─────────────────────┘
```

每个分析维度使用独立的 Prompt 模板，从不同角度审视代码变更，避免单一维度遗漏问题。

## 环境要求

- **Java 17+**
- **Node.js 20+**（前端开发服务器）
- **Redis**（默认 localhost:6379，可通过 `REDIS_HOST`/`REDIS_PORT` 配置）
- **GitHub OAuth App**（登录后免配置 Token，[创建 OAuth App](https://github.com/settings/developers)）
  - Homepage URL: `http://localhost:5173`
  - Authorization callback URL: `http://localhost:5173/login/oauth2/code/github`
- **GitHub Personal Access Token**（可选，无浏览器/webhook 场景使用，需要 `repo` 权限，[在此创建](https://github.com/settings/tokens)）
- **DeepSeek API Key**（[在此获取](https://platform.deepseek.com/api_keys)）

## 快速开始

### 1. 克隆并配置

```bash
git clone <仓库地址>
cd pullcat

# 复制环境变量模板并填入真实密钥
cp .env.example .env
# 编辑 .env 文件，将 DEEPSEEK_API_KEY 和 GITHUB_TOKEN 替换为真实值
```

### 2. 启动 Redis

```bash
# macOS (Homebrew)：
brew install redis && brew services start redis

# Docker：
docker run -d -p 6379:6379 redis:7-alpine
```

### 3. 启动后端

```bash
cd pullcat-server
./mvnw spring-boot:run
```

API 服务启动在 http://localhost:8080。

### 4. 启动前端

```bash
cd pullcat-web
npm install
npm run dev
```

开发页面启动在 http://localhost:5173（自动代理 API 请求到 :8080）。

## 使用说明

1. 打开 http://localhost:5173
2. 粘贴 GitHub PR 链接（例如 `https://github.com/spring-projects/spring-boot/pull/12345`）
3. 点击 **开始审查**
4. 实时查看五个分析维度的进度
5. 逐节审阅发现的问题，取消勾选误报项
6. 点击 **发布到 PR** 将结果发布为 GitHub PR Review

## 配置说明

所有配置在 `pullcat-server/src/main/resources/application.yml`：

| 属性                                    | 说明                                          | 默认值                        |
|---------------------------------------|---------------------------------------------|----------------------------|
| `pullcat.github.token`                | GitHub PAT（或 `GITHUB_TOKEN` 环境变量）           | -                          |
| `spring.ai.openai.api-key`            | DeepSeek API Key（或 `DEEPSEEK_API_KEY` 环境变量） | -                          |
| `spring.ai.openai.base-url`           | DeepSeek API 地址                             | `https://api.deepseek.com` |
| `spring.ai.openai.chat.options.model` | 使用的模型名称                                     | `deepseek-chat`            |
| `pullcat.review.session-ttl-days`     | 审查结果保留天数                                    | `7`                        |
| `spring.data.redis.host`              | Redis 地址                                    | `localhost`                |
| `spring.data.redis.port`              | Redis 端口                                    | `6379`                     |

## 项目结构

```
pullcat-web/          React + Vite + TypeScript + Tailwind CSS（端口 5173）
pullcat-server/       Spring Boot 3 + Spring AI（端口 8080）
  ├── controller/     REST API + SSE 流式推送
  ├── service/
  │   ├── github/     GitHub API 集成（WebClient）
  │   ├── analysis/   上下文构建、任务编排、结果汇总
  │   └── llm/        分析任务实现、JSON 解析
  ├── model/          领域对象（ReviewSession、Issue 等）
  └── resources/
      └── prompts/    Prompt 模板（summary.md、risk.md 等）
```

## 分析维度说明

| 维度    | 说明                         |
|-------|----------------------------|
| 变更总结  | 概括 PR 的核心改动，按逻辑模块组织        |
| 风险检测  | 安全漏洞、并发问题、健壮性风险（NPE、资源泄漏等） |
| 代码质量  | 反模式、过度复杂、重复代码、缺失校验         |
| 一致性分析 | 命名风格、错误处理模式、不完整重构          |
| 测试覆盖  | 变更代码的测试覆盖情况、边界条件缺失         |

## 许可证

MIT
