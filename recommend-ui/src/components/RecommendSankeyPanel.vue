<script setup lang="ts">
import { onMounted, onUnmounted, ref } from 'vue'
import { useRecommendStore } from '@/stores/recommend'
import Icon from './common/Icon.vue'

const recommendStore = useRecommendStore()

const flinkOk = ref<boolean | null>(null)
const realtimeOk = ref<boolean | null>(null)
const hotOk = ref<boolean | null>(null)
const flinkFirstCheck = ref(0)
const WARMUP = 90
let timer: ReturnType<typeof setInterval> | null = null

const flinkWarming = ref(true)
let warmTimer: ReturnType<typeof setInterval> | null = null

function updateWarm() {
  if (flinkOk.value === null) { flinkWarming.value = true; return }
  if (flinkOk.value !== false) { flinkWarming.value = false; return }
  if (!flinkFirstCheck.value) { flinkWarming.value = true; return }
  flinkWarming.value = (Date.now() - flinkFirstCheck.value) / 1000 < WARMUP
}

const PROBES = {
  flink:    { user: 'heartbeat_user_001', item: '' },
  realtime: { user: 'temp_rt_user_001',   item: '1535294' },
  hot:      { user: '__nonexistent__',     item: '0' },
}

async function checkFlink() {
  const r = await recommendStore.probeForUser(PROBES.flink.user)
  flinkOk.value = r.flinkHit
  if (!flinkFirstCheck.value) flinkFirstCheck.value = Date.now()
  updateWarm()
}
async function checkRealtime() { const r = await recommendStore.probeForUser(PROBES.realtime.user, PROBES.realtime.item); realtimeOk.value = r.realtimeHit }
async function checkHot() { const r = await recommendStore.probeForUser(PROBES.hot.user, PROBES.hot.item); hotOk.value = r.hotUsed }
async function checkAll() { await checkFlink(); await checkRealtime(); await checkHot() }

onMounted(() => { checkAll(); timer = setInterval(checkAll, 10000); warmTimer = setInterval(updateWarm, 1000) })
onUnmounted(() => { if (timer) clearInterval(timer); if (warmTimer) clearInterval(warmTimer) })
</script>

<template>
  <div class="panel">
    <div class="panel-title" style="border-color: var(--color-health); color: var(--color-health)">链路健康监控</div>
    <div class="status-card">
      <div class="sc-source" :class="{ active: flinkOk === true, fail: flinkOk === false }">
        <span class="sc-dot" :class="flinkOk === true ? 'on' : flinkOk === false && !flinkWarming ? 'err' : flinkWarming ? 'warm' : ''" />
        <div class="sc-info">
          <span class="sc-label"><Icon name="layers" :size="13" /> Flink 窗口缓存</span>
          <span class="sc-desc">Redis: recommend:user:{{ PROBES.flink.user }}</span>
        </div>
        <span class="sc-status">
          <span v-if="flinkOk === true" class="badge badge-ok">正常</span>
          <span v-else-if="flinkWarming" class="badge badge-warm">预热中</span>
          <span v-else-if="flinkOk === false" class="badge badge-err">异常</span>
          <span v-else class="badge badge-off">检测中</span>
        </span>
      </div>
      <div class="sc-source" :class="{ active: realtimeOk === true, fail: realtimeOk === false }">
        <span class="sc-dot" :class="realtimeOk === true ? 'on' : realtimeOk === false ? 'err' : ''" />
        <div class="sc-info">
          <span class="sc-label"><Icon name="zap" :size="13" /> 实时相似召回</span>
          <span class="sc-desc">Redis: sim:{{ PROBES.realtime.item }}(与用户无关)</span>
        </div>
        <span class="sc-status">
          <span v-if="realtimeOk === true" class="badge badge-ok">正常</span>
          <span v-else-if="realtimeOk === false" class="badge badge-err">异常</span>
          <span v-else class="badge badge-off">检测中</span>
        </span>
      </div>
      <div class="sc-source" :class="{ active: hotOk === true, fail: hotOk === false }">
        <span class="sc-dot" :class="hotOk === true ? 'on' : hotOk === false ? 'err' : ''" />
        <div class="sc-info">
          <span class="sc-label"><Icon name="flame" :size="13" /> 热门兜底</span>
          <span class="sc-desc">两路全空时降级，读固定热门列表</span>
        </div>
        <span class="sc-status">
          <span v-if="hotOk === true" class="badge badge-ok">正常</span>
          <span v-else-if="hotOk === false" class="badge badge-err">异常</span>
          <span v-else class="badge badge-off">检测中</span>
        </span>
      </div>
    </div>
  </div>
</template>

<style scoped>
.panel { background: var(--bg-panel); border: 1px solid var(--border-color); border-radius: var(--radius); padding: 16px; display: flex; flex-direction: column; position: relative; min-height: 0; }
.panel-title { font-size: 14px; font-weight: 600; color: var(--accent); margin-bottom: 14px; padding-left: 12px; border-left: 3px solid var(--accent); letter-spacing: 0.5px; flex-shrink: 0; }
  .status-card { flex: 1; display: flex; flex-direction: column; gap: 32px; justify-content: center; }
.sc-source { display: flex; align-items: center; gap: 12px; padding: 24px 20px; background: rgba(30,41,59,0.6); border: 1px solid var(--border-color); border-radius: 10px; transition: all 0.4s; }
.sc-source.active { border-color: rgba(52,211,153,0.45); background: rgba(52,211,153,0.04); }
.sc-source.fail { border-color: rgba(248,113,113,0.45); background: rgba(248,113,113,0.04); }
.sc-dot { width: 9px; height: 9px; border-radius: 50%; background: #475569; flex-shrink: 0; transition: all 0.3s; }
.sc-dot.on { background: var(--success); box-shadow: 0 0 8px var(--success); }
.sc-dot.err { background: var(--danger); box-shadow: 0 0 8px var(--danger); }
.sc-dot.warm { background: var(--accent); box-shadow: 0 0 8px var(--accent); }
.sc-info { flex: 1; display: flex; flex-direction: column; gap: 2px; min-width: 0; }
.sc-label { font-size: 13px; font-weight: 600; color: var(--text-primary); display: flex; align-items: center; gap: 6px; }
.sc-desc { font-size: 11px; color: var(--text-muted); }
.sc-status { font-size: 12px; font-weight: 600; white-space: nowrap; }
.badge { padding: 3px 10px; border-radius: 4px; font-size: 11px; font-weight: 600; }
.badge-ok  { background: rgba(52,211,153,0.12); color: var(--success); }
.badge-err { background: rgba(248,113,113,0.12); color: var(--danger); }
.badge-warm { background: rgba(56,189,248,0.12); color: var(--accent); }
.badge-off { background: rgba(100,116,139,0.10); color: var(--text-muted); }
</style>
