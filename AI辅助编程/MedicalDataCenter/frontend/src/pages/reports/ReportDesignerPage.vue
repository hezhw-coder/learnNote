<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { ElMessage } from 'element-plus';
import {
  exportReportTemplate,
  fetchReportBindingOptions,
  fetchReportTemplate,
  publishReportTemplate,
  previewReportTemplate,
  submitReportTemplate,
} from '@/api/modules/reports';
import TrendChart from '@/components/charts/TrendChart.vue';
import StatCard from '@/components/common/StatCard.vue';
import DesignerCanvas from '@/components/report-designer/DesignerCanvas.vue';
import DesignerPalette from '@/components/report-designer/DesignerPalette.vue';
import DesignerPropertyPanel from '@/components/report-designer/DesignerPropertyPanel.vue';
import PageHeaderCard from '@/components/common/PageHeaderCard.vue';
import type { TrendPoint } from '@/types/dashboard';
import type {
  ReportDatasetField,
  ReportDatasetOption,
  ReportInteractionContext,
  ReportInteractionRule,
  ReportParameterValue,
  ReportTemplateSchema,
  ReportWidget,
  WidgetType,
} from '@/types/report';
import { cloneWidget, createDefaultTemplate, createFilter, createInteraction, createParameter, createWidgetByType, normalizeTemplateLayout, serializeTemplate } from '@/utils/schema';

const loading = ref(false);
const schema = ref<ReportTemplateSchema>(createDefaultTemplate());
const activeWidgetId = ref('');
const previewJsonVisible = ref(false);
const previewVisible = ref(false);
const previewLoading = ref(false);
const previewRows = ref<Array<Record<string, unknown>>>([]);
const previewAppliedFilters = ref<string[]>([]);
const previewBindingSummary = ref<string[]>([]);
const previewParameterSummary = ref<string[]>([]);
const previewInteractionSummary = ref<string[]>([]);
const previewInteractionContexts = ref<ReportInteractionContext[]>([]);
const previewRuntimeParameters = ref<ReportParameterValue[]>([]);
const previewLayoutColumns = ref(0);
const previewWidgetCount = ref(0);
const previewFilterCount = ref(0);
const datasetOptions = ref<ReportDatasetOption[]>([]);

interface PreviewChartMeta {
  title: string;
  type: 'line' | 'bar';
  points: TrendPoint[];
  color: string;
  sourceField: string;
}

const previewColumns = computed(() => Object.keys(previewRows.value[0] ?? {}));
const previewDatasetCode = computed(() => schema.value.datasetCode || 'cdm_patient');
const templateStateTagType = computed(() => (schema.value.versioning.state === 'published' ? 'success' : 'warning'));
const templateStateText = computed(() => (schema.value.versioning.state === 'published' ? '已发布' : '草稿'));
const isLabResultDataset = computed(() => previewDatasetCode.value === 'cdm_lab_result');
const isLabDataset = computed(() => ['cdm_lab_result', 'cdm_lab'].includes(previewDatasetCode.value));
const isEncounterDataset = computed(() => previewDatasetCode.value === 'cdm_encounter');
const activeDatasetFields = computed<ReportDatasetField[]>(
  () => datasetOptions.value.find((item) => item.code === schema.value.datasetCode)?.fields ?? [],
);
const bindingHighlights = computed(() =>
  schema.value.widgets.map((widget) =>
    `${widget.title}: ${widget.bindings.map((binding) => `${binding.label}=${binding.field || '-'}`).join(' / ')}`,
  ),
);
const filterHighlights = computed(() =>
  schema.value.filters.map((filter) =>
    `${filter.label || filter.field || '未命名筛选'}: ${filter.operator}${filter.operator === 'not_empty' ? '' : ` ${filter.value || '-'}`}`,
  ),
);
const interactionHighlights = computed(() =>
  schema.value.interactions.map((interaction) => {
    const source = schema.value.widgets.find((widget) => widget.id === interaction.sourceWidgetId)?.title || '未选择源组件';
    const target = schema.value.widgets.find((widget) => widget.id === interaction.targetWidgetId)?.title || '未选择目标组件';
    return `${interaction.name}: ${source} -> ${target} (${interaction.sourceField || '-'} => ${interaction.targetField || '-'})`;
  }),
);
const parameterHighlights = computed(() =>
  schema.value.parameters
    .filter((parameter) => parameter.field.trim())
    .map((parameter) => `${parameter.label || parameter.code}: ${parameter.field} / ${parameter.operator}${parameter.required ? ' / 必填' : ''}`),
);
const activeTemplateParameters = computed(() => schema.value.parameters.filter((parameter) => parameter.field.trim()));
const activeInteractionHighlights = computed(() =>
  previewInteractionContexts.value.map((context) => {
    const interaction = schema.value.interactions.find((item) => item.id === context.interactionId);
    return `${interaction?.name || context.interactionId}: ${context.targetField} = ${context.value}`;
  }),
);
const activePreviewParameterHighlights = computed(() =>
  previewRuntimeParameters.value
    .filter((item) => item.value.trim())
    .map((item) => {
      const parameter = schema.value.parameters.find((current) => current.code === item.parameterCode);
      return `${parameter?.label || item.parameterCode}: ${item.value}`;
    }),
);

function textValue(value: unknown) {
  return value == null ? '' : String(value).trim();
}

function countByGender(target: '男' | '女') {
  return previewRows.value.filter((row) => {
    const value = textValue(row.gender);
    if (target === '男') {
      return ['男', 'male', 'm'].includes(value.toLowerCase ? value.toLowerCase() : value);
    }
    return ['女', 'female', 'f'].includes(value.toLowerCase ? value.toLowerCase() : value);
  }).length;
}

function countDistinct(field: string) {
  return new Set(
    previewRows.value.map((row) => textValue(row[field])).filter((value) => value.length > 0),
  ).size;
}

