import { computed } from 'vue';
import { defineStore } from 'pinia';
import type { MenuItem } from '@/types/common';
import { useAuthStore } from './auth';

const menuItems: MenuItem[] = [
  { title: '工作台', path: '/dashboard', icon: 'Monitor', permission: 'AUTH_ME' },
  { title: '数据源', path: '/data-sources', icon: 'Coin', permission: 'DATASOURCE_MANAGE' },
  { title: 'ETL', path: '/etl', icon: 'Connection', permission: 'ETL_MANAGE' },
  { title: '数据集', path: '/datasets', icon: 'Collection', permission: 'DATASET_VIEW' },
  { title: '报表设计', path: '/reports/designer/template-001', icon: 'Histogram', permission: 'REPORT_MANAGE' },
  { title: '报表任务', path: '/reports/jobs', icon: 'Timer', permission: 'REPORT_MANAGE' },
  { title: '开放 API', path: '/open-api', icon: 'Link', permission: 'OPEN_API_MANAGE' },
  {
    title: '系统管理',
    path: '/system',
    icon: 'Setting',
    permissionsAny: ['SYSTEM_ROLE_VIEW', 'SYSTEM_USER_VIEW', 'SYSTEM_ROLE_MANAGE', 'SYSTEM_PARAM_MANAGE'],
  },
];

export const usePermissionStore = defineStore('permission', () => {
  const authStore = useAuthStore();

  const visibleMenus = computed(() => {
    return menuItems.filter((menu) => hasPermission(menu.permission, menu.permissionsAny));
  });

  function hasPermission(permission?: string, permissionsAny?: string[]) {
    const userPermissions = authStore.user?.permissions ?? [];
    if (!permission) {
      return permissionsAny?.some((item) => userPermissions.includes(item)) ?? true;
    }
    return userPermissions.includes(permission);
  }

  return {
    visibleMenus,
    hasPermission,
  };
});
