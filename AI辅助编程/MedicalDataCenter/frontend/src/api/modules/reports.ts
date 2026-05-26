import { httpClient, request } from '@/api/http';
import type {
  ReportInteractionRule,
  ReportInteractionContext,
  ReportParameterValue,
  ReportRuntimeParams,
  ReportDatasetOption,
  ReportJobItem,
  ReportNotificationItem,
  ReportPreviewRequest,
  ReportPreviewResult,
  ReportScheduleForm,
  ReportSnapshotItem,
  ReportTemplateParameter,
  ReportTemplateSchema,
  ReportWidgetBinding,
} from '@/types/report';
import { createDefaultTemplate, createInteraction, createParameter, createDefaultGrid, createDefaultVersioning, normalizeTemplateLayout } from '@/utils/schema';

interface BackendReportTemplate {
  id: number;
  name: string;
  datasetCode: string;
  designJson: string;
  status: string;
  createdAt: string;
}

interface BackendReportSchedule {
  id: number;
  templateId: number;
  templateVersion: number;
  cronExpression: string;
  enabled: boolean;
  channel: string;
  latestRun?: string | null;
  nextRun?: string | null;
  owner?: string;
  runtimeParams?: ReportRuntimeParams;
}

interface BackendDatasetField {
  fieldCode: string;
  fieldName: string;
  fieldType: string;
}

interface BackendDatasetOption {
  code: string;
  name: string;
  description: string;
  fields: BackendDatasetField[];
}

interface BackendReportSnapshot {
  id: number;
  templateId: number;
  scheduleId?: number | null;
  templateVersion: number;
  templateName: string;
  exportPath: string;
  runtimeParams?: ReportRuntimeParams;
  createdAt: string;
}

interface BackendReportNotification {
  id: number;
  title: string;
  content: string;
  recipient: string;
  read: boolean;
  createdAt: string;
}

async function fetchTemplates() {
  return request<BackendReportTemplate[]>({
    url: '/reports/templates',
    method: 'get',
  });
}

function normalizeBindings(schemaDatasetCode: string, widget: ReportTemplateSchema['widgets'][number]): ReportWidgetBinding[] {
  if (widget.bindings?.length) {
    return widget.bindings.map((binding, index) => ({
      id: binding.id || `${widget.id}-binding-${index + 1}`,
      role: binding.role || (index === 0 ? 'category' : 'value'),
      label: binding.label || (index === 0 ? '分类字段' : '数值字段'),
      field: binding.field || '',
      aggregation: binding.aggregation || '',
    }));
  }
  return [
    {
      id: `${widget.id}-binding-1`,
      role: 'category',
      label: '分类字段',
      field: String(widget.config?.xField ?? ''),
      aggregation: '',
    },
    {
      id: `${widget.id}-binding-2`,
      role: 'value',
      label: '数值字段',
      field: String(widget.config?.yField ?? ''),
      aggregation: String(widget.config?.aggregation ?? ''),
    },
  ];
}

function normalizeInteractions(
  interactions: ReportTemplateSchema['interactions'] | undefined,
  widgets: ReportTemplateSchema['widgets'],
): ReportInteractionRule[] {
  const widgetIds = new Set(widgets.map((widget) => widget.id));
  return (interactions ?? [])
    .filter((interaction) => widgetIds.has(interaction.sourceWidgetId) && widgetIds.has(interaction.targetWidgetId))
    .map((interaction, index) => ({
      ...createInteraction(),
      ...interaction,
      id: interaction.id || `interaction-${index + 1}`,
      name: interaction.name || `联动规则-${index + 1}`,
      trigger: interaction.trigger || 'click',
      action: interaction.action || 'filter',
      sourceField: interaction.sourceField || '',
      targetField: interaction.targetField || '',
    }));
}

