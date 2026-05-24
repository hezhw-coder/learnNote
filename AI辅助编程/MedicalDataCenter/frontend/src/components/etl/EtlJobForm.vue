<script setup lang="ts">
import { reactive, watch } from 'vue';
import type { DataSourceItem } from '@/types/data-source';
import type { EtlJobFormModel } from '@/types/etl';

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

const form = reactive<EtlJobFormModel>({
  name: '',
  sourceId: '',
  sourceTable: 'patient_source',
  datasetName: '',
  runMode: 'incremental',
  extractSql: '',
  incrementField: 'update_time',
  idField: 'patient_id',
  nameField: 'name',
  genderField: 'gender',
  birthDateField: 'birth_date',
  cleanRules: 'trim;mask_id_card;gender_mapping',
  mappings: [
    { sourceField: 'patient_id', targetField: 'patientCode', transformRule: 'trim' },
    { sourceField: 'name', targetField: 'patientName', transformRule: 'mask_name' },
  ],
  schedule: '0 */10 * * * ?',
  enabled: true,
});

watch(
  () => props.initialValue,
  (value) => {
    Object.assign(form, {
      name: '',
      sourceId: props.sourceOptions[0]?.id ?? '',
      sourceTable: 'patient_source',
      datasetName: '',
      runMode: 'incremental',
      extractSql: '',
      incrementField: 'update_time',
      idField: 'patient_id',
      nameField: 'name',
      genderField: 'gender',
      birthDateField: 'birth_date',
      cleanRules: 'trim;mask_id_card;gender_mapping',
      mappings: [
        { sourceField: 'patient_id', targetField: 'patientCode', transformRule: 'trim' },
        { sourceField: 'name', targetField: 'patientName', transformRule: 'mask_name' },
      ],
      schedule: '0 */10 * * * ?',
      enabled: true,
    });

    if (value) {
      Object.assign(form, value);
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

function handleSubmit() {
  emit('submit', {
    ...form,
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
      <el-form-item label="目标数据集">
        <el-input v-model="form.datasetName" placeholder="如：CDM_PATIENT" />
      </el-form-item>
      <el-form-item label="源表/集合">
        <el-input v-model="form.sourceTable" placeholder="如：patient_source" />
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
      <el-form-item label="主键字段">
        <el-input v-model="form.idField" placeholder="如：patient_id" />
      </el-form-item>
      <el-form-item label="姓名字段">
        <el-input v-model="form.nameField" placeholder="如：name" />
      </el-form-item>
      <el-form-item label="性别字段">
        <el-input v-model="form.genderField" placeholder="如：gender" />
      </el-form-item>
      <el-form-item label="生日字段">
        <el-input v-model="form.birthDateField" placeholder="如：birth_date" />
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
