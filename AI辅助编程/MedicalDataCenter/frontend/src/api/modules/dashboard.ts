import { fetchDataSources } from './data-sources';
import { fetchEtlJobs, fetchEtlRunLogs } from './etl';
import { fetchApiLogs } from './open-api';
import { fetchReportJobs } from './reports';

export async function fetchDashboard() {
  const [dataSources, etlJobs, etlRuns, apiLogs, reportJobs] = await Promise.all([
    fetchDataSources(),
    fetchEtlJobs(),
    fetchEtlRunLogs(),
    fetchApiLogs(),
    fetchReportJobs(),
  ]);

  return {
    summary: {
      dataSourceTotal: dataSources.length,
      runningJobs: etlJobs.filter((item) => item.status === 'running').length,
      reportRuns: reportJobs.length,
      apiCalls: apiLogs.length,
    },
    etlTrend: etlRuns.slice(0, 7).reverse().map((item, index) => ({
      label: item.startTime === '-' ? `执行${index + 1}` : item.startTime.slice(5, 16),
      value: item.records,
    })),
    apiTrend: apiLogs.slice(0, 7).reverse().map((item, index) => ({
      label: item.timestamp === '-' ? `调用${index + 1}` : item.timestamp.slice(5, 16),
      value: item.status >= 400 ? 1 : 5,
    })),
    alerts: [
      ...dataSources.filter((item) => item.status !== 'enabled').map((item) => `${item.name} 当前不可用，请检查连接配置`),
      ...etlRuns.filter((item) => item.status === 'failed').map((item) => `${item.jobName} 最近一次执行失败`),
    ].slice(0, 5),
    todoItems: [
      '核查开放 API 客户端限流规则',
      '补充 ETL 增量任务字段映射',
      '确认报表模板发布与导出流程',
    ],
  };
}
