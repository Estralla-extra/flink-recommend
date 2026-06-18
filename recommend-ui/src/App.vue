<script setup lang="ts">
import { useRouter } from 'vue-router'
import HeaderBar from '@/components/HeaderBar.vue'
import BottomMarquee from '@/components/BottomMarquee.vue'
import { useEventsStore } from '@/stores/events'
import { useMetricsStore } from '@/stores/metrics'

const router = useRouter()
const eventsStore = useEventsStore()
const metricsStore = useMetricsStore()

// Auth pages should not show header/marquee
const isAuthPage = () => ['/login', '/register'].includes(router.currentRoute.value?.path || '')
</script>

<template>
  <div class="app-layout">
    <template v-if="isAuthPage()">
      <router-view />
    </template>
    <template v-else>
      <HeaderBar />
      <router-view />
      <BottomMarquee />
    </template>
  </div>
</template>

<style>
.app-layout {
  height: 100%;
  display: flex;
  flex-direction: column;
}
</style>
