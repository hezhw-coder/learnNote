export interface ApiResponse<T> {
  code: number;
  message: string;
  data: T;
  requestId: string;
  timestamp: string;
}

export interface PageQuery {
  page: number;
  pageSize: number;
  keyword?: string;
  status?: string;
}

export interface PageResult<T> {
  items: T[];
  total: number;
  page: number;
  pageSize: number;
}

export interface OptionItem {
  label: string;
  value: string;
}

export interface MenuItem {
  title: string;
  path: string;
  icon: string;
  permission: string;
}
