import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { fetchSystemMetrics } from '@/api'
import type { KafkaMetrics, FlinkMetrics, RedisMetrics } from '@/types'

export const useMetricsStore = defineStore('metrics', () => {
  const kafka = ref<KafkaMetrics>({ tps_in: 0, tps_out: 0, lag: [] })
  const flink = ref<FlinkMetrics>({ window_time: 0, redis_time: 0 })
  const redis = ref<RedisMetrics>({ hit_rate: '0', used_memory: 'N/A' })
  const tpsHistory = ref<number[]>([])
  const loading = ref(true)
  const lastTiming = ref(0)
  const lastStatus = ref(0)
  const lastError = ref('')
  const lastRaw = ref('')
  let timer: ReturnType<typeof setInterval> | null = null

  const hitRate = computed(() => parseFloat(redis.value.hit_rate) || 0)

  async function refresh() {
    const t0 = performance.now()
    try {
      const data = await fetchSystemMetrics()
      kafka.value = data.kafka
      flink.value = data.flink
      redis.value = data.redis
      const arr = tpsHistory.value
      arr.push(data.kafka.tps_in)
      if (arr.length > 15) arr.shift()
      lastTiming.value = Math.round(performance.now() - t0)
      lastStatus.value = 200
      lastError.value = ''
      lastRaw.value = JSON.stringify(data, null, 2)
    } catch (e) {
      lastTiming.value = Math.round(performance.now() - t0)
      lastStatus.value = 0
      lastError.value = String(e)
    } finally {
      loading.value = false
    }
  }

  function startPolling(intervalMs = 5000) {
    refresh()
    timer = setInterval(refresh, intervalMs)
  }

  function stopPolling() {
    if (timer) {
      clearInterval(timer)
      timer = null
    }
  }

  return { kafka, flink, redis, tpsHistory, loading, hitRate, lastTiming, lastStatus, lastError, lastRaw, refresh, startPolling, stopPolling }
})
