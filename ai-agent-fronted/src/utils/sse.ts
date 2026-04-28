export type SsePostOptions = {
  signal?: AbortSignal
  onText?: (text: string) => void
  onErrorText?: (text: string) => void
}

function safeJsonParse<T>(s: string): T | null {
  try {
    return JSON.parse(s) as T
  } catch {
    return null
  }
}

function parseSseEvents(buffer: string) {
  const parts = buffer.split(/\n\n+/)
  const remaining = buffer.endsWith('\n\n') ? '' : parts.pop() ?? ''
  const events = parts
    .map((raw) => raw.trim())
    .filter(Boolean)
    .map((raw) => {
      const lines = raw.split('\n')
      const dataLines = lines
        .filter((l) => l.startsWith('data:'))
        .map((l) => l.slice(5).trimStart())
      return dataLines.join('\n')
    })
    .filter(Boolean)
  return { events, remaining }
}

/**
 * 原生 fetch + ReadableStream 解析 SSE（支持 POST）。
 * 兼容两种后端输出：
 * - 标准 SSE: data: xxx\n\n
 * - 纯文本 chunk（直接把 chunk 当作增量文本）
 */
export async function postSseText(url: string, body: unknown, opts: SsePostOptions = {}) {
  const res = await fetch(url, {
    method: 'POST',
    headers: {
      Accept: 'text/event-stream',
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(body),
    signal: opts.signal,
  })

  if (!res.ok || !res.body) {
    const t = await res.text().catch(() => '')
    throw new Error(t || `请求失败：${res.status}`)
  }

  const reader = res.body.getReader()
  const decoder = new TextDecoder('utf-8')
  let buf = ''
  let sawSse = false

  while (true) {
    const { value, done } = await reader.read()
    if (done) break
    const chunk = decoder.decode(value, { stream: true })
    buf += chunk

    if (buf.includes('data:')) sawSse = true

    if (sawSse) {
      const parsed = parseSseEvents(buf)
      buf = parsed.remaining
      for (const ev of parsed.events) {
        // 有些实现会发 JSON（可选兼容），默认当纯文本拼接
        const maybe = safeJsonParse<{ data?: string; content?: string }>(ev)
        const text = maybe?.data ?? maybe?.content ?? ev
        opts.onText?.(text)
      }
    } else {
      // 非标准 SSE：直接把 chunk 当增量文本
      opts.onText?.(chunk)
      buf = ''
    }
  }
}

export type EventSourceSseOptions = {
  onText?: (text: string) => void
  onError?: (err: unknown) => void
  /**
   * 关闭连接时触发（正常结束/手动停止/异常都会走到这里）
   */
  onClose?: () => void
}

/**
 * 原生 EventSource（GET）解析 SSE。
 * 说明：EventSource 只能 GET，所以用于后端 `SseEmitter` 这类接口。
 */
export function openEventSourceText(url: string, opts: EventSourceSseOptions = {}) {
  const es = new EventSource(url)
  es.onmessage = (ev) => {
    opts.onText?.(ev.data ?? '')
  }
  es.onerror = (ev) => {
    opts.onError?.(ev)
    // onerror 可能是临时断开重连，也可能是服务端主动关闭；
    // 这里交给调用方决定是否 close（默认直接 close，避免无限重连）
  }
  return {
    close() {
      try {
        es.close()
      } finally {
        opts.onClose?.()
      }
    },
    raw: es,
  }
}

