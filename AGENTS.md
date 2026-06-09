<!-- superpowers-zh:begin (do not edit between these markers) -->
# Superpowers-ZH 中文增强版

本项目已安装 superpowers-zh 技能框架（20 个 skills）。

## 核心规则

1. **收到任务时，先检查是否有匹配的 skill** — 哪怕只有 1% 的可能性也要检查
2. **设计先于编码** — 收到功能需求时，先用 brainstorming skill 做需求分析
3. **测试先于实现** — 写代码前先写测试（TDD）
4. **验证先于完成** — 声称完成前必须运行验证命令

## 可用 Skills

Skills 位于 `.claude/skills/` 目录，每个 skill 有独立的 `SKILL.md` 文件。

- **brainstorming**: 在任何创造性工作之前必须使用此技能——创建功能、构建组件、添加功能或修改行为。在实现之前先探索用户意图、需求和设计。
- **chinese-code-review**: 中文 review 沟通参考——话术模板、分级标注（必须修复/建议修改/仅供参考）、国内团队常见反模式应对。仅在用户显式 /chinese-code-review 时调用，不要根据上下文自动触发。
- **chinese-commit-conventions**: 中文 commit 与 changelog 配置参考——Conventional Commits 中文适配、commitlint/husky/commitizen 中文模板、conventional-changelog 中文配置。仅在用户显式 /chinese-commit-conventions 时调用，不要根据上下文自动触发。
- **chinese-documentation**: 中文文档排版参考——中英文空格、全半角标点、术语保留、链接格式、中文文案排版指北约定。仅在用户显式 /chinese-documentation 时调用，不要根据上下文自动触发。
- **chinese-git-workflow**: 国内 Git 平台配置参考——Gitee、Coding.net、极狐 GitLab、CNB 的 SSH/HTTPS/凭据/CI 接入差异与镜像同步配置。仅在用户显式 /chinese-git-workflow 时调用，不要根据上下文自动触发。
- **dispatching-parallel-agents**: 当面对 2 个以上可以独立进行、无共享状态或顺序依赖的任务时使用
- **executing-plans**: 当你有一份书面实现计划需要在单独的会话中执行，并设有审查检查点时使用
- **finishing-a-development-branch**: 当实现完成、所有测试通过、需要决定如何集成工作时使用——通过提供合并、PR 或清理等结构化选项来引导开发工作的收尾
- **mcp-builder**: MCP 服务器构建方法论 — 系统化构建生产级 MCP 工具，让 AI 助手连接外部能力
- **receiving-code-review**: 收到代码审查反馈后、实施建议之前使用，尤其当反馈不明确或技术上有疑问时——需要技术严谨性和验证，而非敷衍附和或盲目执行
- **requesting-code-review**: 完成任务、实现重要功能或合并前使用，用于验证工作成果是否符合要求
- **subagent-driven-development**: 当在当前会话中执行包含独立任务的实现计划时使用
- **systematic-debugging**: 遇到任何 bug、测试失败或异常行为时使用，在提出修复方案之前执行
- **test-driven-development**: 在实现任何功能或修复 bug 时使用，在编写实现代码之前
- **using-git-worktrees**: 当需要开始与当前工作区隔离的功能开发，或在执行实现计划之前使用——通过原生工具或 git worktree 回退机制确保隔离工作区存在
- **using-superpowers**: 在开始任何对话时使用——确立如何查找和使用技能，要求在任何响应（包括澄清性问题）之前调用 Skill 工具
- **verification-before-completion**: 在宣称工作完成、已修复或测试通过之前使用，在提交或创建 PR 之前——必须运行验证命令并确认输出后才能声称成功；始终用证据支撑断言
- **workflow-runner**: 在 Claude Code / OpenClaw / Cursor 中直接运行 agency-orchestrator YAML 工作流——无需 API key，使用当前会话的 LLM 作为执行引擎。当用户提供 .yaml 工作流文件或要求多角色协作完成任务时触发。
- **writing-plans**: 当你有规格说明或需求用于多步骤任务时使用，在动手写代码之前
- **writing-skills**: 当创建新技能、编辑现有技能或在部署前验证技能是否有效时使用

## 如何使用

当任务匹配某个 skill 时，使用 `Skill` 工具加载对应 skill 并严格遵循其流程。绝不要用 Read 工具读取 SKILL.md 文件。

如果你认为哪怕只有 1% 的可能性某个 skill 适用于你正在做的事情，你必须调用该 skill 检查。
<!-- superpowers-zh:end -->

## 项目概述

Pullcat 是一个 AI 驱动的 Pull Request 评审工具。

项目分为两个主要部分：

- `pullcat-server`：Spring Boot 后端，负责 PR 分析编排、GitHub API 调用、规则引擎、审查结果存储与统计
- `pullcat-web`：React 前端，负责审查流程展示、问题确认发布、历史记录与统计面板

核心能力包括：

- 多维度 PR 分析
- 审查结果聚合与发布
- 仓库规则管理与统计分析

## 分支规范

- 分支命名建议：
  - `feat/<功能简述>`：新功能
  - `fix/<问题简述>`：Bug 修复
  - `refactor/<重构内容>`：代码重构
  - `docs/<文档内容>`：文档更新
