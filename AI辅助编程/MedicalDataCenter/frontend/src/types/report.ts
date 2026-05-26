export type WidgetType = 'metric' | 'line' | 'bar' | 'pie' | 'table';
export type ReportFilterOperator = 'eq' | 'contains' | 'not_empty';
export type ReportBindingRole = 'category' | 'value';
export type ReportInteractionTrigger = 'click' | 'select';
export type ReportInteractionAction = 'filter';
export type ReportParameterType = 'string';
export type ReportCanvasWidthMode = 'fixed' | 'full';
export type ReportTemplateState = 'draft' | 'published';

export interface ReportWidgetBinding {
  id: string;
  role: ReportBindingRole;
  label: string;
  field: string;
  aggregation?: string;
}

export interface ReportInteractionRule {
  id: string;
  name: string;
  sourceWidgetId: string;
  targetWidgetId: string;
  trigger: ReportInteractionTrigger;
  action: ReportInteractionAction;
  sourceField: string;
  targetField: string;
}

export interface ReportInteractionContext {
  interactionId: string;
  sourceWidgetId: string;
  targetWidgetId: string;
  sourceField: string;
  targetField: string;
  value: string;
}

export interface ReportTemplateParameter {
  id: string;
  code: string;
  label: string;
  field: string;
  operator: ReportFilterOperator;
  defaultValue: string;
  required: boolean;
  type: ReportParameterType;
}

export interface ReportParameterValue {
  parameterCode: string;
  value: string;
}

export interface ReportRuntimeParams {
  parameters: ReportParameterValue[];
  interactions: ReportInteractionContext[];
}

export interface ReportWidgetGrid {
  x: number;
  y: number;
  w: number;
  h: number;
}

export interface ReportWidget {
  id: string;
  type: WidgetType;
  title: string;
  datasetId: string;
  span: number;
  sortOrder: number;
  visible: boolean;
  grid: ReportWidgetGrid;
  description?: string;
  bindings: ReportWidgetBinding[];
  config: Record<string, string | number | boolean | undefined>;
}

export interface ReportTemplateVersioning {
  baseVersion: number;
  draftVersion: number;
  publishedVersion: number;
  effectiveVersion: number;
  state: ReportTemplateState;
  lastSavedAt: string;
  lastPublishedAt: string;
}

export interface ReportTemplateSchema {
  id: string;
  name: string;
  schemaVersion: string;
  status?: string;
  datasetCode: string;
  versioning: ReportTemplateVersioning;
  layout: {
    columns: number;
    gap: number;
    gridColumns: number;
    rowHeight: number;
    canvasWidthMode: ReportCanvasWidthMode;
  };
  filters: ReportFilter[];
  parameters: ReportTemplateParameter[];
  widgets: ReportWidget[];
  interactions: ReportInteractionRule[];
}

export interface ReportFilter {
  id: string;
  field: string;
  label: string;
  operator: ReportFilterOperator;
  value: string;
}

export interface ReportJobItem {
  id: string;
  templateId: string;
  templateName: string;
  templateVersion: number;
  cron: string;
  pushChannel: string;
  latestRun: string;
  nextRun: string;
  status: 'enabled' | 'disabled';
  owner?: string;
  runtimeParams: ReportRuntimeParams;
}

export interface ReportDatasetField {
  fieldCode: string;
  fieldName: string;
  fieldType: string;
}

export interface ReportDatasetOption {
  code: string;
  name: string;
  description: string;
  fields: ReportDatasetField[];
}

export interface ReportScheduleForm {
  templateId: string;
  cronExpression: string;
  enabled: boolean;
  channel: string;
  runtimeParams: ReportRuntimeParams;
}

export interface ReportSnapshotItem {
  id: string;
  templateId: string;
  scheduleId?: string;
  templateVersion: number;
  templateName: string;
  exportPath: string;
  runtimeParams: ReportRuntimeParams;
  createdAt: string;
}

export interface ReportNotificationItem {
  id: string;
  title: string;
  content: string;
  recipient: string;
  read: boolean;
  createdAt: string;
}

export interface ReportPreviewResult {
  templateName: string;
  datasetCode: string;
  layoutColumns: number;
  widgetCount: number;
  filterCount: number;
  appliedFilters: string[];
  bindingSummary: string[];
  interactionSummary: string[];
  parameterSummary: string[];
  rows: Array<Record<string, unknown>>;
}

export interface ReportPreviewRequest extends ReportRuntimeParams {}
