# 企业级AI智能职业规划智能体（Career Planning Agent）

一个面向企业展示的 **AI 职业规划智能体系统**，提供「职业规划师对话」与「智能体（ReAct）执行」两种模式，支持 **多轮对话记忆、SSE 流式输出、RAG 知识库检索增强、工具调用与 ReAct 推理链**。前端采用 **Vue3 + Vite + Axios + 原生 SSE（fetch/EventSource）**，视觉风格为霓虹渐变未来科技风。

---

## 项目介绍

本项目围绕“职业规划”场景，构建一套可落地的 AI 智能体应用：

- 面向用户：提供清晰、可执行的职业规划建议（拒绝空话套话）
- 面向系统：提供多种对话能力组合（记忆、RAG、工具、ReAct）
- 面向体验：前端全站霓虹动态渐变 + 粒子背景 + 炫酷聊天室，支持 SSE 实时流式渲染

---

## 核心功能

### 多轮对话（含会话ID）

- 每次对话都带 `chatId`（会话ID），支持持续多轮交流
- 前端进入聊天室页面会自动生成 `chatId`，并展示在页面顶部

### 持久化记忆（Memory）

- 服务端通过 `chatId` 将历史对话写入本地文件（`chat-memory/<chatId>.log`）
- 再次请求时自动拼接最近历史上下文（默认最近 20 行），用于提升连续对话一致性

### RAG（检索增强生成）

- 基于向量库检索相关知识片段，提高回答准确性与一致性
- 支持默认向量存储与可选 PgVector 向量存储（按配置启用）
- 支持查询重写（Query Rewrite），优化检索命中率

### ReAct（推理 + 工具调用）

- 支持智能体按步骤思考与调用工具（Tool Callbacks）
- 提供智能体相关接口，支持流式输出与过程追踪（如 ReAct trace）

---

## 技术栈说明

### 前端（ai-agent-fronted）

- Vue 3 + TypeScript
- Vite
- Vue Router
- Axios（已配置 `baseURL: http://localhost:8123/api`）
- 原生 SSE：
  - POST SSE：`fetch + ReadableStream`（用于 `/career_app/chat/sse`）
  - GET SSE：`EventSource`（用于 `/manus/chat`）

### 后端（Spring Boot）

- Java 21
- Spring Boot 3.4.4
- Spring Web + Validation
- Spring AI（DashScope）
- 向量库：
  - 内存向量库（默认）
  - PgVector（可选）
- Knife4j（接口文档）

---

## 项目结构

### 根目录概览

- `ai-agent-fronted/`：Vue3 前端项目
- `src/main/java/...`：Spring Boot 后端源码
- `chat-memory/`：多轮对话持久化日志（按 `chatId` 分文件）
- `knowledge-base/`：知识库文件（用于 RAG/索引）

### 前端目录（ai-agent-fronted）

- `src/components/`：
  - `GlobalFx.vue`：全站统一动态渐变 + 粒子流动背景
  - `NeonActionButton.vue`：霓虹 3D 悬浮主按钮组件
- `src/views/`：
  - `HomeView.vue`：首页模式选择（炫酷切换页）
  - `CareerChatView.vue`：AI 职业规划师聊天室（POST SSE）
  - `ManusChatView.vue`：AI 智能体聊天室（GET SSE / EventSource）
- `src/utils/`：
  - `sse.ts`：SSE 工具封装（POST 流式解析 + EventSource）
  - `id.ts`：会话ID生成（≤64）
- `src/api/`：
  - `http.ts`：Axios 实例（统一 baseURL）

### 后端关键类

- `controller/AiController.java`：AI 相关接口入口（SSE、RAG、工具、会话管理、知识库管理等）
- `app/CareerPlanningAgentApp.java`：核心对话能力实现（记忆、流式、RAG、工具）
- `service/PersistentMemoryService.java`：基于文件的会话记忆持久化
- `service/ConversationService.java`：chatId 生成与会话管理封装

