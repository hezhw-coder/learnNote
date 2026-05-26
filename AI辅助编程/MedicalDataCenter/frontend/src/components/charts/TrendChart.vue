<script setup lang="ts">
import { computed } from 'vue';
import { use } from 'echarts/core';
import { CanvasRenderer } from 'echarts/renderers';
import { BarChart, LineChart } from 'echarts/charts';
import { GridComponent, LegendComponent, TooltipComponent } from 'echarts/components';
import VChart from 'vue-echarts';
import type { TrendPoint } from '@/types/dashboard';

use([CanvasRenderer, LineChart, BarChart, GridComponent, TooltipComponent, LegendComponent]);

const props = defineProps<{
  title: string;
  type?: 'line' | 'bar';
  points: TrendPoint[];
  color?: string;
}>();

const emit = defineEmits<{
  pointClick: [point: TrendPoint];
}>();

function handlePointClick(params: { dataIndex?: number }) {
  const target = props.points[params.dataIndex ?? -1];
  if (!target) {
    return;
  }
  emit('pointClick', target);
}

const option = computed(() => ({
  tooltip: { trigger: 'axis' },
  grid: { top: 32, left: 12, right: 12, bottom: 8, containLabel: true },
  xAxis: {
    type: 'category',
    data: props.points.map((item) => item.label),
    axisTick: { show: false },
  },
  yAxis: {
    type: 'value',
    splitLine: { lineStyle: { color: '#e2e8f0' } },
  },
  series: [
    {
      type: props.type ?? 'line',
      data: props.points.map((item) => item.value),
      smooth: true,
      areaStyle: props.type === 'line' ? { opacity: 0.12 } : undefined,
      itemStyle: { color: props.color ?? '#409eff' },
      lineStyle: { color: props.color ?? '#409eff', width: 3 },
      barMaxWidth: 26,
    },
  ],
}));
</script>

<template>
  <el-card class="panel-card" shadow="never">
    <template #header>
      <div class="section-title">
        <h3>{{ title }}</h3>
      </div>
    </template>
    <VChart :option="option" autoresize style="height: 300px" @click="handlePointClick" />
  </el-card>
</template>
