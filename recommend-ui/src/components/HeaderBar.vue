<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useMetricsStore } from '@/stores/metrics'
import StatusDot from './common/StatusDot.vue'
import Icon from './common/Icon.vue'

const route = useRoute()
const router = useRouter()
const metricsStore = useMetricsStore()

const now = ref('')
let timer: ReturnType<typeof setInterval>

const currentPath = computed(() => route.path)

const navItems = [
  { path: '/dashboard', label: '监控大屏', icon: 'chart' },
  { path: '/recommend', label: '用户推荐', icon: 'user' },
  { path: '/analytics', label: '深度分析', icon: 'activity' },
]

const user = computed(() => {
  try {
    const raw = localStorage.getItem('user')
    return raw ? JSON.parse(raw) : null
  } catch { return null }
})

function logout() {
  localStorage.removeItem('token')
  localStorage.removeItem('user')
  router.push('/login')
}

onMounted(() => {
  now.value = new Date().toLocaleTimeString('zh-CN', { hour12: false })
  timer = setInterval(() => {
    now.value = new Date().toLocaleTimeString('zh-CN', { hour12: false })
  }, 1000)
})
onUnmounted(() => clearInterval(timer))
</script>

<template>
  <header class="header-bar">
    <div class="header-left">
      <div class="logo-icon">
        <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2" />
        </svg>
      </div>
      <h1 class="title">实时推荐与流计算监控大屏</h1>
      <nav class="nav-items">
        <router-link
          v-for="item in navItems"
          :key="item.path"
          :to="item.path"
          :class="['nav-item', { active: currentPath === item.path }]"
        >
          <Icon :name="item.icon" :size="13" /> {{ item.label }}
        </router-link>
      </nav>
    </div>
    <div class="header-right">
      <div class="status-item">
        <StatusDot :status="metricsStore.lastStatus === 200 ? 'online' : 'offline'" />
        <span>{{ metricsStore.lastStatus === 200 ? '系统运行中' : '服务异常' }}</span>
      </div>
      <span v-if="user" class="user-name">{{ user.name || user.username }}</span>
      <a class="logout-link" @click.prevent="logout">退出</a>
      <span class="time">{{ now }}</span>
    </div>
  </header>
</template>

<style scoped>
.header-bar {
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  background: rgba(10, 15, 23, 0.9);
  border-bottom: 1px solid var(--border-color);
  backdrop-filter: blur(12px);
  flex-shrink: 0;
  z-index: 10;
}
.header-left { display: flex; align-items: center; gap: 14px; }
.logo-icon { color: var(--accent); filter: drop-shadow(0 0 8px var(--accent-glow)); }

.nav-items {
  display: flex;
  gap: 2px;
  background: rgba(56,189,248,0.05);
  border-radius: 8px;
  padding: 2px;
}
.nav-item {
  display: flex;
  align-items: center;
  gap: 4px;
  color: var(--text-muted);
  padding: 4px 14px;
  border-radius: 6px;
  cursor: pointer;
  font-size: 12px;
  font-weight: 600;
  transition: all 0.2s;
  text-decoration: none;
}
.nav-item:hover { color: var(--text-secondary); }
  .nav-item.active {
    background: rgba(56,189,248,0.15);
    color: var(--accent);
    position: relative;
  }
  .nav-item.active::after {
    content: '';
    position: absolute;
    bottom: -2px;
    left: 50%;
    transform: translateX(-50%);
    width: 60%;
    height: 2px;
    background: var(--accent);
    border-radius: 1px;
    box-shadow: 0 0 6px var(--accent-glow);
  }

.title {
  font-size: 20px;
  font-weight: 700;
  background: var(--gradient-blue);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  letter-spacing: 2px;
  white-space: nowrap;
}
.header-right { display: flex; align-items: center; gap: 16px; font-size: 14px; color: var(--text-secondary); }
.status-item { display: flex; align-items: center; }
.user-name { color: var(--accent); font-weight: 500; font-size: 13px; }
.logout-link { color: var(--text-muted); cursor: pointer; font-size: 12px; text-decoration: none; }
.logout-link:hover { color: var(--danger); }
.time { font-variant-numeric: tabular-nums; color: var(--accent); font-weight: 500; min-width: 80px; text-align: right; }
</style>
