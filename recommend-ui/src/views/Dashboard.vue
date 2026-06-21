<script setup lang="ts">
import { onMounted, onUnmounted, ref } from "vue"
import { useEventsStore } from "@/stores/events"
import { useMetricsStore } from "@/stores/metrics"
import { fetchRealtime } from "@/api"
import type { RealtimeMetrics } from "@/types"
import KafkaTpsPanel from "@/components/KafkaTpsPanel.vue"
import EventStreamPanel from "@/components/EventStreamPanel.vue"
import RecommendSankeyPanel from "@/components/RecommendSankeyPanel.vue"
import Top5Panel from "@/components/Top5Panel.vue"
import FlinkMetricsPanel from "@/components/FlinkMetricsPanel.vue"
import RedisPanel from "@/components/RedisPanel.vue"

const eventsStore = useEventsStore()
const metricsStore = useMetricsStore()
const kpi = ref<RealtimeMetrics>({ events_today: 0, users_today: 0, qps: 0, conversion_rate: 0 })

let pollTimer: ReturnType<typeof setInterval> | null = null
let sortTimer: ReturnType<typeof setInterval> | null = null
let polling = false

async function pollAll() {
  if (polling) return
  polling = true
  try {
    await eventsStore.refresh()
    await metricsStore.refresh()
    kpi.value = await fetchRealtime()
  } catch {} finally { polling = false }
}

onMounted(() => {
  pollAll()
  pollTimer = setInterval(pollAll, 3000)
  eventsStore.sortHotItems()
  sortTimer = setInterval(() => eventsStore.sortHotItems(), 10000)
})

onUnmounted(() => {
  if (pollTimer) clearInterval(pollTimer)
  if (sortTimer) clearInterval(sortTimer)
})
</script>

<template>
  <div class="dashboard-page">


  <div class="kpi-row">
    <div class="kpi-card"><span class="kpi-label">今日事件</span><span class="kpi-val">{{ kpi.events_today.toLocaleString() }}</span></div>
    <div class="kpi-col2">
      <div class="kpi-card"><span class="kpi-label">活跃用户</span><span class="kpi-val">{{ kpi.users_today.toLocaleString() }}</span></div>
      <div class="kpi-card"><span class="kpi-label">实时 QPS</span><span class="kpi-val">{{ metricsStore.kafka.tps_in }}</span></div>
    </div>
    <div class="kpi-card"><span class="kpi-label">转化率</span><span class="kpi-val">{{ kpi.conversion_rate }}%</span></div>
  </div>

    <main class="dashboard-grid">
      <KafkaTpsPanel class="cell-kafka" />
      <RecommendSankeyPanel class="cell-sankey" />
      <RedisPanel class="cell-redis" />
      <EventStreamPanel class="cell-events" />
      <Top5Panel class="cell-top5mid" />
      <FlinkMetricsPanel class="cell-flink" />
    </main>
  </div>
</template>

<style scoped>
.dashboard-page { flex: 1; display: flex; flex-direction: column; position: relative; overflow-y: auto; }
.kpi-row { display: grid; grid-template-columns: 1fr 2.2fr 1fr; gap: 12px; padding: 12px 16px 0; z-index: 5; flex-shrink: 0; min-width: 0; }
.kpi-row > * { min-width: 0; }
.kpi-col2 { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; min-width: 0; }
.kpi-card { flex: 1; background: rgba(16,22,36,0.8); border: 1px solid var(--border-color); border-radius: 8px; padding: 12px 16px; display: flex; flex-direction: column; min-width: 0; }
.kpi-label { font-size: 11px; color: var(--text-muted); letter-spacing: 0.5px; }
.kpi-val { font-size: 22px; font-weight: 700; color: var(--accent); font-variant-numeric: tabular-nums; white-space: nowrap; font-family: Consolas, monospace; text-shadow: 0 0 10px rgba(56,189,248,0.4), 0 0 30px rgba(56,189,248,0.1); }
.dashboard-grid { flex: 1; display: grid; grid-template-columns: 1fr 2.2fr 1fr; grid-template-rows: minmax(240px, 1fr) minmax(220px, 1fr); gap: 12px; padding: 12px 16px 16px; position: relative; z-index: 1; overflow: visible; }
.cell-kafka   { grid-row: 1; grid-column: 1; }
.cell-sankey  { grid-row: 1; grid-column: 2; }
.cell-redis   { grid-row: 1; grid-column: 3; }
.cell-events  { grid-row: 2; grid-column: 1; }
.cell-top5mid { grid-row: 2; grid-column: 2; }
.cell-flink   { grid-row: 2; grid-column: 3; }

