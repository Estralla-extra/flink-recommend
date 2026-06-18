import { defineStore } from 'pinia'
import { ref } from 'vue'
import { fetchEvents } from '@/api'

export const useEventsStore = defineStore('events', () => {
  const events = ref<string[]>([])
  const loading = ref(true)
  const itemCounter = ref<Map<string, number>>(new Map())
  const userCounter = ref<Set<string>>(new Set())
  const hotItems = ref<{ itemId: string; count: number }[]>([])
  let lastDay = new Date().getDate()
  let timer: ReturnType<typeof setInterval> | null = null
  let sortTimer: ReturnType<typeof setInterval> | null = null

  function checkMidnight() {
    const today = new Date().getDate()
    if (today !== lastDay) {
      itemCounter.value = new Map()
      userCounter.value = new Set()
      lastDay = today
    }
  }

  function sortHotItems() {
    checkMidnight()
    hotItems.value = [...itemCounter.value.entries()]
      .sort((a, b) => b[1] - a[1])
      .slice(0, 20)
      .map(([id, count]) => ({ itemId: id, count }))
  }

  async function refresh() {
    try {
      const newEvents = (await fetchEvents()).filter(ev =>
        !['heartbeat_user_001', 'temp_rt_user_001', '__nonexistent__'].some(u => ev.includes(u))
      )
      events.value = newEvents
      checkMidnight()
      const c = itemCounter.value
      const u = userCounter.value
      for (const ev of newEvents) {
        const um = ev.match(/用户(\d+)/)
        if (um) u.add(um[1])
        const match = ev.match(/商品(\d+)/)
        if (match) c.set(match[1], (c.get(match[1]) || 0) + 1)
      }
    } catch (e) {
      console.error('[eventsStore] refresh failed:', e)
    } finally {
      loading.value = false
    }
  }

  function startPolling(intervalMs = 4000) {
    refresh()
    timer = setInterval(refresh, intervalMs)
    sortTimer = setInterval(sortHotItems, 10000)
    sortHotItems()
  }

  function stopPolling() {
    if (timer) { clearInterval(timer); timer = null }
    if (sortTimer) { clearInterval(sortTimer); sortTimer = null }
  }

  return { events, hotItems, itemCounter, userCounter, loading, refresh, sortHotItems, startPolling, stopPolling }
})
