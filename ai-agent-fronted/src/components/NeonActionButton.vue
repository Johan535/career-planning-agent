<script setup lang="ts">
import { ref } from 'vue'

const props = defineProps<{
  label: string
  subLabel?: string
  tone?: 'cyan' | 'violet'
}>()

const emit = defineEmits<{
  click: [ev: MouseEvent]
}>()

const rootEl = ref<HTMLElement | null>(null)

function onClick(ev: MouseEvent) {
  const el = rootEl.value
  if (el) {
    const rect = el.getBoundingClientRect()
    const x = ev.clientX - rect.left
    const y = ev.clientY - rect.top
    el.style.setProperty('--rx', `${x}px`)
    el.style.setProperty('--ry', `${y}px`)
    el.classList.remove('ripple')
    void el.offsetWidth
    el.classList.add('ripple')
  }
  emit('click', ev)
}

const toneClass = props.tone ? `tone-${props.tone}` : 'tone-cyan'
</script>

<template>
  <button ref="rootEl" type="button" class="btn" :class="toneClass" @click="onClick">
    <span class="shine" aria-hidden="true"></span>
    <span class="content">
      <span class="label">{{ label }}</span>
      <span v-if="subLabel" class="sub">{{ subLabel }}</span>
    </span>
    <span class="edge" aria-hidden="true"></span>
  </button>
</template>

<style scoped>
.btn {
  --rx: 50%;
  --ry: 50%;
  --g1: rgba(40, 240, 255, 0.95);
  --g2: rgba(150, 70, 255, 0.92);
  --g3: rgba(30, 255, 200, 0.6);
  --line: rgba(120, 220, 255, 0.28);
  --glow: rgba(40, 240, 255, 0.28);
  --shadow: rgba(0, 0, 0, 0.55);

  position: relative;
  width: min(520px, 100%);
  padding: 22px 22px;
  border-radius: 18px;
  border: 1px solid var(--line);
  background:
    radial-gradient(1200px 500px at 20% 10%, rgba(255, 255, 255, 0.12), transparent 50%),
    linear-gradient(135deg, rgba(20, 30, 60, 0.72), rgba(12, 12, 24, 0.86));
  color: rgba(235, 252, 255, 0.92);
  cursor: pointer;
  text-align: left;
  transform-style: preserve-3d;
  transition:
    transform 0.18s ease,
    box-shadow 0.18s ease,
    border-color 0.18s ease,
    filter 0.18s ease;
  box-shadow:
    0 18px 55px rgba(0, 0, 0, 0.5),
    0 0 0 1px rgba(170, 80, 255, 0.08) inset,
    0 0 40px var(--glow);
  backdrop-filter: blur(10px);
  overflow: hidden;
  user-select: none;
}

.btn::before {
  content: '';
  position: absolute;
  inset: -2px;
  background: linear-gradient(90deg, var(--g1), var(--g2), var(--g3), var(--g1));
  background-size: 260% 260%;
  filter: blur(10px);
  opacity: 0.32;
  animation: flow 6.5s linear infinite;
}

.btn::after {
  content: '';
  position: absolute;
  inset: 0;
  background:
    radial-gradient(900px 400px at var(--rx) var(--ry), rgba(255, 255, 255, 0.2), transparent 45%),
    linear-gradient(135deg, rgba(0, 0, 0, 0.06), rgba(0, 0, 0, 0.25));
  opacity: 0;
  transition: opacity 0.18s ease;
}

.btn .content {
  position: relative;
  z-index: 2;
  display: grid;
  gap: 6px;
}
.label {
  font-size: 22px;
  font-weight: 760;
  letter-spacing: 0.6px;
  text-shadow:
    0 0 18px rgba(40, 240, 255, 0.25),
    0 0 28px rgba(160, 70, 255, 0.18);
}
.sub {
  font-size: 13px;
  letter-spacing: 0.4px;
  color: rgba(235, 252, 255, 0.72);
}

.shine {
  position: absolute;
  inset: 0;
  background: radial-gradient(520px 180px at 10% 10%, rgba(255, 255, 255, 0.16), transparent 55%);
  z-index: 1;
  mix-blend-mode: screen;
  pointer-events: none;
}

.edge {
  position: absolute;
  inset: 0;
  border-radius: 18px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  z-index: 3;
  pointer-events: none;
}

.btn:hover {
  transform: translateY(-6px) perspective(1000px) rotateX(4deg);
  border-color: rgba(170, 255, 255, 0.35);
  box-shadow:
    0 26px 80px rgba(0, 0, 0, 0.55),
    0 0 0 1px rgba(180, 90, 255, 0.1) inset,
    0 0 55px rgba(40, 240, 255, 0.34);
  filter: saturate(1.08) brightness(1.05);
}
.btn:hover::after {
  opacity: 1;
}
.btn:active {
  transform: translateY(-2px) perspective(1000px) rotateX(2deg) scale(0.992);
}
.btn:focus-visible {
  outline: 2px solid rgba(40, 240, 255, 0.65);
  outline-offset: 4px;
}

.btn.ripple .edge::after {
  content: '';
  position: absolute;
  left: var(--rx);
  top: var(--ry);
  width: 10px;
  height: 10px;
  transform: translate(-50%, -50%);
  border-radius: 999px;
  background: radial-gradient(circle, rgba(255, 255, 255, 0.7), rgba(40, 240, 255, 0.22), transparent 65%);
  animation: ripple 0.7s ease-out;
}

.tone-violet {
  --g1: rgba(160, 70, 255, 0.98);
  --g2: rgba(40, 240, 255, 0.92);
  --g3: rgba(255, 60, 220, 0.62);
  --line: rgba(255, 120, 220, 0.26);
  --glow: rgba(200, 80, 255, 0.24);
}

@keyframes ripple {
  0% {
    opacity: 0.9;
    width: 10px;
    height: 10px;
  }
  100% {
    opacity: 0;
    width: 620px;
    height: 620px;
  }
}
@keyframes flow {
  0% {
    background-position: 0% 50%;
  }
  100% {
    background-position: 180% 50%;
  }
}
</style>

