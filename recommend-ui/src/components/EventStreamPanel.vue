<script setup lang="ts">
import { useEventsStore } from '@/stores/events'
import SkeletonPanel from './common/SkeletonPanel.vue'

const eventsStore = useEventsStore()

function getTagClass(text: string) {
  if (text.includes('购买')) return 'tag-buy'
  if (text.includes('加购')) return 'tag-cart'
  if (text.includes('收藏')) return 'tag-fav'
  return ''
}
</script>

<template>
  <div class="panel">
    <div class="panel-title" style="border-color: var(--color-events); color: var(--color-events)">实时行为事件流</div>
    <SkeletonPanel v-if="eventsStore.loading" />
    <div v-else class="event-list">
      <TransitionGroup name="event">
        <p v-for="(ev, i) in eventsStore.events.slice(0, 10)" :key="i" class="event-item">
          <span class="event-tag" :class="getTagClass(ev)">
            {{ ev.includes('购买') ? '购' : ev.includes('加购') ? '加' : ev.includes('收藏') ? '藏' : '·' }}
          </span>
          {{ ev }}
        </p>
      </TransitionGroup>
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
  min-height: 0;
  position: relative;
}
.panel-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--accent);
  margin-bottom: 8px;
  padding-left: 12px;
  border-left: 3px solid var(--accent);
  flex-shrink: 0;
  letter-spacing: 0.5px;
}
.event-list {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  font-size: 13px;
  color: var(--text-secondary);
  line-height: 2;
  position: relative;
}
.event-item {
  padding: 4px 8px;
  border-bottom: 1px solid rgba(56, 189, 248, 0.06);
  display: flex;
  align-items: center;
  gap: 8px;
  transition: background 0.3s;
}
.event-item:hover {
  background: rgba(56, 189, 248, 0.04);
}
.event-tag {
  font-size: 11px;
  font-weight: 700;
  padding: 1px 6px;
  border-radius: 3px;
  flex-shrink: 0;
  min-width: 22px;
  text-align: center;
}
.tag-buy { background: rgba(52, 211, 153, 0.2); color: var(--success); }
.tag-cart { background: rgba(251, 191, 36, 0.2); color: var(--warning); }
.tag-fav { background: rgba(56, 189, 248, 0.2); color: var(--accent); }
.event-enter-active { transition: all 0.4s cubic-bezier(0.16, 1, 0.3, 1); }
.event-leave-active { transition: all 0.3s ease; position: absolute; }
.event-enter-from { opacity: 0; transform: translateX(-20px); }
.event-leave-to { opacity: 0; transform: translateY(-10px); }
.event-move { transition: transform 0.3s ease; }
</style>