function normalizeParameters(parameters: ReportTemplateSchema['parameters'] | undefined): ReportTemplateParameter[] {
  return (parameters ?? [])
    .map((parameter, index) => ({
      ...createParameter(),
      ...parameter,
      id: parameter.id || `param-${index + 1}`,
      code: parameter.code || `param_${index + 1}`,
      label: parameter.label || parameter.field || parameter.code || `参数-${index + 1}`,
      field: parameter.field || '',
      operator: parameter.operator || 'eq',
      defaultValue: parameter.defaultValue || '',
      required: Boolean(parameter.required),
      type: parameter.type || 'string',
    }))
    .filter((parameter) => parameter.field);
}

function normalizeRuntimeParams(
  runtimeParams: ReportRuntimeParams | undefined,
  template?: Pick<ReportTemplateSchema, 'parameters' | 'interactions'>,
): ReportRuntimeParams {
  const parameterDefinitions = template?.parameters ?? [];
  const parameterMap = new Map((runtimeParams?.parameters ?? []).map((item) => [item.parameterCode, item.value]));
  const parameters = parameterDefinitions.map<ReportParameterValue>((parameter) => ({
    parameterCode: parameter.code,
    value: parameterMap.get(parameter.code) ?? parameter.defaultValue ?? '',
  }));
  const interactionDefinitions = template?.interactions ?? [];
  const interactionMap = new Map((runtimeParams?.interactions ?? []).map((item) => [item.interactionId, item]));
  const interactions = interactionDefinitions.map<ReportInteractionContext>((interaction) => ({
    interactionId: interaction.id,
    sourceWidgetId: interaction.sourceWidgetId,
    targetWidgetId: interaction.targetWidgetId,
    sourceField: interaction.sourceField,
    targetField: interaction.targetField,
    value: interactionMap.get(interaction.id)?.value ?? '',
  }));
  return {
    parameters,
    interactions,
  };
}

function parseTemplate(item: BackendReportTemplate): ReportTemplateSchema {
  try {
    const schema = JSON.parse(item.designJson) as ReportTemplateSchema;
    const datasetCode = schema.datasetCode || item.datasetCode;
    const widgets = (schema.widgets ?? []).map((widget) => ({
      ...widget,
      datasetId: widget.datasetId || datasetCode,
      span: Number(widget.span ?? 1),
      sortOrder: Number(widget.sortOrder ?? 0),
      visible: widget.visible !== false,
      grid: widget.grid ?? createDefaultGrid(widget.type),
      bindings: normalizeBindings(datasetCode, widget),
    }));
    return normalizeTemplateLayout({
      ...schema,
      id: String(item.id),
      name: item.name,
      schemaVersion: schema.schemaVersion || '1.1.0',
      status: item.status,
      datasetCode,
      versioning: {
        ...createDefaultVersioning(),
        ...schema.versioning,
        state: item.status === 'PUBLISHED' ? 'published' : (schema.versioning?.state || 'draft'),
      },
      layout: {
        columns: Number(schema.layout?.columns ?? 2),
        gap: Number(schema.layout?.gap ?? 16),
        gridColumns: Number(schema.layout?.gridColumns ?? 24),
        rowHeight: Number(schema.layout?.rowHeight ?? 120),
        canvasWidthMode: schema.layout?.canvasWidthMode || 'full',
      },
      filters: (schema.filters ?? []).map((filter, index) => ({
        id: filter.id || `filter-${index + 1}`,
        field: filter.field || '',
        label: filter.label || filter.field || '',
        operator: filter.operator || 'eq',
        value: filter.value || '',
      })),
      parameters: normalizeParameters(schema.parameters),
      widgets,
      interactions: normalizeInteractions(schema.interactions, widgets),
    });
  } catch {
    return {
      ...createDefaultTemplate(),
      id: String(item.id),
      name: item.name,
      datasetCode: item.datasetCode,
    };
  }
}