const previewSummary = computed(() => {
  const total = previewRows.value.length;
  const maleCount = countByGender('男');
  const femaleCount = countByGender('女');
  const withBirthDate = previewRows.value.filter((row) => textValue(row.birth_date)).length;
  const uniquePatients = new Set(
    previewRows.value.map((row) => textValue(row.patient_code)).filter((value) => value.length > 0),
  ).size;
  const withResultValue = previewRows.value.filter((row) => textValue(row.result_value)).length;
  const withLabDate = previewRows.value.filter((row) => textValue(row.sample_time || row.report_date)).length;
  const abnormalCount = previewRows.value.filter((row) => {
    const value = textValue(row.result_flag).toUpperCase();
    return value.length > 0 && value !== 'NORMAL';
  }).length;
  const outpatientCount = previewRows.value.filter((row) => textValue(row.encounter_type) === '门诊').length;
  const inpatientCount = previewRows.value.filter((row) => textValue(row.encounter_type) === '住院').length;
  return {
    total,
    maleCount,
    femaleCount,
    withBirthDate,
    uniquePatients,
    withResultValue,
    withLabDate,
    abnormalCount,
    uniqueDepartments: countDistinct('department_name'),
    uniqueDoctors: countDistinct('doctor_name'),
    outpatientCount,
    inpatientCount,
    fieldCount: previewColumns.value.length,
  };
});

const genderChartPoints = computed<TrendPoint[]>(() => [
  { label: '男性', value: previewSummary.value.maleCount },
  { label: '女性', value: previewSummary.value.femaleCount },
  {
    label: '未标注',
    value: Math.max(previewSummary.value.total - previewSummary.value.maleCount - previewSummary.value.femaleCount, 0),
  },
]);

const birthDecadeChartPoints = computed<TrendPoint[]>(() => {
  const decadeMap = new Map<string, number>();
  for (const row of previewRows.value) {
    const birthDate = textValue(row.birth_date);
    const year = Number.parseInt(birthDate.slice(0, 4), 10);
    if (!Number.isFinite(year)) {
      continue;
    }
    const decade = `${Math.floor(year / 10) * 10}后`;
    decadeMap.set(decade, (decadeMap.get(decade) ?? 0) + 1);
  }
  return Array.from(decadeMap.entries())
    .sort(([left], [right]) => left.localeCompare(right))
    .map(([label, value]) => ({ label, value }));
});

const labItemChartPoints = computed<TrendPoint[]>(() => {
  const itemMap = new Map<string, number>();
  for (const row of previewRows.value) {
    const itemName = textValue(row.item_name);
    if (!itemName) {
      continue;
    }
    itemMap.set(itemName, (itemMap.get(itemName) ?? 0) + 1);
  }
  return Array.from(itemMap.entries()).map(([label, value]) => ({ label, value }));
});

const encounterTypeChartPoints = computed<TrendPoint[]>(() => {
  const typeMap = new Map<string, number>();
  for (const row of previewRows.value) {
    const encounterType = textValue(row.encounter_type);
    if (!encounterType) {
      continue;
    }
    typeMap.set(encounterType, (typeMap.get(encounterType) ?? 0) + 1);
  }
  return Array.from(typeMap.entries()).map(([label, value]) => ({ label, value }));
});

const departmentChartPoints = computed<TrendPoint[]>(() => {
  const departmentMap = new Map<string, number>();
  for (const row of previewRows.value) {
    const departmentName = textValue(row.department_name);
    if (!departmentName) {
      continue;
    }
    departmentMap.set(departmentName, (departmentMap.get(departmentName) ?? 0) + 1);
  }
  return Array.from(departmentMap.entries()).map(([label, value]) => ({ label, value }));
});

const sampleTimeChartPoints = computed<TrendPoint[]>(() => {
  const timeMap = new Map<string, number>();
  for (const row of previewRows.value) {
    const dateValue = textValue(row.sample_time || row.report_date).slice(0, 10);
    if (!dateValue) {
      continue;
    }
    timeMap.set(dateValue, (timeMap.get(dateValue) ?? 0) + 1);
  }
  return Array.from(timeMap.entries())
    .sort(([left], [right]) => left.localeCompare(right))
    .map(([label, value]) => ({ label, value }));
});

const resultFlagChartPoints = computed<TrendPoint[]>(() => {
  const flagMap = new Map<string, number>();
  for (const row of previewRows.value) {
    const resultFlag = textValue(row.result_flag);
    if (!resultFlag) {
      continue;
    }
    flagMap.set(resultFlag, (flagMap.get(resultFlag) ?? 0) + 1);
  }
  return Array.from(flagMap.entries()).map(([label, value]) => ({ label, value }));
});

const previewStatCards = computed(() => {
  if (isLabResultDataset.value) {
    return [
      { label: '样本检验记录', value: previewSummary.value.total, icon: 'Document', color: '#409eff', trend: '当前预览返回的总记录数' },
      { label: '覆盖患者数', value: previewSummary.value.uniquePatients, icon: 'User', color: '#10b981', trend: '按 patient_code 去重统计' },
      { label: '结果已标注', value: previewSummary.value.withResultValue, icon: 'DataAnalysis', color: '#f59e0b', trend: '具备 result_value 的记录数' },
      { label: '采样时间已标注', value: previewSummary.value.withLabDate, icon: 'Timer', color: '#8b5cf6', trend: '具备 sample_time 的记录数' },
    ];
  }
  if (previewDatasetCode.value === 'cdm_lab') {
    return [
      { label: '样本检验记录', value: previewSummary.value.total, icon: 'Document', color: '#409eff', trend: '当前预览返回的总记录数' },
      { label: '覆盖患者数', value: previewSummary.value.uniquePatients, icon: 'User', color: '#10b981', trend: '按 patient_code 去重统计' },
      { label: '异常结果数', value: previewSummary.value.abnormalCount, icon: 'Warning', color: '#f59e0b', trend: 'result_flag != NORMAL 的记录数' },
      { label: '报告日期已标注', value: previewSummary.value.withLabDate, icon: 'Timer', color: '#8b5cf6', trend: '具备 report_date 的记录数' },
    ];
  }
  if (isEncounterDataset.value) {
    return [
      { label: '样本就诊记录', value: previewSummary.value.total, icon: 'Document', color: '#409eff', trend: '当前预览返回的总记录数' },
      { label: '覆盖患者数', value: previewSummary.value.uniquePatients, icon: 'User', color: '#10b981', trend: '按 patient_code 去重统计' },
      { label: '科室数量', value: previewSummary.value.uniqueDepartments, icon: 'DataAnalysis', color: '#f59e0b', trend: '按 department_name 去重统计' },
      { label: '医生数量', value: previewSummary.value.uniqueDoctors, icon: 'UserFilled', color: '#8b5cf6', trend: '按 doctor_name 去重统计' },
    ];
  }
  return [
    { label: '样本患者数', value: previewSummary.value.total, icon: 'User', color: '#409eff', trend: '当前预览返回的总记录数' },
    { label: '男性患者', value: previewSummary.value.maleCount, icon: 'Male', color: '#409eff', trend: '按 gender 字段统计' },
    { label: '女性患者', value: previewSummary.value.femaleCount, icon: 'Female', color: '#ec4899', trend: '按 gender 字段统计' },
    { label: '生日已标注', value: previewSummary.value.withBirthDate, icon: 'Calendar', color: '#10b981', trend: '具备出生日期的记录数' },
  ];
});

