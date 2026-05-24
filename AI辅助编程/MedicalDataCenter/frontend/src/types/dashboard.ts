export interface DashboardSummary {
  dataSourceTotal: number;
  runningJobs: number;
  reportRuns: number;
  apiCalls: number;
}

export interface TrendPoint {
  label: string;
  value: number;
}

export interface DashboardPayload {
  summary: DashboardSummary;
  etlTrend: TrendPoint[];
  apiTrend: TrendPoint[];
  alerts: string[];
  todoItems: string[];
}