/* === Tech glow === */
.dashboard-grid > * {
  position: relative;
  overflow: hidden;
}
.dashboard-grid > *::before {
  content: '';
  position: absolute;
  top: 0; left: 0;
  width: 100%;
  height: 2px;
  background: linear-gradient(90deg, transparent, var(--accent), transparent);
  background-size: 200% 100%;
  animation: dashFlow 3s linear infinite;
  z-index: 1;
}
.cell-kafka {
  box-shadow: 0 0 20px rgba(20,184,166,0.35), inset 0 0 20px rgba(20,184,166,0.05);
  border-color: rgba(20,184,166,0.5) !important;
}
.cell-kafka::before {
  background: linear-gradient(90deg, transparent, #14b8a6, transparent);
  background-size: 200% 100%;
  animation: dashFlow 2s linear infinite;
}
@keyframes dashFlow {
  0% { background-position: -200% 0; }
  100% { background-position: 200% 0; }
}
/* === Per-panel glow === */
.cell-kafka {
  box-shadow: 0 0 20px rgba(20,184,166,0.35), inset 0 0 20px rgba(20,184,166,0.05);
  border-color: rgba(20,184,166,0.5) !important;
}
.cell-kafka::before {
  background: linear-gradient(90deg, transparent, #14b8a6, transparent);
  background-size: 200% 100%;
}

.cell-sankey {
  box-shadow: 0 0 20px rgba(34,197,94,0.35), inset 0 0 20px rgba(34,197,94,0.05);
  border-color: rgba(34,197,94,0.5) !important;
}
.cell-sankey::before {
  background: linear-gradient(90deg, transparent, #22c55e, transparent);
  background-size: 200% 100%;
}

.cell-redis {
  box-shadow: 0 0 20px rgba(245,158,11,0.35), inset 0 0 20px rgba(245,158,11,0.05);
  border-color: rgba(245,158,11,0.5) !important;
}
.cell-redis::before {
  background: linear-gradient(90deg, transparent, #f59e0b, transparent);
  background-size: 200% 100%;
}

.cell-events {
  box-shadow: 0 0 20px rgba(6,182,212,0.35), inset 0 0 20px rgba(6,182,212,0.05);
  border-color: rgba(6,182,212,0.5) !important;
}
.cell-events::before {
  background: linear-gradient(90deg, transparent, #06b6d4, transparent);
  background-size: 200% 100%;
}

.cell-top5mid {
  box-shadow: 0 0 20px rgba(236,72,153,0.35), inset 0 0 20px rgba(236,72,153,0.05);
  border-color: rgba(236,72,153,0.5) !important;
}
.cell-top5mid::before {
  background: linear-gradient(90deg, transparent, #ec4899, transparent);
  background-size: 200% 100%;
}

.cell-flink {
  box-shadow: 0 0 20px rgba(139,92,246,0.35), inset 0 0 20px rgba(139,92,246,0.05);
  border-color: rgba(139,92,246,0.5) !important;
}
.cell-flink::before {
  background: linear-gradient(90deg, transparent, #8b5cf6, transparent);
  background-size: 200% 100%;
}
/* === KPI cards glow === */
.kpi-card {
  position: relative;
  overflow: hidden;
}
.kpi-card::before {
  content: '';
  position: absolute;
  top: 0; left: 0;
  width: 100%;
  height: 2px;
  background: linear-gradient(90deg, transparent, var(--accent), transparent);
  background-size: 200% 100%;
  animation: dashFlow 3s linear infinite;
}</style>
