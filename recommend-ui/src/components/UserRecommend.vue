<script setup lang="ts">
import { ref, computed } from "vue"
import Icon from "./common/Icon.vue"

const props = defineProps<{ store: any }>()
const userInput = ref('')
const itemInput = ref('')
const loading = ref(false)
const queryTime = ref(0)
const toast = ref(null)

function showMsg(msg: string, type: string) { toast.value = { message: msg, type }
  setTimeout(() => { toast.value = null }, 2500) }

async function search() {
  if (!userInput.value.trim()) return
  loading.value = true
  try { await props.store.fetchForUser(userInput.value.trim(), itemInput.value.trim() || undefined)
    queryTime.value = Date.now(); showMsg("查询完成", "ok")
  } catch { showMsg("连接失败", "err") }
  finally { loading.value = false } }

const flinkList = computed(() => props.store.sourceLists?.flink || [])
const realtimeList = computed(() => props.store.sourceLists?.realtime || [])
const hotList = computed(() => props.store.sourceLists?.hot || [])
const timeStr = computed(() => queryTime.value ? new Date(queryTime.value).toLocaleString("zh-CN") : "")
</script>

<template>
  <div class="ur-root">
    <Transition name="toast">
      <div v-if="toast" :class="'ur-toast ' + (toast.type === 'ok' ? 'toast-ok' : 'toast-err')">{{ toast.message }}</div>
    </Transition>

    <div class="ur-search">
      <div class="ur-row">
        <Icon name="user" :size="15" />
        <input v-model="userInput" class="ur-input" placeholder="用户ID" @keyup.enter="search" :disabled="loading" />
        <Icon name="cart" :size="15" />
        <input v-model="itemInput" class="ur-input" placeholder="商品ID(选填)" @keyup.enter="search" :disabled="loading" />
        <button class="ur-btn" @click="search" :disabled="loading"><Icon name="search" :size="12" /> 查询</button>
      </div>
    </div>

    <div v-if="props.store.items.length" class="ur-results">
      <div class="ur-grid">
        <div class="ur-col" :class="{ hit: props.store.flinkHit }">
          <div class="col-head">
            <span class="col-title">Flink 窗口缓存</span>
            <span class="col-badge" :class="props.store.flinkHit ? 'on' : 'off'">{{ props.store.flinkHit ? "已命中" : "未命中" }}</span>
          </div>
          <div class="col-body">
            <div v-for="(item, i) in flinkList" :key="'f' + i" class="col-item">
              <span class="ci-rank">{{ i + 1 }}</span><span class="ci-id">{{ item.itemId }}</span><span class="ci-score">{{ item.score.toFixed(2) }}</span>
            </div>
            <div v-if="!flinkList.length" class="col-empty">暂无数据</div>
          </div>
        </div>
        <div class="ur-col" :class="{ hit: props.store.realtimeHit }">
          <div class="col-head">
            <span class="col-title">实时相似召回</span>
            <span class="col-badge" :class="props.store.realtimeHit ? 'on' : 'off'">{{ props.store.realtimeHit ? "已命中" : "未命中" }}</span>
          </div>
          <div class="col-body">
            <div v-for="(item, i) in realtimeList" :key="'r' + i" class="col-item">
              <span class="ci-rank">{{ i + 1 }}</span><span class="ci-id">{{ item.itemId }}</span><span class="ci-score">{{ item.score.toFixed(2) }}</span>
            </div>
            <div v-if="!realtimeList.length" class="col-empty">暂无数据</div>
          </div>
        </div>
        <div class="ur-col" :class="{ hot: props.store.hotUsed }">
          <div class="col-head">
            <span class="col-title">热门兜底</span>
            <span class="col-badge" :class="props.store.hotUsed ? 'warn' : 'off'">{{ props.store.hotUsed ? "启用" : "待机" }}</span>
          </div>
          <div class="col-body">
            <div v-for="(item, i) in hotList" :key="'h' + i" class="col-item">
              <span class="ci-rank">{{ i + 1 }}</span><span class="ci-id">{{ item.itemId }}</span><span class="ci-score">{{ item.score.toFixed(2) }}</span>
            </div>
            <div v-if="!hotList.length" class="col-empty">暂无数据</div>
          </div>
        </div>
      </div>
      <p v-if="timeStr" class="ur-ts">查询时间 {{ timeStr }}</p>
    </div>
    <div v-else-if="!loading" class="ur-empty">
      <Icon name="user" :size="32" />
      <span>输入用户ID查询推荐结果</span>
    </div>
  </div>
</template>

<style scoped>
.ur-root { flex:1; display:flex; flex-direction:column; align-items:center; padding:80px 40px 40px; gap:20px; overflow-y:auto; position:relative; }
.ur-search { width:100%; max-width:700px; background:var(--bg-panel); border:1px solid var(--border-color); border-radius:10px; padding:14px 18px; }
.ur-row { display:flex; align-items:center; gap:10px; }
.ur-input { flex:1; background:none; border:none; outline:none; color:var(--text-primary); font-size:14px; padding:6px 0; font-family:Consolas,monospace; }
.ur-btn { background:rgba(56,189,248,0.1); border:1px solid rgba(56,189,248,0.3); color:var(--accent); padding:6px 16px; border-radius:6px; cursor:pointer; font-size:12px; font-weight:600; white-space:nowrap; }
.ur-btn:hover { background:rgba(56,189,248,0.2); } .ur-btn:disabled { opacity:0.5; cursor:not-allowed; }
.ur-results { width:100%; max-width:800px; display:flex; flex-direction:column; gap:16px; }
.ur-grid { display:grid; grid-template-columns:1fr 1fr 1fr; gap:12px; }
.ur-col { background:rgba(16,22,36,0.6); border:1px solid var(--border-color); border-radius:10px; padding:14px; display:flex; flex-direction:column; gap:10px; }
.ur-col.hit { border-color:rgba(52,211,153,0.35); }
.ur-col.hot { border-color:rgba(251,191,36,0.35); }
.col-head { display:flex; justify-content:space-between; align-items:center; padding-bottom:8px; border-bottom:1px solid var(--border-color); }
.col-title { font-size:13px; font-weight:600; color:var(--text-primary); }
.col-badge { font-size:10px; padding:2px 8px; border-radius:4px; font-weight:600; }
.col-badge.on { background:rgba(52,211,153,0.12); color:var(--success); }
.col-badge.off { background:rgba(100,116,139,0.1); color:var(--text-muted); }
.col-badge.warn { background:rgba(251,191,36,0.12); color:var(--warning); }
.col-body { display:flex; flex-direction:column; gap:6px; min-height:60px; }
.col-item { display:flex; align-items:center; gap:8px; padding:4px 8px; background:rgba(56,189,248,0.04); border-radius:4px; }
.ci-rank { font-size:11px; font-weight:700; color:var(--text-muted); min-width:16px; }
.ci-id { font-size:12px; font-family:Consolas,monospace; color:var(--text-primary); flex:1; }
.ci-score { font-size:11px; font-weight:600; color:var(--accent); }
.col-empty { text-align:center; color:var(--text-muted); font-size:12px; padding:10px; }
.ur-ts { font-size:11px; color:var(--text-muted); text-align:center; }
.ur-empty { display:flex; flex-direction:column; align-items:center; gap:12px; color:var(--text-muted); font-size:14px; opacity:0.6; padding-top:60px; }
</style>
