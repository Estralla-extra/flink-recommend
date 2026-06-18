<template>
  <span class="status-dot" :class="status" />
</template>

<script setup lang="ts">
defineProps<{ status?: 'online' | 'warning' | 'offline' }>()
</script>

<style scoped>
.status-dot {
  display: inline-block;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  margin-right: 14px;
  position: relative;
}
.status-dot::after {
  content: '';
  position: absolute;
  inset: -3px;
  border-radius: 50%;
  opacity: 0;
}
.status-dot.online {
  background: var(--success);
  box-shadow: 0 0 8px var(--success), 0 0 16px rgba(52, 211, 153, 0.3);
}
.status-dot.online::after {
  background: var(--success);
  animation: pulse-ring 2s ease-out infinite;
}
.status-dot.warning {
  background: var(--warning);
  box-shadow: 0 0 8px var(--warning);
}
.status-dot.warning::after {
  background: var(--warning);
  animation: pulse-ring 3s ease-out infinite;
}
.status-dot.offline {
  background: var(--danger);
  box-shadow: 0 0 8px var(--danger);
}

@keyframes pulse-ring {
  0% { transform: scale(0.8); opacity: 0.6; }
  100% { transform: scale(1.6); opacity: 0; }
}
</style>