function normalizeTemplate(payload: ReportTemplateSchema): ReportTemplateSchema {
  const datasetCode = payload.datasetCode || payload.widgets[0]?.datasetId || 'cdm_patient';
  const widgets = payload.widgets.map((widget, index) => {
    const bindings = normalizeBindings(datasetCode, {
      ...widget,
      datasetId: datasetCode,
    });
    const categoryBinding = bindings.find((binding) => binding.role === 'category');
    const valueBinding = bindings.find((binding) => binding.role === 'value');
    return {
      ...widget,
      datasetId: datasetCode,
      span: Math.max(1, Number(widget.span || 1)),
      sortOrder: Number(widget.sortOrder || index + 1),
      visible: widget.visible !== false,
      grid: widget.grid ?? createDefaultGrid(widget.type),
      bindings,
      config: {
        ...widget.config,
        xField: categoryBinding?.field || '',
        yField: valueBinding?.field || '',
        aggregation: valueBinding?.aggregation || '',
      },
    };
  });
  return normalizeTemplateLayout({
    ...payload,
    schemaVersion: payload.schemaVersion || '1.1.0',
    status: payload.status || (payload.versioning?.state === 'published' ? 'PUBLISHED' : 'DRAFT'),
    datasetCode,
    versioning: {
      ...createDefaultVersioning(),
      ...payload.versioning,
    },
    layout: {
      columns: Number(payload.layout.columns || 2),
      gap: Number(payload.layout.gap || 16),
      gridColumns: Number(payload.layout.gridColumns || 24),
      rowHeight: Number(payload.layout.rowHeight || 120),
      canvasWidthMode: payload.layout.canvasWidthMode || 'full',
    },
    filters: payload.filters.map((filter, index) => ({
      ...filter,
      id: filter.id || `filter-${index + 1}`,
      label: filter.label || filter.field,
      operator: filter.operator || 'eq',
      value: filter.value || '',
    })),
    parameters: normalizeParameters(payload.parameters),
    widgets,
    interactions: normalizeInteractions(payload.interactions, widgets),
  });
}

export async function fetchReportTemplates() {
  const templates = await fetchTemplates();
  return templates.map(parseTemplate);
}

export async function fetchReportTemplate() {
  const templates = await fetchReportTemplates();
  if (!templates.length) {
    return createDefaultTemplate();
  }
  return templates[0];
}

export function fetchReportBindingOptions() {
  return request<BackendDatasetOption[]>({
    url: '/datasets',
    method: 'get',
  }).then((items) =>
    items.map<ReportDatasetOption>((item) => ({
      code: item.code,
      name: item.name,
      description: item.description,
      fields: item.fields,
    })),
  );
}

export function submitReportTemplate(payload: ReportTemplateSchema) {
  const normalized = normalizeTemplate(payload);
  const isExisting = /^\d+$/.test(normalized.id);
  const versioning = {
    ...normalized.versioning,
    baseVersion: isExisting ? normalized.versioning.draftVersion : 0,
    state: 'draft' as const,
  };
  const body = {
    name: normalized.name,
    datasetCode: normalized.datasetCode,
    designJson: JSON.stringify({
      ...normalized,
      status: 'DRAFT',
      versioning,
    }),
  };
  return request({
    url: isExisting ? `/reports/templates/${normalized.id}` : '/reports/templates',
    method: isExisting ? 'put' : 'post',
    data: body,
  });
}

export function publishReportTemplate(id: string) {
  return request<BackendReportTemplate>({
    url: `/reports/templates/${id}/publish`,
    method: 'post',
  }).then(parseTemplate);
}

export async function fetchReportJobs() {
  const [schedules, templates] = await Promise.all([
    request<BackendReportSchedule[]>({
      url: '/reports/schedules',
      method: 'get',
    }),
    fetchTemplates(),
  ]);
  return schedules.map<ReportJobItem>((item) => ({
    id: String(item.id),
    templateId: String(item.templateId),
    templateName: templates.find((template) => template.id === item.templateId)?.name ?? `模板-${item.templateId}`,
    templateVersion: Number(item.templateVersion ?? 0),
    cron: item.cronExpression,
    pushChannel: item.channel,
    latestRun: item.latestRun ?? '-',
    nextRun: item.nextRun ?? '-',
    status: item.enabled ? 'enabled' : 'disabled',
    owner: item.owner ?? '',
    runtimeParams: normalizeRuntimeParams(item.runtimeParams, parseTemplate(templates.find((template) => template.id === item.templateId) ?? templates[0] ?? {
      id: item.templateId,
      name: `模板-${item.templateId}`,
      datasetCode: 'cdm_patient',
      designJson: JSON.stringify(createDefaultTemplate()),
      status: 'PUBLISHED',
      createdAt: '',
    })),
  }));
}

