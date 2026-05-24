import type { DashboardPayload } from '@/types/dashboard';
import type { LoginForm, LoginResult, UserInfo } from '@/types/auth';
import type { DataSourceFormModel, DataSourceItem } from '@/types/data-source';
import type { EtlJobFormModel, EtlJobItem, EtlRunLog } from '@/types/etl';
import type { ReportJobItem, ReportTemplateSchema } from '@/types/report';

const wait = (ms = 250) => new Promise((resolve) => window.setTimeout(resolve, ms));
const clone = <T>(value: T): T => JSON.parse(JSON.stringify(value)) as T;

const mockUser: UserInfo = {
  id: 'u-admin',
  username: 'admin',
  displayName: '平台管理员',
  orgName: '医疗数据中心',
  roles: ['ADMIN', 'DATA_ENGINEER'],
  permissions: [
    'dashboard:view',
    'data-source:view',
    'data-source:edit',
    'etl:view',
    'etl:edit',
    'dataset:view',
    'report:designer',
    'report:jobs',
    'open-api:view',
    'system:view',
  ],
};

let dataSources: DataSourceItem[] = [
  {
    id: 'ds-001',
    name: 'HIS 主库',
    type: 'MySQL',
    host: '10.10.1.12',
    port: 3306,
    database: 'his_core',
    username: 'his_reader',
    status: 'enabled',
    syncMode: 'incremental',
    updateTime: '2026-05-24 09:30',
    owner: '数据集成组',
  },
  {
    id: 'ds-002',
    name: 'LIS 检验库',
    type: 'PostgreSQL',
    host: '10.10.2.15',
    port: 5432,
    database: 'lis_service',
    username: 'lis_reader',
    status: 'enabled',
    syncMode: 'full',
    updateTime: '2026-05-24 08:42',
    owner: '检验中心',
  },
  {
    id: 'ds-003',
    name: '影像归档库',
    type: 'MongoDB',
    host: '10.10.3.28',
    port: 27017,
    database: 'pacs_archive',
    username: 'pacs_reader',
    status: 'error',
    syncMode: 'incremental',
    updateTime: '2026-05-23 18:16',
    owner: '影像中心',
  },
];

let etlJobs: EtlJobItem[] = [
  {
    id: 'etl-001',
    name: '患者主索引同步',
    sourceName: 'HIS 主库',
    datasetName: 'CDM_PATIENT',
    runMode: 'incremental',
    status: 'running',
    schedule: '0 */10 * * * ?',
    updateTime: '2026-05-24 09:35',
    latestRun: '2026-05-24 09:30',
  },
  {
    id: 'etl-002',
    name: '检验结果全量入仓',
    sourceName: 'LIS 检验库',
    datasetName: 'CDM_LAB_RESULT',
    runMode: 'full',
    status: 'success',
    schedule: '0 0 2 * * ?',
    updateTime: '2026-05-24 08:10',
    latestRun: '2026-05-24 02:00',
  },
];

const etlRunLogs: EtlRunLog[] = [
  {
    id: 'run-001',
    jobName: '患者主索引同步',
    startTime: '2026-05-24 09:30',
    endTime: '2026-05-24 09:31',
    status: 'running',
    records: 2846,
    message: '正在写入 ODS_PATIENT 批次 202605240930',
  },
  {
    id: 'run-002',
    jobName: '检验结果全量入仓',
    startTime: '2026-05-24 02:00',
    endTime: '2026-05-24 02:12',
    status: 'success',
    records: 128432,
    message: '已完成标准化映射与字段脱敏',
  },
];

const datasets = [
  {
    id: 'dataset-001',
    name: 'CDM_PATIENT',
    category: '患者域',
    owner: '标准化模型组',
    updateTime: '2026-05-24 09:30',
    description: '患者基础档案标准表',
    fields: [
      { source: 'patient_id', target: 'patientCode', type: 'varchar(64)', lineage: 'HIS.PATIENT_MASTER' },
      { source: 'name', target: 'patientName', type: 'varchar(128)', lineage: 'HIS.PATIENT_MASTER' },
      { source: 'gender', target: 'genderCode', type: 'varchar(8)', lineage: 'DIM_GENDER' },
    ],
    samples: [
      { patientCode: 'P202605240001', patientName: '张某某', genderCode: 'M', mobileMasked: '138****8841' },
      { patientCode: 'P202605240002', patientName: '李某某', genderCode: 'F', mobileMasked: '137****1208' },
    ],
  },
  {
    id: 'dataset-002',
    name: 'CDM_LAB_RESULT',
    category: '检验域',
    owner: '检验中心',
    updateTime: '2026-05-24 02:12',
    description: '检验结果宽表，支持报表与开放接口复用',
    fields: [
      { source: 'order_no', target: 'orderNo', type: 'varchar(64)', lineage: 'LIS.T_ORDER' },
      { source: 'item_code', target: 'itemCode', type: 'varchar(64)', lineage: 'LIS.T_RESULT' },
      { source: 'result_value', target: 'resultValue', type: 'varchar(64)', lineage: 'LIS.T_RESULT' },
    ],
    samples: [
      { orderNo: 'LAB20260524001', itemCode: 'HB', resultValue: '128', abnormalFlag: 'N' },
      { orderNo: 'LAB20260524002', itemCode: 'WBC', resultValue: '6.8', abnormalFlag: 'N' },
    ],
  },
];