---

## 启动运行步骤（Windows）

### 1) 启动后端（Spring Boot）

在项目根目录 `D:\\code\\career-planning-agent`：

```powershell
.\mvnw.cmd spring-boot:run
```

默认接口基址：

- `http://localhost:8123/api`

说明：

- 项目使用 DashScope 相关能力时需要配置 API Key；若未配置可能出现 401（但不影响前端工程构建）。

### 2) 启动前端（Vue3 + Vite）

进入前端目录：

```powershell
cd D:\code\career-planning-agent\ai-agent-fronted
npm install
npm run dev -- --host
```

浏览器访问（以终端输出端口为准）：

- `http://localhost:5173/`（若端口被占用会自动切换到 5174/5175…）

---

## 接口说明（BaseURL：`http://localhost:8123/api`）

以下接口在 `AiController` 中定义（统一前缀：`/ai`）。

### 职业规划师对话

- **同步对话**
  - `POST /ai/career_app/chat/sync`
  - Body：
    - `message`：用户输入（必填）
    - `chatId`：会话ID（可选，不传则后端生成）

- **SSE 流式对话（前端页面1使用）**
  - `POST /ai/career_app/chat/sse`
  - `Content-Type: application/json`
  - `Accept: text/event-stream`
  - Body：
    - `message`：用户输入（必填）
    - `chatId`：会话ID（可选；前端会自动生成并传入）

### RAG / 工具 / ReAct

- **RAG 对话**
  - `POST /ai/career_app/chat/rag`
  - Body：同 `ChatRequest`

- **工具调用对话**
  - `POST /ai/career_app/chat/tool`
  - Body：同 `ChatRequest`

- **ReAct 过程追踪**
  - `POST /ai/react/process`
  - Body：同 `ChatRequest`

### AI 智能体（Manus）

- **SSE 流式智能体（前端页面2使用）**
  - `GET /ai/manus/chat?message=...`
  - 返回：SSE（`EventSource` 可直接消费）

### 会话管理

- `GET /ai/conversations`：列出所有会话ID
- `GET /ai/conversations/{chatId}/history`：读取该会话历史
- `DELETE /ai/conversations/{chatId}`：清空该会话历史

### 知识库管理

- `POST /ai/knowledge/upload`：上传知识库文件（multipart）
- `GET /ai/knowledge/files`：列出知识库文件
- `POST /ai/knowledge/reindex`：重建索引

---

## 使用演示说明（推荐用于面试时演示）

### 1) 首页模式选择

打开前端首页 `/`，点击进入：

- **AI职业规划师**：`/career-chat`（专业规划对话 + SSE 流式输出）
- **AI智能体**：`/manus-chat`（智能体模式 + SSE 流式输出）

### 2) 职业规划师（/career-chat）

演示建议话术（可直接复制）：

- “我想在 3 个月内从测试转前端，现有基础：HTML/CSS、会一点 JS。每天 2 小时。请给出周计划+项目路线。”
- “我目前是 Java 后端 2 年，想往 AI 应用开发走。请给技能树、作品集方向、面试准备清单。”

亮点展示：

- 页面进入自动生成会话ID（支持连续多轮）
- SSE 逐字/逐段流式输出（炫酷打字机效果）
- 自动滚动与发送状态动画

### 3) 智能体（/manus-chat）

演示建议话术：

- “把我当作应届生：请你以智能体方式，给我 14 天求职冲刺计划（每天任务+产出物）。”
- “请拆解：从 0 到 1 做一个职业规划产品，需要哪些模块、里程碑、验收标准？”

亮点展示：

- GET SSE（EventSource）流式输出
- 与页面1同款视觉与动效体验（统一设计语言）

---

## 许可与声明

- 本项目用于学习、竞赛与项目展示场景。若接入第三方大模型服务，请遵守对应平台的使用条款与合规要求。

