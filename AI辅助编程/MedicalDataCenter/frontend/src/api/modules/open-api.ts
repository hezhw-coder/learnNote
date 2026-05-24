import { request } from '@/api/http';

interface BackendApiClient {
  id: number;
  name: string;
  clientId: string;
  scopes: string[];
  enabled: boolean;
}

interface BackendApiLog {
  id: number;
  clientId: string;
  endpoint: string;
  scope: string;
  status: number;
  message: string;
  createdAt: string;
}

export function fetchApiClients() {
  return request<BackendApiClient[]>({
    url: '/open-api/clients',
    method: 'get',
  }).then((items) =>
    items.map((item) => ({
      id: String(item.id),
      name: item.name,
      appKey: item.clientId,
      scope: item.scopes.join(', '),
      rateLimit: '令牌桶',
      updateTime: '-',
      status: item.enabled ? '启用' : '停用',
    })),
  );
}

export function fetchApiLogs() {
  return request<BackendApiLog[]>({
    url: '/open-api/logs',
    method: 'get',
  }).then((items) =>
    items.map((item) => ({
      id: String(item.id),
      clientName: item.clientId,
      endpoint: item.endpoint,
      status: item.status,
      latency: item.message || '-',
      timestamp: item.createdAt?.replace('T', ' ').slice(0, 19) ?? '-',
    })),
  );
}
