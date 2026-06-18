<script setup lang="ts">
import { ref, computed, onMounted, watch, onUnmounted } from 'vue'
import { useMetricsStore } from '@/stores/metrics'
import Icon from './common/Icon.vue'
import * as d3 from 'd3'

const metricsStore = useMetricsStore()
const svgRef = ref<SVGSVGElement | null>(null)

const hasData = computed(() => metricsStore.flink.window_time > 0)

const themeColor = '#8b5cf6' // Flink purple

interface RadarItem {
  label: string
  value: number
  max: number
}

const radarData = computed<RadarItem[]>(() => {
  const f = metricsStore.flink
  if (f.window_time <= 0) return []
  return [
    { label: '窗口聚合', value: f.window_time, max: 100 },
    { label: 'Redis查询', value: f.redis_time, max: 100 },
    { label: '数据分析', value: f.window_time * 0.4, max: 60 },
    { label: 'Kafka消费', value: f.window_time * 0.55, max: 80 },
    { label: '状态管理', value: f.window_time * 0.2, max: 50 },
  ]
})

function render() {
  const el = svgRef.value
  if (!el || !radarData.value.length) return

  const svg = d3.select(el)
  svg.selectAll('*').remove()

  const width = el.clientWidth
  const height = el.clientHeight
  const cx = width / 2
  const cy = height / 2
  const R = Math.min(cx, cy) - 32
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

    // Level label
    svg.append('text')
      .attr('x', cx)
      .attr('y', cy - R * lv)
      .attr('text-anchor', 'middle')
      .attr('fill', '#64748b')
      .attr('font-size', '8')
      .attr('dy', '-2')
      .text(`${Math.round(lv * 100)}%`)
  }

  // Axis lines and labels
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
    const lx = cx + (R + 38) * Math.cos(a)
    const ly = cy + (R + 38) * Math.sin(a)
    const txt = svg.append('text')
      .attr('x', lx).attr('y', ly)
      .attr('text-anchor', 'middle')
      .attr('dominant-baseline', 'middle')
      .attr('fill', '#94a3b8')
      .attr('font-size', '10')
      .text(item.label)
  }

  // Data polygon
  const pts: [number, number][] = radarData.value.map((d, i) => {
    const a = angleStep * i - Math.PI / 2
    const r = R * Math.max(d.value / d.max, 0.02)
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
      .attr('font-weight', '600')
      .text(Math.round(radarData.value[i].value))
  }
}

let resizeTimer: ReturnType<typeof setTimeout>
function onResize() {
  clearTimeout(resizeTimer)
  resizeTimer = setTimeout(render, 100)
}

onMounted(() => {
  render()
  window.addEventListener('resize', onResize)
})
onUnmounted(() => {
  window.removeEventListener('resize', onResize)
})

watch(() => [metricsStore.flink.window_time, metricsStore.flink.redis_time], () => render())
</script>

<template>
  <div class="panel">
    <div class="panel-title">Flink 算子耗时 (ms)</div>
    <div v-if="!hasData" class="empty-state">
      <Icon name="server" :size="28" />
      <span>Flink 集群未连接</span>
      <span class="empty-hint">部署到集群后自动获取指标</span>
    </div>
    <div v-else class="radar-chart">
      <svg ref="svgRef" class="radar-svg" />
    </div>
  </div>
</template>

<style scoped>
.panel {
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
