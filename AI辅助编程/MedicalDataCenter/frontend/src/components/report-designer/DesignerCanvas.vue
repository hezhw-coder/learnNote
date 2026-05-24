<script setup lang="ts">
import type { ReportWidget } from '@/types/report';
import { widgetLabelMap } from '@/utils/schema';

defineProps<{
  widgets: ReportWidget[];
  activeId?: string;
}>();

const emit = defineEmits<{
  select: [id: string];
  remove: [id: string];
}>();
</script>

<template>
  <el-card shadow="never" class="panel-card">
    <template #header>
      <div class="section-title">
        <h3>设计画布</h3>
      </div>
    </template>

    <div class="canvas-grid">
      <div
        v-for="widget in widgets"
        :key="widget.id"
        class="canvas-widget"
        :class="{ active: widget.id === activeId }"
        @click="emit('select', widget.id)"
      >
        <div class="widget-top">
          <div>
            <strong>{{ widget.title }}</strong>
            <p>{{ widgetLabelMap[widget.type] }} · {{ widget.datasetId }}</p>
          </div>
          <el-button text type="danger" @click.stop="emit('remove', widget.id)">删除</el-button>
        </div>
        <div class="widget-body">
          <el-empty v-if="widget.type === 'table'" description="表格预览区" :image-size="72" />
          <div v-else class="widget-placeholder">
            {{ widgetLabelMap[widget.type] }} 占位预览
          </div>
        </div>
      </div>
    </div>
  </el-card>
</template>

<style scoped>
.canvas-grid {
  display: grid;
  gap: 16px;
}

.canvas-widget {
  border: 1px solid rgba(15, 23, 42, 0.08);
  border-radius: 14px;
  padding: 16px;
  background: linear-gradient(180deg, #fff, #f8fafc);
  cursor: pointer;
}

.canvas-widget.active {
  border-color: #409eff;
  box-shadow: 0 0 0 3px rgba(64, 158, 255, 0.16);
}

.widget-top {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.widget-top p {
  margin: 6px 0 0;
  color: #64748b;
  font-size: 13px;
}

.widget-body {
  margin-top: 14px;
}

.widget-placeholder {
  display: grid;
  place-items: center;
  min-height: 140px;
  border-radius: 12px;
  border: 1px dashed #94a3b8;
  color: #475569;
  background: rgba(241, 245, 249, 0.8);
}
</style>
