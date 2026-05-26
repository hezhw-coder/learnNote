export interface EtlFieldMapping {
  sourceField: string;
  targetField: string;
  transformRule: string;
}

export type EtlDatasetConfigFieldKey =
  | 'idField'
  | 'nameField'
  | 'genderField'
  | 'birthDateField'
  | 'extraCodeField'
  | 'valueField'
  | 'unitField'
  | 'eventTimeField';

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
  datasetCode: string;
  runMode: 'full' | 'incremental';
  extractSql?: string;
  incrementField: string;
  fieldBindings: Record<string, string>;
  idField: string;
  nameField: string;
  genderField?: string;
  birthDateField?: string;
  extraCodeField?: string;
  valueField?: string;
  unitField?: string;
  eventTimeField?: string;
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