const previewCharts = computed<PreviewChartMeta[]>(() => {
  if (isLabResultDataset.value) {
    return [
      { title: '检验项目分布', type: 'bar' as const, points: labItemChartPoints.value, color: '#409eff', sourceField: 'item_name' },
      { title: '采样日期分布', type: 'line' as const, points: sampleTimeChartPoints.value, color: '#10b981', sourceField: 'sample_time' },
    ];
  }
  if (previewDatasetCode.value === 'cdm_lab') {
    return [
      { title: '检验项目分布', type: 'bar' as const, points: labItemChartPoints.value, color: '#409eff', sourceField: 'item_name' },
      { title: '结果标记分布', type: 'line' as const, points: resultFlagChartPoints.value, color: '#10b981', sourceField: 'result_flag' },
    ];
  }
  if (isEncounterDataset.value) {
    return [
      { title: '就诊类型分布', type: 'bar' as const, points: encounterTypeChartPoints.value, color: '#409eff', sourceField: 'encounter_type' },
      { title: '科室分布', type: 'line' as const, points: departmentChartPoints.value, color: '#10b981', sourceField: 'department_name' },
    ];
  }
  return [
    { title: '患者性别分布', type: 'bar' as const, points: genderChartPoints.value, color: '#409eff', sourceField: 'gender' },
    { title: '患者出生年代分布', type: 'line' as const, points: birthDecadeChartPoints.value, color: '#10b981', sourceField: 'birth_date' },
  ];
});

const previewDetailTitle = computed(() => {
  if (isLabDataset.value) {
    return '检验结果明细数据';
  }
  if (isEncounterDataset.value) {
    return '就诊明细数据';
  }
  return '患者明细数据';
});

const previewHighlights = computed(() => [
  `数据集：${previewDatasetCode.value}`,
  `布局列数：${previewLayoutColumns.value || schema.value.layout.columns}`,
  `栅格：${schema.value.layout.gridColumns} 列 / 行高 ${schema.value.layout.rowHeight}px`,
  `组件：${previewWidgetCount.value || schema.value.widgets.length} 个`,
  `筛选条件：${previewFilterCount.value || schema.value.filters.length} 个`,
  `字段：${previewSummary.value.fieldCount} 个`,
]);

const activeWidget = computed<ReportWidget | null>(
  () => schema.value.widgets.find((item) => item.id === activeWidgetId.value) ?? schema.value.widgets[0] ?? null,
);

function clamp(value: number, min: number, max: number) {
  return Math.min(max, Math.max(min, value));
}

function ensureWidgetBinding(widget: ReportWidget): ReportWidget {
  const fields = datasetOptions.value.find((item) => item.code === schema.value.datasetCode)?.fields ?? [];
  const firstField = fields[0]?.fieldCode ?? '';
  const secondField = fields[1]?.fieldCode ?? firstField;
  const fieldSet = new Set(fields.map((field) => field.fieldCode));
  const bindings = (widget.bindings?.length ? widget.bindings : [
    {
      id: `${widget.id}-binding-1`,
      role: 'category' as const,
      label: '分类字段',
      field: String(widget.config.xField ?? ''),
      aggregation: '',
    },
    {
      id: `${widget.id}-binding-2`,
      role: 'value' as const,
      label: '数值字段',
      field: String(widget.config.yField ?? ''),
      aggregation: String(widget.config.aggregation ?? ''),
    },
  ]).map((binding, index) => {
    const fallbackField = index === 0 ? firstField : secondField;
    return {
      ...binding,
      id: binding.id || `${widget.id}-binding-${index + 1}`,
      label: binding.label || (binding.role === 'value' ? '数值字段' : '分类字段'),
      field: fieldSet.has(binding.field) ? binding.field : fallbackField,
      aggregation: binding.aggregation || '',
    };
  });
  const categoryBinding = bindings.find((binding) => binding.role === 'category');
  const valueBinding = bindings.find((binding) => binding.role === 'value');
  return {
    ...widget,
    datasetId: schema.value.datasetCode,
    span: clamp(Number(widget.span || 1), 1, Math.max(1, schema.value.layout.columns)),
    bindings,
    config: {
      ...widget.config,
      xField: categoryBinding?.field || firstField,
      yField: valueBinding?.field || secondField,
      aggregation: valueBinding?.aggregation || '',
    },
  };
}

function normalizeInteractions(nextInteractions: ReportInteractionRule[], widgets: ReportWidget[]) {
  const widgetIds = new Set(widgets.map((widget) => widget.id));
  return nextInteractions
    .filter((interaction) => widgetIds.has(interaction.sourceWidgetId) && widgetIds.has(interaction.targetWidgetId))
    .map((interaction, index) => ({
      ...interaction,
      id: interaction.id || `interaction-${index + 1}`,
      name: interaction.name || `联动规则-${index + 1}`,
      trigger: interaction.trigger || 'click',
      action: interaction.action || 'filter',
      sourceField: interaction.sourceField || '',
      targetField: interaction.targetField || '',
    }));
}

