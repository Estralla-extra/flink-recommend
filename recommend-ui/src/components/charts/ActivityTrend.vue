<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch } from 'vue'
import { fetchActivity } from '@/api'
import * as d3 from 'd3'
const props = defineProps<{ dateKey?: string }>()
const svgRef=ref<SVGSVGElement|null>(null)
var P=['#22c55e','#f59e0b','#f472b6']
var loading=ref(true)
var data=ref([])
function render(){
var el=svgRef.value;if(!el)return
var d=data.value;if(!d.length)return
var sv=d3.select(el);sv.selectAll('*').remove()
var w=el.clientWidth,h=el.clientHeight;if(w<=0||h<=0)return
var ml=60,mr=28,mt=80,mb=46,iw=w-ml-mr,ih=h-mt-mb
if(iw<=0||ih<=0)return
var stack=d3.stack().keys(['light','medium','heavy'])(d)
var yMax=d3.max(stack[stack.length-1],function(s){return s[1]})||1
var xS=d3.scaleLinear().domain([0,23]).range([0,iw])
var yS=d3.scaleLinear().domain([0,yMax*1.05]).range([ih,0])
var defs=sv.append('defs')
var g=sv.append('g').attr('transform','translate('+ml+','+mt+')')

// Legend on SVG root top-right, positioned so "Heavy" text fits
var lg=['Light','Medium','Heavy']
lg.forEach(function(l,i){
  var lx = Math.min(w-40, w-60-(2-i)*76)
  sv.append('circle').attr('cx',lx).attr('cy',8).attr('r',4).attr('fill',P[i])
  sv.append('text').attr('x',lx+8).attr('y',12).attr('fill','#94a3b8').attr('font-size','9')
    .style('font-family',"'Orbitron',monospace").text(l)
})

// Baseline reference line at yMax top
g.append('line')
  .attr('x1',0).attr('y1',yS(yMax*1.05))
  .attr('x2',iw).attr('y2',yS(yMax*1.05))
  .attr('stroke','rgba(56,189,248,0.15)').attr('stroke-width',1).attr('stroke-dasharray','4,4')

// Stacked areas
var aG=d3.area().x(function(dd){return xS(dd.data.hour)}).y0(function(dd){return yS(dd[0])}).y1(function(dd){return yS(dd[1])}).curve(d3.curveMonotoneX)
stack.forEach(function(s,i){
var c=P[i]
var gd=defs.append('linearGradient').attr('id','ag'+i).attr('x1','0').attr('y1','1').attr('x2','0').attr('y2','0')
gd.append('stop').attr('offset','0%').attr('stop-color',c).attr('stop-opacity','0.35')
gd.append('stop').attr('offset','100%').attr('stop-color',c).attr('stop-opacity','0.02')
g.append('path').attr('d',aG(s)).attr('fill','url(#ag'+i+')').attr('opacity',0).transition().delay(i*200+300).duration(800).ease(d3.easeCubicOut).attr('opacity',1)
g.append('path').attr('d',d3.line().x(function(dd){return xS(dd.data.hour)}).y(function(dd){return yS(dd[1])}).curve(d3.curveMonotoneX)(s)).attr('fill','none').attr('stroke',c).attr('stroke-width',1.5).attr('opacity',0).transition().delay(i*200+400).duration(800).ease(d3.easeCubicOut).attr('opacity',0.85)
g.selectAll('.d'+i).data(s).enter().append('circle').attr('class','d'+i).attr('cx',function(dd){return xS(dd.data.hour)}).attr('cy',function(dd){return yS(dd[1])}).attr('fill',c).attr('cursor','pointer').attr('r',0).transition().delay(i*200+600).duration(300).attr('r',3.5)
})

// === HOVER system: vertical line + tooltip on mousemove, click to pin ===
var hoverLine=g.append('line')
  .attr('y1',0).attr('y2',ih)
  .attr('stroke','rgba(56,189,248,0.45)').attr('stroke-width',1).attr('stroke-dasharray','4,3')
  .attr('opacity',0).attr('pointer-events','none')
var hoverTip=g.append('g').attr('opacity',0).attr('pointer-events','none')
var pinned=false;var pinData=null

function buildTip(hd,hour,cx){
  hoverTip.selectAll('*').remove()
  if(!hd)return
  var lines=['Hour: '+hour+':00','Light: '+hd.light,'Medium: '+hd.medium,'Heavy: '+hd.heavy]
  var cols=['#e2e8f0','#22c55e','#f59e0b','#f472b6']
  var padX=14,padY=22,lineH=20
  lines.forEach(function(l,i){
    hoverTip.append('text').attr('class','tip-t').attr('fill',cols[i]).attr('font-size','13')
      .style('font-family',"'Inter','PingFang SC','Microsoft YaHei',sans-serif").text(l)
  })
  var maxW=0;hoverTip.selectAll('.tip-t').each(function(){var w=this.getComputedTextLength();if(w>maxW)maxW=w})
  var tipW=Math.max(maxW+padX*2,100),tipH=(lines.length-1)*lineH+padY+18
  var tx=cx-tipW/2,ty=yS(hd.light+hd.medium+hd.heavy)-tipH-8
  if(tx<0)tx=4;if(tx+tipW>iw)tx=iw-tipW-4;if(ty<0)ty=yS(hd.light+hd.medium+hd.heavy)+12
  hoverTip.selectAll('.tip-t').attr('x',tx+padX).attr('y',function(_,i){return ty+padY+i*lineH})
  hoverTip.insert('rect','.tip-t').attr('x',tx).attr('y',ty).attr('width',tipW).attr('height',tipH).attr('rx',6)
    .attr('fill','#0f172a').attr('stroke','rgba(56,189,248,0.45)').attr('stroke-width',1.5)
}

g.append('rect')
  .attr('width',iw).attr('height',ih)
  .attr('fill','none').attr('pointer-events','all')
  .on('mousemove',function(event){
    var [mx]=d3.pointer(event)
    var hour=Math.round(xS.invert(mx))
    hour=Math.max(0,Math.min(23,hour))
    var hd=d.find(function(dd){return dd.hour===hour})
    if(!hd)return
    var cx=xS(hour)
    if(!pinned){
      hoverLine.attr('x1',cx).attr('x2',cx).attr('opacity',1)
      buildTip(hd,hour,cx)
      hoverTip.attr('opacity',1)
    }
  })
  .on('click',function(event){
    var [mx]=d3.pointer(event)
    var hour=Math.round(xS.invert(mx))
    hour=Math.max(0,Math.min(23,hour))
    var hd=d.find(function(dd){return dd.hour===hour})
    if(!hd)return
    var cx=xS(hour)
    if(pinned&&pinData&&pinData.hour===hour){
      // unpin
      pinned=false;pinData=null
      hoverLine.attr('opacity',0);hoverTip.attr('opacity',0)
    }else{
      pinned=true;pinData=hd
      hoverLine.attr('x1',cx).attr('x2',cx).attr('opacity',1)
      buildTip(hd,hour,cx)
      hoverTip.attr('opacity',1)
    }
  })
  .on('mouseleave',function(){
    if(!pinned){hoverLine.attr('opacity',0);hoverTip.attr('opacity',0)}
  })

// Grid / axis
for(var x=0;x<=23;x+=4){g.append('line').attr('x1',xS(x)).attr('y1',0).attr('x2',xS(x)).attr('y2',ih).attr('stroke','rgba(56,189,248,0.06)').attr('stroke-width',1)}
for(var x=0;x<=23;x+=4){g.append('text').attr('x',xS(x)).attr('y',ih+18).attr('text-anchor','middle').attr('fill','#64748b').attr('font-size','9').style('font-family',"'Orbitron',monospace").text(x+':00')}
var yT=5;for(var y=0;y<=yT;y++){g.append('text').attr('x',-8).attr('y',yS(y*yMax*1.05/yT)+4).attr('text-anchor','end').attr('fill','#64748b').attr('font-size','9').style('font-family',"'Orbitron',monospace").text(Math.round(y*yMax*1.05/yT))}
}
async function loadData(){
var now=new Date();var t=props.dateKey||(now.getFullYear()+String(now.getMonth()+1).padStart(2,'0')+String(now.getDate()).padStart(2,'0'))
var ps=[];for(var h=0;h<24;h++){ps.push(fetchActivity(t+String(h).padStart(2,'0')).catch(function(){return{buckets:[]}}))}
var rs=await Promise.all(ps)
data.value=rs.map(function(r,i){var light=0,medium=0,heavy=0;if(r&&r.buckets){r.buckets.forEach(function(b){if(b.label.indexOf('light')===0)light=b.count;else if(b.label.indexOf('medium')===0)medium=b.count;else heavy=b.count})};return{hour:i,light:light,medium:medium,heavy:heavy}});loading.value=false;render()
}
var rt,refreshTimer
function rs(){clearTimeout(rt);rt=setTimeout(render,100)}
onMounted(function(){loadData();refreshTimer=setInterval(loadData,30000);window.addEventListener('resize',rs)})
onUnmounted(function(){clearInterval(refreshTimer);window.removeEventListener('resize',rs)})
watch(function(){return data.value},function(){render()},{deep:true})
</script>
<template><svg ref="svgRef" style="width:100%;height:100%;display:block"/></template>
<style scoped>.tip{fill:#1e293b;stroke:rgba(56,189,248,0.25);stroke-width:1}</style>
