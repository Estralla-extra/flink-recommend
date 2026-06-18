<script setup lang="ts">
import UserRecommend from '@/components/UserRecommend.vue'
import { useRecommendStore } from '@/stores/recommend'

const recommendStore = useRecommendStore()
</script>

<template>
  <div class="recommend-page">
    <UserRecommend :store="recommendStore" />
  </div>
</template>

<style scoped>
.recommend-page {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow-y: auto;
}
</style>
