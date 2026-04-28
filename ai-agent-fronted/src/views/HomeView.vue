<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import NeonActionButton from '../components/NeonActionButton.vue'

const router = useRouter()
const canvasRef = ref<HTMLCanvasElement | null>(null)

type Particle = {
  x: number
  y: number
  r: number
  vx: number
  vy: number
  hue: number
  a: number
}

let raf = 0
let dpr = 1
let w = 0
let h = 0
let particles: Particle[] = []

function resize() {
  const canvas = canvasRef.value
  if (!canvas) return
  dpr = Math.min(2, window.devicePixelRatio || 1)
  w = Math.floor(window.innerWidth)
  h = Math.floor(window.innerHeight)
  canvas.width = Math.floor(w * dpr)
  canvas.height = Math.floor(h * dpr)
  canvas.style.width = `${w}px`
  canvas.style.height = `${h}px`
  const ctx = canvas.getContext('2d')
  if (ctx) ctx.setTransform(dpr, 0, 0, dpr, 0, 0)

  const target = Math.max(34, Math.floor((w * h) / 46000))
  if (particles.length > target) particles = particles.slice(0, target)
  while (particles.length < target) {
    particles.push({
      x: Math.random() * w,
      y: Math.random() * h,
      r: 0.8 + Math.random() * 2.4,
      vx: (Math.random() - 0.5) * 0.32,
      vy: (Math.random() - 0.5) * 0.26,
      hue: 185 + Math.random() * 95,
      a: 0.18 + Math.random() * 0.22,
    })
  }
}

function tick() {
  const canvas = canvasRef.value
  if (!canvas) return
  const ctx = canvas.getContext('2d')
  if (!ctx) return

  ctx.clearRect(0, 0, w, h)

  for (const p of particles) {
    p.x += p.vx
    p.y += p.vy

    if (p.x < -20) p.x = w + 20
    if (p.x > w + 20) p.x = -20
    if (p.y < -20) p.y = h + 20
    if (p.y > h + 20) p.y = -20

    const g = ctx.createRadialGradient(p.x, p.y, 0, p.x, p.y, p.r * 10)
    g.addColorStop(0, `hsla(${p.hue}, 100%, 70%, ${p.a})`)
    g.addColorStop(0.6, `hsla(${p.hue + 30}, 100%, 60%, ${p.a * 0.35})`)
    g.addColorStop(1, 'rgba(0,0,0,0)')
    ctx.fillStyle = g
    ctx.beginPath()
    ctx.arc(p.x, p.y, p.r * 10, 0, Math.PI * 2)
    ctx.fill()
  }

  // 轻微连线（保持克制，避免性能问题）
  for (let i = 0; i < particles.length; i++) {
    const a = particles[i]
    for (let j = i + 1; j < particles.length; j++) {
      const b = particles[j]
      const dx = a.x - b.x
      const dy = a.y - b.y
      const dist = Math.hypot(dx, dy)
      if (dist > 120) continue
      const alpha = (1 - dist / 120) * 0.08
      ctx.strokeStyle = `rgba(120, 220, 255, ${alpha})`
      ctx.lineWidth = 1
      ctx.beginPath()
      ctx.moveTo(a.x, a.y)
      ctx.lineTo(b.x, b.y)
      ctx.stroke()
    }
  }

  raf = window.requestAnimationFrame(tick)
}

onMounted(() => {
  resize()
  window.addEventListener('resize', resize, { passive: true })
  raf = window.requestAnimationFrame(tick)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resize)
  window.cancelAnimationFrame(raf)
})
</script>

<template>
  <main class="home">
    <canvas ref="canvasRef" class="particles" aria-hidden="true"></canvas>

    <div class="vignette" aria-hidden="true"></div>
    <div class="grid" aria-hidden="true"></div>
    <div class="glow-orbs" aria-hidden="true">
      <span class="orb o1"></span>
      <span class="orb o2"></span>
      <span class="orb o3"></span>
    </div>

    <section class="panel">
      <div class="brand">
        <div class="chip">企业级 AI 智能职业规划智能体</div>
        <h1 class="title">AI职业规划智能体</h1>
        <p class="subtitle">选择模式，开启霓虹未来的职业进化路线。</p>
      </div>

      <div class="actions">
        <NeonActionButton
          label="进入：AI职业规划师"
          sub-label="职业定位 · 简历优化 · 面试策略 · 发展路线"
          tone="cyan"
          @click="router.push('/career-chat')"
        />
        <NeonActionButton
          label="进入：AI智能体"
          sub-label="多步推理 · 任务执行 · 连续对话 · 流式输出"
          tone="violet"
          @click="router.push('/manus-chat')"
        />
      </div>

      <div class="foot">
        <span class="hint">提示：建议使用 Chrome/Edge，开启硬件加速，效果更炸裂。</span>
      </div>
    </section>
  </main>
</template>

<style scoped>
.home {
  min-height: 100svh;
  position: relative;
  overflow: hidden;
  display: grid;
  place-items: center;
  padding: 54px 18px;
  isolation: isolate;
}

.particles {
  position: absolute;
  inset: 0;
  z-index: 0;
  opacity: 0.95;
}

