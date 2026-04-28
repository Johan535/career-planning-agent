<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'

const canvasRef = ref<HTMLCanvasElement | null>(null)

type P = { x: number; y: number; vx: number; vy: number; r: number; hue: number; a: number }

let raf = 0
let dpr = 1
let w = 0
let h = 0
let ps: P[] = []

function resize() {
  const c = canvasRef.value
  if (!c) return
  dpr = Math.min(2, window.devicePixelRatio || 1)
  w = Math.floor(window.innerWidth)
  h = Math.floor(window.innerHeight)
  c.width = Math.floor(w * dpr)
  c.height = Math.floor(h * dpr)
  c.style.width = `${w}px`
  c.style.height = `${h}px`
  const ctx = c.getContext('2d')
  if (ctx) ctx.setTransform(dpr, 0, 0, dpr, 0, 0)

  const target = Math.max(42, Math.floor((w * h) / 42000))
  if (ps.length > target) ps = ps.slice(0, target)
  while (ps.length < target) {
    ps.push({
      x: Math.random() * w,
      y: Math.random() * h,
      vx: (Math.random() - 0.5) * 0.42,
      vy: (Math.random() - 0.5) * 0.34,
      r: 0.8 + Math.random() * 2.8,
      hue: 175 + Math.random() * 120,
      a: 0.16 + Math.random() * 0.22,
    })
  }
}

function tick() {
  const c = canvasRef.value
  if (!c) return
  const ctx = c.getContext('2d')
  if (!ctx) return

  ctx.clearRect(0, 0, w, h)

  for (const p of ps) {
    p.x += p.vx
    p.y += p.vy

    if (p.x < -20) p.x = w + 20
    if (p.x > w + 20) p.x = -20
    if (p.y < -20) p.y = h + 20
    if (p.y > h + 20) p.y = -20

    const glow = ctx.createRadialGradient(p.x, p.y, 0, p.x, p.y, p.r * 12)
    glow.addColorStop(0, `hsla(${p.hue}, 100%, 72%, ${p.a})`)
    glow.addColorStop(0.55, `hsla(${p.hue + 24}, 100%, 62%, ${p.a * 0.35})`)
    glow.addColorStop(1, 'rgba(0,0,0,0)')
    ctx.fillStyle = glow
    ctx.beginPath()
    ctx.arc(p.x, p.y, p.r * 12, 0, Math.PI * 2)
    ctx.fill()
  }

  // 极轻连线：不抢戏但更“科技”
  for (let i = 0; i < ps.length; i++) {
    const a = ps[i]
    for (let j = i + 1; j < ps.length; j++) {
      const b = ps[j]
      const dx = a.x - b.x
      const dy = a.y - b.y
      const dist = Math.hypot(dx, dy)
      if (dist > 140) continue
      const alpha = (1 - dist / 140) * 0.06
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
  <div class="fx" aria-hidden="true">
    <div class="bg"></div>
    <canvas ref="canvasRef" class="p"></canvas>
    <div class="v"></div>
    <div class="g"></div>
    <div class="orbs">
      <span class="o o1"></span>
      <span class="o o2"></span>
      <span class="o o3"></span>
    </div>
  </div>
</template>

<style scoped>
.fx {
  position: fixed;
  inset: 0;
  z-index: -1;
  overflow: hidden;
  background: #050610;
}

.bg {
  position: absolute;
  inset: -35%;
  background:
    radial-gradient(1200px 700px at 15% 12%, rgba(30, 240, 255, 0.38), transparent 60%),
    radial-gradient(900px 700px at 88% 18%, rgba(170, 80, 255, 0.34), transparent 62%),
    radial-gradient(900px 700px at 50% 95%, rgba(35, 255, 200, 0.18), transparent 66%),
    linear-gradient(115deg, #050610, #07081a 28%, #070a22 55%, #050610);
  filter: saturate(1.25) contrast(1.1);
  animation: drift 11s ease-in-out infinite;
}

.bg::after {
  content: '';
  position: absolute;
  inset: -70%;
  background: linear-gradient(
    90deg,
    rgba(30, 240, 255, 0.24),
    rgba(160, 70, 255, 0.22),
    rgba(35, 255, 200, 0.18),
    rgba(255, 60, 220, 0.2),
    rgba(30, 240, 255, 0.24)
  );
  background-size: 260% 260%;
  mix-blend-mode: screen;
  opacity: 0.34;
  filter: blur(32px);
  animation: flow 9s linear infinite;
}

.p {
  position: absolute;
  inset: 0;
  opacity: 0.92;
}

.v {
  position: absolute;
  inset: -2px;
  background: radial-gradient(circle at 50% 35%, transparent 0 38%, rgba(0, 0, 0, 0.52) 72%, rgba(0, 0, 0, 0.85));
  pointer-events: none;
}

.g {
  position: absolute;
  inset: 0;
  opacity: 0.12;
  background:
    linear-gradient(rgba(255, 255, 255, 0.08) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.08) 1px, transparent 1px);
  background-size: 70px 70px;
  transform: perspective(900px) rotateX(62deg) translateY(220px);
  mask-image: radial-gradient(closest-side at 50% 70%, rgba(0, 0, 0, 1), rgba(0, 0, 0, 0));
  pointer-events: none;
}

.orbs {
  position: absolute;
  inset: 0;
  pointer-events: none;
}
.o {
  position: absolute;
  width: 520px;
  height: 520px;
  border-radius: 999px;
  filter: blur(44px);
  opacity: 0.28;
  mix-blend-mode: screen;
}
.o1 {
  left: -140px;
  top: 10%;
  background: radial-gradient(circle, rgba(30, 240, 255, 0.65), transparent 62%);
  animation: float1 8.2s ease-in-out infinite;
}
.o2 {
  right: -170px;
  top: 14%;
  background: radial-gradient(circle, rgba(170, 80, 255, 0.62), transparent 62%);
  animation: float2 9.2s ease-in-out infinite;
}
.o3 {
  left: 18%;
  bottom: -260px;
  width: 700px;
  height: 700px;
  background: radial-gradient(circle, rgba(35, 255, 200, 0.42), transparent 65%);
  animation: float3 10.6s ease-in-out infinite;
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
@keyframes float1 {
  0%,
  100% {
    transform: translate3d(0, 0, 0);
  }
  50% {
    transform: translate3d(22px, -26px, 0);
  }
}
@keyframes float2 {
  0%,
  100% {
    transform: translate3d(0, 0, 0);
  }
  50% {
    transform: translate3d(-22px, 30px, 0);
  }
}
@keyframes float3 {
  0%,
  100% {
    transform: translate3d(0, 0, 0);
  }
  50% {
    transform: translate3d(18px, -20px, 0);
  }
}
</style>

