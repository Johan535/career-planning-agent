# 前端项目（ai-agent-fronted）— 企业级AI智能职业规划智能体

本目录为项目的 Vue3 前端部分，提供霓虹未来科技风 UI、SSE 流式聊天室与模式切换页。

## 技术栈

- Vue 3 + TypeScript
- Vite
- Vue Router
- Axios（统一 `baseURL: http://localhost:8123/api`）
- 原生 SSE
  - POST SSE：`fetch + ReadableStream`（职业规划师聊天室）
  - GET SSE：`EventSource`（智能体聊天室）

## 启动方式（Windows）

```powershell
cd D:\code\career-planning-agent\ai-agent-fronted
npm install
npm run dev -- --host
```

## 页面路由

- `/`：炫酷模式切换首页
- `/career-chat`：AI 智能职业规划师（POST SSE 流式）
- `/manus-chat`：AI 智能体（GET SSE / EventSource）

## 说明

更完整的项目介绍、接口说明与演示流程请查看仓库根目录的 `README.md`。
