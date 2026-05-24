import type { EtlJobFormModel } from '@/types/etl';
import { request } from '@/api/http';
import { fetchDataSources } from './data-sources';

interface BackendEtlJob {
  id: number;
  name: string;
  dataSourceId: number;
  loadMode: string;
  sourceTable: string;
  extractSql?: string;
  incrementField?: string;
  idField: string;
  nameField: string;
  genderField?: string;
  birthDateField?: string;
  status: string;
}

interface BackendEtlRun {
  id: number;
  jobId: number;
  status: string;
  extractedCount: number;
  message: string;
  startedAt: string;
  finishedAt?: string;
}

function normalizeStatus(status: string): 'draft' | 'running' | 'success' | 'failed' {
  const upper = status.toUpperCase();
  if (upper === 'RUNNING') {
    return 'running';
  }
  if (upper === 'SUCCESS') {
    return 'success';
  }
  if (upper === 'FAILED') {
    return 'failed';
  }
  return 'draft';
}

function formatTime(value?: string) {
  if (!value) {
    return '-';
  }
  return value.replace('T', ' ').slice(0, 19);
}

export async function fetchEtlJobs() {
  const [jobs, dataSources] = await Promise.all([
    request<BackendEtlJob[]>({
      url: '/etl/jobs',
      method: 'get',
    }),
    fetchDataSources(),
  ]);

  return jobs.map((item) => ({
    id: String(item.id),
    name: item.name,
    sourceId: String(item.dataSourceId),
    sourceName: dataSources.find((source) => source.id === String(item.dataSourceId))?.name ?? `数据源-${item.dataSourceId}`,
    datasetName: 'CDM_PATIENT',
    runMode: item.loadMode.toLowerCase() === 'incremental' ? 'incremental' : 'full',
    status: normalizeStatus(item.status),
    schedule: item.loadMode.toLowerCase() === 'incremental' ? '增量抽取' : '全量抽取',
    updateTime: '-',
    latestRun: '-',
  }));
}

export function submitEtlJob(payload: EtlJobFormModel) {
  return request({
    url: '/etl/jobs',
    method: 'post',
    data: {
      name: payload.name,
      dataSourceId: Number(payload.sourceId),
      loadMode: payload.runMode.toUpperCase(),
      sourceTable: payload.sourceTable,
      extractSql: payload.extractSql || undefined,
      incrementField: payload.runMode === 'incremental' ? payload.incrementField : undefined,
      idField: payload.idField,
      nameField: payload.nameField,
      genderField: payload.genderField || undefined,
      birthDateField: payload.birthDateField || undefined,
    },
  });
}

export function fetchEtlRunLogs() {
  return request<BackendEtlRun[]>({
    url: '/etl/runs',
    method: 'get',
  }).then((items) =>
    items.map((item) => ({
      id: String(item.id),
      jobId: String(item.jobId),
      jobName: `任务-${item.jobId}`,
      startTime: formatTime(item.startedAt),
      endTime: formatTime(item.finishedAt),
      status: normalizeStatus(item.status),
      records: item.extractedCount,
      message: item.message,
    })),
  );
}

export function triggerEtlJob(id: string) {
  return request<{
    id: number;
    jobId: number;
    status: string;
    extractedCount: number;
    message: string;
    startedAt: string;
    finishedAt?: string;
  }>({
    url: `/etl/jobs/${id}/run`,
    method: 'post',
  }).then((item) => ({
    message: item.message || `任务 ${item.jobId} 已执行`,
    status: item.status,
  }));
}
