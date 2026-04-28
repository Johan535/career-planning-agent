<template>
  <GlobalFx />
  <RouterView v-slot="{ Component, route }">
    <Transition name="route-fx" mode="out-in">
      <component :is="Component" :key="route.fullPath" />
    </Transition>
  </RouterView>
</template>

<script setup lang="ts">
import GlobalFx from './components/GlobalFx.vue'
import { RouterView } from 'vue-router'
</script>

<style>
.route-fx-enter-active,
.route-fx-leave-active {
  transition:
    opacity 0.35s ease,
    transform 0.35s ease,
    filter 0.35s ease;
}
.route-fx-enter-from {
  opacity: 0;
  transform: translateY(10px) scale(0.985);
  filter: blur(6px);
}
.route-fx-leave-to {
  opacity: 0;
  transform: translateY(-10px) scale(0.988);
  filter: blur(6px);
}
</style>
