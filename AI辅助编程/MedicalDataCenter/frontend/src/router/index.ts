import { createRouter, createWebHistory } from 'vue-router';
import { usePermissionStore } from '@/stores/permission';
import LoginPage from '@/pages/LoginPage.vue';
import AppLayout from '@/layouts/AppLayout.vue';
import DashboardPage from '@/pages/dashboard/DashboardPage.vue';
import DataSourcesPage from '@/pages/data-sources/DataSourcesPage.vue';
import EtlPage from '@/pages/etl/EtlPage.vue';
import DatasetsPage from '@/pages/datasets/DatasetsPage.vue';
import ReportDesignerPage from '@/pages/reports/ReportDesignerPage.vue';
import ReportJobsPage from '@/pages/reports/ReportJobsPage.vue';
import OpenApiPage from '@/pages/open-api/OpenApiPage.vue';
import SystemPage from '@/pages/system/SystemPage.vue';

declare module 'vue-router' {
  interface RouteMeta {
    title: string;
    requiresAuth?: boolean;
    permission?: string;
  }
}

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: LoginPage,
      meta: { title: '登录' },
    },
    {
      path: '/',
      component: AppLayout,
      redirect: '/dashboard',
      children: [
        {
          path: 'dashboard',
          name: 'dashboard',
          component: DashboardPage,
          meta: { title: '工作台', requiresAuth: true, permission: 'AUTH_ME' },
        },
        {
          path: 'data-sources',
          name: 'data-sources',
          component: DataSourcesPage,
          meta: { title: '数据源管理', requiresAuth: true, permission: 'DATASOURCE_MANAGE' },
        },
        {
          path: 'etl',
          name: 'etl',
          component: EtlPage,
          meta: { title: '抽取集成', requiresAuth: true, permission: 'ETL_MANAGE' },
        },
        {
          path: 'datasets',
          name: 'datasets',
          component: DatasetsPage,
          meta: { title: '数据集管理', requiresAuth: true, permission: 'DATASET_VIEW' },
        },
        {
          path: 'reports/designer/:id',
          name: 'report-designer',
          component: ReportDesignerPage,
          meta: { title: '报表设计器', requiresAuth: true, permission: 'REPORT_MANAGE' },
        },
        {
          path: 'reports/jobs',
          name: 'report-jobs',
          component: ReportJobsPage,
          meta: { title: '报表任务', requiresAuth: true, permission: 'REPORT_MANAGE' },
        },
        {
          path: 'open-api',
          name: 'open-api',
          component: OpenApiPage,
          meta: { title: '开放 API', requiresAuth: true, permission: 'OPEN_API_MANAGE' },
        },
        {
          path: 'system',
          name: 'system',
          component: SystemPage,
          meta: { title: '系统管理', requiresAuth: true, permission: 'SYSTEM_ROLE_VIEW' },
        },
      ],
    },
  ],
});

router.beforeEach((to) => {
  document.title = `${to.meta.title ?? '医疗数据中心'} - 医疗数据中心`;

  const token = window.localStorage.getItem('mdc-token');
  if (to.meta.requiresAuth && !token) {
    return { path: '/login', query: { redirect: to.fullPath } };
  }

  if (to.meta.permission) {
    const permissionStore = usePermissionStore();
    if (!permissionStore.hasPermission(to.meta.permission)) {
      return { path: '/dashboard' };
    }
  }

  if (to.path === '/login' && token) {
    return { path: '/dashboard' };
  }

  return true;
});

export default router;
