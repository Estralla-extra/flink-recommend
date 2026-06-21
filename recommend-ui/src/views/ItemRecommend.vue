<script setup lang="ts">
import ItemRecommend from '@/components/ItemRecommend.vue'
import { useRecommendStore } from '@/stores/recommend'

const recommendStore = useRecommendStore()
</script>

<template>
  <div class="item-recommend-page">
    <ItemRecommend :store="recommendStore" />
  </div>
</template>

<style scoped>
.item-recommend-page {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow-y: auto;
}
</style>