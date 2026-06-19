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
  const margin = { t: 16, b: 16, l: 48, r: 48 }
  const innerW = width - margin.l - margin.r
  const innerH = height - margin.t - margin.b

  const data = props.steps
  const maxVal = d3.max(data, d => d.count) || 1
  const bandH = innerH / data.length
  const colors = ['#06b6d4','#f59e0b','#8b5cf6','#34d399']

  data.forEach((d, i) => {
    const y = margin.t + i * bandH
    const barH = bandH * 0.7
    const barW = (d.count / maxVal) * innerW
    const cx = margin.l + innerW / 2

    // Bar
    svg.append('rect')
      .attr('x', cx - barW / 2).attr('y', y + (bandH - barH) / 2)
      .attr('width', barW).attr('height', barH)
      .attr('fill', colors[i % colors.length])
      .attr('opacity', 0.85).attr('rx', 2)

    // Connecting trapezoid
    if (i < data.length - 1) {
      const nextW = (data[i + 1].count / maxVal) * innerW
      const nextY = margin.t + (i + 1) * bandH
      svg.append('polygon')
        .attr('points', [
          [cx - barW / 2, y + (bandH - barH) / 2 + barH],
          [cx + barW / 2, y + (bandH - barH) / 2 + barH],
          [cx + nextW / 2, nextY + (bandH - barH) / 2],
          [cx - nextW / 2, nextY + (bandH - barH) / 2],
        ].map(p => p.join(',')).join(' '))
        .attr('fill', colors[i % colors.length]).attr('opacity', 0.12)
    }

    // Behavior label (left)
    svg.append('text')
      .attr('x', margin.l - 8).attr('y', y + bandH / 2)
      .attr('text-anchor', 'end').attr('dominant-baseline', 'middle')
      .attr('fill', '#94a3b8').attr('font-size', '11').attr('font-weight', '600')
      .text(d.behavior.toUpperCase())

    // Count (inside bar)
    svg.append('text')
      .attr('x', cx).attr('y', y + bandH / 2)
      .attr('text-anchor', 'middle').attr('dominant-baseline', 'middle')
      .attr('fill', '#fff').attr('font-size', '14').attr('font-weight', '700')
      .text(d.count)
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