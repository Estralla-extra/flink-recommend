<script setup lang="ts">
import { ref, onMounted, watch, onUnmounted } from 'vue'
import type { CategoryItem } from '@/types'
import * as d3 from 'd3'

const props = defineProps<{ items: CategoryItem[] }>()
const svgRef = ref<SVGSVGElement | null>(null)

const colors = d3.scaleOrdinal(d3.schemeSet3)

function render() {
  const el = svgRef.value
  if (!el || !props.items.length) return

  const svg = d3.select(el)
  svg.selectAll('*').remove()

  const width = el.clientWidth
  const height = el.clientHeight
  const margin = { t: 12, b: 20, l: 56, r: 36 }
  const innerW = width - margin.l - margin.r
  const innerH = height - margin.t - margin.b
  const data = props.items.slice(0, 12)
  const maxVal = d3.max(data, d => d.count) || 1

  const yScale = d3.scaleBand()
    .domain(data.map((_, i) => i.toString()))
    .range([0, innerH])
    .padding(0.35)

  const xScale = d3.scaleLinear()
    .domain([0, maxVal * 1.1])
    .range([0, innerW])

  const g = svg.append('g').attr('transform', `translate(${margin.l},${margin.t})`)

  data.forEach((d, i) => {
    const y = yScale(i.toString())!
    const barH = yScale.bandwidth()
    const barW = xScale(d.count)

    g.append('rect')
      .attr('x', 0).attr('y', y)
      .attr('width', 0).attr('height', barH)
      .attr('fill', colors(String(i)))
      .attr('rx', 2).attr('opacity', 0.8)
      .on('mouseenter', function () {
        d3.select(this).attr('opacity', 1).attr('stroke', '#fff').attr('stroke-width', 1)
        svg.select('#ctip').remove()
        svg.append('text').attr('id', 'ctip')
          .attr('x', width / 2).attr('y', 14)
          .attr('text-anchor', 'middle')
          .attr('fill', '#38bdf8').attr('font-size', '12').attr('font-weight', '600')
          .text(d.categoryId + ' - ' + d.count + ' events')
      })
      .on('mouseleave', function () {
        d3.select(this).attr('opacity', 0.8).attr('stroke', 'none')
        svg.select('#ctip').remove()
      })
      .transition().duration(500).ease(d3.easeCubicOut)
      .attr('width', barW)

    g.append('text')
      .attr('x', 4).attr('y', y + barH / 2)
      .attr('dominant-baseline', 'middle')
      .attr('fill', '#e2e8f0').attr('font-size', '10')
      .text(`品类 ${d.categoryId}`)

    g.append('text')
      .attr('x', innerW).attr('y', y + barH / 2)
      .attr('text-anchor', 'end').attr('dominant-baseline', 'middle')
      .attr('fill', colors(String(i))).attr('font-size', '10').attr('font-weight', '600')
      .text(d.count)
  })
}

let resizeTimer: ReturnType<typeof setTimeout>
function onResize() { clearTimeout(resizeTimer); resizeTimer = setTimeout(render, 100) }
onMounted(() => { render(); window.addEventListener('resize', onResize) })
onUnmounted(() => { window.removeEventListener('resize', onResize) })
watch(() => props.items, () => render(), { deep: true })
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