function normalizeSchema(nextSchema: ReportTemplateSchema) {
  const normalized = normalizeTemplateLayout({
    ...nextSchema,
    datasetCode: nextSchema.datasetCode || 'cdm_patient',
    schemaVersion: nextSchema.schemaVersion || '1.1.0',
    layout: {
      columns: clamp(Number(nextSchema.layout.columns || 2), 1, 4),
      gap: clamp(Number(nextSchema.layout.gap || 16), 8, 32),
      gridColumns: clamp(Number(nextSchema.layout.gridColumns || 24), 12, 24),
      rowHeight: clamp(Number(nextSchema.layout.rowHeight || 120), 80, 200),
      canvasWidthMode: nextSchema.layout.canvasWidthMode || 'full',
    },
  });
  const normalizedWidgets = normalized.widgets.map((widget) => ensureWidgetBinding(widget));
  schema.value = {
    ...normalized,
    filters: normalized.filters.map((filter, index) => ({
      ...filter,
      id: filter.id || `filter-${index + 1}`,
      label: filter.label || filter.field,
      operator: filter.operator || 'eq',
      value: filter.value || '',
    })),
    widgets: normalizedWidgets,
    interactions: normalizeInteractions(normalized.interactions ?? [], normalizedWidgets),
  };
  syncPreviewRuntimeParameters(previewRuntimeParameters.value);
  activeWidgetId.value = schema.value.widgets.find((item) => item.id === activeWidgetId.value)?.id ?? schema.value.widgets[0]?.id ?? '';
}

function syncPreviewRuntimeParameters(existing: ReportParameterValue[] = []) {
  const valueMap = new Map(existing.map((item) => [item.parameterCode, item.value]));
  previewRuntimeParameters.value = schema.value.parameters
    .filter((parameter) => parameter.field.trim())
    .map((parameter) => ({
      parameterCode: parameter.code,
      value: valueMap.get(parameter.code) ?? parameter.defaultValue ?? '',
    }));
}

async function loadData() {
  loading.value = true;
  try {
    const [template, options] = await Promise.all([fetchReportTemplate(), fetchReportBindingOptions()]);
    datasetOptions.value = options;
    normalizeSchema(template);
  } finally {
    loading.value = false;
  }
}

function handleAddWidget(type: WidgetType) {
  const nextWidget = createWidgetByType(type);
  schema.value.widgets.push(
    ensureWidgetBinding({
      ...nextWidget,
      datasetId: schema.value.datasetCode,
    }),
  );
  activeWidgetId.value = nextWidget.id;
}

function handleRemoveWidget(id: string) {
  schema.value.widgets = schema.value.widgets.filter((item) => item.id !== id);
  activeWidgetId.value = schema.value.widgets[0]?.id ?? '';
}

function handleUpdateWidget(widget: ReportWidget) {
  schema.value.widgets = schema.value.widgets.map((item) => (item.id === widget.id ? ensureWidgetBinding(widget) : item));
}

function handleSelectWidget(id: string) {
  activeWidgetId.value = id;
}

function handleMoveWidget(id: string, direction: 'up' | 'down') {
  const index = schema.value.widgets.findIndex((item) => item.id === id);
  if (index === -1) {
    return;
  }
  const targetIndex = direction === 'up' ? index - 1 : index + 1;
  if (targetIndex < 0 || targetIndex >= schema.value.widgets.length) {
    return;
  }
  const widgets = [...schema.value.widgets];
  const [widget] = widgets.splice(index, 1);
  widgets.splice(targetIndex, 0, widget);
  normalizeSchema({
    ...schema.value,
    widgets,
  });
}

function handleDuplicateWidget(id: string) {
  const target = schema.value.widgets.find((item) => item.id === id);
  if (!target) {
    return;
  }
  const duplicated = ensureWidgetBinding(cloneWidget(target));
  const index = schema.value.widgets.findIndex((item) => item.id === id);
  const widgets = [...schema.value.widgets];
  widgets.splice(index + 1, 0, duplicated);
  normalizeSchema({
    ...schema.value,
    widgets,
  });
  activeWidgetId.value = duplicated.id;
}

function handleReorderWidget(id: string, targetIndex: number) {
  const currentIndex = schema.value.widgets.findIndex((item) => item.id === id);
  if (currentIndex === -1 || targetIndex < 0 || targetIndex >= schema.value.widgets.length) {
    return;
  }
  const widgets = [...schema.value.widgets];
  const [widget] = widgets.splice(currentIndex, 1);
  widgets.splice(targetIndex, 0, widget);
  normalizeSchema({
    ...schema.value,
    widgets,
  });
  activeWidgetId.value = widget.id;
}

function handleAddInteraction() {
  schema.value.interactions.push(createInteraction());
}

function handleUpdateInteraction(index: number, patch: Partial<ReportInteractionRule>) {
  schema.value.interactions = schema.value.interactions.map((interaction, currentIndex) =>
    currentIndex === index
      ? {
          ...interaction,
          ...patch,
        }
      : interaction,
  );
}

function handleRemoveInteraction(index: number) {
  schema.value.interactions = schema.value.interactions.filter((_, currentIndex) => currentIndex !== index);
}

function handleAddParameter() {
  schema.value.parameters.push(createParameter());
  syncPreviewRuntimeParameters(previewRuntimeParameters.value);
}

function handleUpdateParameter(index: number, patch: Partial<ReportTemplateSchema['parameters'][number]>) {
  schema.value.parameters = schema.value.parameters.map((parameter, currentIndex) =>
    currentIndex === index
      ? {
          ...parameter,
          ...patch,
        }
      : parameter,
  );
  syncPreviewRuntimeParameters(previewRuntimeParameters.value);
}

function handleRemoveParameter(index: number) {
  schema.value.parameters = schema.value.parameters.filter((_, currentIndex) => currentIndex !== index);
  syncPreviewRuntimeParameters(previewRuntimeParameters.value);
}

function handleDatasetChange(datasetCode: string) {
  normalizeSchema({
    ...schema.value,
    datasetCode,
    widgets: schema.value.widgets.map((widget) => ({
      ...widget,
      datasetId: datasetCode,
    })),
  });
}

function handleLayoutChange() {
  normalizeSchema(schema.value);
}

function handleAddFilter() {
  schema.value.filters.push(createFilter());
}

function handleUpdateFilter(index: number, patch: Partial<ReportTemplateSchema['filters'][number]>) {
  schema.value.filters = schema.value.filters.map((filter, currentIndex) =>
    currentIndex === index
      ? {
          ...filter,
          ...patch,
        }
      : filter,
  );
}

