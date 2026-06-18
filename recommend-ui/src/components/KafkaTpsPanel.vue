<script setup lang="ts">
import { ref, computed, onMounted, watch, onUnmounted } from 'vue'
import { useMetricsStore } from '@/stores/metrics'
import * as d3 from 'd3'

const metricsStore = useMetricsStore()
const svgRef = ref<SVGSVGElement | null>(null)

const themeColor = '#14b8a6' // Kafka teal

function render() {
  const el = svgRef.value
  if (!el) return

  const data = metricsStore.tpsHistory
  if (data.length < 2) return

  const svg = d3.select(el)
  svg.selectAll('*').remove()

  const width = el.clientWidth
  const height = el.clientHeight
  const margin = { top: 8, right: 8, bottom: 20, left: 32 }
  const innerW = width - margin.left - margin.right
  const innerH = height - margin.top - margin.bottom

  const maxVal = d3.max(data) || 10
  const xScale = d3.scaleLinear().domain([0, data.length - 1]).range([0, innerW])
  const yScale = d3.scaleLinear().domain([0, maxVal * 1.15]).range([innerH, 0])

  const areaGen = d3.area<number>()
    .x((_, i) => xScale(i))
    .y0(yScale(0))
    .y1(d => yScale(d))

  const lineGen = d3.line<number>()
    .x((_, i) => xScale(i))
    .y(d => yScale(d))
    .curve(d3.curveMonotoneX)

  const g = svg.append('g').attr('transform', `translate(${margin.left},${margin.top})`)

  // Area fill
  g.append('path')
    .datum(data)
    .attr('fill', `rgba(20, 184, 166, 0.15)`)
    .attr('d', areaGen)

  // Line
  g.append('path')
    .datum(data)
    .attr('fill', 'none')
    .attr('stroke', themeColor)
    .attr('stroke-width', 2)
    .attr('d', lineGen)

  // Current value label
  const lastVal = data[data.length - 1]
  g.append('text')
    .attr('x', innerW)
    .attr('y', yScale(lastVal) - 8)
    .attr('text-anchor', 'end')
    .attr('fill', themeColor)
    .attr('font-size', '20')
    .attr('font-weight', '700')
    .text(lastVal)

  // X axis (time labels)
  const xAxis = d3.axisBottom(xScale)
    .ticks(5)
    .tickFormat(() => '')
  g.append('g')
    .attr('transform', `translate(0,${innerH})`)
    .call(xAxis)
    .selectAll('text')
    .remove()

  // Y axis
  const yAxis = d3.axisLeft(yScale)
    .ticks(4)
    .tickSize(-innerW)
    .tickFormat(d => d.toString())
  g.append('g')
    .call(yAxis)
    .selectAll('text')
    .attr('fill', '#64748b')
    .attr('font-size', '9')
  g.selectAll('.domain').remove()
  g.selectAll('.tick line').attr('stroke', 'rgba(56,189,248,0.08)')
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

watch(() => metricsStore.tpsHistory, () => render(), { deep: true })
</script>

<template>
  <div class="panel">
    <div class="panel-title">Kafka TPS 趋势</div>
    <div class="chart-area">
      <svg ref="svgRef" class="area-svg" />
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
  font-size: 14px; font-weight: 600; color: var(--color-kafka);
  margin-bottom: 6px; padding-left: 12px;
  border-left: 3px solid var(--color-kafka);
  flex-shrink: 0;
}
.chart-area {
  flex: 1;
  min-height: 0;
}
.area-svg {
  width: 100%;
  height: 100%;
}
</style>
