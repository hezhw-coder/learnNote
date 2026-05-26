<script setup lang="ts">
import { ref } from 'vue';
import type { ReportWidget } from '@/types/report';
import { widgetLabelMap } from '@/utils/schema';

const props = defineProps<{
  widgets: ReportWidget[];
  activeId?: string;
  layoutColumns: number;
  gridColumns: number;
  rowHeight: number;
  layoutGap: number;
}>();

const emit = defineEmits<{
  select: [id: string];
  remove: [id: string];
  move: [id: string, direction: 'up' | 'down'];
  duplicate: [id: string];
  reorder: [id: string, targetIndex: number];
}>();

const draggingWidgetId = ref('');

function handleDragStart(id: string) {
  draggingWidgetId.value = id;
}

function handleDrop(targetIndex: number) {
  if (!draggingWidgetId.value) {
    return;
  }
  emit('reorder', draggingWidgetId.value, targetIndex);
  draggingWidgetId.value = '';
}
</script>

<template>
  <el-card shadow="never" class="panel-card">
    <template #header>
      <div class="section-title">
        <h3>设计画布</h3>
      </div>
    </template>

    <div
      class="canvas-grid"
      :style="{
        gridTemplateColumns: `repeat(${gridColumns}, minmax(0, 1fr))`,
        gridAutoRows: `${rowHeight}px`,
        gap: `${layoutGap}px`,
      }"
    >
      <div
        v-for="widget in widgets"
        :key="widget.id"
        class="canvas-widget"
        :class="{ active: widget.id === activeId, dragging: widget.id === draggingWidgetId }"
        :style="{
          gridColumn: `${widget.grid.x + 1} / span ${Math.max(1, Math.min(widget.grid.w, gridColumns))}`,
          gridRow: `${widget.grid.y + 1} / span ${Math.max(1, widget.grid.h)}`,
          opacity: widget.visible ? 1 : 0.72,
        }"
        draggable="true"
        @dragstart="handleDragStart(widget.id)"
        @dragover.prevent
        @drop.prevent="handleDrop(widgets.findIndex((item) => item.id === widget.id))"
        @click="emit('select', widget.id)"
      >
        <div class="widget-top">
          <div>
            <strong>{{ widget.title }}</strong>
            <p>
              {{ widgetLabelMap[widget.type] }} · {{ widget.datasetId }} · 栅格 {{ widget.grid.w }} x {{ widget.grid.h }}
              · 位置 ({{ widget.grid.x }}, {{ widget.grid.y }})
            </p>
          </div>
          <div class="widget-actions">
            <el-button text @click.stop="emit('move', widget.id, 'up')">上移</el-button>
            <el-button text @click.stop="emit('move', widget.id, 'down')">下移</el-button>
            <el-button text @click.stop="emit('duplicate', widget.id)">复制</el-button>
            <el-button text type="danger" @click.stop="emit('remove', widget.id)">删除</el-button>
          </div>
        </div>
        <div class="widget-body">
          <el-tag v-if="!widget.visible" size="small" type="info" effect="plain">已隐藏，仅设计态保留</el-tag>
          <el-empty v-if="widget.type === 'table'" description="表格预览区" :image-size="72" />
          <div v-else class="widget-placeholder">
            {{ widgetLabelMap[widget.type] }} 占位预览
          </div>
          <div class="widget-meta">
            排序：{{ widget.sortOrder }} · 绑定：{{ String(widget.config.xField ?? '-') }} / {{ String(widget.config.yField ?? '-') }}
          </div>
        </div>
      </div>
      <div
        v-if="widgets.length && draggingWidgetId"
        class="canvas-drop-zone"
        :style="{ gridColumn: `1 / span ${gridColumns}` }"
        @dragover.prevent
        @drop.prevent="handleDrop(widgets.length - 1)"
      >
        拖到此处可移动到末尾
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
  transition: box-shadow 0.2s ease, transform 0.2s ease, opacity 0.2s ease;
}

.canvas-widget.active {
  border-color: #409eff;
  box-shadow: 0 0 0 3px rgba(64, 158, 255, 0.16);
}

.canvas-widget.dragging {
  transform: scale(0.98);
}

.widget-top {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.widget-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.widget-top p {
  margin: 6px 0 0;
  color: #64748b;
  font-size: 13px;
}

.widget-body {
  margin-top: 14px;
}

.widget-meta {
  margin-top: 10px;
  color: #64748b;
  font-size: 12px;
}

.widget-placeholder {
  display: grid;
  place-items: center;
  min-height: 100%;
  border-radius: 12px;
  border: 1px dashed #94a3b8;
  color: #475569;
  background: rgba(241, 245, 249, 0.8);
}

.canvas-drop-zone {
  display: grid;
  place-items: center;
  min-height: 72px;
  border: 1px dashed #409eff;
  border-radius: 12px;
  color: #409eff;
  background: rgba(64, 158, 255, 0.06);
}
</style>
