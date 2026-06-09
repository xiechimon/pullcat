<p style="text-align: center;">
  <img src="pullcat-web/public/cat.png" width="120" alt="pullcat logo" />
</p>

<h1 style="text-align: center;">Pullcat</h1>

<p style="text-align: center;">
  <strong>AI 驱动的代码评审助手，让每次 Pull Request Review 都高效、全面、零遗漏。</strong>
</p>

<p style="text-align: center;">
  <a href="https://xmon.me">
    <img src="https://img.shields.io/badge/🔗-xmon.me-4D6BFE?style=for-the-badge" alt="xmon.me" />
  </a>
</p>

<p style="text-align: center;">
  <img src="https://img.shields.io/badge/Java-17+-orange" alt="Java 17+">
  <img src="https://img.shields.io/badge/Spring%20Boot-3.3.5-brightgreen" alt="Spring Boot">
  <img src="https://img.shields.io/badge/React-19-blue" alt="React 19">
  <img src="https://img.shields.io/badge/license-MIT-green" alt="MIT">
</p>

---

## Demo

<p style="text-align: center;">
  <a href="【PullCat - 七牛云×XEngineer暑期实训营议题三】https://www.bilibili.com/video/BV1EoVD65ETW?vd_source=ac3f9fd11ce481368546414fcfa6ed77">
    <img src="https://img.shields.io/badge/📹-Demo%20Video-grey?style=for-the-badge" alt="Demo Video" />
  </a>
</p>