.home::before {
  content: '';
  position: absolute;
  inset: -30%;
  z-index: 0;
  background:
    radial-gradient(1200px 700px at 15% 15%, rgba(30, 240, 255, 0.42), transparent 60%),
    radial-gradient(900px 700px at 85% 20%, rgba(170, 80, 255, 0.38), transparent 62%),
    radial-gradient(900px 700px at 50% 90%, rgba(35, 255, 200, 0.24), transparent 66%),
    linear-gradient(115deg, #050610, #07081a 28%, #080a20 52%, #050610);
  filter: saturate(1.2) contrast(1.1);
  animation: drift 10s ease-in-out infinite;
}

.home::after {
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
  opacity: 0.42;
  filter: blur(30px);
  animation: flow 9s linear infinite;
}

.vignette {
  position: absolute;
  inset: -2px;
  z-index: 2;
  background: radial-gradient(circle at 50% 35%, transparent 0 38%, rgba(0, 0, 0, 0.52) 72%, rgba(0, 0, 0, 0.85));
  pointer-events: none;
}

.grid {
  position: absolute;
  inset: 0;
  z-index: 2;
  pointer-events: none;
  opacity: 0.18;
  background:
    linear-gradient(rgba(255, 255, 255, 0.08) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.08) 1px, transparent 1px);
  background-size: 70px 70px;
  transform: perspective(900px) rotateX(62deg) translateY(220px);
  filter: blur(0.2px);
  mask-image: radial-gradient(closest-side at 50% 70%, rgba(0, 0, 0, 1), rgba(0, 0, 0, 0));
}

.glow-orbs {
  position: absolute;
  inset: 0;
  z-index: 2;
  pointer-events: none;
}
.orb {
  position: absolute;
  width: 520px;
  height: 520px;
  border-radius: 999px;
  filter: blur(40px);
  opacity: 0.35;
  mix-blend-mode: screen;
}
.o1 {
  left: -120px;
  top: 10%;
  background: radial-gradient(circle, rgba(30, 240, 255, 0.65), transparent 62%);
  animation: float1 8.2s ease-in-out infinite;
}
.o2 {
  right: -160px;
  top: 18%;
  background: radial-gradient(circle, rgba(170, 80, 255, 0.62), transparent 62%);
  animation: float2 9.2s ease-in-out infinite;
}
.o3 {
  left: 18%;
  bottom: -240px;
  width: 680px;
  height: 680px;
  background: radial-gradient(circle, rgba(35, 255, 200, 0.42), transparent 65%);
  animation: float3 10.6s ease-in-out infinite;
}

.panel {
  position: relative;
  z-index: 3;
  width: min(1080px, 100%);
  border-radius: 26px;
  padding: clamp(22px, 3.2vw, 34px);
  background:
    radial-gradient(1200px 500px at 20% 10%, rgba(255, 255, 255, 0.12), transparent 55%),
    linear-gradient(135deg, rgba(10, 12, 22, 0.6), rgba(6, 7, 16, 0.82));
  border: 1px solid rgba(120, 220, 255, 0.22);
  box-shadow:
    0 0 0 1px rgba(170, 80, 255, 0.08) inset,
    0 22px 120px rgba(0, 0, 0, 0.68);
  backdrop-filter: blur(14px);
}

.brand {
  text-align: center;
  margin-bottom: 20px;
}
.chip {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  padding: 8px 12px;
  border-radius: 999px;
  border: 1px solid rgba(120, 220, 255, 0.22);
  color: rgba(235, 252, 255, 0.82);
  background: linear-gradient(135deg, rgba(30, 240, 255, 0.13), rgba(170, 80, 255, 0.11));
  box-shadow: 0 0 35px rgba(40, 240, 255, 0.16);
  font-size: 13px;
  letter-spacing: 0.4px;
}
.title {
  margin: 14px 0 8px;
  font-size: clamp(38px, 5.6vw, 64px);
  letter-spacing: 1px;
  color: #eaf7ff;
  text-shadow:
    0 0 18px rgba(40, 240, 255, 0.4),
    0 0 40px rgba(170, 80, 255, 0.22),
    0 0 80px rgba(35, 255, 200, 0.14);
  animation: neon 2.6s ease-in-out infinite;
}
.subtitle {
  margin: 0;
  color: rgba(235, 252, 255, 0.74);
  letter-spacing: 0.5px;
}

.actions {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
  align-items: stretch;
  margin-top: 26px;
}

.foot {
  margin-top: 18px;
  display: flex;
  justify-content: center;
}
.hint {
  font-size: 12px;
  color: rgba(235, 252, 255, 0.62);
}

@media (max-width: 980px) {
  .actions {
    grid-template-columns: 1fr;
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
@keyframes neon {
  0%,
  100% {
    filter: drop-shadow(0 0 18px rgba(40, 240, 255, 0.18));
  }
  50% {
    filter: drop-shadow(0 0 26px rgba(170, 80, 255, 0.22));
  }
}
@keyframes float1 {
  0%,
  100% {
    transform: translate3d(0, 0, 0);
  }
  50% {
    transform: translate3d(20px, -24px, 0);
  }
}
@keyframes float2 {
  0%,
  100% {
    transform: translate3d(0, 0, 0);
  }
  50% {
    transform: translate3d(-20px, 28px, 0);
  }
}
@keyframes float3 {
  0%,
  100% {
    transform: translate3d(0, 0, 0);
  }
  50% {
    transform: translate3d(16px, -18px, 0);
  }
}
</style>

