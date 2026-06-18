<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import type { FunnelStep } from '@/types'
import * as d3 from 'd3'

const props = defineProps<{ steps: FunnelStep[] }>()
const svgRef = ref<SVGSVGElement | null>(null)

function render() {
  const el = svgRef.value
  if (!el || !props.steps.length) return

  const svg = d3.select(el)
  svg.selectAll('*').remove()

  const width = el.clientWidth
  const height = el.clientHeight
  const margin = { t: 16, b: 24, l: 16, r: 16 }

  const data = props.steps
  const maxVal = d3.max(data, d => d.count) || 1

  const barW = Math.max(24, (width - margin.l - margin.r) / data.length - 8)
  const xScale = d3.scaleBand().domain(data.map(d => d.behavior)).range([margin.l, width - margin.r]).padding(0.25)
  const yScale = d3.scaleLinear().domain([0, maxVal * 1.2]).range([height - margin.b, margin.t])

  const colors = d3.scaleOrdinal<string>().domain(['pv','cart','fav','buy']).range(['#06b6d4','#f59e0b','#8b5cf6','#34d399'])

  // Bars with funnel width reduction
  data.forEach((d, i) => {
    const x = xScale(d.behavior)!
    const y = yScale(d.count)
    const w = xScale.bandwidth() * (1 - i * 0.08)
    const cx = x + xScale.bandwidth() / 2

    svg.append('rect')
      .attr('x', cx - w / 2)
      .attr('y', y)
      .attr('width', w)
      .attr('height', yScale(0) - y)
      .attr('fill', colors(d.behavior))
      .attr('rx', 3)
      .attr('opacity', 0.85)

    svg.append('text')
      .attr('x', cx)
      .attr('y', y - 8)
      .attr('text-anchor', 'middle')
      .attr('fill', '#e2e8f0')
      .attr('font-size', '11')
      .attr('font-weight', '600')
      .text(d.count)

    // rate arrow line
    if (i > 0) {
      const prevY = yScale(data[i - 1].count)
      svg.append('text')
        .attr('x', cx)
        .attr('y', (y + prevY) / 2)
        .attr('text-anchor', 'middle')
        .attr('fill', '#94a3b8')
        .attr('font-size', '10')
        .text(`${d.rate}%`)
    }
  })

  // Axis labels
  data.forEach(d => {
    const x = xScale(d.behavior)!
    svg.append('text')
      .attr('x', x + xScale.bandwidth() / 2)
      .attr('y', height - 4)
      .attr('text-anchor', 'middle')
      .attr('fill', '#94a3b8')
      .attr('font-size', '11')
      .text(d.behavior.toUpperCase())
  })
}

onMounted(render)
watch(() => props.steps, render, { deep: true })
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
