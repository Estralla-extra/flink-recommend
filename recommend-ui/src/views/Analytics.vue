<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch } from "vue"
import { fetchFunnel, fetchHeatmap, fetchCategoryTop } from "@/api"
import type { FunnelStep, HourlyHeatmap, CategoryItem } from "@/types"
import FunnelChart from "@/components/charts/FunnelChart.vue"
import HeatmapChart from "@/components/charts/HeatmapChart.vue"
import CategoryBar from "@/components/charts/CategoryBar.vue"
import ActivityTrend from "@/components/charts/ActivityTrend.vue"
function td(){const d=new Date();return d.getFullYear().toString()+String(d.getMonth()+1).padStart(2,'0')+String(d.getDate()).padStart(2,'0')}

const selectedDate = ref(td())
const dateModel = computed({get:()=>selectedDate.value.substring(0,4)+'-'+selectedDate.value.substring(4,6)+'-'+selectedDate.value.substring(6,8),set:(v)=>{selectedDate.value=v.replace(/-/g,'')}})
const maxDateStr = td().substring(0,4)+'-'+td().substring(4,6)+'-'+td().substring(6,8)
const selectedHour = ref(String(new Date().getHours()).padStart(2,'0'))
const currentHour = computed(() => selectedDate.value + selectedHour.value)
const isLive = computed(() => selectedDate.value === td())
const funnelSteps=ref<FunnelStep[]>([]);const heatmapData=ref<HourlyHeatmap[]>([]);const categoryItems=ref<CategoryItem[]>([]);const loading=ref(true)
async function refresh(){try{const[ft,hm,ct]=await Promise.all([fetchFunnel(currentHour.value).catch(()=>({steps:[]})),fetchHeatmap(selectedDate.value).catch(()=>({data:[]})),selectedDate.value===td()?fetchCategoryTop(12).catch(()=>({items:[]})):fetchCategoryTop(12,selectedDate.value).catch(()=>({items:[]}))]);funnelSteps.value=ft.steps||[];heatmapData.value=hm.data||[];categoryItems.value=ct.items||[]}catch{}}
var timer:ReturnType<typeof setInterval>
onMounted(async()=>{await refresh();loading.value=false;timer=setInterval(refresh,30000)})
watch(selectedDate,async()=>{if(timer){clearInterval(timer);timer=null}await refresh();if(selectedDate.value===td())timer=setInterval(refresh,30000)})
watch(selectedHour,()=>refresh())
onUnmounted(()=>{if(timer)clearInterval(timer)})
</script>
<template>
  <div class="analytics-page">
    <div class="an-header"><div class="an-breadcrumb"><span class="an-breadcrumb-item">深度分析</span><span class="an-breadcrumb-sep">/</span><span class="an-breadcrumb-item muted">小时热力图 · 环节事件数对比 · 品类分布</span></div></div><div class="an-datebar"><span class="ad-label">日期</span><input type="date" v-model="dateModel" class="an-datepicker" /><select v-model="selectedHour" class="an-hourpicker" title="小时"><option v-for="h in 24" :key="h" :value="(h-1>9?'':'0')+(h-1)">{{ (h-1>9?'':'0')+(h-1) }}:00</option></select><button v-if="!isLive" class="ad-live-btn" @click="selectedDate=td()">实时</button><span v-else class="ad-live-badge">实时</span></div>
    <div class="heatmap-section"><h3 class="section-title accent">小时热力图 ({{ selectedDate }})</h3><div class="chart-wrapper"><HeatmapChart v-if="!loading" :data="heatmapData" /><div v-else class="chart-placeholder">加载中...</div></div></div>
    <div class="activity-section" >
      <h3>用户活跃度时段趋势</h3>
      <div class="chart-wrapper">
        <ActivityTrend :dateKey="selectedDate" :key="selectedDate" />
      </div>
    </div>

    <div class="bottom-grid">
      <div class="an-card funnel-card"><h3>环节事件数对比 ({{ currentHour.slice(-2) }}:00)</h3><div class="chart-wrapper"><FunnelChart v-if="!loading && funnelSteps.length" :steps="funnelSteps" /><div v-else-if="!loading" class="chart-placeholder"><span class="empty-icon">&#8600;</span><span>{{ isLive ? "本小时漏斗数据生成中（15分钟窗口）" : "该时段暂无漏斗数据" }}</span></div><div v-else class="chart-placeholder">加载中...</div></div></div>
      <div class="an-card category-card"><h3>品类 Top-12</h3><div class="chart-wrapper"><CategoryBar v-if="!loading && categoryItems.length" :items="categoryItems" /><div v-else-if="!loading" class="chart-placeholder"><span class="empty-icon">&#8600;</span><span>{{ isLive ? "品类热度数据生成中（15分钟窗口）" : "该日期暂无品类热度数据" }}</span></div><div v-else class="chart-placeholder">加载中...</div></div></div>
    </div>
  </div>
