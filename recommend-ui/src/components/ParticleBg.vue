<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'

const canvasRef = ref<HTMLCanvasElement | null>(null)

interface Particle {
  x: number; y: number; ox: number; oy: number; vx: number; vy: number; r: number; alpha: number; da: number
}

let particles: Particle[] = []
let animId = 0
let ctx: CanvasRenderingContext2D | null = null
let w = 0
let h = 0

function initParticles() {
  particles = []
  const count = Math.floor((w * h) / 18000)
  for (let i = 0; i < count; i++) {
    particles.push({
      x: Math.random() * w,
      y: Math.random() * h,
      vx: (Math.random() - 0.5) * 0.25,
      vy: (Math.random() - 0.5) * 0.25 - 0.05,
      r: Math.random() * 2.5 + 1.5,
      alpha: Math.random() * 0.3 + 0.1,
      da: (Math.random() - 0.5) * 0.006,
    })
  }
}

function resize() {
  const c = canvasRef.value
  if (!c) return
  w = window.innerWidth
  h = window.innerHeight
  c.width = w
  c.height = h
  ctx = c.getContext('2d')!
  initParticles()
}

function draw() {
  if (!ctx) return
  ctx.clearRect(0, 0, w, h)

  const cx = w / 2
  const cy = h / 2

  for (const p of particles) {
    p.x += p.vx
    p.y += p.vy
    p.alpha += p.da

    if (p.alpha > 0.6 || p.alpha < 0.05) p.da *= -1
    if (p.x < -10) p.x = w + 10
    if (p.x > w + 10) p.x = -10
    if (p.y < -10) p.y = h + 10
    if (p.y > h + 10) p.y = -10

    // Connection lines
    if (particles.indexOf(p) % 10 === 0) {
      const dx = cx - p.x
      const dy = cy - p.y
      const dist = Math.sqrt(dx * dx + dy * dy)
      if (dist < 350) {
        ctx.beginPath()
        ctx.moveTo(p.x, p.y)
        ctx.lineTo(cx, cy)
        ctx.strokeStyle = `rgba(56,189,248,${0.06 * (1 - dist / 350)})`
        ctx.stroke()
      }
    }

    // Particle glow
    const grad = ctx.createRadialGradient(p.x, p.y, 0, p.x, p.y, p.r * 3)
    grad.addColorStop(0, `rgba(56,189,248,${p.alpha})`)
    grad.addColorStop(0.3, `rgba(56,189,248,${p.alpha * 0.6})`)
    grad.addColorStop(1, 'rgba(56,189,248,0)')
    ctx.fillStyle = grad
    ctx.beginPath()
    ctx.arc(p.x, p.y, p.r * 3, 0, Math.PI * 2)
    ctx.fill()

    ctx.fillStyle = `rgba(255,255,255,${p.alpha * 0.6})`
    ctx.beginPath()
    ctx.arc(p.x, p.y, p.r * 0.7, 0, Math.PI * 2)
    ctx.fill()
  }

  animId = requestAnimationFrame(draw)
}

onMounted(() => {
  resize()
  animId = requestAnimationFrame(draw)
  window.addEventListener('resize', resize)
})
onUnmounted(() => {
  cancelAnimationFrame(animId)
  window.removeEventListener('resize', resize)
})
</script>

<template>
  <canvas ref="canvasRef" class="particle-bg" />
</template>

<style scoped>
.particle-bg {
  position: fixed;
  top: 0; left: 0;
  width: 100%; height: 100%;
  pointer-events: none;
  z-index: 3;
}
</style>
