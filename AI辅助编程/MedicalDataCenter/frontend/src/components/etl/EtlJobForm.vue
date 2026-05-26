<script setup lang="ts">
import { computed, reactive, watch } from 'vue';
import {
  ETL_DATASET_PRESETS,
  createEtlJobFormDefaults,
  getEtlDatasetPreset,
} from '@/constants/etl-presets';
import type { DataSourceItem } from '@/types/data-source';
import type { EtlDatasetConfigFieldKey, EtlJobFormModel } from '@/types/etl';

const props = defineProps<{
  modelValue: boolean;
  initialValue?: Partial<EtlJobFormModel> | null;
  sourceOptions: DataSourceItem[];
  loading?: boolean;
}>();

const emit = defineEmits<{
  'update:modelValue': [value: boolean];
  submit: [value: EtlJobFormModel];
}>();
const form = reactive<EtlJobFormModel>(createEtlJobFormDefaults());
const activeDatasetPreset = computed(() => getEtlDatasetPreset(form.datasetCode));
const activeFieldDefinitions = computed(() => activeDatasetPreset.value.fieldDefinitions);

function applyDatasetPreset(datasetCode: string) {
  const preset = getEtlDatasetPreset(datasetCode);
  Object.assign(form, {
    sourceTable: preset.sourceTable,
    datasetCode: preset.datasetCode,
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
  });
}

function createDefaultFormState() {
  return {
    ...createEtlJobFormDefaults(),
    sourceId: props.sourceOptions[0]?.id ?? '',
  };
}

function fieldValue(key: EtlDatasetConfigFieldKey) {
  const fieldDefinition = activeFieldDefinitions.value.find((item) => item.key === key);
  if (!fieldDefinition) {
    return form[key] ?? '';
  }
  return form.fieldBindings[fieldDefinition.targetField] ?? form[key] ?? '';
}

function setFieldValue(key: EtlDatasetConfigFieldKey, targetField: string, value: string) {
  form[key] = value;
  form.fieldBindings[targetField] = value;
}

watch(
  () => props.initialValue,
  (value) => {
    Object.assign(form, createDefaultFormState());

    if (value) {
      Object.assign(form, value);
      const preset = getEtlDatasetPreset(form.datasetCode);
      form.fieldBindings = {
        ...Object.fromEntries(preset.fieldDefinitions.map((field) => [field.targetField, form[field.key] ?? ''])),
        ...(value.fieldBindings ?? {}),
      };
    }
  },
  { immediate: true },
);

function addMapping() {
  form.mappings.push({
    sourceField: '',
    targetField: '',
    transformRule: '',
  });
}

function removeMapping(index: number) {
  form.mappings.splice(index, 1);
}

function handleDatasetChange(value: string) {
  applyDatasetPreset(value);
}

function handleSubmit() {
  emit('submit', {
    ...form,
    fieldBindings: { ...form.fieldBindings },
    mappings: form.mappings.map((item) => ({ ...item })),
  });
}
</script>

<template>
  <el-drawer
    :model-value="modelValue"
    :title="form.id ? '编辑 ETL 任务' : '新建 ETL 任务'"
    size="620px"
    @close="emit('update:modelValue', false)"
  >
    <el-form label-width="100px">
      <el-form-item label="任务名称">
        <el-input v-model="form.name" placeholder="如：患者主索引同步" />
      </el-form-item>
      <el-form-item label="数据源">
        <el-select v-model="form.sourceId" placeholder="请选择数据源">
          <el-option v-for="source in sourceOptions" :key="source.id" :label="source.name" :value="source.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="目标主题">
        <el-select v-model="form.datasetCode" placeholder="请选择标准主题" @change="handleDatasetChange">
          <el-option
            v-for="preset in ETL_DATASET_PRESETS"
            :key="preset.code"
            :label="`${preset.label} (${preset.code})`"
            :value="preset.code"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="源表/集合">
        <el-input v-model="form.sourceTable" placeholder="如：patient_source / lab_result_source" />
      </el-form-item>
      <el-form-item label="抽取策略">
        <el-radio-group v-model="form.runMode">
          <el-radio-button label="full">全量</el-radio-button>
          <el-radio-button label="incremental">增量</el-radio-button>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="抽取 SQL">
        <el-input v-model="form.extractSql" type="textarea" :rows="3" placeholder="可选，留空则按源表全量查询" />
      </el-form-item>
      <el-form-item label="增量字段" v-if="form.runMode === 'incremental'">
        <el-input v-model="form.incrementField" placeholder="如：update_time / id" />
      </el-form-item>
      <el-form-item
        v-for="field in activeFieldDefinitions"
        :key="field.key"
        :label="field.label"
      >
        <el-input
          :model-value="fieldValue(field.key)"
          :placeholder="field.placeholder"
          @update:model-value="setFieldValue(field.key, field.targetField, $event)"
        />
      </el-form-item>
      <el-form-item label="清洗规则">
        <el-input v-model="form.cleanRules" type="textarea" :rows="3" />
      </el-form-item>
      <el-form-item label="执行计划">
        <el-input v-model="form.schedule" placeholder="Quartz Cron 表达式" />
      </el-form-item>
      <el-form-item label="字段映射">
        <div class="mapping-wrapper">
          <div v-for="(mapping, index) in form.mappings" :key="index" class="mapping-row">
            <el-input v-model="mapping.sourceField" placeholder="源字段" />
            <el-input v-model="mapping.targetField" placeholder="目标字段" />
            <el-input v-model="mapping.transformRule" placeholder="转换规则" />
            <el-button text type="danger" @click="removeMapping(index)">删除</el-button>
          </div>
          <el-button dashed class="mapping-button" @click="addMapping">新增映射</el-button>
        </div>
      </el-form-item>
      <el-form-item label="启用">
        <el-switch v-model="form.enabled" />
      </el-form-item>
    </el-form>

    <template #footer>
      <div class="footer-actions">
        <el-button @click="emit('update:modelValue', false)">取消</el-button>
        <el-button type="primary" :loading="loading" @click="handleSubmit">保存任务</el-button>
      </div>
    </template>
  </el-drawer>
</template>

<style scoped>
.mapping-wrapper {
  display: flex;
  flex-direction: column;
  gap: 12px;
  width: 100%;
}

.mapping-row {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr auto;
  gap: 10px;
}

.mapping-button {
  width: 100%;
}

.footer-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
</style>
