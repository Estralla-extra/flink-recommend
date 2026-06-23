export interface RecommendItem { itemId: string; score: number }
export interface RecommendSources { flink: boolean; realtime: boolean; hot: boolean; current_item: string }
export interface RecommendResponse { user_id: string; recommendations: RecommendItem[]; sources: RecommendSources; timestamp: number }
export interface KafkaMetrics { tps_in: number; tps_out: number; lag: number[] }
export interface FlinkMetrics { window_time: number; redis_time: number }
export interface RedisMetrics { hit_rate: string; used_memory: string }
export interface SystemMetricsResponse { kafka: KafkaMetrics; flink: FlinkMetrics; redis: RedisMetrics }

export interface FunnelStep { behavior: string; count: number; rate: number }
export interface FunnelResponse { hour: string; steps: FunnelStep[] }

export interface HourlyHeatmap { hour: string; pv: number; cart: number; fav: number; buy: number; total: number }
export interface HeatmapResponse { date: string; data: HourlyHeatmap[] }

export interface CategoryItem { categoryId: string; count: number }
export interface CategoryTopResponse { items: CategoryItem[] }

export interface ActivityBucket { label: string; count: number }
export interface ActivityResponse { hour: string; buckets: ActivityBucket[] }


export interface RealtimeMetrics {
  events_today: number
  users_today: number
  qps: number
  conversion_rate: number
}
 
 export interface UserProfile {
   id: number
   username: string
   name: string
   avatar?: string
   email?: string
   createdAt?: string
 }
 
 export interface LoginResponse {
   token: string
   user: { id: number; username: string; name: string }
 }
 
 export interface AuthErrorResponse {
   error: string
 }
