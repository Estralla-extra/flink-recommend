import type {
  RecommendResponse, SystemMetricsResponse,
  FunnelResponse, HeatmapResponse, CategoryTopResponse, ActivityResponse,
} from '@/types'

const BASE = '/api'

export async function fetchRecommend(userId: string, currentItem?: string): Promise<RecommendResponse> {
  const params = new URLSearchParams({ user_id: userId })
  if (currentItem) params.set('current_item', currentItem)
  const res = await fetch(`${BASE}/recommend?${params}`)
  if (!res.ok) throw new Error(`Recommend API error: ${res.status}`)
  return res.json()
}

export async function fetchEvents(): Promise<string[]> {
  const res = await fetch(`${BASE}/events/recent`)
  if (!res.ok) throw new Error(`Events API error: ${res.status}`)
  return res.json()
}

export async function fetchSystemMetrics(): Promise<SystemMetricsResponse> {
  const res = await fetch(`${BASE}/metrics/system`)
  if (!res.ok) throw new Error(`Metrics API error: ${res.status}`)
  return res.json()
}

export async function fetchFunnel(hour: string): Promise<FunnelResponse> {
  const res = await fetch(`${BASE}/analytics/funnel?hour=${hour}`)
  if (!res.ok) throw new Error(`Funnel API error: ${res.status}`)
  return res.json()
}

export async function fetchHeatmap(date: string): Promise<HeatmapResponse> {
  const res = await fetch(`${BASE}/analytics/heatmap?date=${date}`)
  if (!res.ok) throw new Error(`Heatmap API error: ${res.status}`)
  return res.json()
}

export async function fetchCategoryTop(limit = 20): Promise<CategoryTopResponse> {
  const res = await fetch(`${BASE}/analytics/category/top?limit=${limit}`)
  if (!res.ok) throw new Error(`Category API error: ${res.status}`)
  return res.json()
}

export async function fetchActivity(hour: string): Promise<ActivityResponse> {
  const res = await fetch(`${BASE}/analytics/user/activity?hour=${hour}`)
  if (!res.ok) throw new Error(`Activity API error: ${res.status}`)
  return res.json()
}

export async function fetchRealtime(): Promise<import('@/types').RealtimeMetrics> {
  const res = await fetch('/api/metrics/realtime')
  if (!res.ok) throw new Error('Realtime API error: ' + res.status)
  return res.json()
}
