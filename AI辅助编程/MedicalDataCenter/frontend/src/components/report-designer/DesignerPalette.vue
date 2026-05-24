<script setup lang="ts">
import type { WidgetType } from '@/types/report';
import { widgetLabelMap } from '@/utils/schema';

const widgetTypes = Object.keys(widgetLabelMap) as WidgetType[];

const emit = defineEmits<{
  add: [type: WidgetType];
}>();
</script>

<template>
  <el-card shadow="never" class="panel-card">
    <template #header>
      <div class="section-title">
        <h3>组件面板</h3>
      </div>
    </template>

    <div class="palette-list">
      <el-button
        v-for="type in widgetTypes"
        :key="type"
        class="palette-item"
        @click="emit('add', type)"
      >
        {{ widgetLabelMap[type] }}
      </el-button>
    </div>
  </el-card>
</template>

<style scoped>
.palette-list {
  display: grid;
  gap: 12px;
}

.palette-item {
  justify-content: flex-start;
}
</style>
