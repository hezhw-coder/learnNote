import type { ReportTemplateSchema, ReportWidget, WidgetType } from '@/types/report';

export function createWidgetByType(type: WidgetType): ReportWidget {
  return {
    id: `widget-${Date.now()}-${Math.random().toString(16).slice(2, 6)}`,
    type,
    title: `新建${widgetLabelMap[type]}`,
    datasetId: 'dataset-001',
    config: {
      xField: 'date',
      yField: 'value',
      color: '#409eff',
    },
  };
}

export function createDefaultTemplate(): ReportTemplateSchema {
  return {
    id: 'template-new',
    name: '未命名报表',
    layout: {
      columns: 2,
      gap: 16,
    },
    filters: [],
    widgets: [createWidgetByType('metric')],
  };
}

export function serializeTemplate(schema: ReportTemplateSchema): string {
  return JSON.stringify(schema, null, 2);
}

export function cloneTemplate(schema: ReportTemplateSchema): ReportTemplateSchema {
  return JSON.parse(JSON.stringify(schema)) as ReportTemplateSchema;
}

export const widgetLabelMap: Record<WidgetType, string> = {
  metric: '指标卡',
  line: '折线图',
  bar: '柱状图',
  pie: '饼图',
  table: '数据表',
};
