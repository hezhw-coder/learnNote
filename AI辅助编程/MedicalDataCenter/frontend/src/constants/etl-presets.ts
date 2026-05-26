import type { EtlDatasetConfigFieldKey, EtlJobFormModel } from '@/types/etl';

type PresetFieldValue = Pick<
  EtlJobFormModel,
  | 'sourceTable'
  | 'datasetCode'
  | 'idField'
  | 'nameField'
  | 'genderField'
  | 'birthDateField'
  | 'extraCodeField'
  | 'valueField'
  | 'unitField'
  | 'eventTimeField'
  | 'cleanRules'
  | 'mappings'
>;

export interface EtlDatasetFieldDefinition {
  key: EtlDatasetConfigFieldKey;
  targetField: string;
  label: string;
  placeholder: string;
}

export interface EtlDatasetPreset extends PresetFieldValue {
  code: string;
  label: string;
  fieldDefinitions: EtlDatasetFieldDefinition[];
}

const presetDefinitions: EtlDatasetPreset[] = [
  {
    code: 'cdm_patient',
    label: '患者主题',
    sourceTable: 'patient_source',
    datasetCode: 'cdm_patient',
    idField: 'patient_id',
    nameField: 'patient_name',
    genderField: 'gender',
    birthDateField: 'birth_date',
    extraCodeField: '',
    valueField: '',
    unitField: '',
    eventTimeField: '',
    cleanRules: 'trim;gender_mapping',
    mappings: [
      { sourceField: 'patient_id', targetField: 'patient_code', transformRule: 'trim' },
      { sourceField: 'patient_name', targetField: 'patient_name', transformRule: 'trim' },
      { sourceField: 'gender', targetField: 'gender', transformRule: 'gender_mapping' },
      { sourceField: 'birth_date', targetField: 'birth_date', transformRule: 'date_format' },
    ],
    fieldDefinitions: [
      { key: 'idField', targetField: 'patient_code', label: '主标识字段', placeholder: '如：patient_id / patient_code' },
      { key: 'nameField', targetField: 'patient_name', label: '名称字段', placeholder: '如：patient_name' },
      { key: 'genderField', targetField: 'gender', label: '性别字段', placeholder: '如：gender' },
      { key: 'birthDateField', targetField: 'birth_date', label: '生日字段', placeholder: '如：birth_date' },
    ],
  },
  {
    code: 'cdm_encounter',
    label: '就诊主题',
    sourceTable: 'encounter_source',
    datasetCode: 'cdm_encounter',
    idField: 'patient_code',
    nameField: 'encounter_type',
    genderField: '',
    birthDateField: '',
    extraCodeField: 'encounter_code',
    valueField: 'department_name',
    unitField: 'doctor_name',
    eventTimeField: 'encounter_date',
    cleanRules: 'trim;datetime_format',
    mappings: [
      { sourceField: 'patient_code', targetField: 'patient_code', transformRule: 'trim' },
      { sourceField: 'encounter_code', targetField: 'encounter_code', transformRule: 'trim' },
      { sourceField: 'encounter_type', targetField: 'encounter_type', transformRule: 'trim' },
      { sourceField: 'department_name', targetField: 'department_name', transformRule: 'trim' },
      { sourceField: 'doctor_name', targetField: 'doctor_name', transformRule: 'trim' },
      { sourceField: 'encounter_date', targetField: 'encounter_date', transformRule: 'datetime_format' },
    ],
    fieldDefinitions: [
      { key: 'idField', targetField: 'patient_code', label: '主标识字段', placeholder: '如：patient_code' },
      { key: 'nameField', targetField: 'encounter_type', label: '就诊类型字段', placeholder: '如：encounter_type' },
      { key: 'extraCodeField', targetField: 'encounter_code', label: '就诊编码字段', placeholder: '如：encounter_code' },
      { key: 'valueField', targetField: 'department_name', label: '科室字段', placeholder: '如：department_name' },
      { key: 'unitField', targetField: 'doctor_name', label: '医生字段', placeholder: '如：doctor_name' },
      { key: 'eventTimeField', targetField: 'encounter_date', label: '就诊日期字段', placeholder: '如：encounter_date' },
    ],
  },
  {
    code: 'cdm_lab',
    label: '检验主题',
    sourceTable: 'lab_source',
    datasetCode: 'cdm_lab',
    idField: 'patient_code',
    nameField: 'item_name',
    genderField: 'encounter_code',
    birthDateField: 'result_flag',
    extraCodeField: 'item_code',
    valueField: 'result_value',
    unitField: 'unit',
    eventTimeField: 'report_date',
    cleanRules: 'trim;result_numeric_normalize',
    mappings: [
      { sourceField: 'patient_code', targetField: 'patient_code', transformRule: 'trim' },
      { sourceField: 'encounter_code', targetField: 'encounter_code', transformRule: 'trim' },
      { sourceField: 'item_code', targetField: 'item_code', transformRule: 'trim' },
      { sourceField: 'item_name', targetField: 'item_name', transformRule: 'trim' },
      { sourceField: 'result_value', targetField: 'result_value', transformRule: 'result_numeric_normalize' },
      { sourceField: 'unit', targetField: 'unit', transformRule: 'trim' },
      { sourceField: 'result_flag', targetField: 'result_flag', transformRule: 'trim' },
      { sourceField: 'report_date', targetField: 'report_date', transformRule: 'datetime_format' },
    ],
    fieldDefinitions: [
      { key: 'idField', targetField: 'patient_code', label: '主标识字段', placeholder: '如：patient_code' },
      { key: 'nameField', targetField: 'item_name', label: '项目名称字段', placeholder: '如：item_name' },
      { key: 'genderField', targetField: 'encounter_code', label: '就诊编码字段', placeholder: '如：encounter_code' },
      { key: 'birthDateField', targetField: 'result_flag', label: '结果标记字段', placeholder: '如：result_flag' },
      { key: 'extraCodeField', targetField: 'item_code', label: '项目编码字段', placeholder: '如：item_code' },
      { key: 'valueField', targetField: 'result_value', label: '结果值字段', placeholder: '如：result_value' },
      { key: 'unitField', targetField: 'unit', label: '单位字段', placeholder: '如：unit' },
      { key: 'eventTimeField', targetField: 'report_date', label: '报告日期字段', placeholder: '如：report_date' },
    ],
  },
  {
    code: 'cdm_lab_result',
    label: '检验结果主题',
    sourceTable: 'lab_result_source',
    datasetCode: 'cdm_lab_result',
    idField: 'patient_code',
    nameField: 'item_name',
    genderField: '',
    birthDateField: '',
    extraCodeField: 'item_code',
    valueField: 'result_value',
    unitField: 'result_unit',
    eventTimeField: 'sample_time',
    cleanRules: 'trim;result_numeric_normalize',
    mappings: [
      { sourceField: 'patient_code', targetField: 'patient_code', transformRule: 'trim' },
      { sourceField: 'item_code', targetField: 'item_code', transformRule: 'trim' },
      { sourceField: 'item_name', targetField: 'item_name', transformRule: 'trim' },
      { sourceField: 'result_value', targetField: 'result_value', transformRule: 'result_numeric_normalize' },
      { sourceField: 'sample_time', targetField: 'sample_time', transformRule: 'datetime_format' },
    ],
    fieldDefinitions: [
      { key: 'idField', targetField: 'patient_code', label: '主标识字段', placeholder: '如：patient_code' },
      { key: 'nameField', targetField: 'item_name', label: '项目名称字段', placeholder: '如：item_name' },
      { key: 'extraCodeField', targetField: 'item_code', label: '项目编码字段', placeholder: '如：item_code' },
      { key: 'valueField', targetField: 'result_value', label: '结果值字段', placeholder: '如：result_value' },
      { key: 'unitField', targetField: 'result_unit', label: '结果单位字段', placeholder: '如：result_unit' },
      { key: 'eventTimeField', targetField: 'sample_time', label: '采样时间字段', placeholder: '如：sample_time' },
    ],
  },
];

