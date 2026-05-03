export type TypewriterController = {
  push: (text: string) => void
  stop: () => void
  /** 取出队列中尚未展示的文字（不触发逐字回调），用于切换步骤气泡时收尾 */
  flush: () => string
}

export function createSseGetUrl(url: string, params: Record<string, string | number | null | undefined>) {
  const parsed = new URL(url)
  Object.entries(params).forEach(([key, value]) => {
    if (value !== null && value !== undefined && `${value}`.length > 0) {
      parsed.searchParams.set(key, `${value}`)
    }
  })
  return parsed.toString()
}

export function createTypewriter(onText: (text: string) => void, delay = 22, onIdle?: () => void): TypewriterController {
  const queue: string[] = []
  let timer: number | null = null
  let stopped = false

  const tick = () => {
    if (stopped) return
    const next = queue.shift()
    if (next !== undefined) onText(next)

    if (queue.length > 0) {
      timer = window.setTimeout(tick, delay)
    } else {
      timer = null
      onIdle?.()
    }
  }

  const start = () => {
    if (!stopped && timer === null && queue.length > 0) {
      timer = window.setTimeout(tick, delay)
    }
  }

  return {
    push(text: string) {
      if (!text || stopped) return
      const normalized = text.replace(/\\n/g, '\n').replace(/<br\s*\/?>/gi, '\n')
      queue.push(...Array.from(normalized))
      start()
    },
    flush(): string {
      if (timer !== null) {
        window.clearTimeout(timer)
        timer = null
      }
      const s = queue.join('')
      queue.length = 0
      return s
    },
    stop() {
      stopped = true
      queue.length = 0
      if (timer !== null) {
        window.clearTimeout(timer)
        timer = null
      }
    },
  }
}

export type EventSourceSseOptions = {
  onText?: (text: string) => void
  onDone?: () => void
  onError?: (err: unknown) => void
  onClose?: () => void
}

export function openEventSourceText(url: string, opts: EventSourceSseOptions = {}) {
  const es = new EventSource(url)
  let closed = false

  const close = () => {
    if (closed) return
    closed = true
    es.close()
    opts.onClose?.()
  }

  es.onmessage = (ev) => {
    const data = ev.data ?? ''
    if (data === '[DONE]') {
      opts.onDone?.()
      close()
      return
    }
    opts.onText?.(data)
  }

  es.addEventListener('done', () => {
    opts.onDone?.()
    close()
  })

  // EventSource 在连接正常结束时常触发 error + CLOSED，不宜一律当作业务错误
  es.onerror = (ev) => {
    if (closed) return
    if (es.readyState === EventSource.CONNECTING) {
      opts.onError?.(ev)
    }
    close()
  }

  return {
    close,
    raw: es,
  }
}

export type ManusArtifact = {
  fileId: string
  filename: string
}

export type ManusChatSseHandlers = {
  /** 每步正文（默认 SSE message） */
  onStep?: (text: string) => void
  onArtifact?: (item: ManusArtifact) => void
  onError?: (err: unknown) => void
  onClose?: () => void
}

/**
 * Manus：默认 message 为步骤文本；自定义事件 artifact 为可下载文件（JSON）。
 */
export function openManusChatSse(url: string, handlers: ManusChatSseHandlers = {}) {
  const es = new EventSource(url)
  let closed = false

  const close = () => {
    if (closed) return
    closed = true
    es.close()
    handlers.onClose?.()
  }

  es.onmessage = (ev) => {
    handlers.onStep?.(ev.data ?? '')
  }

  es.addEventListener('artifact', (ev) => {
    try {
      const raw = (ev as MessageEvent).data ?? '{}'
      const o = JSON.parse(raw) as Record<string, string>
      handlers.onArtifact?.({
        fileId: o.fileId ?? '',
        filename: o.filename ?? 'report.pdf',
      })
    } catch {
      /* ignore */
    }
  })

  es.onerror = (ev) => {
    if (closed) return
    if (es.readyState === EventSource.CONNECTING) {
      handlers.onError?.(ev)
    }
    close()
  }

  return { close, raw: es }
}

export type DownloadGeneratedFileParams = {
  baseUrl: string
  fileId: string
  chatId: string
  apiKey?: string
}

/** 下载工具注册的可生成文件（PDF、Markdown、文本等） */
export async function downloadGeneratedFileBlob(params: DownloadGeneratedFileParams): Promise<Blob> {
  const u = new URL(`${params.baseUrl.replace(/\/$/, '')}/ai/files/download`)
  u.searchParams.set('fileId', params.fileId)
  u.searchParams.set('chatId', params.chatId)
  if (params.apiKey && params.apiKey.length > 0) {
    u.searchParams.set('apiKey', params.apiKey)
  }
  const res = await fetch(u.toString(), { method: 'GET' })
  if (!res.ok) {
    let detail = `${res.status}`
    try {
      const j = (await res.json()) as { message?: string }
      if (j?.message) detail = j.message
    } catch {
      try {
        const t = await res.text()
        if (t) detail = t.slice(0, 200)
      } catch {
        /* ignore */
      }
    }
    throw new Error(detail)
  }
  return res.blob()
}

/** @deprecated 使用 {@link downloadGeneratedFileBlob} */
export type DownloadPdfParams = DownloadGeneratedFileParams

/** @deprecated 使用 {@link downloadGeneratedFileBlob} */
export async function downloadPdfBlob(params: DownloadPdfParams): Promise<Blob> {
  return downloadGeneratedFileBlob(params)
}

export function triggerBlobDownload(blob: Blob, filename: string) {
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = filename || 'download.pdf'
  a.rel = 'noopener'
  document.body.appendChild(a)
  a.click()
  a.remove()
  window.setTimeout(() => URL.revokeObjectURL(url), 60_000)
}
