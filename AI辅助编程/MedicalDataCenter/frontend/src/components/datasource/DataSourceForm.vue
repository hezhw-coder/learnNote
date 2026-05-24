<script setup lang="ts">
import { computed, reactive, watch } from 'vue';
import type { DataSourceFormModel, DataSourceType } from '@/types/data-source';

const props = defineProps<{
  modelValue: boolean;
  loading?: boolean;
  initialValue?: Partial<DataSourceFormModel> | null;
}>();

const emit = defineEmits<{
  'update:modelValue': [value: boolean];
  submit: [value: DataSourceFormModel];
  test: [value: DataSourceFormModel];
}>();

const portMap: Record<DataSourceType, number> = {
  MySQL: 3306,
  PostgreSQL: 5432,
  SQLServer: 1433,
  Oracle: 1521,
  MongoDB: 27017,
  TiDB: 4000,
};

const form = reactive<DataSourceFormModel>({
  name: '',
  type: 'MySQL',
  host: '',
  port: 3306,
  database: '',
  schema: '',
  username: '',
  password: '',
  params: '',
  connectTimeout: 10,
  syncMode: 'incremental',
  enabled: true,
});

const isDocumentDb = computed(() => form.type === 'MongoDB');

watch(
  () => props.initialValue,
  (value) => {
    Object.assign(form, {
      name: '',
      type: 'MySQL',
      host: '',
      port: 3306,
      database: '',
      schema: '',
      username: '',
      password: '',
      params: '',
      connectTimeout: 10,
      syncMode: 'incremental',
      enabled: true,
    });

    if (value) {
      Object.assign(form, value);
    }
  },
  { immediate: true },
);

watch(
  () => form.type,
  (type) => {
    form.port = portMap[type];
  },
);

function handleSubmit() {
  emit('submit', { ...form });
}

function handleTest() {
  emit('test', { ...form });
}
</script>

<template>
  <el-drawer
    :model-value="modelValue"
    :title="form.id ? '编辑数据源' : '新增数据源'"
    size="520px"
    @close="emit('update:modelValue', false)"
  >
    <el-form label-width="100px" class="datasource-form">
      <el-form-item label="名称">
        <el-input v-model="form.name" placeholder="如：HIS 主库" />
      </el-form-item>
      <el-form-item label="类型">
        <el-select v-model="form.type">
          <el-option v-for="type in Object.keys(portMap)" :key="type" :label="type" :value="type" />
        </el-select>
      </el-form-item>
      <el-form-item label="主机">
        <el-input v-model="form.host" placeholder="10.10.1.12" />
      </el-form-item>
      <el-form-item label="端口">
        <el-input-number v-model="form.port" :min="1" :max="65535" style="width: 100%" />
      </el-form-item>
      <el-form-item :label="isDocumentDb ? '库名' : '数据库'">
        <el-input v-model="form.database" placeholder="请输入数据库名称" />
      </el-form-item>
      <el-form-item v-if="!isDocumentDb" label="Schema">
        <el-input v-model="form.schema" placeholder="可选" />
      </el-form-item>
      <el-form-item label="用户名">
        <el-input v-model="form.username" placeholder="请输入账号" />
      </el-form-item>
      <el-form-item label="密码">
        <el-input v-model="form.password" show-password placeholder="请输入密码" />
      </el-form-item>
      <el-form-item label="连接参数">
        <el-input v-model="form.params" type="textarea" :rows="3" placeholder="SSL、时区、只读参数等" />
      </el-form-item>
      <el-form-item label="同步策略">
        <el-radio-group v-model="form.syncMode">
          <el-radio-button label="full">全量</el-radio-button>
          <el-radio-button label="incremental">增量</el-radio-button>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="超时(s)">
        <el-slider v-model="form.connectTimeout" :min="5" :max="60" show-input />
      </el-form-item>
      <el-form-item label="启用状态">
        <el-switch v-model="form.enabled" />
      </el-form-item>
    </el-form>

    <template #footer>
      <div class="footer-actions">
        <el-button @click="emit('update:modelValue', false)">取消</el-button>
        <el-button :loading="loading" @click="handleTest">测试连接</el-button>
        <el-button type="primary" :loading="loading" @click="handleSubmit">保存</el-button>
      </div>
    </template>
  </el-drawer>
</template>

<style scoped>
.datasource-form {
  padding-right: 14px;
}

.footer-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
</style>