function handleRemoveFilter(index: number) {
  schema.value.filters = schema.value.filters.filter((_, currentIndex) => currentIndex !== index);
}

async function handleSave() {
  normalizeSchema(schema.value);
  await submitReportTemplate(schema.value);
  ElMessage.success('草稿已保存');
  await loadData();
}

async function handlePublish() {
  normalizeSchema(schema.value);
  if (!/^\d+$/.test(schema.value.id)) {
    await handleSave();
  }
  const published = await publishReportTemplate(schema.value.id);
  normalizeSchema(published);
  ElMessage.success(`模板已发布，当前生效版本 v${published.versioning.effectiveVersion}`);
}

async function handlePreview() {
  await loadPreview([]);
}

async function loadPreview(interactions: ReportInteractionContext[]) {
  if (!/^\d+$/.test(schema.value.id)) {
    ElMessage.warning('请先保存模板后再预览');
    return;
  }
  previewLoading.value = true;
  previewVisible.value = true;
  try {
    const result = await previewReportTemplate(schema.value.id, {
      parameters: previewRuntimeParameters.value,
      interactions,
    });
    previewRows.value = result.rows;
    previewAppliedFilters.value = result.appliedFilters;
    previewBindingSummary.value = result.bindingSummary;
    previewParameterSummary.value = result.parameterSummary;
    previewInteractionSummary.value = result.interactionSummary;
    previewInteractionContexts.value = interactions;
    previewLayoutColumns.value = result.layoutColumns;
    previewWidgetCount.value = result.widgetCount;
    previewFilterCount.value = result.filterCount;
    ElMessage.success(result.message);
  } finally {
    previewLoading.value = false;
  }
}

async function handlePreviewChartPointClick(chart: PreviewChartMeta, point: TrendPoint) {
  const matchedInteractions = schema.value.interactions.filter((interaction) => interaction.sourceField === chart.sourceField);
  if (!matchedInteractions.length) {
    ElMessage.info(`当前图表未配置基于字段 ${chart.sourceField} 的联动规则`);
    return;
  }
  const contexts = matchedInteractions.map<ReportInteractionContext>((interaction) => ({
    interactionId: interaction.id,
    sourceWidgetId: interaction.sourceWidgetId,
    targetWidgetId: interaction.targetWidgetId,
    sourceField: interaction.sourceField,
    targetField: interaction.targetField,
    value: point.label,
  }));
  await loadPreview(contexts);
  ElMessage.success(`已按 ${chart.sourceField} = ${point.label} 应用联动筛选`);
}

async function clearPreviewInteractions() {
  await loadPreview([]);
  ElMessage.success('已清空联动筛选');
}

async function handleExport(format: 'pdf' | 'excel') {
  if (!/^\d+$/.test(schema.value.id)) {
    ElMessage.warning('请先保存模板后再导出');
    return;
  }
  await exportReportTemplate(schema.value.id, format, {
    parameters: previewRuntimeParameters.value,
    interactions: previewInteractionContexts.value,
  });
  ElMessage.success(
    previewInteractionContexts.value.length || previewRuntimeParameters.value.some((item) => item.value.trim())
      ? `已开始下载带运行时参数的 ${format.toUpperCase()} 文件`
      : `已开始下载 ${format.toUpperCase()} 文件`,
  );
}

onMounted(loadData);
</script>

