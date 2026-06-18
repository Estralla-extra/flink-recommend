<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from "vue"
import { useMetricsStore } from "@/stores/metrics"

const metricsStore = useMetricsStore()
const startTime = Date.now()
const uptime = ref('0m 0s')
let timer: ReturnType<typeof setInterval>

const flinkState = computed(() => {
  if (metricsStore.flink.window_time > 0) return "RUNNING"
  if (metricsStore.loading) return "CONNECTING"
  return "DISCONNECTED"
})

onMounted(() => {
  timer = setInterval(() => {
    const s = Math.floor((Date.now() - startTime) / 1000)
    uptime.value = `${Math.floor(s / 60)}m ${s % 60}s`
  }, 1000)
})
onUnmounted(() => clearInterval(timer))
</script>

<template>
  <footer class="marquee">
    <span>集群: hadoop102 | 103 | 104</span>
    <span>Redis 内存: <b>{{ metricsStore.redis.used_memory }}</b></span>
    <span>命中率: <b>{{ metricsStore.hitRate.toFixed(1) }}%</b></span>
    <span>Flink: <b :class="flinkState === 'RUNNING' ? 'online' : 'offline'">{{ flinkState }}</b></span>
    <span>运行: <b>{{ uptime }}</b></span>
    <span>TPS: <b>{{ metricsStore.kafka.tps_in }}</b></span>
  </footer>
</template>

<style scoped>
.marquee { height: 40px; background: rgba(10,15,23,0.95); border-top: 1px solid var(--border-color); display: flex; align-items: center; justify-content: space-around; padding: 0 24px; font-size: 13px; color: var(--text-secondary); flex-shrink: 0; }
.marquee b { color: var(--accent); font-weight: 600; }
.online b { color: var(--success) !important; }
.offline b { color: var(--danger) !important; }
</style>