</template>
<style>@import url('https://fonts.googleapis.com/css2?family=Orbitron:wght@400;700&display=swap');
@keyframes dashFlow{0%{background-position:-200% 0}100%{background-position:200% 0}}
.on{font-family:'Orbitron','Consolas',monospace;font-variant-numeric:tabular-nums}
</style>
<style scoped>
.analytics-page{flex:1;overflow-y:auto;padding:20px 32px;display:flex;flex-direction:column;gap:16px}
.an-header{display:flex;align-items:center}
.an-breadcrumb{display:flex;align-items:baseline;gap:6px}
.an-breadcrumb-item{font-size:20px;font-weight:700;background:var(--gradient-blue);-webkit-background-clip:text;-webkit-text-fill-color:transparent;background-clip:text}
.an-breadcrumb-item.muted{font-size:11px;-webkit-text-fill-color:var(--text-muted);background:none;font-weight:400;opacity:0.5}
.an-breadcrumb-sep{color:var(--text-muted);opacity:0.2;font-size:12px}
.heatmap-section,.an-card,.activity-section{position:relative;overflow:hidden}
.heatmap-section::before,.an-card::before,.activity-section::before{content:'';position:absolute;top:0;left:0;width:100%;height:2px;background:linear-gradient(90deg,transparent,#8b5cf6,transparent);background-size:200% 100%;animation:dashFlow 4s linear infinite;z-index:1}
.heatmap-section:hover,.an-card:hover,.activity-section:hover{border-color:var(--border-glow)!important;box-shadow:0 4px 20px rgba(0,0,0,0.4),0 0 16px rgba(56,189,248,0.08)}
.section-title.accent{font-size:14px;font-weight:600;color:#a78bfa;border-color:#a78bfa}
.activity-section{flex:0 0 60vh;min-height:320px;background:var(--bg-panel);border:1px solid var(--border-color);border-radius:12px;padding:20px;display:flex;flex-direction:column;gap:12px}.heatmap-section{flex:0 0 60vh;min-height:320px;background:rgba(16,22,36,0.8);border:1px solid rgba(139,92,246,0.35);border-radius:12px;padding:20px;display:flex;flex-direction:column;gap:12px;box-shadow:0 0 20px rgba(139,92,246,0.06)}
.bottom-grid{display:grid;grid-template-columns:1fr 1fr;gap:16px;flex-shrink:0;min-height:480px}
.an-card{background:rgba(16,22,36,0.6);border:1px solid var(--border-color);border-radius:12px;padding:20px;display:flex;flex-direction:column;gap:12px}
.an-card h3{font-size:14px;font-weight:600;padding-left:10px;border-left:3px solid var(--accent);flex-shrink:0}
.funnel-card h3{color:var(--color-events);border-color:var(--color-events)}
.category-card h3{color:var(--color-hot);border-color:var(--color-hot)}
.chart-wrapper{flex:1;min-height:0;display:flex}
.chart-placeholder{flex:1;display:flex;flex-direction:column;align-items:center;justify-content:center;gap:8px;color:var(--text-muted);font-size:13px}
.empty-icon{font-size:28px;opacity:0.4}
.activity-card{flex:0 0 60vh;min-height:320px}.activity-section h3{font-size:14px;font-weight:600;color:var(--color-flink);padding-left:10px;border-left:3px solid var(--color-flink);flex-shrink:0}.an-datebar{display:flex;align-items:center;gap:10px;padding:0 4px 8px;flex-shrink:0}.ad-label{font-size:12px;color:var(--text-muted);font-weight:500}.an-datepicker{color-scheme:dark;background:rgba(30,41,59,0.6);border:1px solid rgba(56,189,248,0.3);border-radius:6px;padding:6px 12px;color:var(--text-primary);font-size:13px;font-family:Consolas,monospace;outline:none;cursor:pointer;min-width:150px}.an-datepicker:focus{border-color:var(--accent)}.an-hourpicker{background:rgba(30,41,59,0.6);border:1px solid var(--border-color);border-radius:6px;padding:6px 8px;color:var(--text-primary);font-size:13px;font-family:Consolas,monospace;outline:none;cursor:pointer}.an-hourpicker:focus{border-color:var(--accent)}.ad-live-btn{padding:5px 14px;background:linear-gradient(135deg,#0ea5e9,#38bdf8);border:none;border-radius:6px;color:#fff;font-size:12px;font-weight:600;cursor:pointer;box-shadow:0 0 8px rgba(56,189,248,0.4)}.ad-live-btn:hover{background:rgba(52,211,153,0.2)}.ad-live-badge{padding:5px 14px;background:linear-gradient(135deg,#059669,#34d399);border:none;border-radius:6px;color:#fff;font-size:12px;font-weight:600;box-shadow:0 0 8px rgba(52,211,153,0.4)}
</style>