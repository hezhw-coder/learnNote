export type DataSourceType = 'MySQL' | 'PostgreSQL' | 'SQLServer' | 'Oracle' | 'MongoDB' | 'TiDB';

export interface DataSourceItem {
  id: string;
  name: string;
  type: DataSourceType;
  host: string;
  port: number;
  database: string;
  username: string;
  status: 'enabled' | 'disabled' | 'error';
  syncMode: 'full' | 'incremental';
  updateTime: string;
  owner: string;
}

export interface DataSourceFormModel {
  id?: string;
  name: string;
  type: DataSourceType;
  host: string;
  port: number;
  database: string;
  schema?: string;
  username: string;
  password: string;
  params?: string;
  connectTimeout: number;
  syncMode: 'full' | 'incremental';
  enabled: boolean;
}
