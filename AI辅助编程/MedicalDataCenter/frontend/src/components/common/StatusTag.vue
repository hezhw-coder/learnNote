<script setup lang="ts">
import { computed } from 'vue';

const props = defineProps<{
  status: string;
}>();

const tagMap: Record<string, { type: 'success' | 'warning' | 'danger' | 'info'; label: string }> = {
  enabled: { type: 'success', label: '启用' },
  disabled: { type: 'info', label: '停用' },
  error: { type: 'danger', label: '异常' },
  running: { type: 'warning', label: '运行中' },
  success: { type: 'success', label: '成功' },
  failed: { type: 'danger', label: '失败' },
  draft: { type: 'info', label: '草稿' },
};

const display = computed(() => tagMap[props.status] ?? { type: 'info', label: props.status });
</script>

<template>
  <el-tag :type="display.type" effect="light">{{ display.label }}</el-tag>
</template>
