<script setup lang="ts"></script>

<template>
  <div class="stub">
    <div class="card">
      <h1 class="title">AI智能体</h1>
      <p class="desc">页面开发中：这里将接入智能体模式与流式输出。</p>
      <RouterLink class="back" to="/">返回首页</RouterLink>
    </div>
  </div>
</template>

<style scoped>
.stub {
  min-height: 100svh;
  display: grid;
  place-items: center;
  padding: 28px 18px;
}
.card {
  width: min(920px, 100%);
  border-radius: 18px;
  padding: 28px;
  background: rgba(10, 10, 18, 0.55);
  border: 1px solid rgba(255, 120, 220, 0.22);
  box-shadow:
    0 0 0 1px rgba(40, 220, 255, 0.06) inset,
    0 20px 80px rgba(0, 0, 0, 0.55);
  backdrop-filter: blur(12px);
}
.title {
  margin: 0 0 10px;
  font-size: 28px;
  color: #ffeefe;
  letter-spacing: 0.5px;
}
.desc {
  margin: 0 0 18px;
  color: rgba(255, 238, 254, 0.78);
}
.back {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 10px 14px;
  border-radius: 12px;
  text-decoration: none;
  color: #ffe2ff;
  border: 1px solid rgba(255, 120, 220, 0.28);
  background: linear-gradient(135deg, rgba(255, 60, 220, 0.18), rgba(35, 220, 255, 0.14));
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}
.back:hover {
  transform: translateY(-2px);
  box-shadow: 0 10px 30px rgba(255, 80, 230, 0.18);
}
</style>

