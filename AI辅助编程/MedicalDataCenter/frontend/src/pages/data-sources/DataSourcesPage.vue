<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import {
  fetchDataSources,
  removeDataSourceById,
  submitDataSource,
  testDataSource,
  toggleDataSourceStatus,
} from '@/api/modules/data-sources';
import type { DataSourceFormModel, DataSourceItem } from '@/types/data-source';
import PageHeaderCard from '@/components/common/PageHeaderCard.vue';
import StatusTag from '@/components/common/StatusTag.vue';
import DataSourceForm from '@/components/datasource/DataSourceForm.vue';

const loading = ref(false);
const drawerVisible = ref(false);
const keyword = ref('');
const tableData = ref<DataSourceItem[]>([]);
const currentRecord = ref<Partial<DataSourceFormModel> | null>(null);

const filteredData = computed(() =>
  tableData.value.filter((item) => !keyword.value || item.name.includes(keyword.value) || item.type.includes(keyword.value)),
);

async function loadData() {
  loading.value = true;
  try {
    tableData.value = await fetchDataSources();
  } finally {
    loading.value = false;
  }
}

function openCreate() {
  currentRecord.value = null;
  drawerVisible.value = true;
}

function openEdit(row: DataSourceItem) {
  currentRecord.value = {
    id: row.id,
    name: row.name,
    type: row.type,
    host: row.host,
    port: row.port,
    database: row.database,
    username: row.username,
    connectTimeout: 10,
    syncMode: row.syncMode,
    enabled: row.status === 'enabled',
  };
  drawerVisible.value = true;
}

async function handleSubmit(payload: DataSourceFormModel) {
  await submitDataSource(payload);
  ElMessage.success('数据源已保存');
  drawerVisible.value = false;
  await loadData();
}

async function handleTest(payload: DataSourceFormModel) {
  const result = await testDataSource(payload);
  if (result.success) {
    ElMessage.success(result.message);
    return;
  }
  ElMessage.error(result.message);
}

async function handleQuickTest(row: DataSourceItem) {
  await handleTest({
    id: row.id,
    name: row.name,
    type: row.type,
    host: row.host,
    port: row.port,
    database: row.database,
    username: row.username,
    password: '',
    schema: '',
    params: '',
    connectTimeout: 10,
    syncMode: row.syncMode,
    enabled: row.status === 'enabled',
  });
}

async function handleToggle(id: string) {
  await toggleDataSourceStatus(id);
  ElMessage.success('状态已切换');
  await loadData();
}

async function handleDelete(id: string) {
  await ElMessageBox.confirm('删除后仅执行软删除占位，是否继续？', '删除确认', { type: 'warning' });
  await removeDataSourceById(id);
  ElMessage.success('数据源已删除');
  await loadData();
}

onMounted(loadData);
</script>

<template>
  <div class="page-shell">
    <PageHeaderCard
      title="数据源管理"
      tag="多源接入"
      description="覆盖 MySQL、PostgreSQL、SQL Server、Oracle、MongoDB、TiDB 的接入骨架与动态配置表单。"
    >
      <template #actions>
        <el-button @click="loadData">刷新</el-button>
        <el-button type="primary" @click="openCreate">新增数据源</el-button>
      </template>
    </PageHeaderCard>

    <el-card class="panel-card" shadow="never">
      <div class="toolbar-row">
        <el-input v-model="keyword" placeholder="按名称或类型搜索" clearable style="width: 280px" />
        <el-alert title="敏感连接信息后续由后端加密存储，本页仅保留录入与测试交互。" type="info" :closable="false" />
      </div>
    </el-card>

    <el-card class="panel-card" shadow="never">
      <el-table :data="filteredData" v-loading="loading">
        <el-table-column prop="name" label="数据源名称" min-width="160" />
        <el-table-column prop="type" label="类型" width="120" />
        <el-table-column label="连接地址" min-width="220">
          <template #default="{ row }">
            {{ row.host }}:{{ row.port }}/{{ row.database }}
          </template>
        </el-table-column>
        <el-table-column prop="syncMode" label="同步策略" width="110" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <StatusTag :status="row.status" />
          </template>
        </el-table-column>
        <el-table-column prop="owner" label="责任团队" min-width="120" />
        <el-table-column prop="updateTime" label="更新时间" width="160" />
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button text @click="handleToggle(row.id)">{{ row.status === 'enabled' ? '停用' : '启用' }}</el-button>
            <el-button text @click="handleQuickTest(row)">测试</el-button>
            <el-button text type="danger" @click="handleDelete(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <DataSourceForm
      v-model="drawerVisible"
      :loading="loading"
      :initial-value="currentRecord"
      @submit="handleSubmit"
      @test="handleTest"
    />
  </div>
</template>
