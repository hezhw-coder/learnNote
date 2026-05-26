<script setup lang="ts">
import type { ReportDatasetField, ReportWidget } from '@/types/report';

const props = defineProps<{
  widget?: ReportWidget | null;
  datasetFields: ReportDatasetField[];
  layoutColumns: number;
}>();

const emit = defineEmits<{
  update: [widget: ReportWidget];
}>();

function patchWidget(patch: Partial<ReportWidget>) {
  if (!props.widget) {
    return;
  }

  emit('update', {
    ...props.widget,
    ...patch,
  });
}

function patchConfig(key: string, value: string | number | boolean) {
  if (!props.widget) {
    return;
  }

  emit('update', {
    ...props.widget,
    config: {
      ...props.widget.config,
      [key]: value,
    },
  });
}

function patchBinding(index: number, patch: Partial<ReportWidget['bindings'][number]>) {
  if (!props.widget) {
    return;
  }
  emit('update', {
    ...props.widget,
    bindings: props.widget.bindings.map((binding, currentIndex) =>
      currentIndex === index
        ? {
            ...binding,
            ...patch,
          }
        : binding,
    ),
  });
}
</script>

<template>
  <el-card shadow="never" class="panel-card">
    <template #header>
      <div class="section-title">
        <h3>属性面板</h3>
      </div>
    </template>

    <el-empty v-if="!widget" description="请选择一个报表组件" :image-size="88" />
    <el-form v-else label-width="88px">
      <el-form-item label="标题">
        <el-input :model-value="widget.title" @update:model-value="(value) => patchWidget({ title: value })" />
      </el-form-item>
      <el-form-item label="数据集">
        <el-input :model-value="widget.datasetId" disabled />
      </el-form-item>
      <el-form-item label="说明">
        <el-input
          :model-value="widget.description"
          type="textarea"
          :rows="3"
          @update:model-value="(value) => patchWidget({ description: value })"
        />
      </el-form-item>
      <el-form-item label="布局占列">
        <el-slider
          :min="1"
          :max="Math.max(1, layoutColumns)"
          :model-value="Number(widget.span || 1)"
          show-input
          @update:model-value="(value) => patchWidget({ span: Number(value) })"
        />
      </el-form-item>
      <el-form-item label="显隐状态">
        <el-switch
          :model-value="widget.visible"
          inline-prompt
          active-text="显示"
          inactive-text="隐藏"
          @update:model-value="(value) => patchWidget({ visible: Boolean(value) })"
        />
      </el-form-item>
      <el-form-item label="网格宽度">
        <el-slider
          :min="6"
          :max="24"
          :step="6"
          :model-value="Number(widget.grid.w || 12)"
          show-input
          @update:model-value="(value) => patchWidget({ grid: { ...widget.grid, w: Number(value) } })"
        />
      </el-form-item>
      <el-form-item label="网格高度">
        <el-slider
          :min="1"
          :max="4"
          :model-value="Number(widget.grid.h || 1)"
          show-input
          @update:model-value="(value) => patchWidget({ grid: { ...widget.grid, h: Number(value) } })"
        />
      </el-form-item>
      <el-form-item label="栅格坐标">
        <el-text type="info">x={{ widget.grid.x }} / y={{ widget.grid.y }} / 排序={{ widget.sortOrder }}</el-text>
      </el-form-item>
      <div v-for="(binding, index) in widget.bindings" :key="binding.id">
        <el-form-item :label="binding.label">
          <el-select
            :model-value="binding.field"
            placeholder="选择字段"
            style="width: 100%"
            @update:model-value="(value) => patchBinding(index, { field: String(value) })"
          >
            <el-option
              v-for="field in datasetFields"
              :key="`${binding.id}-${field.fieldCode}`"
              :label="`${field.fieldName} (${field.fieldCode})`"
              :value="field.fieldCode"
            />
          </el-select>
        </el-form-item>
        <el-form-item v-if="binding.role === 'value'" label="聚合方式">
          <el-select
            :model-value="binding.aggregation || ''"
            placeholder="选择聚合方式"
            style="width: 100%"
            @update:model-value="(value) => patchBinding(index, { aggregation: String(value) })"
          >
            <el-option label="不聚合" value="" />
            <el-option label="计数" value="count" />
            <el-option label="求和" value="sum" />
            <el-option label="去重计数" value="distinct_count" />
          </el-select>
        </el-form-item>
      </div>
      <el-form-item label="颜色">
        <el-color-picker
          :model-value="String(widget.config.color ?? '#409eff')"
          @update:model-value="(value) => patchConfig('color', value)"
        />
      </el-form-item>
      <el-form-item label="绑定摘要">
        <el-text type="info">
          {{ widget.bindings.map((binding) => `${binding.label}: ${binding.field || '-'}`).join(' / ') }}
        </el-text>
      </el-form-item>
    </el-form>
  </el-card>
</template>
