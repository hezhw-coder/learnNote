import { request } from '@/api/http';

interface BackendDatasetField {
  fieldCode: string;
  fieldName: string;
  fieldType: string;
}

interface BackendDataset {
  id: number;
  code: string;
  name: string;
  description: string;
  fields: BackendDatasetField[];
  sampleRows: Array<Record<string, string>>;
}

export function fetchDatasets() {
  return request<BackendDataset[]>({
    url: '/datasets',
    method: 'get',
  }).then((items) =>
    items.map((item) => ({
      id: String(item.id),
      name: item.code,
      category: item.code.startsWith('cdm_') ? '标准层' : '主题域',
      owner: '标准化模型组',
      updateTime: '-',
      description: item.description,
      fields: item.fields.map((field) => ({
        source: field.fieldCode,
        target: field.fieldName,
        type: field.fieldType,
        lineage: item.code,
      })),
      samples: item.sampleRows,
    })),
  );
}
