<template>
  <div class="panel skeleton">
    <div class="panel-header">
      <div class="skeleton-line skeleton-title" />
    </div>
    <div class="skeleton-body">
      <div class="skeleton-block" />
    </div>
  </div>
</template>

<style scoped>
.skeleton {
  animation: pulse 1.8s ease-in-out infinite;
}
.skeleton-line {
  height: 16px;
  background: linear-gradient(90deg, rgba(56,189,248,0.08), rgba(56,189,248,0.15), rgba(56,189,248,0.08));
  border-radius: 4px;
}
.skeleton-title {
  width: 40%;
}
.skeleton-body {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}
.skeleton-block {
  width: 80%;
  height: 70%;
  background: linear-gradient(135deg, rgba(30,41,59,0.5), rgba(56,189,248,0.06));
  border-radius: var(--radius);
  border: 1px solid var(--border-color);
}
@keyframes pulse {
  0%, 100% { opacity: 0.5; }
  50% { opacity: 1; }
}
</style>
