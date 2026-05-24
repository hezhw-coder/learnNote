<script setup lang="ts">
defineProps<{
  label: string;
  value: number | string;
  trend?: string;
  icon?: string;
  color?: string;
}>();
</script>

<template>
  <el-card class="stat-card panel-card" shadow="never">
    <div class="stat-top">
      <span class="stat-label">{{ label }}</span>
      <el-icon v-if="icon" :style="{ color: color ?? '#409eff' }"><component :is="icon" /></el-icon>
    </div>
    <div class="stat-value">{{ value }}</div>
    <div class="stat-trend" :style="{ color: color ?? '#409eff' }">{{ trend ?? '持续健康运行' }}</div>
  </el-card>
</template>

<style scoped>
.stat-card :deep(.el-card__body) {
  padding: 18px 20px;
}

.stat-top,
.stat-trend {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.stat-label {
  color: #64748b;
}

.stat-value {
  margin: 12px 0;
  font-size: 32px;
  font-weight: 700;
  color: #0f172a;
}

.stat-trend {
  font-size: 13px;
}
</style>
