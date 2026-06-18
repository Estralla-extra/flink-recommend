import { defineStore } from 'pinia'
import { ref } from 'vue'
import { fetchRecommend } from '@/api'
import type { RecommendItem } from '@/types'

export const useRecommendStore = defineStore('recommend', () => {
  const items = ref<RecommendItem[]>([])
  const sourceLists = ref<{ flink: RecommendItem[]; realtime: RecommendItem[]; hot: RecommendItem[] }>({ flink: [], realtime: [], hot: [] })
  const userId = ref('100')
  const loading = ref(true)
  const flinkHit = ref(false)
  const realtimeHit = ref(false)
  const hotUsed = ref(false)
  const currentItem = ref('')
  const lastTiming = ref(0)
  const lastStatus = ref(0)
  const lastError = ref('')
  const lastRaw = ref('')
  const responseTimestamp = ref(0)

  async function fetchForUser(id: string, item?: string) {
    const t0 = performance.now()
    try {
      userId.value = id
      const data = await fetchRecommend(id, item || undefined)
      items.value = data.recommendations
      if (data.source_lists) sourceLists.value = data.source_lists
      if (data.sources) {
        flinkHit.value = data.sources.flink
        realtimeHit.value = data.sources.realtime
        hotUsed.value = data.sources.hot
        currentItem.value = data.sources.current_item || ''
      }
      lastTiming.value = Math.round(performance.now() - t0)
      lastStatus.value = 200
      lastError.value = ''
      lastRaw.value = JSON.stringify(data, null, 2)
      responseTimestamp.value = data.timestamp || Date.now()
    } catch (e) {
      lastTiming.value = Math.round(performance.now() - t0)
      lastStatus.value = 0
      lastError.value = String(e)
    } finally {
      loading.value = false
    }
  }

  async function probeForUser(id: string, item?: string): Promise<{ flinkHit: boolean; realtimeHit: boolean; hotUsed: boolean }> {
    try {
      const data = await fetchRecommend(id, item || undefined)
      return {
        flinkHit: data.sources?.flink ?? false,
        realtimeHit: data.sources?.realtime ?? false,
        hotUsed: data.sources?.hot ?? false,
      }
    } catch {
      return { flinkHit: false, realtimeHit: false, hotUsed: false }
    }
  }

  return { items, sourceLists, userId, loading, flinkHit, realtimeHit, hotUsed, currentItem, lastTiming, lastStatus, lastError, lastRaw, responseTimestamp, fetchForUser, probeForUser }
})
