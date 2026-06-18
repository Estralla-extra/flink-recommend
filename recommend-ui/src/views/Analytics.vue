<script setup lang="ts">
import { ref, onMounted, onUnmounted } from "vue"
import { fetchFunnel, fetchHeatmap, fetchCategoryTop } from "@/api"
import type { FunnelStep, HourlyHeatmap, CategoryItem } from "@/types"
import FunnelChart from "@/components/charts/FunnelChart.vue"
import HeatmapChart from "@/components/charts/HeatmapChart.vue"
import CategoryPie from "@/components/charts/CategoryPie.vue"

const now = new Date()
const currentHour = now.getFullYear().toString() +
  String(now.getMonth() + 1).padStart(2, '0') +
  String(now.getDate()).padStart(2, '0') + '00'
const today = now.getFullYear().toString() +
  String(now.getMonth() + 1).padStart(2, '0') +
  String(now.getDate()).padStart(2, '0')

const funnelSteps = ref<FunnelStep[]>([])
const heatmapData = ref<HourlyHeatmap[]>([])
const categoryItems = ref<CategoryItem[]>([])
const loading = ref(true)

async function refresh() {
  try {
    const [ft, hm, ct] = await Promise.all([
      fetchFunnel(currentHour).catch(() => ({ steps: [] })),
      fetchHeatmap(today).catch(() => ({ data: [] })),
      fetchCategoryTop(12).catch(() => ({ items: [] })),
    ])
    funnelSteps.value = ft.steps || []
    heatmapData.value = hm.data || []
    categoryItems.value = ct.items || []
  } catch {}
}

let timer: ReturnType<typeof setInterval>
onMounted(async () => {
  await refresh()
  loading.value = false
  timer = setInterval(refresh, 30000)
})
onUnmounted(() => { if (timer) clearInterval(timer) })
</script>

<template>
  <div class="analytics-page">
    <div class="an-header">
      <h2>深度分析</h2>
      <p class="an-sub">小时热力图 · 转化漏斗 · 品类分布</p>
    </div>

    <div class="heatmap-section">
      <h3 class="section-title accent">小时热力图 ({{ today }})</h3>
      <div class="chart-wrapper">
        <HeatmapChart v-if="!loading" :data="heatmapData" />
        <div v-else class="chart-placeholder">加载中...</div>
      </div>
    </div>

    <div class="bottom-grid">
      <div class="an-card funnel-card">
        <h3>转化漏斗 ({{ currentHour.slice(-2) }}:00)</h3>
        <div class="chart-wrapper">
          <FunnelChart v-if="!loading && funnelSteps.length" :steps="funnelSteps" />
          <div v-else-if="!loading" class="chart-placeholder">
            <span class="empty-icon">&#8600;</span>
            <span>暂无漏斗数据</span>
          </div>
          <div v-else class="chart-placeholder">加载中...</div>
        </div>
      </div>
      <div class="an-card category-card">
        <h3>品类 Top-12</h3>
        <div class="chart-wrapper">
          <CategoryPie v-if="!loading" :items="categoryItems" />
          <div v-else class="chart-placeholder">加载中...</div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.analytics-page { flex: 1; overflow-y: auto; padding: 20px 32px; display: flex; flex-direction: column; gap: 16px; }
.an-header h2 { font-size: 22px; font-weight: 700; color: var(--text-primary); }
.an-sub { font-size: 13px; color: var(--text-muted); }
.section-title { font-size: 14px; font-weight: 600; padding-left: 10px; border-left: 3px solid var(--accent); flex-shrink: 0; }
.section-title.accent { color: #a78bfa; border-color: #a78bfa; }
.heatmap-section { flex: 0 0 60vh; min-height: 320px; background: rgba(16,22,36,0.8); border: 1px solid rgba(139,92,246,0.35); border-radius: 12px; padding: 20px; display: flex; flex-direction: column; gap: 12px; box-shadow: 0 0 20px rgba(139,92,246,0.06); }
.bottom-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; flex-shrink: 0; min-height: 480px; }
.an-card { background: rgba(16,22,36,0.6); border: 1px solid var(--border-color); border-radius: 12px; padding: 20px; display: flex; flex-direction: column; gap: 12px; }
.an-card h3 { font-size: 14px; font-weight: 600; padding-left: 10px; border-left: 3px solid var(--accent); flex-shrink: 0; }
.funnel-card h3 { color: var(--color-events); border-color: var(--color-events); }
.category-card h3 { color: var(--color-hot); border-color: var(--color-hot); }
.chart-wrapper { flex: 1; min-height: 0; display: flex; }
.chart-placeholder { flex: 1; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 8px; color: var(--text-muted); font-size: 13px; }
.empty-icon { font-size: 28px; opacity: 0.4; }
</style>
