export interface EtlFieldMapping {
  sourceField: string;
  targetField: string;
  transformRule: string;
}

export interface EtlJobItem {
  id: string;
  name: string;
  sourceId?: string;
  sourceName: string;
  datasetName: string;
  runMode: 'full' | 'incremental';
  status: 'draft' | 'running' | 'success' | 'failed';
  schedule: string;
  updateTime: string;
  latestRun: string;
}

export interface EtlJobFormModel {
  id?: string;
  name: string;
  sourceId: string;
  sourceTable: string;
  datasetName: string;
  runMode: 'full' | 'incremental';
  extractSql?: string;
  incrementField: string;
  idField: string;
  nameField: string;
  genderField?: string;
  birthDateField?: string;
  cleanRules: string;
  mappings: EtlFieldMapping[];
  schedule: string;
  enabled: boolean;
}

export interface EtlRunLog {
  id: string;
  jobId?: string;
  jobName: string;
  startTime: string;
  endTime: string;
  status: 'success' | 'failed' | 'running';
  records: number;
  message: string;
}
