import { request } from '@/api/http';

export interface SystemUserRecord {
  id: number;
  username: string;
  displayName: string;
  enabled: boolean;
  createdAt: string;
  roles: string[];
}

export interface SystemRoleRecord {
  id: number;
  code: string;
  name: string;
  permissions: string[];
}

export interface SystemPermissionRecord {
  id: number;
  code: string;
  name: string;
  module: string;
}

export interface SystemAuditLogRecord {
  id: number;
  actor: string;
  action: string;
  targetType: string;
  targetId: string;
  detail: string;
  createdAt: string;
}

export interface SystemPayload {
  users: SystemUserRecord[];
  roles: SystemRoleRecord[];
  permissions: SystemPermissionRecord[];
  dictionaries: Array<{
    type: string;
    code: string;
    label: string;
    enabled: boolean;
  }>;
  auditLogs: SystemAuditLogRecord[];
  parameters: SystemParameterRecord[];
}

export interface SystemParameterRecord {
  key: string;
  value: string;
  description: string;
  updatedBy: string;
  updatedAt: string;
}

export function fetchSystemUsers() {
  return request<SystemUserRecord[]>({
    url: '/system/users',
    method: 'get',
  });
}

export function fetchSystemRoles() {
  return request<SystemRoleRecord[]>({
    url: '/system/roles',
    method: 'get',
  });
}

export function fetchSystemPermissions() {
  return request<SystemPermissionRecord[]>({
    url: '/system/permissions',
    method: 'get',
  });
}

export function fetchSystemDictionaries() {
  return request<SystemPayload['dictionaries']>({
    url: '/system/dictionaries',
    method: 'get',
  });
}

export function fetchSystemAuditLogs() {
  return request<SystemAuditLogRecord[]>({
    url: '/system/audit-logs',
    method: 'get',
  });
}

export function fetchSystemParameters() {
  return request<SystemParameterRecord[]>({
    url: '/system/parameters',
    method: 'get',
  });
}

export function fetchSystemPayload() {
  return request<SystemPayload>({
    url: '/system/overview',
    method: 'get',
  });
}

export function createSystemUser(payload: {
  username: string;
  password: string;
  displayName: string;
  roleIds: number[];
}) {
  return request<SystemUserRecord>({
    url: '/system/users',
    method: 'post',
    data: payload,
  });
}

export function updateSystemUserStatus(id: number, enabled: boolean) {
  return request<SystemUserRecord>({
    url: `/system/users/${id}/status`,
    method: 'put',
    data: { enabled },
  });
}

export function assignSystemUserRoles(id: number, roleIds: number[]) {
  return request<SystemUserRecord>({
    url: `/system/users/${id}/roles`,
    method: 'put',
    data: { roleIds },
  });
}

export function createSystemRole(payload: {
  code: string;
  name: string;
  permissionIds: number[];
}) {
  return request<SystemRoleRecord>({
    url: '/system/roles',
    method: 'post',
    data: payload,
  });
}

export function updateSystemRole(
  id: number,
  payload: {
    code: string;
    name: string;
  },
) {
  return request<SystemRoleRecord>({
    url: `/system/roles/${id}`,
    method: 'put',
    data: payload,
  });
}

export function assignSystemRolePermissions(id: number, permissionIds: number[]) {
  return request<SystemRoleRecord>({
    url: `/system/roles/${id}/permissions`,
    method: 'put',
    data: { permissionIds },
  });
}

export function updateSystemParameter(key: string, value: string) {
  return request<SystemParameterRecord>({
    url: `/system/parameters/${encodeURIComponent(key)}`,
    method: 'put',
    data: { value },
  });
}