export const ETL_DATASET_PRESETS = presetDefinitions.map((preset) => ({
  ...preset,
  mappings: preset.mappings.map((mapping) => ({ ...mapping })),
  fieldDefinitions: preset.fieldDefinitions.map((field) => ({ ...field })),
}));

const presetMap = new Map(ETL_DATASET_PRESETS.map((preset) => [preset.code, preset]));

export function getEtlDatasetPreset(datasetCode?: string) {
  return presetMap.get(datasetCode ?? '') ?? ETL_DATASET_PRESETS[0];
}

export function createEtlJobFormDefaults(): EtlJobFormModel {
  const preset = ETL_DATASET_PRESETS[0];
  return {
    name: '',
    sourceId: '',
    sourceTable: preset.sourceTable,
    datasetCode: preset.datasetCode,
    runMode: 'incremental',
    extractSql: '',
    incrementField: 'update_time',
    fieldBindings: Object.fromEntries(preset.fieldDefinitions.map((field) => [field.targetField, preset[field.key] ?? ''])),
    idField: preset.idField,
    nameField: preset.nameField,
    genderField: preset.genderField,
    birthDateField: preset.birthDateField,
    extraCodeField: preset.extraCodeField,
    valueField: preset.valueField,
    unitField: preset.unitField,
    eventTimeField: preset.eventTimeField,
    cleanRules: preset.cleanRules,
    mappings: preset.mappings.map((item) => ({ ...item })),
    schedule: '0 */10 * * * ?',
    enabled: true,
  };
}
