<script setup lang="ts">
import { ref, onMounted, watch, onUnmounted } from 'vue'
import type { HourlyHeatmap } from '@/types'
import * as d3 from 'd3'

const props = defineProps<{ data: HourlyHeatmap[] }>()
const svgRef = ref<SVGSVGElement | null>(null)

function render() {
  const el = svgRef.value
  if (!el || !props.data.length) return
  const svg = d3.select(el)
  svg.selectAll('*').remove()

  const width = el.clientWidth
  const height = el.clientHeight
  const margin = { t: 32, b: 28, l: 36, r: 8 }

  const hours = props.data.map(d => d.hour)
  const behaviors = ['pv', 'cart', 'fav', 'buy']
  const maxV = d3.max(props.data, d => d.total) || 1

  const cellW = Math.max(16, (width - margin.l - margin.r) / hours.length)
  const cellH = Math.max(20, (height - margin.t - margin.b) / behaviors.length)

  // Color scale
  const colorScale = d3.scaleSequentialLog(d3.interpolateViridis).domain([0.1, maxV])

  // Legend
  const legendH = 10
  const legendW = Math.min(200, width * 0.4)
  const legendX = width - legendW - margin.r
  const legendY = 6

  const defs = svg.append('defs')
  const lg = defs.append('linearGradient').attr('id', 'heat-legend')
  for (let i = 0; i <= 10; i++) {
    const pct = i * 10
    const val = maxV * (pct / 100)
    const c = colorScale(Math.max(val, 0.1))
    lg.append('stop').attr('offset', `${pct}%`).attr('stop-color', c)
  }

  svg.append('rect')
    .attr('x', legendX).attr('y', legendY)
    .attr('width', legendW).attr('height', legendH)
    .attr('fill', 'url(#heat-legend)').attr('rx', 2)

  svg.append('text')
    .attr('x', legendX).attr('y', legendY + legendH + 10)
    .attr('fill', '#64748b').attr('font-size', '8')
    .text('0')

  svg.append('text')
    .attr('x', legendX + legendW).attr('y', legendY + legendH + 10)
    .attr('text-anchor', 'end')
    .attr('fill', '#64748b').attr('font-size', '8')
    .text(maxV > 0 ? maxV.toString() : '')

  // Cells
  behaviors.forEach((b, row) => {
    hours.forEach((h, col) => {
      const hd = props.data.find(d => d.hour === h)
      const val = hd ? (hd as any)[b] as number : 0
      const x = margin.l + col * cellW
      const y = margin.t + row * cellH

      svg.append('rect')
        .attr('x', x).attr('y', y)
        .attr('width', cellW - 1).attr('height', cellH - 1)
        .attr('fill', val > 0 ? colorScale(val) : 'rgba(30,41,59,0.4)')
        .attr('rx', 1)

      // Value label (only if cell is wide enough)
      if (cellW > 28 && val > 0) {
        svg.append('text')
          .attr('x', x + (cellW - 1) / 2)
          .attr('y', y + (cellH - 1) / 2)
          .attr('text-anchor', 'middle')
          .attr('dominant-baseline', 'middle')
          .attr('fill', val > maxV * 0.5 ? '#fff' : '#e2e8f0')
          .attr('font-size', '9').attr('font-weight', '600')
          .text(val > 999 ? '999+' : val.toString())
      }

      svg.append('title').text(`${h}:00 ${b.toUpperCase()} = ${val}`)
    })
  })

  // Y axis labels
  behaviors.forEach((b, i) => {
    svg.append('text')
      .attr('x', margin.l - 6)
      .attr('y', margin.t + i * cellH + cellH / 2)
      .attr('text-anchor', 'end')
      .attr('dominant-baseline', 'middle')
      .attr('fill', '#94a3b8').attr('font-size', '10').attr('font-weight', '600')
      .text(b.toUpperCase())
  })

  // X axis labels (every 2 hours)
  hours.forEach((h, i) => {
    if (parseInt(h) % 2 !== 0) return
    svg.append('text')
      .attr('x', margin.l + i * cellW + cellW / 2)
      .attr('y', height - 6)
      .attr('text-anchor', 'middle')
      .attr('fill', '#64748b').attr('font-size', '9')
      .text(`${h}:00`)
  })
}

let resizeTimer: ReturnType<typeof setTimeout>
function onResize() { clearTimeout(resizeTimer); resizeTimer = setTimeout(render, 100) }
onMounted(() => { render(); window.addEventListener('resize', onResize) })
onUnmounted(() => { window.removeEventListener('resize', onResize) })
watch(() => props.data, render, { deep: true })
</script>

<template>
  <div class="chart-box">
    <svg ref="svgRef" class="chart-svg" />
  </div>
</template>

<style scoped>
  .chart-box { flex: 1; min-height: 0; display: flex; }
  .chart-svg { flex: 1; min-height: 0; }
</style>
