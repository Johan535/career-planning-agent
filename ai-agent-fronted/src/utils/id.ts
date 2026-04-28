export function createChatId() {
  const cryptoAny = crypto as unknown as { randomUUID?: () => string }
  if (cryptoAny?.randomUUID) return cryptoAny.randomUUID()

  // 兼容兜底：不超过 64 字符
  const s = `${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 10)}`
  return s.slice(0, 64)
}

