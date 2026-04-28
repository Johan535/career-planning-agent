<script setup lang="ts">
import { useRouter } from 'vue-router'
import NeonActionButton from '../components/NeonActionButton.vue'

const router = useRouter()
</script>

<template>
  <main class="home">
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

@keyframes neon {
  0%,
  100% {
    filter: drop-shadow(0 0 18px rgba(40, 240, 255, 0.18));
  }
  50% {
    filter: drop-shadow(0 0 26px rgba(170, 80, 255, 0.22));
  }
}
</style>

