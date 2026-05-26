import type {
  ReportCanvasWidthMode,
  ReportBindingRole,
  ReportFilter,
  ReportInteractionRule,
  ReportTemplateParameter,
  ReportTemplateSchema,
  ReportTemplateVersioning,
  ReportWidget,
  ReportWidgetGrid,
  ReportWidgetBinding,
  WidgetType,
} from '@/types/report';

const DEFAULT_SCHEMA_VERSION = '1.1.0';
const DEFAULT_GRID_COLUMNS = 24;
const DEFAULT_ROW_HEIGHT = 120;
const DEFAULT_CANVAS_WIDTH_MODE: ReportCanvasWidthMode = 'full';

function clamp(value: number, min: number, max: number) {
  return Math.min(max, Math.max(min, value));
}

function createBinding(role: ReportBindingRole, label: string, field: string, aggregation = ''): ReportWidgetBinding {
  return {
    id: `binding-${Date.now()}-${Math.random().toString(16).slice(2, 6)}`,
    role,
    label,
    field,
    aggregation,
  };
}

function createDefaultBindings(type: WidgetType): ReportWidgetBinding[] {
  if (type === 'pie') {
    return [
      createBinding('category', '分类字段', 'patient_name'),
      createBinding('value', '数值字段', 'patient_code', 'count'),
    ];
  }
  if (type === 'metric') {
    return [
      createBinding('category', '主展示字段', 'patient_name'),
      createBinding('value', '指标字段', 'patient_code', 'count'),
    ];
  }
  if (type === 'table') {
    return [
      createBinding('category', '主列字段', 'patient_name'),
      createBinding('value', '辅助列字段', 'patient_code'),
    ];
  }
  return [
    createBinding('category', '维度字段', 'patient_name'),
    createBinding('value', '指标字段', 'patient_code', 'count'),
  ];
}

export function createFilter(): ReportFilter {
  return {
    id: `filter-${Date.now()}-${Math.random().toString(16).slice(2, 6)}`,
    field: '',
    label: '',
    operator: 'eq',
    value: '',
  };
}

export function createParameter(): ReportTemplateParameter {
  return {
    id: `param-${Date.now()}-${Math.random().toString(16).slice(2, 6)}`,
    code: '',
    label: '',
    field: '',
    operator: 'eq',
    defaultValue: '',
    required: false,
    type: 'string',
  };
}

export function createWidgetByType(type: WidgetType): ReportWidget {
  const grid = createDefaultGrid(type);
  return {
    id: `widget-${Date.now()}-${Math.random().toString(16).slice(2, 6)}`,
    type,
    title: `新建${widgetLabelMap[type]}`,
    datasetId: 'cdm_patient',
    span: type === 'table' ? 2 : 1,
    sortOrder: 0,
    visible: true,
    grid,
    bindings: createDefaultBindings(type),
    config: {
      xField: 'patient_name',
      yField: 'patient_code',
      color: '#409eff',
    },
  };
}

export function createInteraction(): ReportInteractionRule {
  return {
    id: `interaction-${Date.now()}-${Math.random().toString(16).slice(2, 6)}`,
    name: '新建联动规则',
    sourceWidgetId: '',
    targetWidgetId: '',
    trigger: 'click',
    action: 'filter',
    sourceField: '',
    targetField: '',
  };
}

export function createDefaultTemplate(): ReportTemplateSchema {
  return normalizeTemplateLayout({
    id: 'template-new',
    name: '未命名报表',
    schemaVersion: DEFAULT_SCHEMA_VERSION,
    status: 'DRAFT',
    datasetCode: 'cdm_patient',
    versioning: createDefaultVersioning(),
    layout: {
      columns: 2,
      gap: 16,
      gridColumns: DEFAULT_GRID_COLUMNS,
      rowHeight: DEFAULT_ROW_HEIGHT,
      canvasWidthMode: DEFAULT_CANVAS_WIDTH_MODE,
    },
    filters: [],
    parameters: [],
    widgets: [createWidgetByType('metric')],
    interactions: [],
  });
}

export function createDefaultVersioning(): ReportTemplateVersioning {
  return {
    baseVersion: 0,
    draftVersion: 1,
    publishedVersion: 0,
    effectiveVersion: 0,
    state: 'draft',
    lastSavedAt: '',
    lastPublishedAt: '',
  };
}