- 开发完成后，优先通过 PR 的方式合入 `main`

## 开发工作流

当前仓库未启用 `openspec/` 目录，默认按下面流程执行：

1. **从 main 开始工作**：同步最新代码，再按需创建开发分支
2. **先更新设计与规范认知**：实现前先检查 `AGENTS.md`、`README.md` 与相关模块代码
3. **实现变更**：优先保持小步提交，避免一次混入多个独立主题
4. **验证通过**：至少运行与改动范围匹配的构建、测试或 lint 命令
5. **同步文档**：若包结构、命名规范或使用方式变更，必须同步更新 Markdown 文档
6. **集成回 main**：优先通过 PR 合入，也允许在确认后直接合并到 `main`

## PR 规范

### 基本原则

- **每个 PR 只做一件事**：每个 PR 只实现或修改单一功能，鼓励尽可能小、粒度尽可能细的 PR。大功能应拆分为多个独立 PR 分步提交
- **PR 合并后主分支代码需保持可运行状态**：评委在任意时间查看应能复现演示效果

### PR 内容要求

每个 PR 的标题与描述需清晰完整，内容包含：

- **功能描述**：说明该功能的作用与使用方式
- **实现思路**：简要说明技术选型或核心实现逻辑
- **标题**：一句话说明本 PR 新增或修改了什么
- **测试方式**：如何验证该功能正常运行

## 后端包结构

当前后端主包为 `com.pullcat`：

```text
com.pullcat
├── common
│   ├── biz.user                 # 用户上下文透传预留目录
│   ├── constant                 # 公共常量（如 RedisKeys）
│   ├── convention
│   │   ├── errorcode            # 错误码协议
│   │   ├── exception            # 业务异常体系
│   │   └── result               # 统一响应对象与工厂
│   ├── database                 # 数据库公共抽象预留目录
│   ├── enums                    # 通用业务枚举
│   ├── serialize                # 自定义序列化预留目录
│   └── web                      # 全局异常处理
├── config                       # Spring 配置（安全、Redis、CORS、指标、AI）
├── controller                   # REST 控制层
├── dao
│   ├── entity                   # 持久化对象 / Redis 存储对象
│   └── mapper                   # Mapper 预留目录
├── dto
│   ├── req                      # 请求 DTO
│   └── resp                     # 响应 DTO
├── remote
│   ├── config                   # 远程调用配置预留目录
│   └── dto                      # 第三方接口 DTO
├── service
│   ├── analysis                 # 评审编排、仓库/会话存储、规则等领域接口
│   ├── analysis/impl            # analysis 领域实现
│   ├── github                   # 迁移预留目录
│   ├── impl                     # 应用服务实现
│   ├── llm                      # 多维分析接口
│   └── llm/impl                 # 多维分析实现
└── toolkit                      # 工具类
```

说明：

- `model` 目录正在退出使用，新代码不要再放入 `com.pullcat.model`
- Controller 统一使用 `*Controller` 后缀
- `service` 根目录优先只保留接口，类名统一使用 `*Service` 后缀
- `service.impl` 下实现类统一使用 `*ServiceImpl` 后缀
- `service.analysis`、`service.llm` 优先定义接口，具体实现统一收敛到各自的 `impl` 子包
- `service.github`、`dao.mapper`、`common.database`、`common.serialize` 当前主要作为扩展位，新增代码前先确认是否真的需要落在这些目录
- 远程调用相关代码统一收敛到 `remote`，不要继续在 `service` 下新增第三方 API 客户端
- `dao.entity` 下实体统一使用 `*DO` 后缀
- `dto.req` 下请求对象统一使用 `*ReqDTO` 后缀
- `dto.resp` 下响应对象统一使用 `*RespDTO` 后缀

## 编码规范

### 统一响应

所有 Controller 返回值统一使用 `Result<T>`，通过 `Results` 工厂方法构造。

### 实体类

当前 `dao.entity` 下对象主要用于 Redis 持久化和服务内部存储，例如 `RepoDO`、`UserDO`。

约束：

- 放在 `dao.entity` 的类应只承载数据，不写业务流程
- 类名统一使用 `*DO` 后缀
- 优先使用 Lombok 消除样板代码，但字段语义不清时要补 Javadoc
- 时间字段当前统一使用 `Instant`，新增字段保持一致，除非接入层协议明确要求别的类型
- 若对象同时承担 API 出参职责，应优先拆到 `dto.resp`，不要让 Controller 直接暴露复杂持久化结构

### 请求与响应 DTO

- 请求对象放在 `dto.req`，响应对象放在 `dto.resp`
- 请求对象类名统一使用 `*ReqDTO` 后缀
- 响应对象类名统一使用 `*RespDTO` 后缀
- Controller 的 `@RequestBody` 不直接使用 `Map`，也不直接复用 `DO` 作为入参
- Controller 不要长期使用 `Map<String, Object>` 承接复杂请求；超过两个明确字段的请求，优先定义 DTO
- DTO 字段命名与接口 JSON 字段保持一致，避免额外转换层

