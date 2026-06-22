<script setup lang="ts">
import { ref, computed } from "vue"
import Icon from "./common/Icon.vue"

const props = defineProps<{ store: any }>()

const itemInput = ref("")
const loading = ref(false)
const queryTime = ref(0)

const realtimeList = computed(() => props.store.sourceLists?.realtime || [])
const hotList = computed(() => props.store.sourceLists?.hot || [])
const timeStr = computed(() => queryTime.value ? new Date(queryTime.value).toLocaleString("zh-CN") : "")
const hasResults = computed(() => props.store.items.length > 0)

async function search() {
  if (!itemInput.value.trim()) return
  loading.value = true
  try {
    await props.store.fetchForUser("__item__", itemInput.value.trim())
    queryTime.value = Date.now()
  } catch {} finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="ir-root">
    <div class="ir-search">
      <div class="ir-row">
        <Icon name="cart" :size="16" />
        <input v-model="itemInput" class="ir-input" placeholder="输入商品ID" @keyup.enter="search" :disabled="loading" />
        <button class="ir-btn" @click="search" :disabled="loading"><Icon name="search" :size="12" /> 查询</button>
      </div>
    </div>

    <div v-if="hasResults" class="ir-card">
      <div class="ir-card-head">
        <span class="ir-card-title">实时相似商品</span>
        <span class="ir-card-flag" :class="props.store.realtimeHit ? 'on' : 'off'">{{ props.store.realtimeHit ? "已命中" : "未命中" }}</span>
      </div>
      <div v-for="(item, i) in realtimeList" :key="i" class="ir-row-item">
        <span class="ri-rank">{{ i+1 }}</span>
        <span class="ri-id">{{ item.itemId }}</span>
        <span class="ri-score on">{{ item.score.toFixed(2) }}</span>
      </div>
      <div v-if="!realtimeList.length" class="ri-empty">暂无推荐数据</div>

      <div v-if="hotList.length" class="ir-hot-section">
        <div class="ir-card-head">
          <span class="ir-card-title">热门兜底</span>
          <span class="ir-card-flag warn">{{ props.store.hotUsed ? "启用" : "待机" }}</span>
        </div>
        <div class="hot-tags">
          <span v-for="(item, i) in hotList" :key="i" class="hot-tag">{{ item.itemId }}</span>
        </div>
      </div>
    </div>

    <p v-if="timeStr" class="ir-ts">查询时间 {{ timeStr }}</p>

    <div v-if="!loading && !hasResults" class="ir-empty">
      <Icon name="cart" :size="32" />
      <span>输入商品ID查询相似推荐</span>
    </div>
  </div>
</template>

<style scoped>
 .ir-root { flex:1; display:flex; flex-direction:column; align-items:center; padding:120px 40px 40px; gap:16px; overflow-y:auto; position:relative; }
.ir-search { width:100%; max-width:700px; background:rgba(16,22,36,0.8); border:1px solid rgba(56,189,248,0.12); border-radius:12px; padding:20px; }
.ir-row { display:flex; align-items:center; gap:12px; color:var(--accent); }
.ir-input { flex:1; background:none; border:none; outline:none; color:var(--text-primary); font-size:18px; padding:8px 0; font-family:Consolas,monospace; }
.ir-btn { background:rgba(56,189,248,0.1); border:1px solid rgba(56,189,248,0.3); color:var(--accent); padding:8px 20px; border-radius:6px; cursor:pointer; font-size:14px; font-weight:600; white-space:nowrap; }
.ir-card { width:100%; max-width:700px; background:rgba(16,22,36,0.7); border:1px solid rgba(56,189,248,0.12); border-radius:12px; padding:24px; display:flex; flex-direction:column; gap:8px; box-shadow:0 4px 24px rgba(0,0,0,0.3); }
.ir-card-head { display:flex; justify-content:space-between; align-items:center; padding-bottom:8px; border-bottom:1px solid rgba(56,189,248,0.08); }
.ir-card-title { font-size:15px; font-weight:600; color:var(--text-primary); }
.ir-card-flag { font-size:12px; padding:3px 12px; border-radius:4px; font-weight:500; letter-spacing:0.5px; }
.ir-card-flag.on { background:rgba(52,211,153,0.12); color:var(--success); }
.ir-card-flag.off { background:rgba(100,116,139,0.1); color:var(--text-muted); }
.ir-card-flag.warn { background:rgba(251,191,36,0.12); color:var(--warning); }
.ir-row-item { display:flex; align-items:center; gap:12px; padding:10px 12px; background:rgba(56,189,248,0.04); border-radius:6px; }
.ri-rank { font-size:14px; font-weight:700; color:var(--text-muted); min-width:22px; }
.ri-id { font-size:15px; font-family:Consolas,monospace; color:var(--text-primary); flex:1; }
.ri-score{font-size:13px;font-weight:600;color:var(--accent);font-family:'Orbitron','Consolas',monospace}
.ri-empty { text-align:center; color:var(--text-muted); padding:24px; font-size:13px; }
.ir-hot-section { margin-top:8px; border-top:1px solid rgba(56,189,248,0.08); padding-top:12px; }
.hot-tags { display:flex; flex-wrap:wrap; gap:8px; padding-top:8px; }
.hot-tag { padding:5px 12px; background:rgba(251,191,36,0.1); border:1px solid rgba(251,191,36,0.2); border-radius:4px; font-size:13px; font-family:Consolas,monospace; color:var(--warning); }
.ir-ts { font-size:12px; color:var(--text-muted); max-width:700px; width:100%; text-align:center; }
.ir-empty { display:flex; flex-direction:column; align-items:center; gap:12px; color:var(--text-muted); font-size:14px; opacity:0.6; padding-top:60px; }
</style>
