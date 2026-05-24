<script setup lang="ts">
import type { ReportWidget } from '@/types/report';

const props = defineProps<{
  widget?: ReportWidget | null;
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
        <el-input :model-value="widget.datasetId" @update:model-value="(value) => patchWidget({ datasetId: value })" />
      </el-form-item>
      <el-form-item label="说明">
        <el-input
          :model-value="widget.description"
          type="textarea"
          :rows="3"
          @update:model-value="(value) => patchWidget({ description: value })"
        />
      </el-form-item>
      <el-form-item label="X 维度">
        <el-input
          :model-value="String(widget.config.xField ?? '')"
          placeholder="date / dept / doctor"
          @update:model-value="(value) => patchConfig('xField', value)"
        />
      </el-form-item>
      <el-form-item label="Y 指标">
        <el-input
          :model-value="String(widget.config.yField ?? '')"
          placeholder="count / amount / rate"
          @update:model-value="(value) => patchConfig('yField', value)"
        />
      </el-form-item>
      <el-form-item label="颜色">
        <el-color-picker
          :model-value="String(widget.config.color ?? '#409eff')"
          @update:model-value="(value) => patchConfig('color', value)"
        />
      </el-form-item>
    </el-form>
  </el-card>
</template>
