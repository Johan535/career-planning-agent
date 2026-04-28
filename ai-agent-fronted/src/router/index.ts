import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  { path: '/', name: 'home', component: () => import('../views/HomeView.vue') },
  {
    path: '/career-chat',
    name: 'career-chat',
    component: () => import('../views/CareerChatView.vue'),
  },
  {
    path: '/manus-chat',
    name: 'manus-chat',
    component: () => import('../views/ManusChatView.vue'),
  },
]

export const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior() {
    return { top: 0 }
  },
})

