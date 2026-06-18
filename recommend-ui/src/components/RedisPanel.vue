<script setup lang="ts">
import { computed } from 'vue'
import { useMetricsStore } from '@/stores/metrics'

const metricsStore = useMetricsStore()

const hitRate = computed(() => metricsStore.hitRate)
const gaugeR = 56
const gaugeC = 2 * Math.PI * gaugeR
const arcRatio = 0.75

const gaugeColor = computed(() => {
  const v = hitRate.value
  if (v >= 80) return '#34d399'
  if (v >= 50) return '#fbbf24'
  return '#f87171'
})

const fillLen = computed(() => gaugeC * arcRatio * hitRate.value / 100)
const gapLen = computed(() => gaugeC - fillLen.value)

const ticks = computed(() => {
  const cx = 80; const cy = 80; const r = gaugeR
  const startAngle = 135
  const totalArc = 270
  const inner = r - 10; const outer = r + 10
  return [0, 25, 50, 75, 100].map(pct => {
    const deg = startAngle + totalArc * pct / 100
    const rad = (deg * Math.PI) / 180
    const x1 = cx + inner * Math.cos(rad)
    const y1 = cy + inner * Math.sin(rad)
    const x2 = cx + outer * Math.cos(rad)
    const y2 = cy + outer * Math.sin(rad)
    return { x1: +x1.toFixed(1), y1: +y1.toFixed(1), x2: +x2.toFixed(1), y2: +y2.toFixed(1), pct }
  })
})
</script>

<template>
 <div class="panel">
    <div class="panel-title" style="border-color: var(--color-redis); color: var(--color-redis)">Redis 仪表盘</div>

    <div class="gauge-section">
      <svg viewBox="0 0 160 150" class="g-svg">
        <circle cx="80" cy="80" :r="gaugeR"
          fill="none"
          stroke="rgba(56,189,248,0.08)"
          stroke-width="12"
          :stroke-dasharray="gaugeC * arcRatio + ' ' + gaugeC * (1 - arcRatio)"
          stroke-dashoffset="0"
          transform="rotate(135 80 80)"
          stroke-linecap="round"
        />
        <line v-for="t in ticks" :key="t.pct"
          :x1="t.x1" :y1="t.y1" :x2="t.x2" :y2="t.y2"
          stroke="rgba(56,189,248,0.25)" stroke-width="1.5"
        />
        <text v-for="t in ticks" :key="'l'+t.pct"
          :x="t.x2" :y="t.y2"
          text-anchor="middle"
          :dy="-6"
          font-size="7"
          fill="#64748b"
        >{{ t.pct }}</text>
        <circle cx="80" cy="80" :r="gaugeR"
          fill="none"
          :stroke="gaugeColor"
          stroke-width="12"
          :stroke-dasharray="fillLen + ' ' + gapLen"
          stroke-dashoffset="0"
          transform="rotate(135 80 80)"
          stroke-linecap="round"
          style="transition: stroke-dasharray 0.7s ease, stroke 0.5s ease"
        />
        <text x="80" y="78" text-anchor="middle" class="g-pct">{{ hitRate.toFixed(1) }}%</text>
        <text x="80" y="94" text-anchor="middle" class="g-sub">命中率</text>
      </svg>
    </div>

    <div class="stats">
      <div class="stat">
        <span class="slabel">内存占用</span>
        <span class="sval">{{ metricsStore.redis.used_memory }}</span>
      </div>
      <div class="stat">
        <span class="slabel">连接状态</span>
        <span class="sval" :style="{ color: hitRate > 0 ? 'var(--success)' : 'var(--danger)' }">
          {{ hitRate > 0 ? 'CONNECTED' : 'DISCONNECTED' }}
        </span>
      </div>
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
  min-height: 0;
}
.panel-title {
  font-size: 14px; font-weight: 600; color: var(--accent);
  margin-bottom: 8px; padding-left: 12px;
  border-left: 3px solid var(--accent);
  letter-spacing: 0.5px;
  flex-shrink: 0;
}

.gauge-section {
  display: flex;
  justify-content: center;
  flex-shrink: 0;
  margin-top: 32px;
  flex: 1;
}
.g-svg {
  width: 100%;
  max-width: 200px;
}
.g-pct {
  font-size: 22px; font-weight: 700; fill: var(--text-primary);
}
.g-sub {
  font-size: 10px; fill: var(--text-muted);
}

.stats {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 0 4px;
  margin-top: 16px;
}
.stat {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 5px 10px;
  background: rgba(56,189,248,0.04);
  border-radius: 4px;
}
.slabel { font-size: 12px; color: var(--text-muted); }
.sval { font-size: 12px; color: var(--accent); font-weight: 600; font-variant-numeric: tabular-nums; }
</style>
