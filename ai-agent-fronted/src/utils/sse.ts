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
