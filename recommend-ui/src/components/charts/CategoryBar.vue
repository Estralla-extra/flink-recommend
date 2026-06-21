<script setup lang="ts">
import { ref, onMounted, watch, onUnmounted } from 'vue'
import * as d3 from 'd3'
import type { CategoryItem } from '@/types'
const props = defineProps<{ items: CategoryItem[] }>()
const svgRef = ref<SVGSVGElement | null>(null)
var P=['#fbbf24','#94a3b8','#d97706','#38bdf8','#6366f1','#14b8a6','#8b5cf6','#ec4899','#06b6d4','#22c55e']
function render(){
  var el=svgRef.value;if(!el)return
  var d=props.items;if(!d.length)return
  var sv=d3.select(el);sv.selectAll('*').remove()
  var w=el.clientWidth,h=el.clientHeight;if(w<=0||h<=0)return
  var ml=56,mr=48,mt=12,mb=12,iw=w-ml-mr,ih=h-mt-mb
  if(iw<=0||ih<=0)return
  var max=d[0].count||1
  var bh=Math.min(ih/d.length*0.7,26)
  var ySc=d3.scaleBand().domain(d.map(function(_,i){return i.toString()})).range([0,ih]).padding(0.3)
  var xSc=d3.scaleLinear().domain([0,max*1.15]).range([0,iw])
  var g=sv.append('g').attr('transform','translate('+ml+','+mt+')')
  d.forEach(function(di,i){
    var y=ySc(i.toString())!,bw=Math.max(xSc(di.count)||0,4),bh2=ySc.bandwidth(),c=P[i%10]
    g.append('rect').attr('x',0).attr('y',y+1).attr('width',iw).attr('height',bh2-2).attr('fill','#0f1724').attr('rx',3)
    g.append('rect').attr('x',0).attr('y',y).attr('width',0).attr('height',bh2).attr('fill',c).attr('opacity',0.85).attr('rx',3).attr('class','br').transition().delay(i*50).duration(600).ease(d3.easeCubicOut).attr('width',bw)
    g.append('rect').attr('x',0).attr('y',y).attr('width',0).attr('height',2).attr('fill',c).attr('opacity',0.7).attr('rx',1).attr('class','br').transition().delay(i*50).duration(600).ease(d3.easeCubicOut).attr('width',bw)
    g.append('text').attr('x',-16).attr('y',y+bh2/2).attr('text-anchor','middle').attr('dy','0.35em').attr('fill','#94a3b8').attr('font-size','10').attr('font-weight','700').text(i+1)
    var idTxt=String(di.categoryId).slice(-8),pillH=bh2-4
    g.append('rect').attr('x',10).attr('y',y+2).attr('width',72).attr('height',pillH).attr('fill','rgba(30,41,59,0.7)').attr('stroke',c).attr('stroke-opacity',0.2).attr('stroke-width',1).attr('rx',pillH/2)
    g.append('text').attr('x',46).attr('y',y+bh2/2).attr('text-anchor','middle').attr('dy','0.35em').attr('fill','#e2e8f0').attr('font-size','10').attr('font-weight','600').style('font-family',"'Orbitron','Consolas',monospace").style('letter-spacing','1px').style('text-shadow','0 0 4px rgba(0,0,0,0.9)').text(idTxt)
    var ct=g.append('text').attr('x',iw).attr('y',y+bh2/2).attr('text-anchor','end').attr('dy','0.35em').attr('fill',c).attr('font-size','13').attr('font-weight','700').style('font-family',"'Orbitron','Consolas',monospace").style('letter-spacing','2px').style('text-shadow','0 0 6px ' + c + '80').text('0')
    ct.transition().delay(i*50+350).duration(500).ease(d3.easeQuadOut).tween('text',function(){var tgt=di.count,ip=d3.interpolate(0,tgt),el=d3.select(this);return function(t){el.text(Math.round(ip(t)))}})
  })
}
var rt
function rs(){clearTimeout(rt);rt=setTimeout(render,100)}
onMounted(function(){render();window.addEventListener('resize',rs)})
onUnmounted(function(){window.removeEventListener('resize',rs)})
watch(function(){return props.items},function(){render()},{deep:true})
</script>
<template><div class="chart-box"><svg ref="svgRef" class="chart-svg" /></div></template>
<style scoped>
.chart-box{flex:1;min-height:0;display:flex}
.chart-svg{flex:1;min-height:0}
</style>