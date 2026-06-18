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
import ParticleBg from "@/components/ParticleBg.vue"

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
    <ParticleBg />

  <div class="kpi-row">
    <div class="kpi-card"><span class="kpi-label">今日事件</span><span class="kpi-val">{{ kpi.events_today.toLocaleString() }}</span></div>
    <div class="kpi-card"><span class="kpi-label">活跃用户</span><span class="kpi-val">{{ kpi.users_today.toLocaleString() }}</span></div>
    <div class="kpi-card"><span class="kpi-label">实时 QPS</span><span class="kpi-val">{{ metricsStore.kafka.tps_in }}</span></div>
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
.kpi-row { display: flex; gap: 12px; padding: 12px 16px 0; z-index: 5; flex-shrink: 0; }
.kpi-card { flex: 1; background: rgba(16,22,36,0.8); border: 1px solid var(--border-color); border-radius: 8px; padding: 12px 16px; display: flex; flex-direction: column; }
.kpi-label { font-size: 11px; color: var(--text-muted); letter-spacing: 0.5px; }
.kpi-val { font-size: 22px; font-weight: 700; color: var(--text-primary); font-variant-numeric: tabular-nums; }
.dashboard-grid { flex: 1; display: grid; grid-template-columns: 1fr 2.2fr 1fr; grid-template-rows: minmax(240px, 1fr) minmax(220px, 1fr); gap: 12px; padding: 12px 16px 16px; position: relative; z-index: 1; overflow: visible; }
.cell-kafka   { grid-row: 1; grid-column: 1; }
.cell-sankey  { grid-row: 1; grid-column: 2; }
.cell-redis   { grid-row: 1; grid-column: 3; }
.cell-events  { grid-row: 2; grid-column: 1; }
.cell-top5mid { grid-row: 2; grid-column: 2; }
.cell-flink   { grid-row: 2; grid-column: 3; }
</style>
