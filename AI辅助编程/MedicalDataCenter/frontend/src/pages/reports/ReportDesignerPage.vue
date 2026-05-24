<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { exportReportTemplate, fetchReportTemplate, previewReportTemplate, submitReportTemplate } from '@/api/modules/reports';
import DesignerCanvas from '@/components/report-designer/DesignerCanvas.vue';
import DesignerPalette from '@/components/report-designer/DesignerPalette.vue';
import DesignerPropertyPanel from '@/components/report-designer/DesignerPropertyPanel.vue';
import PageHeaderCard from '@/components/common/PageHeaderCard.vue';
import type { ReportTemplateSchema, ReportWidget, WidgetType } from '@/types/report';
import { createDefaultTemplate, createWidgetByType, serializeTemplate } from '@/utils/schema';

const loading = ref(false);
const schema = ref<ReportTemplateSchema>(createDefaultTemplate());
const activeWidgetId = ref('');
const previewJsonVisible = ref(false);

const activeWidget = computed<ReportWidget | null>(
  () => schema.value.widgets.find((item) => item.id === activeWidgetId.value) ?? schema.value.widgets[0] ?? null,
);

async function loadData() {
  loading.value = true;
  try {
    schema.value = await fetchReportTemplate();
    activeWidgetId.value = schema.value.widgets[0]?.id ?? '';
  } finally {
    loading.value = false;
  }
}

function handleAddWidget(type: WidgetType) {
  const nextWidget = createWidgetByType(type);
  schema.value.widgets.push(nextWidget);
  activeWidgetId.value = nextWidget.id;
}

function handleRemoveWidget(id: string) {
  schema.value.widgets = schema.value.widgets.filter((item) => item.id !== id);
  activeWidgetId.value = schema.value.widgets[0]?.id ?? '';
}

function handleUpdateWidget(widget: ReportWidget) {
  schema.value.widgets = schema.value.widgets.map((item) => (item.id === widget.id ? { ...widget } : item));
}

function handleSelectWidget(id: string) {
  activeWidgetId.value = id;
}

async function handleSave() {
  await submitReportTemplate(schema.value);
  ElMessage.success('报表模板已保存');
  await loadData();
}

async function handlePreview() {
  const result = await previewReportTemplate(schema.value.id);
  ElMessage.success(result.message);
}

function handleExport(format: 'pdf' | 'excel') {
  if (!/^\d+$/.test(schema.value.id)) {
    ElMessage.warning('请先保存模板后再导出');
    return;
  }
  exportReportTemplate(schema.value.id, format);
}

onMounted(loadData);
</script>

<template>
  <div class="page-shell">
    <PageHeaderCard
      title="报表设计器"
      tag="低代码"
      description="采用组件树 + 布局树 + 数据绑定 JSON Schema 的结构，当前实现组件面板、画布和属性面板基础骨架。"
    >
      <template #actions>
        <el-button @click="previewJsonVisible = true">查看 Schema</el-button>
        <el-button @click="handlePreview">预览</el-button>
        <el-button @click="handleExport('pdf')">导出 PDF</el-button>
        <el-button @click="handleExport('excel')">导出 Excel</el-button>
        <el-button type="primary" :loading="loading" @click="handleSave">保存模板</el-button>
      </template>
    </PageHeaderCard>

    <div class="designer-grid">
      <DesignerPalette @add="handleAddWidget" />
      <DesignerCanvas
        :widgets="schema.widgets"
        :active-id="activeWidgetId"
        @select="handleSelectWidget"
        @remove="handleRemoveWidget"
      />
      <DesignerPropertyPanel :widget="activeWidget" @update="handleUpdateWidget" />
    </div>

    <el-card class="panel-card" shadow="never">
      <template #header>
        <div class="section-title">
          <h3>数据绑定与发布说明</h3>
        </div>
      </template>
      <el-alert
        title="当前模板预设绑定 dataset-001，后续需联调数据集字段列表、预览接口、导出 PDF/Excel 接口。"
        type="info"
        :closable="false"
      />
      <el-descriptions :column="3" border style="margin-top: 16px">
        <el-descriptions-item label="模板名称">{{ schema.name }}</el-descriptions-item>
        <el-descriptions-item label="组件数量">{{ schema.widgets.length }}</el-descriptions-item>
        <el-descriptions-item label="布局列数">{{ schema.layout.columns }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <el-dialog v-model="previewJsonVisible" title="报表 Schema" width="760px">
      <pre class="schema-preview">{{ serializeTemplate(schema) }}</pre>
    </el-dialog>
  </div>
</template>

<style scoped>
.designer-grid {
  display: grid;
  grid-template-columns: 240px minmax(0, 1fr) 320px;
  gap: 16px;
}

.schema-preview {
  margin: 0;
  padding: 16px;
  overflow: auto;
  border-radius: 14px;
  background: #0f172a;
  color: #e2e8f0;
}

@media (max-width: 1400px) {
  .designer-grid {
    grid-template-columns: 1fr;
  }
}
</style>
