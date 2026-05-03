/** 后端 context-path 为 /api 时的完整基底 URL（勿结尾斜杠） */
export const API_BASE = (
  (import.meta.env.VITE_API_BASE_URL as string | undefined)?.replace(/\/$/, '') ?? 'http://localhost:8123/api'
)

/** 启用 app.security 时，SSE 可带 query apiKey（EventSource 无法自定义 Header） */
export const OPTIONAL_API_KEY = (import.meta.env.VITE_API_KEY as string | undefined)?.trim() ?? ''