> 点击即可查看：[【PullCat - 七牛云×XEngineer暑期实训营议题三】](https://www.bilibili.com/video/BV1EoVD65ETW?vd_source=ac3f9fd11ce481368546414fcfa6ed77)
> 
> 
> 长链：https://www.bilibili.com/video/BV1EoVD65ETW?vd_source=ac3f9fd11ce481368546414fcfa6ed77

## 它是怎么工作的
![img.png](assert/img.png)


每个分析维度使用独立的 Prompt，从不同视角审视同一批代码变更。摘要和测试覆盖使用轻量模型降低成本，风险/质量/一致性使用重量模型保证深度。


---

## 功能亮点

| 功能                     | 说明                                        |
|------------------------|-------------------------------------------|
| 🔍 **五维分析**            | 变更总结、风险检测、代码质量、一致性、测试覆盖——五个独立 Prompt 并行分析 |
| 🧠 **双模型策略**           | 轻量/重量模型按需分流，平衡分析深度与成本                     |
| 📡 **实时进度**            | Server-Sent Events 流式推送，分析过程全程可见          |
| ✅ **人工审核**             | AI 生成问题列表，用户勾选确认后发布，控制误报                  |
| 📝 **Suggestion Diff** | 附带可应用的代码修复建议，发布为 GitHub suggestion block  |
| 🔧 **自定义规则**           | 仓库级别正则规则引擎，补充团队特定约定                       |
| 💡 **AI 规则建议**         | 从历史 Review 数据中自动提炼高频问题，生成规则建议             |
| 🔄 **对比审查**            | 两次 Review 结果 Diff，追踪新增/修复/遗留问题            |
| 🪝 **Webhook 自动审查**    | PR 打开或更新时自动触发分析                           |
| 🔐 **OAuth 登录**        | GitHub 登录后免配 Token，OAuth token 直接调用 API   |
| 📊 **统计分析**            | 仪表盘、历史记录、严重度分布图表                          |
| 🌓 **深色模式**            | Light / Dark 主题切换                         |
| 🩺 **运维就绪**            | Prometheus 指标、Health Check、请求限流、重试机制      |

### 原创功能部分
以下为核心原创设计，未直接依赖任何第三方库：
- 五维 Prompt 拆分 + 并行编排引擎                               
- 上下文构建器（Import 解析、依赖文件关联、Token 预算管理）        
- 跨维度去重合并算法（ResultAggregator）                          
- 自定义规则引擎（Regex + 多类型匹配）                            
- AI 规则建议（历史 Issue 聚合 + LLM 规则生成）                  
- SSE 流式进度推送 + StreamRegistry                              
- Diff 查看器（Git diff 解析 + 行级 Issue 标记 + 文件 Tab）      
- Suggestion Diff 生成与 GitHub suggestion block 发布       

---

## 技术栈与依赖

### 后端 (Spring Boot 3.3.5 / Java 17)

| 依赖                                           | 用途                               |  类型  |
|----------------------------------------------|----------------------------------|:----:|
| Spring Boot Web                              | REST API + SSE 流式推送              |  框架  |
| Spring Boot WebFlux                          | GitHub API 响应式调用 (WebClient)     |  框架  |
| Spring AI (OpenAI)                           | 对接 DeepSeek LLM，统一 ChatClient 接口 | SDK  |
| Spring Boot Data Redis                       | 审查会话存储、缓存与限流计数                   | 基础设施 |
| MyBatis-Plus                                | `Repo` / `Rule` / `User` 基础数据访问        | ORM  |
| MySQL Connector/J                           | MySQL 驱动                            | 驱动  |
| Spring Boot OAuth2 Client                    | GitHub OAuth 登录                  | 基础设施 |
| Spring Boot Actuator + Micrometer Prometheus | 健康检查、指标暴露                        |  运维  |
| Meemaw `spring-dotenv`                       | `.env` 文件加载                      |  工具  |
| Lombok                                       | 减少样板代码                           |  工具  |

### 前端 (React 19 / Vite 8 / TypeScript 6)

| 依赖                                 | 版本       | 用途                  |
|------------------------------------|----------|---------------------|
| React + ReactDOM                   | ^19      | UI 框架               |
| React Router                       | ^7       | 客户端路由               |
| Tailwind CSS                       | ^4       | 原子化 CSS，深色模式        |
| Recharts                           | ^3       | 严重度饼图、问题类型柱状图       |
| react-markdown + remark-gfm        | ^10 / ^4 | LLM 输出的 Markdown 渲染 |
| Radix UI (Dialog/Dropdown/Tooltip) | ^1-^2    | 无样式无障碍基础组件          |
| Sonner                             | ^2       | Toast 通知            |

### AI 服务

| 服务       | 模型                | 用途   |
|----------|-------------------|------|
| DeepSeek | deepseek-v4-flash | 五维分析 |

---

## 系统架构
![img_1.png](assert/img_1.png)

---

## 快速开始

### 环境要求

- **Java 17+** / **Node.js 20+** / **Redis** / **MySQL 8+** / **DeepSeek API Key**

### 1. 克隆并配置

```bash
git clone https://github.com/xiechimon/pullcat.git && cd pullcat
cp .env.example .env
# 编辑 .env，填入 DEEPSEEK_API_KEY
# GitHub OAuth 登录后无需配置 GITHUB_TOKEN
```

### 2. 启动 Redis

```bash
# macOS
brew install redis && brew services start redis
# Docker
docker run -d -p 6379:6379 redis:7-alpine
```

### 3. 初始化 MySQL

```bash
mysql -uroot -p < sql/schema.sql
```

当前 `Repo`、`Rule`、`User`、`Review` 会话和自动发布开关均使用 MySQL 持久化，Redis 主要用于缓存与限流。

### 4. 启动后端

```bash
cd pullcat-server && ./mvnw spring-boot:run
# → http://localhost:8080
```

### 5. 启动前端

```bash
cd pullcat-web && npm install && npm run dev
# → http://localhost:5173（自动代理 API 到 :8080）
```

---

## 分析维度

| 维度       |  模型   | 检查项                  |
|----------|:-----:|----------------------|
| 📝 变更总结  | Light | PR 核心改动概括，按逻辑模块组织叙述  |
| 🔴 风险检测  | Heavy | 安全漏洞、并发问题、NPE、资源泄漏   |
| 🟡 代码质量  | Heavy | 反模式、复杂度、重复代码、缺失校验    |
| 🔵 一致性分析 | Heavy | 命名风格、错误处理、架构模式、不完整重构 |
| 🟢 测试覆盖  | Light | 测试缺口、边界条件、关键路径覆盖     |

Prompt 模板位于 `pullcat-server/src/main/resources/prompts/`。

---

## 项目结构

```text
pullcat/
├── pullcat-web/                      React 19 + Vite + Tailwind CSS
│   └── src/
│       ├── pages/                    页面层
│       ├── components/               通用组件与业务组件
│       ├── hooks/                    SSE、主题、发布等状态管理
│       ├── lib/api.ts                REST API 客户端
│       └── types/review.ts           前端类型定义
├── pullcat-server/                   Spring Boot 3 + Spring AI
│   └── src/main/java/com/pullcat/
│       ├── common/                   公共协议、异常、结果对象、常量
│       ├── config/                   安全、Redis、CORS、重试、指标配置
│       ├── controller/               REST Controller 与 SSE 入口，统一使用 *Controller 命名
│       ├── dao/entity/               持久化对象
│       ├── dao/mapper/               MyBatis-Plus Mapper
│       ├── dto/req                   请求 DTO
│       ├── dto/resp                  响应 DTO
│       ├── remote/                   第三方调用接口（GitHubApiService）
│       ├── remote/dto/req/           第三方接口请求 DTO
│       ├── remote/dto/resp/          第三方接口响应 DTO
│       ├── remote/impl/              第三方调用实现
│       ├── service/                  应用服务接口，统一使用 *Service 命名
│       ├── service/impl/             应用服务实现，统一使用 *ServiceImpl 命名
│       ├── service/analysis/         编排、上下文、规则、存储访问等领域接口
│       ├── service/analysis/impl/    analysis 领域实现
│       ├── service/llm/              五维分析接口
│       ├── service/llm/impl/         五维分析实现
│       └── toolkit/                  工具类
├── AGENTS.md                         仓库协作与提交流程约束
└── .env.example                      环境变量模板
```

### 后端分层约定

- Controller 仅负责接参与返回，类名统一使用 `*Controller`
- `service` 根目录仅放应用服务接口，类名统一使用 `*Service`
- `service/impl` 放应用服务实现，类名统一使用 `*ServiceImpl`
- `service/analysis` 与 `service/llm` 优先放接口，具体实现分别收敛到各自的 `impl` 子包
- `Repo`、`Rule`、`User` 等基础数据通过 `dao/mapper` 访问，缓存逻辑直接收敛在对应 `*ServiceImpl`
- `service.analysis` 下通过领域接口承载审查会话这类聚合态存储能力，避免在 Controller 中直接操作持久化细节

---

## 配置参考

| 属性                                          | 说明                     | 默认值                 |
|---------------------------------------------|------------------------|---------------------|
| `DEEPSEEK_API_KEY`                          | DeepSeek API Key       | -                   |
| `GITHUB_CLIENT_ID` / `GITHUB_CLIENT_SECRET` | GitHub OAuth App 凭据    | -                   |
| `GITHUB_TOKEN`                              | GitHub PAT（可选，登录后无需配置） | 空                   |
| `REDIS_HOST` / `REDIS_PORT`                 | Redis 连接               | `localhost:6379`    |
| `SPRING_DATASOURCE_URL`                     | MySQL JDBC 连接串         | -                   |
| `SPRING_DATASOURCE_USERNAME`                | MySQL 用户名              | -                   |
| `SPRING_DATASOURCE_PASSWORD`                | MySQL 密码               | -                   |
| `pullcat.llm.light-model`                   | 轻量模型                   | `deepseek-v4-flash` |
| `pullcat.llm.heavy-model`                   | 重量模型                   | `deepseek-v4-flash` |

---

## 许可证

MIT