let reportTemplate: ReportTemplateSchema = {
  id: 'template-001',
  name: '患者就诊综合分析',
  layout: {
    columns: 2,
    gap: 16,
  },
  filters: [
    { field: 'visitDate', label: '就诊日期', value: '近30天' },
    { field: 'department', label: '科室', value: '全部' },
  ],
  widgets: [
    {
      id: 'widget-001',
      type: 'metric',
      title: '日均就诊量',
      datasetId: 'dataset-001',
      config: { value: 1328, trend: '+4.2%' },
    },
    {
      id: 'widget-002',
      type: 'line',
      title: '近7日门诊趋势',
      datasetId: 'dataset-001',
      config: { xField: 'date', yField: 'count' },
    },
  ],
};

const reportJobs: ReportJobItem[] = [
  {
    id: 'job-001',
    templateName: '患者就诊综合分析',
    cron: '0 0 7 * * ?',
    pushChannel: '站内消息',
    latestRun: '2026-05-24 07:00',
    status: 'enabled',
  },
];

const apiClients = [
  {
    id: 'client-001',
    name: '科研分析平台',
    appKey: 'mdc_research',
    scope: 'patient.read report.read',
    rateLimit: '200 req/min',
    updateTime: '2026-05-24 09:00',
    status: 'enabled',
  },
  {
    id: 'client-002',
    name: '区域卫生平台',
    appKey: 'mdc_region',
    scope: 'encounter.read lab.read',
    rateLimit: '120 req/min',
    updateTime: '2026-05-23 16:20',
    status: 'enabled',
  },
];

const apiLogs = [
  {
    id: 'log-001',
    clientName: '科研分析平台',
    endpoint: '/open-api/v1/patients',
    status: 200,
    latency: '86ms',
    timestamp: '2026-05-24 09:12:03',
  },
  {
    id: 'log-002',
    clientName: '区域卫生平台',
    endpoint: '/open-api/v1/labs',
    status: 429,
    latency: '12ms',
    timestamp: '2026-05-24 08:58:21',
  },
];

const systemPayload = {
  users: [
    { id: 'user-001', username: 'admin', displayName: '平台管理员', roles: '管理员', status: '启用' },
    { id: 'user-002', username: 'etl_user', displayName: '数据工程师', roles: '数据工程师', status: '启用' },
  ],
  roles: [
    { id: 'role-001', name: '管理员', permissionCount: 24, updateTime: '2026-05-22 15:40' },
    { id: 'role-002', name: '数据工程师', permissionCount: 16, updateTime: '2026-05-21 10:18' },
  ],
  permissions: [
    { module: '数据源管理', code: 'data-source:edit', description: '新增、编辑、启停数据源' },
    { module: 'ETL 管理', code: 'etl:edit', description: '创建任务、执行抽取与查看日志' },
    { module: '系统管理', code: 'system:view', description: '查看系统配置与审计日志' },
  ],
  dictionaries: [
    { type: 'source_type', code: 'MySQL', label: 'MySQL', enabled: true },
    { type: 'etl_run_mode', code: 'incremental', label: '增量同步', enabled: true },
  ],
  auditLogs: [
    { action: '登录', operator: 'admin', result: '成功', timestamp: '2026-05-24 09:00:02' },
    { action: '测试数据源连接', operator: 'admin', result: '失败', timestamp: '2026-05-24 08:58:02' },
  ],
  parameters: [
    { key: 'security.jwt.expire', value: '7200', description: 'JWT 访问令牌过期秒数' },
    { key: 'openapi.rate-limit.default', value: '100', description: '默认限流阈值/分钟' },
  ],
};

export async function login(payload: LoginForm): Promise<LoginResult> {
  await wait();

  return {
    accessToken: `mock-token-${payload.username}`,
    refreshToken: 'mock-refresh-token',
    expiresIn: 7200,
    user: clone(mockUser),
  };
}