### 前端 API 类型命名

- 前端与后端接口一一对应的传输对象，统一放在 `pullcat-web/src/types/review.ts` 等类型文件中
- 这类 API 传输对象命名统一使用 `*RespDTO`
- 不再为同一份后端响应同时维护 `Repo` / `Rule` / `StatusResponse` 这类前端别名
- 如果某个类型已经不是接口传输对象，而是前端独立的展示或交互模型，才可以使用不带 `RespDTO` 的领域命名
- 若后续需要明确拆分“接口传输类型”和“前端领域类型”，优先新增独立类型文件，不要继续在同一文件中混用两套命名

### 异常体系

当前业务异常分三类：

- `ClientException`：参数错误、权限不足、资源不存在、业务前置校验失败
- `ServiceException`：服务内部执行失败
- `RemoteException`：第三方服务或下游调用失败

约束：

- 用户可感知且可修正的问题，使用 `ClientException`
- 外部依赖调用失败，优先抛 `RemoteException`
- 未分类内部故障使用 `ServiceException`，并保留原始异常链

### 错误码

错误码协议定义在 `common.convention.errorcode.IErrorCode`，通用错误码在 `CommonErrorCodeEnum`。

前缀约定：

- `A` 开头：客户端错误
- `B` 开头：服务端错误
- `C` 开头：远程调用错误

建议：

- 新增模块级错误码时，按领域拆分独立枚举，不要把所有错误都塞进 `CommonErrorCodeEnum`
- 枚举必须实现 `IErrorCode`，同时提供稳定的 `code` 和可读的 `message`

### 参数校验

- DTO 字段使用 `@NotBlank`、`@NotNull`、`@Size` 等注解表达约束
- `MethodArgumentNotValidException` 统一由 `GlobalExceptionHandler` 处理，不要重复 try/catch
- 简单接口若暂时使用 `Map` 接收参数，至少在 Controller 首层完成空值与格式校验

### 控制层

Controller 只负责四件事：

- 接收参数
- 调用 Service / Repository
- 做最薄的一层权限与存在性校验
- 返回 `Result<T>`

约束：

- 不在 Controller 中编排复杂业务流程
- 不在 Controller 中拼装跨多个领域的核心逻辑
- SSE 接口的流事件命名保持稳定，避免随意修改前端依赖的事件名

### Service、Mapper 与缓存职责

当前后端按“应用服务 + Mapper + 缓存”划分职责，例如：

- `AnalysisOrchestrator`
- `RepoService`
- `ReviewSessionService`
- `RuleService`
- `StatsService`

约束：

- 面向 Controller 暴露的应用服务统一通过接口注入，不直接依赖 `*ServiceImpl`
- `service.impl` 只承载应用服务实现，不再把接口与实现混放
- 编排逻辑放 `AnalysisOrchestrator` 这类 Service 中
- `Rule`、`Repo`、`User` 这类基础数据访问统一通过 `dao.mapper` 完成，不再额外引入 `Repository`
- Redis 作为缓存层时，缓存读写逻辑直接收敛在对应 `*ServiceImpl` 中，不要在多个 Controller 中直接操作 Redis
- `Review` 这类会话态或聚合态存储，允许按实际复杂度保留独立持久化组件
- 涉及 LLM 的具体分析实现放 `service.llm`，不要把 Prompt 调用直接散落到 Controller 或通用 Service

### Redis Key 命名

Redis Key 常量统一放在 `common.constant.RedisKeys`。

约束：

- 使用 `:` 分隔命名空间层级
- Key 构造统一通过 `RedisKeys` 中的方法或常量完成，不要在业务代码里手写字符串
- TTL 也应与 Key 常量一起集中管理，避免魔法值散落

### 远程调用

GitHub 相关 HTTP 调用统一通过 `remote.GitHubApiService` 接口处理，具体实现收敛在 `remote.impl`。

约束：

- 第三方 API 访问代码集中在 `remote` 层
- `remote` 根目录优先定义接口，具体实现放到 `remote.impl`
- 认证、重试、限流、错误翻译优先在远程调用层封装
- 不要在业务层直接 new `WebClient` 或拼 GitHub API URL

### API 路径规范

当前后端 API 以 `/api` 为统一前缀，例如：

```text
/api/repos
/api/reviews
/api/reviews/{id}/sse
/api/stats/overview
/api/user
```

约束：

- 新接口继续挂在 `/api` 前缀下
- 资源命名优先使用复数名词
- 子资源路径保持语义稳定，例如 `/{reviewId}/issues/{issueId}/feedback`

### 注释约束

允许给字段补充简短 Javadoc，包括基础包装对象。

但中文短句注释末尾不要加句号。

禁止这样写：

```java
/**
 * 是否成功。
 */
```

应该这样写：

```java
/**
 * 是否成功
 */
```

适用于字段注释、方法摘要注释以及类似的简短中文说明。

## 文档维护约定

- 新增公共约束时，先改代码，再同步更新本文档
- 若后端包结构调整，请同时更新根目录 `README.md` 的项目结构说明
- 若规范与现有代码不一致，以当前已合并代码为准，文档需要尽快修正，不能长期漂移
