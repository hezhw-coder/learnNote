import type { ReportTemplateSchema } from '@/types/report';
import { request } from '@/api/http';
import { createDefaultTemplate } from '@/utils/schema';

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
  cronExpression: string;
  enabled: boolean;
  channel: string;
}

async function fetchTemplates() {
  return request<BackendReportTemplate[]>({
    url: '/reports/templates',
    method: 'get',
  });
}

function parseTemplate(item: BackendReportTemplate): ReportTemplateSchema {
  try {
    const schema = JSON.parse(item.designJson) as ReportTemplateSchema;
    return {
      ...schema,
      id: String(item.id),
      name: item.name,
    };
  } catch {
    return {
      ...createDefaultTemplate(),
      id: String(item.id),
      name: item.name,
    };
  }
}

export async function fetchReportTemplate() {
  const templates = await fetchTemplates();
  if (!templates.length) {
    return createDefaultTemplate();
  }
  return parseTemplate(templates[0]);
}

export function submitReportTemplate(payload: ReportTemplateSchema) {
  const body = {
    name: payload.name,
    datasetCode: payload.widgets[0]?.datasetId ?? 'cdm_patient',
    designJson: JSON.stringify(payload),
  };
  const isExisting = /^\d+$/.test(payload.id);
  return request({
    url: isExisting ? `/reports/templates/${payload.id}` : '/reports/templates',
    method: isExisting ? 'put' : 'post',
    data: body,
  });
}

export async function fetchReportJobs() {
  const [schedules, templates] = await Promise.all([
    request<BackendReportSchedule[]>({
      url: '/reports/schedules',
      method: 'get',
    }),
    fetchTemplates(),
  ]);
  return schedules.map((item) => ({
    id: String(item.id),
    templateName: templates.find((template) => template.id === item.templateId)?.name ?? `模板-${item.templateId}`,
    cron: item.cronExpression,
    pushChannel: item.channel,
    latestRun: '-',
    status: item.enabled ? ('enabled' as const) : ('disabled' as const),
  }));
}

export async function previewReportTemplate(id?: string) {
  const templates = await fetchTemplates();
  const targetId = id ?? String(templates[0]?.id ?? '');
  const data = await request<{ datasetCode: string; rows: Array<Record<string, unknown>> }>({
    url: `/reports/templates/${targetId}/preview`,
    method: 'get',
  });
  return {
    message: `预览成功，数据集 ${data.datasetCode} 返回 ${data.rows.length} 行样本数据`,
    rows: data.rows,
  };
}

export function exportReportTemplate(id: string, format: 'pdf' | 'excel') {
  const suffix = format === 'pdf' ? 'pdf' : 'excel';
  const baseUrl = import.meta.env.VITE_API_BASE_URL ?? '/api';
  window.open(`${baseUrl}/reports/templates/${id}/export/${suffix}`, '_blank');
}