export async function getDashboard(): Promise<DashboardPayload> {
  await wait();

  return clone({
    summary: {
      dataSourceTotal: dataSources.length,
      runningJobs: etlJobs.filter((job) => job.status === 'running').length,
      reportRuns: 28,
      apiCalls: 18642,
    },
    etlTrend: [
      { label: '05-18', value: 12 },
      { label: '05-19', value: 16 },
      { label: '05-20', value: 14 },
      { label: '05-21', value: 18 },
      { label: '05-22', value: 22 },
      { label: '05-23', value: 26 },
      { label: '05-24', value: 19 },
    ],
    apiTrend: [
      { label: '患者查询', value: 8450 },
      { label: '就诊查询', value: 5600 },
      { label: '检验查询', value: 3890 },
      { label: '报表查询', value: 702 },
    ],
    alerts: [
      '影像归档库最近一次连通性测试失败，请检查网络白名单。',
      '患者主索引同步任务正在执行增量批次，预计 2 分钟内完成。',
      '开放 API 命中 1 次限流告警，建议核查第三方调用频率。',
    ],
    todoItems: [
      '补齐 OAuth2 客户端审批流',
      '完成报表导出 PDF/Excel 后端接口联调',
      '确认 ODS 到 CDM 的字典映射口径',
    ],
  });
}

export async function listDataSources(): Promise<DataSourceItem[]> {
  await wait();
  return clone(dataSources);
}

export async function saveDataSource(payload: DataSourceFormModel): Promise<DataSourceItem> {
  await wait();

  const nextItem: DataSourceItem = {
    id: payload.id ?? `ds-${Date.now()}`,
    name: payload.name,
    type: payload.type,
    host: payload.host,
    port: payload.port,
    database: payload.database,
    username: payload.username,
    status: payload.enabled ? 'enabled' : 'disabled',
    syncMode: payload.syncMode,
    updateTime: '2026-05-24 10:00',
    owner: '平台管理员',
  };

  dataSources = dataSources.some((item) => item.id === nextItem.id)
    ? dataSources.map((item) => (item.id === nextItem.id ? nextItem : item))
    : [nextItem, ...dataSources];

  return clone(nextItem);
}

export async function toggleDataSource(id: string): Promise<void> {
  await wait();
  dataSources = dataSources.map((item) =>
    item.id === id
      ? {
          ...item,
          status: item.status === 'enabled' ? 'disabled' : 'enabled',
          updateTime: '2026-05-24 10:00',
        }
      : item,
  );
}

export async function removeDataSource(id: string): Promise<void> {
  await wait();
  dataSources = dataSources.filter((item) => item.id !== id);
}

export async function testDataSourceConnection(): Promise<{ success: boolean; message: string }> {
  await wait(500);
  return { success: true, message: '连接测试成功，已返回元数据摘要。' };
}

export async function listEtlJobs(): Promise<EtlJobItem[]> {
  await wait();
  return clone(etlJobs);
}

export async function saveEtlJob(payload: EtlJobFormModel): Promise<EtlJobItem> {
  await wait();

  const sourceName = dataSources.find((item) => item.id === payload.sourceId)?.name ?? '未命名数据源';
  const nextItem: EtlJobItem = {
    id: payload.id ?? `etl-${Date.now()}`,
    name: payload.name,
    sourceName,
    datasetName: payload.datasetName,
    runMode: payload.runMode,
    status: 'draft',
    schedule: payload.schedule,
    updateTime: '2026-05-24 10:00',
    latestRun: '未执行',
  };

  etlJobs = etlJobs.some((item) => item.id === nextItem.id)
    ? etlJobs.map((item) => (item.id === nextItem.id ? nextItem : item))
    : [nextItem, ...etlJobs];

  return clone(nextItem);
}

export async function listEtlRunLogs(): Promise<EtlRunLog[]> {
  await wait();
  return clone(etlRunLogs);
}

export async function runEtlJob(id: string): Promise<{ message: string }> {
  await wait(600);
  return { message: `任务 ${id} 已提交执行，请在日志中查看实时状态。` };
}

export async function listDatasets() {
  await wait();
  return clone(datasets);
}

export async function getReportTemplate() {
  await wait();
  return clone(reportTemplate);
}

export async function saveReportTemplate(payload: ReportTemplateSchema) {
  await wait();
  reportTemplate = clone(payload);
  return clone(reportTemplate);
}

export async function listReportJobs() {
  await wait();
  return clone(reportJobs);
}

export async function previewReport() {
  await wait(500);
  return {
    url: '/mock/report-preview',
    message: '预览生成成功，后续可替换为后端渲染接口。',
  };
}

export async function listApiClients() {
  await wait();
  return clone(apiClients);
}

export async function listApiLogs() {
  await wait();
  return clone(apiLogs);
}

export async function getSystemPayload() {
  await wait();
  return clone(systemPayload);
}
