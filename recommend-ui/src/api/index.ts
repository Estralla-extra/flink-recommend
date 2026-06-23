import type {
  RecommendResponse, SystemMetricsResponse,
  FunnelResponse, HeatmapResponse, CategoryTopResponse, ActivityResponse,
  UserProfile, LoginResponse,
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

export async function fetchCategoryTop(limit = 20, date?: string): Promise<CategoryTopResponse> {
  let url = `${BASE}/analytics/category/top?limit=${limit}`
  if (date) url += '&date=' + date
  const res = await fetch(url)
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
 
 // ===== Auth API =====
 
 export async function loginApi(username: string, password: string): Promise<LoginResponse> {
   const res = await fetch('/api/auth/login', {
     method: 'POST',
     headers: { 'Content-Type': 'application/json' },
     body: JSON.stringify({ username, password }),
   })
   if (!res.ok) {
     const err = await res.json().catch(() => ({ error: '登录失败' }))
     throw new Error(err.error || '登录失败')
   }
   return res.json()
 }
 
 export async function registerApi(username: string, password: string): Promise<void> {
   const res = await fetch('/api/auth/register', {
     method: 'POST',
     headers: { 'Content-Type': 'application/json' },
     body: JSON.stringify({ username, password }),
   })
   if (!res.ok) {
     const err = await res.json().catch(() => ({ error: '注册失败' }))
     throw new Error(err.error || '注册失败')
   }
 }
 
 export async function fetchUserProfile(): Promise<UserProfile> {
   const token = localStorage.getItem('token')
   const res = await fetch('/api/auth/me', {
     headers: { 'Authorization': token || '' },
   })
   if (!res.ok) {
     if (res.status === 401) {
       localStorage.removeItem('token')
       localStorage.removeItem('user')
       window.location.hash = '#/login'
     }
     throw new Error('获取用户信息失败')
   }
   return res.json()
 }
 
 export async function logoutApi(): Promise<void> {
   const token = localStorage.getItem('token')
   await fetch('/api/auth/logout', {
     method: 'POST',
     headers: { 'Authorization': token || '' },
   })
 }
