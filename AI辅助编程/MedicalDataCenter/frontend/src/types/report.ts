export type WidgetType = 'metric' | 'line' | 'bar' | 'pie' | 'table';

export interface ReportWidget {
  id: string;
  type: WidgetType;
  title: string;
  datasetId: string;
  description?: string;
  config: Record<string, string | number | boolean | undefined>;
}

export interface ReportTemplateSchema {
  id: string;
  name: string;
  layout: {
    columns: number;
    gap: number;
  };
  filters: Array<{
    field: string;
    label: string;
    value: string;
  }>;
  widgets: ReportWidget[];
}

export interface ReportJobItem {
  id: string;
  templateName: string;
  cron: string;
  pushChannel: string;
  latestRun: string;
  status: 'enabled' | 'disabled';
}