<template>
  <div class="page-shell report-designer-page">
    <PageHeaderCard
      class="designer-page-header"
      title="报表设计器"
      tag="低代码"
      description="采用组件树 + 布局树 + 数据绑定 JSON Schema 结构，当前已补齐模板级布局、结构化字段绑定与联动规则标准化所需的最小闭环。"
    >
      <template #actions>
        <el-tag :type="templateStateTagType">{{ templateStateText }}</el-tag>
        <el-button @click="previewJsonVisible = true">查看 Schema</el-button>
        <el-button @click="handlePreview">预览</el-button>
        <el-button @click="handleExport('pdf')">导出 PDF</el-button>
        <el-button @click="handleExport('excel')">导出 Excel</el-button>
        <el-button :loading="loading" @click="handleSave">保存草稿</el-button>
        <el-button type="primary" :loading="loading" @click="handlePublish">发布模板</el-button>
      </template>
    </PageHeaderCard>

    <el-card class="panel-card" shadow="never">
      <template #header>
        <div class="section-title">
          <h3>模板设置</h3>
        </div>
      </template>
      <el-form label-width="96px" class="template-form">
        <el-form-item label="模板名称">
          <el-input v-model="schema.name" maxlength="128" show-word-limit />
        </el-form-item>
        <el-form-item label="绑定数据集">
          <el-select :model-value="schema.datasetCode" style="width: 100%" @update:model-value="handleDatasetChange">
            <el-option v-for="dataset in datasetOptions" :key="dataset.code" :label="`${dataset.name} (${dataset.code})`" :value="dataset.code" />
          </el-select>
        </el-form-item>
        <el-form-item label="布局列数">
          <el-slider v-model="schema.layout.columns" :min="1" :max="4" show-input @change="handleLayoutChange" />
        </el-form-item>
        <el-form-item label="栅格列数">
          <el-slider v-model="schema.layout.gridColumns" :min="12" :max="24" :step="12" show-input @change="handleLayoutChange" />
        </el-form-item>
        <el-form-item label="组件间距">
          <el-slider v-model="schema.layout.gap" :min="8" :max="32" show-input @change="handleLayoutChange" />
        </el-form-item>
        <el-form-item label="行高">
          <el-slider v-model="schema.layout.rowHeight" :min="80" :max="200" :step="10" show-input @change="handleLayoutChange" />
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="panel-card" shadow="never">
      <template #header>
        <div class="section-title">
          <h3>筛选条件</h3>
          <el-button text type="primary" @click="handleAddFilter">新增筛选</el-button>
        </div>
      </template>
      <el-alert
        title="筛选条件会随模板一起保存，并由预览、导出和调度共用同一份规则。当前支持等于、包含、非空三种最小高价值匹配模式。"
        type="info"
        :closable="false"
        style="margin-bottom: 16px"
      />
      <el-empty v-if="!schema.filters.length" description="暂未配置筛选条件" :image-size="72" />
      <div v-else class="filter-list">
        <div v-for="(filter, index) in schema.filters" :key="filter.id" class="filter-row">
          <el-select
            :model-value="filter.field"
            placeholder="字段"
            @update:model-value="(value) => handleUpdateFilter(index, { field: String(value), label: filter.label || String(value) })"
          >
            <el-option
              v-for="field in activeDatasetFields"
              :key="field.fieldCode"
              :label="`${field.fieldName} (${field.fieldCode})`"
              :value="field.fieldCode"
            />
          </el-select>
          <el-input
            :model-value="filter.label"
            placeholder="显示名称"
            @update:model-value="(value) => handleUpdateFilter(index, { label: String(value) })"
          />
          <el-select
            :model-value="filter.operator"
            placeholder="操作符"
            @update:model-value="(value) => handleUpdateFilter(index, { operator: value as 'eq' | 'contains' | 'not_empty' })"
          >
            <el-option label="等于" value="eq" />
            <el-option label="包含" value="contains" />
            <el-option label="非空" value="not_empty" />
          </el-select>
          <el-input
            :model-value="filter.value"
            :disabled="filter.operator === 'not_empty'"
            placeholder="筛选值"
            @update:model-value="(value) => handleUpdateFilter(index, { value: String(value) })"
          />
          <el-button text type="danger" @click="handleRemoveFilter(index)">删除</el-button>
        </div>
      </div>
      <div v-if="filterHighlights.length" class="preview-highlights" style="margin-top: 16px">
        <el-tag v-for="item in filterHighlights" :key="item" effect="plain" round>{{ item }}</el-tag>
      </div>
    </el-card>

    <el-card class="panel-card" shadow="never">
      <template #header>
        <div class="section-title">
          <h3>版本治理</h3>
        </div>
      </template>
      <el-descriptions :column="4" border>
        <el-descriptions-item label="模板状态">
          <el-tag :type="templateStateTagType" effect="plain">{{ templateStateText }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="草稿版本">v{{ schema.versioning.draftVersion }}</el-descriptions-item>
        <el-descriptions-item label="已发布版本">v{{ schema.versioning.publishedVersion }}</el-descriptions-item>
        <el-descriptions-item label="当前生效版本">v{{ schema.versioning.effectiveVersion }}</el-descriptions-item>
        <el-descriptions-item label="基线版本">v{{ schema.versioning.baseVersion }}</el-descriptions-item>
        <el-descriptions-item label="最近保存">{{ schema.versioning.lastSavedAt || '-' }}</el-descriptions-item>
        <el-descriptions-item label="最近发布">{{ schema.versioning.lastPublishedAt || '-' }}</el-descriptions-item>
        <el-descriptions-item label="Schema 版本">{{ schema.schemaVersion }}</el-descriptions-item>
      </el-descriptions>
      <el-alert
        title="当前阶段已支持单模板草稿/发布状态和基础乐观锁校验。保存草稿会推进 draftVersion，发布会把当前草稿提升为 effectiveVersion。"
        type="info"
        :closable="false"
        style="margin-top: 16px"
      />
    </el-card>

    <el-card class="panel-card" shadow="never">
      <template #header>
        <div class="section-title">
          <h3>模板参数</h3>
          <el-button text type="primary" @click="handleAddParameter">新增参数</el-button>
        </div>
      </template>
      <el-alert
        title="模板参数用于声明可复用的运行时输入，会统一传递到预览、导出和调度链路。当前支持字符串类型，后端按字段 + 操作符生成动态筛选。"
        type="info"
        :closable="false"
        style="margin-bottom: 16px"
      />
      <el-empty v-if="!schema.parameters.length" description="暂未声明模板参数" :image-size="72" />
      <div v-else class="filter-list">
        <div v-for="(parameter, index) in schema.parameters" :key="parameter.id" class="parameter-row">
          <el-input
            :model-value="parameter.code"
            placeholder="参数编码"
            @update:model-value="(value) => handleUpdateParameter(index, { code: String(value) })"
          />
          <el-input
            :model-value="parameter.label"
            placeholder="显示名称"
            @update:model-value="(value) => handleUpdateParameter(index, { label: String(value) })"
          />
          <el-select
            :model-value="parameter.field"
            placeholder="绑定字段"
            @update:model-value="(value) => handleUpdateParameter(index, { field: String(value) })"
          >
            <el-option
              v-for="field in activeDatasetFields"
              :key="`${parameter.id}-${field.fieldCode}`"
              :label="`${field.fieldName} (${field.fieldCode})`"
              :value="field.fieldCode"
            />
          </el-select>
          <el-select
            :model-value="parameter.operator"
            placeholder="操作符"
            @update:model-value="(value) => handleUpdateParameter(index, { operator: value as 'eq' | 'contains' | 'not_empty' })"
          >
            <el-option label="等于" value="eq" />
            <el-option label="包含" value="contains" />
            <el-option label="非空" value="not_empty" />
          </el-select>
          <el-input
            :model-value="parameter.defaultValue"
            :disabled="parameter.operator === 'not_empty'"
            placeholder="默认值"
            @update:model-value="(value) => handleUpdateParameter(index, { defaultValue: String(value) })"
          />
          <el-switch
            :model-value="parameter.required"
            inline-prompt
            active-text="必填"
            inactive-text="选填"
            @update:model-value="(value) => handleUpdateParameter(index, { required: Boolean(value) })"
          />
          <el-button text type="danger" @click="handleRemoveParameter(index)">删除</el-button>
        </div>
      </div>
      <div v-if="parameterHighlights.length" class="preview-highlights" style="margin-top: 16px">
        <el-tag v-for="item in parameterHighlights" :key="item" type="warning" effect="plain" round>{{ item }}</el-tag>
      </div>
    </el-card>

    <el-card class="panel-card" shadow="never">
      <template #header>
        <div class="section-title">
          <h3>运行时输入</h3>
        </div>
      </template>
      <el-alert
        title="这里填写的是当前预览/导出的临时参数值；保存模板不会写入这些运行值，调度页会独立维护固定运行参数。"
        type="success"
        :closable="false"
        style="margin-bottom: 16px"
      />
      <el-empty v-if="!activeTemplateParameters.length" description="当前模板未声明可输入参数" :image-size="72" />
      <div v-else class="runtime-param-list">
        <div
          v-for="parameter in previewRuntimeParameters"
          :key="parameter.parameterCode"
          class="runtime-param-row"
        >
          <div class="runtime-param-meta">
            <strong>{{ schema.parameters.find((item) => item.code === parameter.parameterCode)?.label || parameter.parameterCode }}</strong>
            <div class="notify-content">
              {{
                `${schema.parameters.find((item) => item.code === parameter.parameterCode)?.field || '-'} / ${
                  schema.parameters.find((item) => item.code === parameter.parameterCode)?.operator || 'eq'
                }`
              }}
            </div>
          </div>
          <el-input
            v-model="parameter.value"
            :disabled="schema.parameters.find((item) => item.code === parameter.parameterCode)?.operator === 'not_empty'"
            placeholder="输入预览参数值"
          />
        </div>
      </div>
      <div v-if="activePreviewParameterHighlights.length" class="preview-highlights" style="margin-top: 16px">
        <el-tag v-for="item in activePreviewParameterHighlights" :key="item" type="success" effect="plain" round>{{ item }}</el-tag>
      </div>
    </el-card>

    <el-card class="panel-card" shadow="never">
      <template #header>
        <div class="section-title">
          <h3>联动规则</h3>
          <el-button text type="primary" @click="handleAddInteraction">新增联动</el-button>
        </div>
      </template>
      <el-alert
        title="联动规则会与模板一起保存，当前标准化为“源组件字段 -> 目标组件筛选字段”的结构化规则，为后续点击联动预览、导出和调度参数统一打基础。"
        type="info"
        :closable="false"
        style="margin-bottom: 16px"
      />
      <el-empty v-if="!schema.interactions.length" description="暂未配置联动规则" :image-size="72" />
      <div v-else class="filter-list">
        <div v-for="(interaction, index) in schema.interactions" :key="interaction.id" class="interaction-row">
          <el-input
            :model-value="interaction.name"
            placeholder="规则名称"
            @update:model-value="(value) => handleUpdateInteraction(index, { name: String(value) })"
          />
          <el-select
            :model-value="interaction.sourceWidgetId"
            placeholder="源组件"
            @update:model-value="(value) => handleUpdateInteraction(index, { sourceWidgetId: String(value) })"
          >
            <el-option v-for="widget in schema.widgets" :key="widget.id" :label="widget.title" :value="widget.id" />
          </el-select>
          <el-select
            :model-value="interaction.sourceField"
            placeholder="源字段"
            @update:model-value="(value) => handleUpdateInteraction(index, { sourceField: String(value) })"
          >
            <el-option
              v-for="field in activeDatasetFields"
              :key="`${interaction.id}-source-${field.fieldCode}`"
              :label="`${field.fieldName} (${field.fieldCode})`"
              :value="field.fieldCode"
            />
          </el-select>
          <el-select
            :model-value="interaction.targetWidgetId"
            placeholder="目标组件"
            @update:model-value="(value) => handleUpdateInteraction(index, { targetWidgetId: String(value) })"
          >
            <el-option v-for="widget in schema.widgets" :key="`${widget.id}-target`" :label="widget.title" :value="widget.id" />
          </el-select>
          <el-select
            :model-value="interaction.targetField"
            placeholder="目标筛选字段"
            @update:model-value="(value) => handleUpdateInteraction(index, { targetField: String(value) })"
          >
            <el-option
              v-for="field in activeDatasetFields"
              :key="`${interaction.id}-target-${field.fieldCode}`"
              :label="`${field.fieldName} (${field.fieldCode})`"
              :value="field.fieldCode"
            />
          </el-select>
          <el-button text type="danger" @click="handleRemoveInteraction(index)">删除</el-button>
        </div>
      </div>
      <div v-if="interactionHighlights.length" class="preview-highlights" style="margin-top: 16px">
        <el-tag v-for="item in interactionHighlights" :key="item" type="success" effect="plain" round>{{ item }}</el-tag>
      </div>
    </el-card>

    <div class="designer-grid">
      <DesignerPalette class="designer-sticky-card" @add="handleAddWidget" />
      <DesignerCanvas
        :widgets="schema.widgets"
        :active-id="activeWidgetId"
        :layout-columns="schema.layout.columns"
        :grid-columns="schema.layout.gridColumns"
        :row-height="schema.layout.rowHeight"
        :layout-gap="schema.layout.gap"
        @select="handleSelectWidget"
        @remove="handleRemoveWidget"
        @move="handleMoveWidget"
        @duplicate="handleDuplicateWidget"
        @reorder="handleReorderWidget"
      />
      <DesignerPropertyPanel
        class="designer-sticky-card"
        :widget="activeWidget"
        :dataset-fields="activeDatasetFields"
        :layout-columns="schema.layout.columns"
        @update="handleUpdateWidget"
      />
    </div>

    <el-card class="panel-card" shadow="never">
      <template #header>
        <div class="section-title">
          <h3>数据绑定与发布说明</h3>
        </div>
      </template>
      <el-alert
        :title="`当前模板绑定 ${schema.datasetCode} 数据集，保存后会以同一份配置驱动预览、导出与调度。`"
        type="info"
        :closable="false"
      />
      <el-descriptions :column="3" border style="margin-top: 16px">
        <el-descriptions-item label="模板名称">{{ schema.name }}</el-descriptions-item>
        <el-descriptions-item label="绑定数据集">{{ schema.datasetCode }}</el-descriptions-item>
        <el-descriptions-item label="组件数量">{{ schema.widgets.length }}</el-descriptions-item>
        <el-descriptions-item label="布局列数">{{ schema.layout.columns }}</el-descriptions-item>
        <el-descriptions-item label="栅格列数">{{ schema.layout.gridColumns }}</el-descriptions-item>
        <el-descriptions-item label="模板参数">{{ activeTemplateParameters.length }}</el-descriptions-item>
        <el-descriptions-item label="行高 / 间距">{{ schema.layout.rowHeight }} / {{ schema.layout.gap }} px</el-descriptions-item>
      </el-descriptions>
      <div class="preview-highlights" style="margin-top: 16px">
        <el-tag v-for="item in bindingHighlights" :key="item" effect="plain" round>{{ item }}</el-tag>
      </div>
    </el-card>

    <el-dialog v-model="previewJsonVisible" title="报表 Schema" width="760px">
      <pre class="schema-preview">{{ serializeTemplate(schema) }}</pre>
    </el-dialog>

    <el-dialog v-model="previewVisible" title="报表预览结果" width="960px">
      <el-alert
        title="当前预览展示的是后端基于同一份 schema 归一化后的样例数据与规则摘要，可用于确认字段绑定和联动规则已统一保存。"
        type="success"
        :closable="false"
        style="margin-bottom: 16px"
      />
      <div v-loading="previewLoading" class="page-shell preview-shell">
        <el-card class="panel-card" shadow="never">
          <template #header>
            <div class="section-title">
              <h3>报表概览</h3>
              <el-tag type="success">{{ schema.name }}</el-tag>
            </div>
          </template>
          <div class="preview-highlights">
            <el-tag v-for="item in previewHighlights" :key="item" effect="plain" round>{{ item }}</el-tag>
          </div>
          <div v-if="previewAppliedFilters.length" class="preview-highlights" style="margin-top: 12px">
            <el-tag v-for="item in previewAppliedFilters" :key="item" type="warning" effect="plain" round>{{ item }}</el-tag>
          </div>
          <div v-if="previewBindingSummary.length" class="preview-highlights" style="margin-top: 12px">
            <el-tag v-for="item in previewBindingSummary" :key="item" type="success" effect="plain" round>{{ item }}</el-tag>
          </div>
          <div v-if="previewParameterSummary.length" class="preview-highlights" style="margin-top: 12px">
            <el-tag v-for="item in previewParameterSummary" :key="item" type="warning" effect="plain" round>{{ item }}</el-tag>
          </div>
          <div v-if="previewInteractionSummary.length" class="preview-highlights" style="margin-top: 12px">
            <el-tag v-for="item in previewInteractionSummary" :key="item" type="info" effect="plain" round>{{ item }}</el-tag>
          </div>
          <div v-if="activeInteractionHighlights.length" class="preview-highlights" style="margin-top: 12px">
            <el-tag v-for="item in activeInteractionHighlights" :key="item" type="danger" effect="plain" round>{{ item }}</el-tag>
            <el-button text type="primary" @click="clearPreviewInteractions">清空联动</el-button>
          </div>
        </el-card>

        <div class="page-grid preview-stats">
          <StatCard
            v-for="card in previewStatCards"
            :key="card.label"
            :label="card.label"
            :value="card.value"
            :icon="card.icon"
            :color="card.color"
            :trend="card.trend"
          />
        </div>

        <div v-if="previewRows.length" class="page-grid columns-2">
          <TrendChart
            v-for="chart in previewCharts"
            :key="chart.title"
            :title="chart.title"
            :type="chart.type"
            :points="chart.points"
            :color="chart.color"
            @point-click="(point) => handlePreviewChartPointClick(chart, point)"
          />
        </div>

        <el-card class="panel-card" shadow="never">
          <template #header>
            <div class="section-title">
              <h3>{{ previewDetailTitle }}</h3>
              <el-tag effect="plain">{{ previewRows.length }} 行</el-tag>
            </div>
          </template>
          <el-table v-if="previewRows.length" :data="previewRows" max-height="420" border>
            <el-table-column
              v-for="column in previewColumns"
              :key="column"
              :prop="column"
              :label="column"
              min-width="160"
            />
          </el-table>
          <el-empty v-else description="当前预览无数据，请先确认已插入示例数据并保存模板。" />
        </el-card>
      </div>
    </el-dialog>
  </div>
</template>

<style scoped>
.report-designer-page {
  padding-bottom: 12px;
}

.designer-page-header {
  top: -1px;
  z-index: 15;
}

.designer-page-header :deep(.el-card) {
  border-color: rgba(15, 23, 42, 0.08);
  background: rgba(255, 255, 255, 0.94);
  backdrop-filter: blur(14px);
}

.designer-page-header :deep(.header-content) {
  align-items: flex-start;
}

.designer-page-header :deep(.header-actions) {
  flex-wrap: wrap;
  justify-content: flex-end;
}

.designer-grid {
  display: grid;
  grid-template-columns: 240px minmax(0, 1fr) 320px;
  gap: 16px;
  align-items: start;
}

.designer-sticky-card {
  position: sticky;
  top: 112px;
}

.template-form {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  column-gap: 16px;
}

.schema-preview {
  margin: 0;
  padding: 16px;
  overflow: auto;
  border-radius: 14px;
  background: #0f172a;
  color: #e2e8f0;
}

.filter-list {
  display: grid;
  gap: 12px;
}

.filter-row {
  display: grid;
  grid-template-columns: 1.3fr 1fr 120px 1fr auto;
  gap: 12px;
  align-items: center;
}

.interaction-row {
  display: grid;
  grid-template-columns: 1.1fr 1fr 1fr 1fr 1fr auto;
  gap: 12px;
  align-items: center;
}

.parameter-row {
  display: grid;
  grid-template-columns: 1fr 1fr 1.2fr 120px 1fr 100px auto;
  gap: 12px;
  align-items: center;
}

.runtime-param-list {
  display: grid;
  gap: 12px;
}

.runtime-param-row {
  display: grid;
  grid-template-columns: minmax(0, 220px) minmax(0, 1fr);
  gap: 12px;
  align-items: center;
}

.runtime-param-meta {
  color: #1f2937;
}

.notify-content {
  margin-top: 4px;
  color: #475569;
}

.preview-shell {
  min-height: 320px;
}

.preview-highlights {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.preview-stats {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

@media (max-width: 1400px) {
  .designer-grid {
    grid-template-columns: 1fr;
  }

  .designer-sticky-card {
    position: static;
  }
}

@media (max-width: 1200px) {
  .designer-page-header {
    top: 0;
  }

  .template-form {
    grid-template-columns: 1fr;
  }

  .filter-row {
    grid-template-columns: 1fr;
  }

  .interaction-row {
    grid-template-columns: 1fr;
  }

  .parameter-row {
    grid-template-columns: 1fr;
  }

  .preview-stats {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .preview-stats {
    grid-template-columns: 1fr;
  }
}
</style>
