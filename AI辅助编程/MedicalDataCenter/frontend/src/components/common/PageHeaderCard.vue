<script setup lang="ts">
defineProps<{
  title: string;
  description: string;
  tag?: string;
}>();
</script>

<template>
  <el-card class="page-header-card panel-card" shadow="never">
    <div class="header-content">
      <div>
        <div class="title-row">
          <h2>{{ title }}</h2>
          <el-tag v-if="tag" effect="plain" type="primary">{{ tag }}</el-tag>
        </div>
        <p>{{ description }}</p>
      </div>
      <div class="header-actions">
        <slot name="actions" />
      </div>
    </div>
  </el-card>
</template>

<style scoped>
.page-header-card :deep(.el-card__body) {
  padding: 20px 22px;
}

.header-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.title-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

h2 {
  margin: 0;
  font-size: 24px;
}

p {
  margin: 10px 0 0;
  color: #64748b;
}

.header-actions {
  display: flex;
  gap: 12px;
}
</style>
