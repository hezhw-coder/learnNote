import type { DataSourceFormModel } from '@/types/data-source';
import { request } from '@/api/http';

interface BackendDataSource {
  id: number;
  name: string;
  type: string;
  jdbcUrl: string;
  username: string;
  databaseName: string;
  status: string;
}

function normalizeType(type: string): DataSourceFormModel['type'] {
  const mapping: Record<string, DataSourceFormModel['type']> = {
    MYSQL: 'MySQL',
    POSTGRESQL: 'PostgreSQL',
    SQLSERVER: 'SQLServer',
    ORACLE: 'Oracle',
    MONGODB: 'MongoDB',
    TIDB: 'TiDB',
  };
  return mapping[type.toUpperCase()] ?? 'MySQL';
}

function parseJdbcUrl(type: string, jdbcUrl?: string, databaseName?: string) {
  if (!jdbcUrl) {
    return { host: '', port: 0, database: databaseName ?? '' };
  }

  if (type.toUpperCase() === 'SQLSERVER') {
    const match = jdbcUrl.match(/^jdbc:sqlserver:\/\/([^:;]+):(\d+);databaseName=([^;]+)/i);
    return {
      host: match?.[1] ?? '',
      port: Number(match?.[2] ?? 1433),
      database: match?.[3] ?? databaseName ?? '',
    };
  }

  if (type.toUpperCase() === 'ORACLE') {
    const match = jdbcUrl.match(/^jdbc:oracle:thin:@([^:]+):(\d+)\/(.+)$/i);
    return {
      host: match?.[1] ?? '',
      port: Number(match?.[2] ?? 1521),
      database: match?.[3] ?? databaseName ?? '',
    };
  }

  const normalized = jdbcUrl
    .replace(/^jdbc:/i, '')
    .replace(/^mongodb:\/\//i, 'http://')
    .replace(/^(mysql|postgresql|tidb):\/\//i, 'http://');
  try {
    const url = new URL(normalized);
    return {
      host: url.hostname,
      port: Number(url.port || 0),
      database: url.pathname.replace(/^\//, '') || databaseName || '',
    };
  } catch {
    return { host: '', port: 0, database: databaseName ?? '' };
  }
}

function buildJdbcUrl(payload: DataSourceFormModel) {
  const params = payload.params?.trim();
  const suffix = params ? (params.startsWith('?') || params.startsWith(';') ? params : `?${params}`) : '';
  switch (payload.type) {
    case 'PostgreSQL':
      return `jdbc:postgresql://${payload.host}:${payload.port}/${payload.database}${suffix}`;
    case 'SQLServer':
      return `jdbc:sqlserver://${payload.host}:${payload.port};databaseName=${payload.database}${suffix}`;
    case 'Oracle':
      return `jdbc:oracle:thin:@${payload.host}:${payload.port}/${payload.database}`;
    case 'MongoDB':
      return `mongodb://${payload.host}:${payload.port}/${payload.database}${params ? `?${params.replace(/^\?/, '')}` : ''}`;
    case 'TiDB':
      return `jdbc:mysql://${payload.host}:${payload.port}/${payload.database}${suffix}`;
    case 'MySQL':
    default:
      return `jdbc:mysql://${payload.host}:${payload.port}/${payload.database}${suffix}`;
  }
}

function mapDataSource(item: BackendDataSource) {
  const parsed = parseJdbcUrl(item.type, item.jdbcUrl, item.databaseName);
  return {
    id: String(item.id),
    name: item.name,
    type: normalizeType(item.type),
    host: parsed.host,
    port: parsed.port,
    database: parsed.database,
    username: item.username,
    status: item.status.toLowerCase() === 'enabled' ? 'enabled' : 'disabled',
    syncMode: 'incremental' as const,
    updateTime: new Date().toLocaleString('zh-CN', { hour12: false }),
    owner: '平台管理',
  };
}

function toRequest(payload: DataSourceFormModel) {
  return {
    name: payload.name,
    type: payload.type,
    jdbcUrl: buildJdbcUrl(payload),
    username: payload.username,
    password: payload.password,
    databaseName: payload.database,
  };
}

export function fetchDataSources() {
  return request<BackendDataSource[]>({
    url: '/data-sources',
    method: 'get',
  }).then((items) => items.map(mapDataSource));
}

export function submitDataSource(payload: DataSourceFormModel) {
  return request<BackendDataSource>({
    url: payload.id ? `/data-sources/${payload.id}` : '/data-sources',
    method: payload.id ? 'put' : 'post',
    data: toRequest(payload),
  }).then(mapDataSource);
}

export function removeDataSourceById(id: string) {
  return request<void>({
    url: `/data-sources/${id}`,
    method: 'delete',
  });
}

export function toggleDataSourceStatus(id: string) {
  return fetchDataSources().then((items) => {
    const target = items.find((item) => item.id === id);
    return request({
      url: `/data-sources/${id}/status`,
      method: 'patch',
      data: {
        status: target?.status === 'enabled' ? 'DISABLED' : 'ENABLED',
      },
    });
  });
}

export function testDataSource(payload: DataSourceFormModel) {
  return request<{ success: boolean; message: string }>({
    url: '/data-sources/test',
    method: 'post',
    data: toRequest(payload),
  });
}
