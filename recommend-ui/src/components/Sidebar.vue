<script setup lang="ts">
import { ref, computed } from 'vue'
import { useMetricsStore } from '@/stores/metrics'
import { useEventsStore } from '@/stores/events'
import Icon from './common/Icon.vue'

const metricsStore = useMetricsStore()
const eventsStore = useEventsStore()

const visible = ref(false)
defineExpose({ visible })

const health = computed(() => [
  {
    label: 'Kafka', icon: 'activity',
    ok: metricsStore.lastStatus === 200,
    detail: `TPS ${metricsStore.kafka.tps_in}`,
    sub: 'hadoop102:9092',
  },
  {
    label: 'Redis', icon: 'database',
    ok: metricsStore.hitRate >= 0,
    detail: metricsStore.redis.used_memory,
    sub: `命中率 ${metricsStore.hitRate.toFixed(0)}%`,
  },
  {
    label: 'Flink', icon: 'cpu',
    ok: metricsStore.flink?.window_time > 0,
    detail: metricsStore.flink.window_time > 0 ? 'RUNNING' : 'DISCONNECTED',
    sub: metricsStore.flink.window_time > 0 ? `${metricsStore.flink.window_time}ms` : '部署集群后连接',
  },
  {
    label: '服务', icon: 'server',
    ok: metricsStore.lastStatus === 200,
    detail: metricsStore.lastStatus === 200 ? 'HEALTHY' : 'ERROR',
    sub: `响应 ${metricsStore.lastTiming}ms`,
  },
])

const stats = computed(() => {
  let total = 0
  for (const count of eventsStore.itemCounter.values()) {
    total += count
  }
  return {
    total,
    users: eventsStore.userCounter.size,
    items: eventsStore.itemCounter.size,
  }
})
</script>

<template>
  <Transition name="drawer">
    <aside v-if="visible" class="sidebar">
      <div class="sb-header">
        <span class="sb-title"><Icon name="search" :size="14" /> 系统巡检</span>
        <button class="sb-close" @click="visible = false">关闭</button>
      </div>

      <!-- 系统健康 -->
      <div class="sb-section">
        <div class="sb-label">系统健康</div>
        <div class="health-grid">
          <div v-for="h in health" :key="h.label" class="h-card" :class="{ 'h-ok': h.ok, 'h-err': !h.ok }">
            <div class="h-top">
              <span class="h-icon"><Icon :name="h.icon" :size="16" /></span>
              <span class="h-dot" :class="h.ok ? 'dot-on' : 'dot-off'" />
            </div>
            <span class="h-label">{{ h.label }}</span>
            <span class="h-detail">{{ h.detail }}</span>
            <span class="h-sub">{{ h.sub }}</span>
          </div>
        </div>
      </div>

      <!-- 事件流统计 -->
      <div class="sb-section">
        <div class="sb-label">事件流统计</div>
        <div class="stats-grid">
          <div class="st-item">
            <span class="st-val">{{ stats.total }}</span>
            <span class="st-label">事件总数</span>
          </div>
          <div class="st-item">
            <span class="st-val">{{ stats.users }}</span>
            <span class="st-label">独立用户</span>
          </div>
          <div class="st-item">
            <span class="st-val">{{ stats.items }}</span>
            <span class="st-label">独立商品</span>
          </div>
        </div>
      </div>
    </aside>
  </Transition>
</template>

<style scoped>
.sidebar {
  position: fixed;
  top: 0; left: 0;
  width: 340px;
  height: 100%;
  background: rgb(10, 15, 23);
  border-right: 1px solid var(--border-color);
  z-index: 100;
  display: flex;
  flex-direction: column;
  overflow-y: auto;
  padding: 20px;
  gap: 16px;
  box-shadow: 4px 0 30px rgba(0,0,0,0.6);
  will-change: transform;
}

.sb-header { display: flex; justify-content: space-between; align-items: center; }
.sb-title { font-size: 16px; font-weight: 700; color: var(--accent); }
.sb-close {
  background: none; border: 1px solid var(--border-color); color: var(--text-muted);
  width: auto; padding: 4px 10px; border-radius: 6px; cursor: pointer; font-size: 12px;
  transition: all 0.2s;
}
.sb-close:hover { border-color: var(--accent); color: var(--text-primary); }

.sb-section { display: flex; flex-direction: column; gap: 8px; }
.sb-label {
  font-size: 11px; font-weight: 600; color: var(--text-muted);
  text-transform: uppercase; letter-spacing: 1px;
  padding-bottom: 4px; border-bottom: 1px solid var(--border-color);
}

/* Health grid */
.health-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 6px;
}
.h-card {
  padding: 10px;
  border-radius: 8px;
  border: 1px solid var(--border-color);
  display: flex;
  flex-direction: column;
  gap: 2px;
  transition: border-color 0.3s;
}
.h-ok { border-color: rgba(52,211,153,0.3); }
.h-err { border-color: rgba(248,113,113,0.3); }
.h-top { display: flex; justify-content: space-between; align-items: center; }
.h-icon { font-size: 16px; }
.h-dot { width: 7px; height: 7px; border-radius: 50%; }
.dot-on { background: var(--success); box-shadow: 0 0 6px var(--success); }
.dot-off { background: var(--danger); box-shadow: 0 0 6px var(--danger); }
.h-label { font-size: 12px; font-weight: 600; color: var(--text-primary); }
.h-detail { font-size: 13px; font-weight: 700; }
.h-ok .h-detail { color: var(--success); }
.h-err .h-detail { color: var(--danger); }
.h-sub { font-size: 10px; color: var(--text-muted); }

/* API blocks */
.stats-grid {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr;
  gap: 6px;
}
.st-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  padding: 10px 6px;
  background: rgba(30,41,59,0.6);
  border: 1px solid var(--border-color);
  border-radius: 8px;
}
.st-val { font-size: 20px; font-weight: 700; color: var(--accent); font-variant-numeric: tabular-nums; }
.st-label { font-size: 10px; color: var(--text-muted); }

.drawer-enter-active { transition: transform 0.3s cubic-bezier(0.16, 1, 0.3, 1); }
.drawer-leave-active { transition: transform 0.2s ease-in; }
.drawer-enter-from,
.drawer-leave-to { transform: translateX(-100%); }
</style>