export async function previewReportTemplate(id?: string, payload?: ReportPreviewRequest) {
  const templates = await fetchTemplates();
  const targetId = id ?? String(templates[0]?.id ?? '');
  const hasRuntimeParams = Boolean(payload?.interactions?.length || payload?.parameters?.length);
  const data = await request<ReportPreviewResult>({
    url: `/reports/templates/${targetId}/preview`,
    method: hasRuntimeParams ? 'post' : 'get',
    data: hasRuntimeParams ? payload : undefined,
  });
  return {
    ...data,
    message: `预览成功，数据集 ${data.datasetCode} 返回 ${data.rows.length} 行样本数据`,
  };
}

export function createReportSchedule(payload: ReportScheduleForm) {
  return request({
    url: '/reports/schedules',
    method: 'post',
    data: {
      templateId: Number(payload.templateId),
      cronExpression: payload.cronExpression,
      enabled: payload.enabled,
      channel: payload.channel,
      runtimeParams: payload.runtimeParams,
    },
  });
}

export function updateReportSchedule(id: string, payload: ReportScheduleForm) {
  return request({
    url: `/reports/schedules/${id}`,
    method: 'put',
    data: {
      templateId: Number(payload.templateId),
      cronExpression: payload.cronExpression,
      enabled: payload.enabled,
      channel: payload.channel,
      runtimeParams: payload.runtimeParams,
    },
  });
}

export function runReportSchedule(id: string) {
  return request<{ message: string }>({
    url: `/reports/schedules/${id}/run`,
    method: 'post',
  });
}

export function fetchReportSnapshots() {
  return request<BackendReportSnapshot[]>({
    url: '/reports/snapshots',
    method: 'get',
  }).then((items) =>
    items.map<ReportSnapshotItem>((item) => ({
      id: String(item.id),
      templateId: String(item.templateId),
      scheduleId: item.scheduleId ? String(item.scheduleId) : '',
      templateVersion: Number(item.templateVersion ?? 0),
      templateName: item.templateName,
      exportPath: item.exportPath,
      runtimeParams: item.runtimeParams ?? { parameters: [], interactions: [] },
      createdAt: item.createdAt,
    })),
  );
}

export function fetchReportNotifications() {
  return request<BackendReportNotification[]>({
    url: '/reports/notifications',
    method: 'get',
  }).then((items) =>
    items.map<ReportNotificationItem>((item) => ({
      id: String(item.id),
      title: item.title,
      content: item.content,
      recipient: item.recipient,
      read: item.read,
      createdAt: item.createdAt,
    })),
  );
}

function resolveDownloadName(disposition: string | undefined, fallback: string) {
  const matched = disposition?.match(/filename="?([^"]+)"?/i);
  return matched?.[1] ?? fallback;
}

export async function exportReportTemplate(id: string, format: 'pdf' | 'excel', runtimeParams?: ReportRuntimeParams) {
  const suffix = format === 'pdf' ? 'pdf' : 'excel';
  const hasRuntimeParams = Boolean(runtimeParams?.interactions?.length || runtimeParams?.parameters?.length);
  const response = hasRuntimeParams
    ? await httpClient.post<Blob>(`/reports/templates/${id}/export/${suffix}`, runtimeParams, {
        responseType: 'blob',
      })
    : await httpClient.get<Blob>(`/reports/templates/${id}/export/${suffix}`, {
        responseType: 'blob',
      });
  const blobUrl = window.URL.createObjectURL(response.data);
  const link = document.createElement('a');
  link.href = blobUrl;
  link.download = resolveDownloadName(response.headers['content-disposition'], `report-${id}.${suffix === 'excel' ? 'xlsx' : 'pdf'}`);
  document.body.appendChild(link);
  link.click();
  link.remove();
  window.URL.revokeObjectURL(blobUrl);
}
