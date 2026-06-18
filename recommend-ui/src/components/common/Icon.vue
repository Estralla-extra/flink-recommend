<template>
  <svg :width="size" :height="size" viewBox="0 0 24 24" fill="none" stroke="currentColor" :stroke-width="stroke" stroke-linecap="round" stroke-linejoin="round">
    <path v-if="name === 'user'" d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/>
    <circle v-if="name === 'user'" cx="12" cy="7" r="4"/>
    <path v-if="name === 'cpu'" d="M4 6a2 2 0 0 1 2-2h12a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V6zM11 11h2v2h-2zM11 1v3m2-3v3M11 20v3m2-3v3M1 11h3m16 0h3M1 13h3m16 0h3"/>
    <path v-if="name === 'zap'" d="M13 2 3 14h7l-2 8 10-12h-7l2-8z"/>
    <path v-if="name === 'flame'" d="M8.5 14.5A2.5 2.5 0 0 0 11 12c0-1.38-.5-2-1-3-1.072-2.143-.224-4.054 2-6.5 1.5 1.5 3 4.5 3 7a2.5 2.5 0 0 1-4.5 2"/>
    <path v-if="name === 'database'" d="M4 6c0 1.657 3.582 3 8 3s8-1.343 8-3M4 6v6c0 1.657 3.582 3 8 3s8-1.343 8-3V6M4 12v6c0 1.657 3.582 3 8 3s8-1.343 8-3v-6"/>
    <path v-if="name === 'chart'" d="M3 3v16a2 2 0 0 0 2 2h16M7 16l4-4 4 4 5-5"/>
    <rect v-if="name === 'server'" x="2" y="2" width="20" height="6" rx="2"/>
    <rect v-if="name === 'server'" x="2" y="16" width="20" height="6" rx="2"/>
    <circle v-if="name === 'server'" cx="6" cy="5" r="1" fill="currentColor"/>
    <circle v-if="name === 'server'" cx="6" cy="19" r="1" fill="currentColor"/>
    <path v-if="name === 'server'" d="M12 8h.01M12 8"/>
    <circle v-if="name === 'search'" cx="11" cy="11" r="8"/>
    <path v-if="name === 'search'" d="M21 21l-4.3-4.3"/>
    <path v-if="name === 'cart'" d="M1 1h4l2.68 13.39a2 2 0 0 0 2 1.61h9.72a2 2 0 0 0 2-1.61L23 6H6"/>
    <circle v-if="name === 'cart'" cx="9" cy="21" r="1"/>
    <circle v-if="name === 'cart'" cx="20" cy="21" r="1"/>
    <path v-if="name === 'star'" d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z"/>
    <path v-if="name === 'plus'" d="M12 5v14M5 12h14"/>
    <path v-if="name === 'cloud'" d="M17.5 19H9a7 7 0 1 1 6.71-9h1.79a4.5 4.5 0 1 1 0 9Z"/>
    <path v-if="name === 'check'" d="M20 6 9 17l-5-5"/>
    <path v-if="name === 'x'" d="M18 6 6 18M6 6l12 12"/>
    <circle v-if="name === 'award'" cx="12" cy="8" r="7"/>
    <path v-if="name === 'award'" d="M8.21 13.89 7 23l5-3 5 3-1.21-9.12"/>
    <path v-if="name === 'window'" d="M4 5v14h16V5H4zM4 12h16"/>
    <path v-if="name === 'activity'" d="M22 12h-4l-3 9L9 3l-3 9H2"/>
    <path v-if="name === 'layers'" d="M12 2 2 7l10 5 10-5-10-5zM2 17l10 5 10-5M2 12l10 5 10-5"/>
  </svg>
</template>

<script setup lang="ts">
withDefaults(defineProps<{
  name: string
  size?: number
  stroke?: number
}>(), {
  size: 14,
  stroke: 2,
})
</script>