export function cloneWidget(widget: ReportWidget): ReportWidget {
  return {
    ...JSON.parse(JSON.stringify(widget)) as ReportWidget,
    id: `widget-${Date.now()}-${Math.random().toString(16).slice(2, 6)}`,
    title: `${widget.title}-副本`,
  };
}

export function serializeTemplate(schema: ReportTemplateSchema): string {
  return JSON.stringify(schema, null, 2);
}

export function cloneTemplate(schema: ReportTemplateSchema): ReportTemplateSchema {
  return JSON.parse(JSON.stringify(schema)) as ReportTemplateSchema;
}

export function createDefaultGrid(type: WidgetType): ReportWidgetGrid {
  if (type === 'table') {
    return { x: 0, y: 0, w: DEFAULT_GRID_COLUMNS, h: 2 };
  }
  if (type === 'metric') {
    return { x: 0, y: 0, w: 12, h: 1 };
  }
  return { x: 0, y: 0, w: 12, h: 2 };
}

export function normalizeTemplateLayout(schema: ReportTemplateSchema): ReportTemplateSchema {
  const gridColumns = clamp(Number(schema.layout.gridColumns || DEFAULT_GRID_COLUMNS), 12, 24);
  const columns = clamp(Number(schema.layout.columns || 2), 1, 4);
  const gap = clamp(Number(schema.layout.gap || 16), 8, 32);
  const rowHeight = clamp(Number(schema.layout.rowHeight || DEFAULT_ROW_HEIGHT), 80, 200);
  const canvasWidthMode = schema.layout.canvasWidthMode || DEFAULT_CANVAS_WIDTH_MODE;

  let cursorX = 0;
  let cursorY = 0;
  let currentRowHeight = 1;

  const widgets = schema.widgets
    .map((widget, index) => {
      const baseGrid = widget.grid ?? createDefaultGrid(widget.type);
      const width = clamp(Number(baseGrid.w || (widget.span || 1) * Math.floor(gridColumns / columns)), 1, gridColumns);
      const height = clamp(Number(baseGrid.h || (widget.type === 'metric' ? 1 : 2)), 1, 4);
      if (cursorX + width > gridColumns) {
        cursorX = 0;
        cursorY += currentRowHeight;
        currentRowHeight = 1;
      }
      const normalizedWidget: ReportWidget = {
        ...widget,
        span: clamp(Math.round(width / Math.max(1, Math.floor(gridColumns / columns))), 1, columns),
        sortOrder: index + 1,
        visible: widget.visible !== false,
        grid: {
          x: cursorX,
          y: cursorY,
          w: width,
          h: height,
        },
      };
      cursorX += width;
      currentRowHeight = Math.max(currentRowHeight, height);
      if (cursorX >= gridColumns) {
        cursorX = 0;
        cursorY += currentRowHeight;
        currentRowHeight = 1;
      }
      return normalizedWidget;
    })
    .sort((left, right) => left.sortOrder - right.sortOrder);

  return {
    ...schema,
    schemaVersion: schema.schemaVersion || DEFAULT_SCHEMA_VERSION,
    status: schema.status || (schema.versioning?.state === 'published' ? 'PUBLISHED' : 'DRAFT'),
    versioning: {
      ...createDefaultVersioning(),
      ...schema.versioning,
      baseVersion: Number(schema.versioning?.baseVersion ?? schema.versioning?.draftVersion ?? 0),
      draftVersion: Math.max(1, Number(schema.versioning?.draftVersion ?? 1)),
      publishedVersion: Math.max(0, Number(schema.versioning?.publishedVersion ?? 0)),
      effectiveVersion: Math.max(0, Number(schema.versioning?.effectiveVersion ?? schema.versioning?.publishedVersion ?? 0)),
      state: schema.versioning?.state === 'published' ? 'published' : 'draft',
      lastSavedAt: schema.versioning?.lastSavedAt || '',
      lastPublishedAt: schema.versioning?.lastPublishedAt || '',
    },
    layout: {
      columns,
      gap,
      gridColumns,
      rowHeight,
      canvasWidthMode,
    },
    widgets,
  };
}

export const widgetLabelMap: Record<WidgetType, string> = {
  metric: '指标卡',
  line: '折线图',
  bar: '柱状图',
  pie: '饼图',
  table: '数据表',
};
