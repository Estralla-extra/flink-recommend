<script setup lang="ts">
import { ref, onMounted, watch, onUnmounted } from 'vue'
import { useEventsStore } from '@/stores/events'
import { useMetricsStore } from '@/stores/metrics'
import * as d3 from 'd3'

const eventsStore = useEventsStore()
const metricsStore = useMetricsStore()
const svgRef = ref<SVGSVGElement | null>(null)

const colors = ['#fbbf24','#94a3b8','#d97706','#38bdf8','#6366f1','#14b8a6','#8b5cf6','#ec4899','#06b6d4','#22c55e']

function render() {
  const el = svgRef.value
  if (!el) return
  const data = eventsStore.hotItems.slice(0, 10)
  if (!data.length) return

  const svg = d3.select(el)
  svg.selectAll('*').remove()

  const width = el.clientWidth
  const height = el.clientHeight
  const margin = { t: 4, b: 4, l: 28, r: 36 }
  const innerW = width - margin.l - margin.r
  const innerH = height - margin.t - margin.b
  const maxVal = data[0].count || 1

  const yScale = d3.scaleBand()
    .domain(data.map((_, i) => i.toString())).range([0, innerH]).padding(0.3)
  const xScale = d3.scaleLinear().domain([0, maxVal * 1.05]).range([0, innerW])

  const g = svg.append('g').attr('transform', `translate(${margin.l},${margin.t})`)

  data.forEach((d, i) => {
    const y = yScale(i.toString())!
    const barH = yScale.bandwidth()
    const barW = xScale(d.count)
    const color = colors[i % colors.length]

    g.append('text').attr('x', -6).attr('y', y + barH / 2)
      .attr('text-anchor', 'end').attr('dominant-baseline', 'middle')
      .attr('fill', '#64748b').attr('font-size', '11').attr('font-weight', '700').text(i + 1)

    g.append('rect').attr('x', 0).attr('y', y).attr('width', innerW).attr('height', barH)
      .attr('fill', 'rgba(56,189,248,0.04)').attr('rx', 3)

    g.append('rect').attr('x', 0).attr('y', y).attr('width', 0).attr('height', barH)
      .attr('fill', color).attr('opacity', 0.75).attr('rx', 3)
      .transition().duration(600).ease(d3.easeCubicOut).attr('width', barW)

    g.append('text').attr('x', 6).attr('y', y + barH / 2)
      .attr('dominant-baseline', 'middle').attr('fill', '#e2e8f0').attr('font-size', '11')
      .attr('font-weight', '600').style('font-family', "'Consolas','Courier New',monospace")
      .text(d.itemId.slice(-6))

    g.append('text').attr('x', innerW).attr('y', y + barH / 2)
      .attr('text-anchor', 'end').attr('dominant-baseline', 'middle')
      .attr('fill', color).attr('font-size', '12').attr('font-weight', '700').text(d.count)
  })
}

let resizeTimer: ReturnType<typeof setTimeout>
function onResize() { clearTimeout(resizeTimer); resizeTimer = setTimeout(render, 100) }
onMounted(() => { render(); window.addEventListener('resize', onResize) })
onUnmounted(() => { window.removeEventListener('resize', onResize) })
watch(() => eventsStore.hotItems, () => render(), { deep: true })
</script>

<template>
  <div class="panel">
    <div class="panel-title">热点商品 Top-10</div>
    <div class="chart-box">
      <svg ref="svgRef" class="chart-svg" />
    </div>
    <div v-if="!eventsStore.hotItems.length" class="empty-overlay">等待事件数据...</div>
  </div>
</template>

<style scoped>
.panel { background: var(--bg-panel); border: 1px solid var(--border-color); border-radius: var(--radius); padding: 16px; display: flex; flex-direction: column; position: relative; min-height: 0; }
.panel-title { font-size: 14px; font-weight: 600; color: var(--color-hot); margin-bottom: 6px; padding-left: 12px; border-left: 3px solid var(--color-hot); flex-shrink: 0; }
.chart-box { flex: 1; min-height: 0; display: flex; }
.chart-svg { flex: 1; min-height: 0; }
.empty-overlay { position: absolute; inset: 0; display: flex; align-items: center; justify-content: center; color: var(--text-muted); font-size: 13px; pointer-events: none; }
</style>