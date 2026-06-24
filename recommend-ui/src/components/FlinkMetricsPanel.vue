<script setup lang="ts">
import { ref, computed, onMounted, watch, onUnmounted, nextTick } from 'vue'
import { useMetricsStore } from '@/stores/metrics'
import Icon from './common/Icon.vue'
import * as d3 from 'd3'

const metricsStore = useMetricsStore()
const svgRef = ref<SVGSVGElement | null>(null)

const hasData = computed(() => metricsStore.flink?.window_time > 0)

const themeColor = '#8b5cf6' // Flink purple

interface RadarItem {
  label: string
  value: number
  max: number
}

const radarData = computed<RadarItem[]>(() => {
  const f = metricsStore.flink
  if (!f || f.window_time <= 0) return []
  return [
    { label: '拉取频率(Hz)', value: f.m1 ?? 0, max: 50 },
    { label: '展平算子(条/秒)', value: f.m2 ?? 0, max: 100 },
    { label: '批记录数(条/批)', value: f.m3 ?? 0, max: 10 },
    { label: '拉取大小(B/次)', value: f.m4 ?? 0, max: 500 },
    { label: '窗口吞吐(条/秒)', value: f.m5 ?? 0, max: 100 },
  ]
})

function render() {
  const el = svgRef.value
  if (!el || !radarData.value || !radarData.value.length) return

  const svg = d3.select(el)
  svg.selectAll('*').remove()

  const width = el.clientWidth
  const height = el.clientHeight
  const cx = width / 2
  const cy = height / 2
  const R = Math.min(cx, cy) * 0.6
  const N = radarData.value.length
  const angleStep = (2 * Math.PI) / N

  // Grid levels
  const levels = [0.25, 0.5, 0.75, 1]
  for (const lv of levels) {
    const pts: [number, number][] = []
    for (let i = 0; i < N; i++) {
      const a = angleStep * i - Math.PI / 2
      pts.push([cx + R * lv * Math.cos(a), cy + R * lv * Math.sin(a)])
    }
    svg.append('polygon')
      .attr('points', pts.map(p => p.join(',')).join(' '))
      .attr('fill', 'none')
      .attr('stroke', 'rgba(139,92,246,0.08)')
     .attr('stroke-width', 1)
  }

 for (let i = 0; i < N; i++) {
    const a = angleStep * i - Math.PI / 2
    const x = cx + R * Math.cos(a)
    const y = cy + R * Math.sin(a)
    svg.append('line')
      .attr('x1', cx).attr('y1', cy)
      .attr('x2', x).attr('y2', y)
      .attr('stroke', 'rgba(139,92,246,0.08)')
      .attr('stroke-width', 1)

    const item = radarData.value[i]
    const shiftLabel = i === 1 || i === 4
    const labelAngle = shiftLabel ? a + (i === 1 ? -1 : 1) * angleStep / 4 : a
    const cosA = Math.cos(a)
    const labelOff = R + 14
    const lx = cx + labelOff * Math.cos(labelAngle)
    const ly = cy + labelOff * Math.sin(labelAngle)
    const txt = svg.append('text')
      .attr('x', lx)
      .attr('y', ly)
      .attr('text-anchor', lx < cx - 5 ? 'end' : lx > cx + 5 ? 'start' : 'middle')
      .attr('dominant-baseline', 'middle')
      .attr('fill', '#94a3b8')
      .attr('font-weight', '600')
      .attr('font-size', '10')
      .text(item.label)
  }

  // Data polygon
  const pts: [number, number][] = radarData.value.map((d, i) => {
    const a = angleStep * i - Math.PI / 2
    const r = R * Math.log(d.value + 1) / Math.log(Math.max(...radarData.value.map(x => x.value), 1) + 1)
    return [cx + r * Math.cos(a), cy + r * Math.sin(a)]
  })
  svg.append('polygon')
    .attr('points', pts.map(p => p.join(',')).join(' '))
    .attr('fill', 'rgba(139,92,246,0.12)')
    .attr('stroke', themeColor)
    .attr('stroke-width', 2)

  // Data points
  for (const [i, p] of pts.entries()) {
    svg.append('circle')
      .attr('cx', p[0]).attr('cy', p[1])
      .attr('r', 4)
      .attr('fill', themeColor)
      .attr('stroke', '#0a0e17')
      .attr('stroke-width', 2)

    // Value label
    svg.append('text')
      .attr('x', p[0]).attr('y', p[1] - 10)
      .attr('text-anchor', 'middle')
      .attr('fill', themeColor)
      .attr('font-size', '9')
      .attr('font-size', '11')
      .attr('font-weight', '600')
      .text(Math.round(radarData.value[i].value))
  }
}

let resizeTimer: ReturnType<typeof setTimeout>
let updateTimer: ReturnType<typeof setInterval>
function onResize() {
  clearTimeout(resizeTimer)
  resizeTimer = setTimeout(render, 100)
}

onMounted(() => {
  render()
  // Re-render every 3s to match polling interval
  updateTimer = setInterval(() => { if (hasData.value) render() }, 3000)
  setTimeout(() => render(), 1000)
  window.addEventListener('resize', onResize)
})
onUnmounted(() => {
  window.removeEventListener('resize', onResize)
  if (updateTimer) clearInterval(updateTimer)
})

watch(hasData, (v) => { if (v) requestAnimationFrame(() => render()) })
</script>

<template>
  <div class="panel">
    <div class="panel-title">Flink 算子指标</div>
    <div v-show="!hasData" class="empty-state">
      <Icon name="server" :size="28" />
      <span>Flink 集群未连接</span>
      <span class="empty-hint">部署到集群后自动获取指标</span>
    </div>
    <div v-show="hasData" class="radar-chart">
      <svg ref="svgRef" class="radar-svg" />
    </div>
  </div>
</template>

<style scoped>
.panel {
  overflow: visible !important;
  background: var(--bg-panel);
  border: 1px solid var(--border-color);
  border-radius: var(--radius);
  padding: 16px;
  display: flex;
  flex-direction: column;
  position: relative;
}
.panel-title {
  font-size: 14px; font-weight: 600; color: var(--color-flink);
  margin-bottom: 4px; padding-left: 12px;
  border-left: 3px solid var(--color-flink);
  flex-shrink: 0;
}
.radar-chart {
  flex: 1;
  min-height: 0;
  position: relative;
}
.radar-svg {
  width: 100%;
  height: 100%;
}
.empty-state {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  color: var(--text-muted);
  font-size: 13px;
}
.empty-hint { font-size: 11px; opacity: 0.6; }
</style>
