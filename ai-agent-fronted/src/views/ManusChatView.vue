<script setup lang="ts">
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { RouterLink } from 'vue-router'
import { createChatId } from '../utils/id'
import { createSseGetUrl, createTypewriter, openEventSourceText, type TypewriterController } from '../utils/sse'

type Role = 'user' | 'ai'
type ChatMsg = {
  id: string
  role: Role
  content: string
  streaming?: boolean
}

const chatId = ref('')
const input = ref('')
const sending = ref(false)
const messages = ref<ChatMsg[]>([])
const scrollerRef = ref<HTMLElement | null>(null)
const esCloser = ref<null | (() => void)>(null)
const typewriter = ref<TypewriterController | null>(null)
const hasChatted = ref(false)
const sentFlash = ref(false)

const canSend = computed(() => !sending.value && input.value.trim().length > 0)

function normalizeDelta(text: string) {
  return text.replace(/\\n/g, '\n').replace(/<br\s*\/?>/gi, '\n')
}

function escapeHtml(text: string) {
  return text
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
}

function renderContent(content: string) {
  return escapeHtml(normalizeDelta(content))
    .replace(/^###\s+(.+)$/gm, '<h3>$1</h3>')
    .replace(/^##\s+(.+)$/gm, '<h2>$1</h2>')
    .replace(/^#\s+(.+)$/gm, '<h1>$1</h1>')
    .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
    .replace(/`([^`]+)`/g, '<code>$1</code>')
    .replace(/^[-*]\s+(.+)$/gm, '<li>$1</li>')
    .replace(/\n/g, '<br>')
}

function scrollToBottom(smooth = true) {
  const el = scrollerRef.value
  if (!el) return
  el.scrollTo({ top: el.scrollHeight, behavior: smooth ? 'smooth' : 'auto' })
}

async function send() {
  const text = input.value.trim()
  if (!text || sending.value) return

  hasChatted.value = true
  sending.value = true
  input.value = ''

  const userMsg: ChatMsg = {
    id: `${Date.now()}-u`,
    role: 'user',
    content: text,
  }
  messages.value.push(userMsg)

  const aiMsg: ChatMsg = {
    id: `${Date.now()}-a`,
    role: 'ai',
    content: '',
    streaming: true,
  }
  messages.value.push(aiMsg)

  await nextTick()
  scrollToBottom(false)

  // 关闭上一次连接，避免并发
  esCloser.value?.()
  esCloser.value = null
  typewriter.value?.stop()

  try {
    const base = 'http://localhost:8123/api'
    const url = createSseGetUrl(`${base}/ai/manus/chat`, { message: text })
    let streamClosed = false
    let pendingChars = 0
    const finishIfDone = () => {
      if (!streamClosed || pendingChars > 0) return
      if (typewriter.value === writer) typewriter.value = null
      aiMsg.streaming = false
      sending.value = false
      sentFlash.value = true
      window.setTimeout(() => (sentFlash.value = false), 520)
      void nextTick().then(() => scrollToBottom(true))
    }
    const writer = createTypewriter((char) => {
      aiMsg.content += char
      pendingChars = Math.max(0, pendingChars - 1)
      scrollToBottom(true)
      finishIfDone()
    })
    typewriter.value = writer

    const handle = openEventSourceText(url, {
      onText: (delta) => {
        const normalized = normalizeDelta(delta)
        pendingChars += Array.from(normalized).length
        writer.push(normalized)
      },
      onError: () => {
        // 这里默认不让它无限重连，直接关闭并给出提示
        handle.close()
        if (!aiMsg.content && pendingChars === 0) {
          aiMsg.content = '连接异常：请确认后端已启动（8123端口）且接口可访问。'
        }
      },
      onClose: () => {
        streamClosed = true
        finishIfDone()
      },
    })

    esCloser.value = () => handle.close()
  } catch (e) {
    typewriter.value?.stop()
    typewriter.value = null
    aiMsg.streaming = false
    aiMsg.content =
      aiMsg.content ||
      `请求失败：${e instanceof Error ? e.message : '未知错误'}。请确认后端已启动（8123端口）并允许跨域。`
    sending.value = false
  }
}

function stop() {
  esCloser.value?.()
  esCloser.value = null
  typewriter.value?.stop()
  typewriter.value = null
  sending.value = false
  const last = [...messages.value].reverse().find((m) => m.role === 'ai' && m.streaming)
  if (last) last.streaming = false
}

onMounted(() => {
  chatId.value = createChatId()
  messages.value.push({
    id: `${Date.now()}-hello`,
    role: 'ai',
    content: '你好，我是AI智能体。把任务目标发给我，我会边思考边输出执行步骤与结果。',
  })
  scrollToBottom(false)
})

watch(
  () => messages.value.length,
  () => {
    void nextTick().then(() => scrollToBottom(true))
  },
)
</script>

<template>
  <main class="page">
    <header class="top">
      <div class="left">
        <RouterLink class="back" to="/">返回首页</RouterLink>
        <div class="titleWrap">
          <div class="kicker">企业级 AI 智能职业规划</div>
          <h1 class="title">AI 智能体</h1>
        </div>
      </div>

      <div class="right">
        <div class="meta">
          <span class="dot"></span>
          <span class="label">会话ID：</span>
          <span class="value" :title="chatId">{{ chatId }}</span>
        </div>
      </div>
    </header>

    <section class="stage">
      <div class="glass">
        <div ref="scrollerRef" class="chat">
          <div v-if="!hasChatted" class="empty">
            <div class="emptyCard">
              <div class="badge">科技提示</div>
              <div class="big">把你的任务发来</div>
              <div class="small">例如：让智能体帮你拆解目标、生成执行清单、或者逐步推理一个复杂问题。</div>
              <div class="scan" aria-hidden="true"></div>
            </div>
          </div>
          <div v-for="m in messages" :key="m.id" class="row" :class="m.role">
            <div class="bubble" :class="{ streaming: m.streaming }">
              <div class="txt" v-html="renderContent(m.content)"></div>
              <div v-if="m.role === 'ai' && m.streaming" class="caret" aria-hidden="true"></div>
            </div>
          </div>
        </div>

        <div class="composer">
          <div class="inputWrap">
            <textarea
              v-model="input"
              class="input"
              rows="1"
              placeholder="输入你的任务：比如“帮我制定转岗路线”“帮我拆解一个复杂目标”“帮我生成执行清单”……"
              :disabled="sending"
              @keydown.enter.exact.prevent="send"
              @keydown.enter.shift.exact.stop
            />
          </div>

          <div class="btns">
            <button class="btn ghost" type="button" :disabled="!sending" @click="stop">停止</button>
            <button class="btn send" :class="{ loading: sending, flash: sentFlash }" type="button" :disabled="!canSend" @click="send">
              <span class="pulse" aria-hidden="true"></span>
              <span v-if="sending" class="dots" aria-hidden="true"></span>
              <span v-else>发送</span>
            </button>
          </div>
        </div>
      </div>
    </section>
  </main>
</template>

<style scoped>
.page {
  min-height: 100svh;
  position: relative;
  overflow: hidden;
  display: grid;
  grid-template-rows: auto 1fr;
  padding: 18px;
  isolation: isolate;
}

.page::before {
  content: '';
  position: absolute;
  inset: -30%;
  z-index: 0;
  background:
    radial-gradient(1200px 700px at 15% 10%, rgba(30, 240, 255, 0.38), transparent 60%),
    radial-gradient(900px 700px at 90% 20%, rgba(170, 80, 255, 0.34), transparent 62%),
    radial-gradient(900px 700px at 50% 95%, rgba(35, 255, 200, 0.18), transparent 66%),
    linear-gradient(115deg, #050610, #07081a 28%, #070a22 55%, #050610);
  filter: saturate(1.25) contrast(1.1);
  animation: drift 11s ease-in-out infinite;
}
.page::after {
  content: '';
  position: absolute;
  inset: -60%;
  z-index: 1;
  background: linear-gradient(
    90deg,
    rgba(30, 240, 255, 0.25),
    rgba(160, 70, 255, 0.22),
    rgba(35, 255, 200, 0.18),
    rgba(255, 60, 220, 0.2),
    rgba(30, 240, 255, 0.25)
  );
  background-size: 260% 260%;
  mix-blend-mode: screen;
  opacity: 0.34;
  filter: blur(30px);
  animation: flow 9s linear infinite;
}

.top {
  position: relative;
  z-index: 2;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  padding: 14px 16px;
  border-radius: 18px;
  border: 1px solid rgba(120, 220, 255, 0.18);
  background: linear-gradient(135deg, rgba(10, 12, 22, 0.55), rgba(6, 7, 16, 0.78));
  box-shadow:
    0 0 0 1px rgba(170, 80, 255, 0.08) inset,
    0 18px 80px rgba(0, 0, 0, 0.55);
  backdrop-filter: blur(12px);
}
.left {
  display: flex;
  align-items: center;
  gap: 14px;
  min-width: 0;
}
.back {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 10px 12px;
  border-radius: 12px;
  text-decoration: none;
  color: rgba(235, 252, 255, 0.86);
  border: 1px solid rgba(120, 220, 255, 0.22);
  background: linear-gradient(135deg, rgba(30, 240, 255, 0.12), rgba(170, 80, 255, 0.1));
  transition: transform 0.2s ease, box-shadow 0.2s ease;
  white-space: nowrap;
}
.back:hover {
  transform: translateY(-2px);
  box-shadow: 0 12px 40px rgba(40, 240, 255, 0.14);
}
.titleWrap {
  min-width: 0;
}
.kicker {
  font-size: 12px;
  letter-spacing: 0.6px;
  color: rgba(235, 252, 255, 0.62);
}
.title {
  margin: 0;
  font-size: clamp(18px, 2.2vw, 24px);
  letter-spacing: 0.8px;
  color: #eaf7ff;
  text-shadow:
    0 0 18px rgba(40, 240, 255, 0.35),
    0 0 40px rgba(170, 80, 255, 0.18);
}
.right {
  display: flex;
  align-items: center;
  gap: 12px;
}
.meta {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  border-radius: 999px;
  border: 1px solid rgba(255, 120, 220, 0.16);
  background: rgba(10, 10, 18, 0.3);
  color: rgba(235, 252, 255, 0.72);
  max-width: min(520px, 46vw);
}
.dot {
  width: 10px;
  height: 10px;
  border-radius: 999px;
  background: radial-gradient(circle, rgba(35, 255, 200, 0.9), rgba(35, 255, 200, 0.25));
  box-shadow: 0 0 18px rgba(35, 255, 200, 0.35);
}
.value {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-family: ui-monospace, Consolas, monospace;
  color: rgba(235, 252, 255, 0.86);
}

.stage {
  position: relative;
  z-index: 2;
  display: grid;
  place-items: center;
  padding: 16px 0 0;
}

.glass {
  width: min(1100px, 100%);
  height: calc(100svh - 120px);
  border-radius: 24px;
  border: 1px solid rgba(120, 220, 255, 0.22);
  background:
    radial-gradient(1200px 500px at 20% 10%, rgba(255, 255, 255, 0.1), transparent 55%),
    linear-gradient(135deg, rgba(10, 12, 22, 0.56), rgba(6, 7, 16, 0.82));
  box-shadow:
    0 0 0 1px rgba(170, 80, 255, 0.08) inset,
    0 26px 140px rgba(0, 0, 0, 0.72),
    0 0 60px rgba(40, 240, 255, 0.14);
  backdrop-filter: blur(14px);
  overflow: hidden;
  display: grid;
  grid-template-rows: 1fr auto;
}

.chat {
  padding: 18px 16px;
  overflow: auto;
  scroll-behavior: smooth;
  position: relative;
}
.empty {
  position: sticky;
  top: 0;
  display: grid;
  place-items: center;
  padding: 18px 6px 8px;
}
.emptyCard {
  width: min(860px, 100%);
  border-radius: 20px;
  padding: 18px 18px 16px;
  border: 1px solid rgba(120, 220, 255, 0.18);
  background:
    radial-gradient(900px 280px at 20% 10%, rgba(255, 255, 255, 0.1), transparent 60%),
    linear-gradient(135deg, rgba(10, 12, 22, 0.48), rgba(6, 7, 16, 0.76));
  box-shadow:
    0 0 0 1px rgba(170, 80, 255, 0.06) inset,
    0 18px 70px rgba(0, 0, 0, 0.52),
    0 0 55px rgba(30, 240, 255, 0.12);
  backdrop-filter: blur(12px);
  position: relative;
  overflow: hidden;
}
.emptyCard .badge {
  display: inline-flex;
  align-items: center;
  padding: 6px 10px;
  border-radius: 999px;
  border: 1px solid rgba(255, 120, 220, 0.14);
  background: linear-gradient(135deg, rgba(255, 60, 220, 0.14), rgba(35, 255, 200, 0.1));
  color: rgba(235, 252, 255, 0.8);
  font-size: 12px;
  letter-spacing: 0.5px;
}
.emptyCard .big {
  margin-top: 10px;
  font-size: 18px;
  color: rgba(235, 252, 255, 0.92);
  text-shadow: 0 0 18px rgba(30, 240, 255, 0.18);
  letter-spacing: 0.5px;
}
.emptyCard .small {
  margin-top: 6px;
  font-size: 13px;
  color: rgba(235, 252, 255, 0.66);
  line-height: 1.6;
}
.emptyCard .scan {
  position: absolute;
  inset: -40% -10%;
  background: linear-gradient(90deg, transparent, rgba(30, 240, 255, 0.14), transparent);
  transform: rotate(18deg);
  animation: scan 2.8s ease-in-out infinite;
  mix-blend-mode: screen;
}
.chat::-webkit-scrollbar {
  width: 10px;
}
.chat::-webkit-scrollbar-thumb {
  background: linear-gradient(180deg, rgba(40, 240, 255, 0.25), rgba(170, 80, 255, 0.22));
  border-radius: 999px;
}
.row {
  display: flex;
  margin: 10px 0;
  animation: rise 0.22s ease both;
}
.row.user {
  justify-content: flex-end;
}
.row.ai {
  justify-content: flex-start;
}

.bubble {
  position: relative;
  max-width: min(740px, 88%);
  padding: 12px 14px;
  border-radius: 16px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  box-shadow:
    0 12px 40px rgba(0, 0, 0, 0.35),
    0 0 0 1px rgba(255, 255, 255, 0.04) inset;
  backdrop-filter: blur(10px);
  transition: transform 0.18s ease, box-shadow 0.18s ease;
}
.bubble:hover {
  transform: translateY(-2px);
  box-shadow:
    0 16px 55px rgba(0, 0, 0, 0.4),
    0 0 45px rgba(40, 240, 255, 0.12);
}
.row.user .bubble {
  background: linear-gradient(135deg, rgba(255, 60, 220, 0.22), rgba(170, 80, 255, 0.18), rgba(35, 255, 200, 0.14));
  border-color: rgba(255, 120, 220, 0.18);
  color: rgba(255, 245, 255, 0.92);
}
.row.ai .bubble {
  background: linear-gradient(135deg, rgba(30, 240, 255, 0.18), rgba(20, 40, 80, 0.4));
  border-color: rgba(40, 240, 255, 0.2);
  color: rgba(235, 252, 255, 0.92);
  box-shadow:
    0 12px 40px rgba(0, 0, 0, 0.35),
    0 0 40px rgba(40, 240, 255, 0.12);
}

.row.user .bubble::after {
  content: '';
  position: absolute;
  right: -8px;
  top: 14px;
  width: 14px;
  height: 14px;
  transform: rotate(45deg);
  background: linear-gradient(135deg, rgba(255, 60, 220, 0.18), rgba(170, 80, 255, 0.16));
  border-right: 1px solid rgba(255, 120, 220, 0.16);
  border-top: 1px solid rgba(255, 120, 220, 0.12);
}
.row.ai .bubble::after {
  content: '';
  position: absolute;
  left: -8px;
  top: 14px;
  width: 14px;
  height: 14px;
  transform: rotate(45deg);
  background: linear-gradient(135deg, rgba(30, 240, 255, 0.16), rgba(20, 40, 80, 0.3));
  border-left: 1px solid rgba(40, 240, 255, 0.16);
  border-bottom: 1px solid rgba(40, 240, 255, 0.12);
}

.txt {
  white-space: pre-wrap;
  word-break: break-word;
  line-height: 1.6;
  letter-spacing: 0.2px;
}

.caret {
  width: 10px;
  height: 16px;
  display: inline-block;
  margin-left: 6px;
  vertical-align: -2px;
  border-radius: 3px;
  background: rgba(40, 240, 255, 0.85);
  box-shadow: 0 0 18px rgba(40, 240, 255, 0.35);
  animation: blink 0.9s steps(1) infinite;
}

.composer {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 12px;
  padding: 14px;
  border-top: 1px solid rgba(120, 220, 255, 0.16);
  background: linear-gradient(135deg, rgba(8, 10, 18, 0.35), rgba(6, 7, 16, 0.55));
}
.inputWrap {
  position: relative;
}
.input {
  width: 100%;
  resize: none;
  min-height: 44px;
  max-height: 140px;
  padding: 12px 12px;
  border-radius: 16px;
  border: 1px solid rgba(120, 220, 255, 0.18);
  background:
    radial-gradient(600px 140px at 20% 10%, rgba(255, 255, 255, 0.08), transparent 60%),
    rgba(10, 10, 18, 0.35);
  color: rgba(235, 252, 255, 0.92);
  outline: none;
  box-shadow:
    0 0 0 1px rgba(170, 80, 255, 0.06) inset,
    0 10px 40px rgba(0, 0, 0, 0.35);
  backdrop-filter: blur(10px);
  transition: box-shadow 0.18s ease, border-color 0.18s ease, transform 0.18s ease;
}
.input::placeholder {
  color: rgba(235, 252, 255, 0.5);
}
.input:focus {
  border-color: rgba(40, 240, 255, 0.35);
  box-shadow:
    0 0 0 1px rgba(40, 240, 255, 0.12) inset,
    0 0 45px rgba(40, 240, 255, 0.16),
    0 14px 55px rgba(0, 0, 0, 0.4);
}

.btns {
  display: flex;
  gap: 10px;
  align-items: stretch;
}
.btn {
  position: relative;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 16px;
  padding: 0 18px;
  min-width: 96px;
  cursor: pointer;
  color: rgba(235, 252, 255, 0.92);
  transition: transform 0.18s ease, box-shadow 0.18s ease, filter 0.18s ease;
}
.btn:disabled {
  opacity: 0.45;
  cursor: not-allowed;
  filter: grayscale(0.4);
}
.btn.ghost {
  background: rgba(10, 10, 18, 0.28);
  border-color: rgba(255, 120, 220, 0.14);
}
.btn.send {
  background: linear-gradient(135deg, rgba(30, 240, 255, 0.22), rgba(170, 80, 255, 0.18), rgba(255, 60, 220, 0.16));
  border-color: rgba(40, 240, 255, 0.18);
  box-shadow: 0 0 35px rgba(40, 240, 255, 0.14);
  overflow: hidden;
}
.btn.send.loading {
  filter: saturate(1.1) brightness(1.05);
}
.btn.send.flash::after {
  content: '';
  position: absolute;
  inset: -40%;
  background: radial-gradient(circle, rgba(255, 255, 255, 0.22), transparent 60%);
  animation: flash 0.5s ease-out;
  pointer-events: none;
}
.dots {
  display: inline-block;
  width: 46px;
  height: 14px;
  position: relative;
}
.dots::before,
.dots::after {
  content: '';
  position: absolute;
  inset: 0;
  background: radial-gradient(circle, rgba(235, 252, 255, 0.92) 2px, transparent 3px);
  background-size: 14px 14px;
  animation: dots 0.9s linear infinite;
  opacity: 0.85;
}
.dots::after {
  filter: drop-shadow(0 0 10px rgba(30, 240, 255, 0.25));
}
.btn.send .pulse {
  position: absolute;
  inset: -60%;
  background: radial-gradient(circle, rgba(255, 255, 255, 0.12), transparent 55%);
  animation: pulse 1.9s ease-in-out infinite;
  pointer-events: none;
}
.btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 16px 55px rgba(0, 0, 0, 0.45), 0 0 55px rgba(40, 240, 255, 0.16);
}
.btn:active:not(:disabled) {
  transform: translateY(-1px) scale(0.99);
}

@media (max-width: 720px) {
  .meta {
    display: none;
  }
  .glass {
    height: calc(100svh - 96px);
  }
}

@keyframes blink {
  0%,
  49% {
    opacity: 1;
  }
  50%,
  100% {
    opacity: 0;
  }
}
@keyframes pulse {
  0%,
  100% {
    transform: translate3d(-2%, -2%, 0) scale(1);
    opacity: 0.6;
  }
  50% {
    transform: translate3d(2%, 2%, 0) scale(1.05);
    opacity: 0.9;
  }
}
@keyframes rise {
  from {
    opacity: 0;
    transform: translateY(6px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
@keyframes flow {
  0% {
    transform: translate3d(-2%, -2%, 0) rotate(0deg);
    background-position: 0% 50%;
  }
  100% {
    transform: translate3d(2%, 2%, 0) rotate(12deg);
    background-position: 180% 50%;
  }
}
@keyframes drift {
  0%,
  100% {
    transform: translate3d(0, 0, 0) scale(1.02);
  }
  50% {
    transform: translate3d(1.2%, -1.2%, 0) scale(1.04);
  }
}
@keyframes scan {
  0% {
    transform: translateX(-18%) rotate(18deg);
    opacity: 0.2;
  }
  50% {
    opacity: 0.9;
  }
  100% {
    transform: translateX(18%) rotate(18deg);
    opacity: 0.2;
  }
}
@keyframes dots {
  0% {
    background-position: 0 0;
  }
  100% {
    background-position: 42px 0;
  }
}
@keyframes flash {
  from {
    opacity: 0.9;
    transform: scale(0.98);
  }
  to {
    opacity: 0;
    transform: scale(1.06);
  }
}
</style>

