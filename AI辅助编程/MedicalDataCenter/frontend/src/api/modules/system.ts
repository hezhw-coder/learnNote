import { request } from '@/api/http';

export function fetchSystemPayload() {
  return request<{
    users: Array<{
      id: number;
      username: string;
      displayName: string;
      enabled: boolean;
      createdAt: string;
    }>;
    roles: Array<{
      id: number;
      code: string;
      name: string;
    }>;
    permissions: Array<{
      id: number;
      code: string;
      name: string;
      module: string;
    }>;
    dictionaries: Array<{
      type: string;
      code: string;
      label: string;
      enabled: boolean;
    }>;
    auditLogs: Array<{
      id: number;
      actor: string;
      action: string;
      targetType: string;
      targetId: string;
      detail: string;
      createdAt: string;
    }>;
    parameters: Array<{
      key: string;
      value: string;
      description: string;
    }>;
  }>({
    url: '/system/overview',
    method: 'get',
  }).then((payload) => ({
    users: payload.users.map((item) => ({
      username: item.username,
      displayName: item.displayName,
      roles: 'ADMIN',
      status: item.enabled ? '启用' : '停用',
    })),
    roles: payload.roles.map((item) => ({
      name: item.name,
      permissionCount: payload.permissions.length,
      updateTime: '-',
    })),
    permissions: payload.permissions.map((item) => ({
      module: item.module,
      code: item.code,
      description: item.name,
    })),
    dictionaries: payload.dictionaries,
    auditLogs: payload.auditLogs.map((item) => ({
      action: item.action,
      operator: item.actor,
      result: '成功',
      timestamp: item.createdAt?.replace('T', ' ').slice(0, 19) ?? '-',
    })),
    parameters: payload.parameters,
  }));
}
