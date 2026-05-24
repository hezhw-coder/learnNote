import { beforeEach, describe, expect, it } from 'vitest';
import { createPinia, setActivePinia } from 'pinia';
import { useAuthStore } from './auth';
import { usePermissionStore } from './permission';

describe('permission store', () => {
  beforeEach(() => {
    setActivePinia(createPinia());
    window.localStorage.clear();
  });

  it('filters visible menus by permission', () => {
    const authStore = useAuthStore();
    const permissionStore = usePermissionStore();

    authStore.user = {
      id: 'u1',
      username: 'admin',
      displayName: '平台管理员',
      orgName: '医疗数据中心',
      roles: ['ADMIN'],
      permissions: ['AUTH_ME', 'SYSTEM_ROLE_VIEW'],
    };

    expect(permissionStore.hasPermission('AUTH_ME')).toBe(true);
    expect(permissionStore.hasPermission('ETL_MANAGE')).toBe(false);
    expect(permissionStore.visibleMenus.map((item) => item.path)).toEqual(['/dashboard', '/system']);
  });
});
