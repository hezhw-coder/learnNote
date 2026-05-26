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
.page-header-card {
  position: sticky;
  top: 0;
  z-index: 12;
  border-color: rgba(15, 23, 42, 0.08);
  background: rgba(255, 255, 255, 0.94);
  backdrop-filter: blur(14px);
}

.page-header-card :deep(.el-card__body) {
  padding: 20px 22px;
}

.header-content {
  display: flex;
  align-items: flex-start;
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
  flex-wrap: wrap;
  gap: 12px;
  justify-content: flex-end;
}

@media (max-width: 992px) {
  .header-content {
    flex-direction: column;
  }

  .header-actions {
    width: 100%;
    justify-content: flex-start;
  }
}
</style>
