<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { createApiClient, fetchApiClients, fetchApiLogs, fetchApiScopes } from '@/api/modules/open-api';
import PageHeaderCard from '@/components/common/PageHeaderCard.vue';

interface ApiClientRecord {
  id: string;
  name: string;
  appKey: string;
  scope: string;
  rateLimit: string;
  updateTime: string;
  status: string;
}

interface ApiLogRecord {
  id: string;
  clientName: string;
  endpoint: string;
  status: number;
  latency: string;
  timestamp: string;
}

interface ApiScopeRecord {
  id: number;
  code: string;
  name: string;
  description: string;
}

const clients = ref<ApiClientRecord[]>([]);
const logs = ref<ApiLogRecord[]>([]);
const scopes = ref<ApiScopeRecord[]>([]);
const createDialogVisible = ref(false);
const createForm = reactive({
  name: '',
  clientId: '',
  clientSecret: '',
  scopes: [] as string[],
});

const rateLimitedCount = computed(() => logs.value.filter((item) => item.status === 429).length);

async function loadData() {
  const [clientList, logList, scopeList] = await Promise.all([
    fetchApiClients(),
    fetchApiLogs(),
    fetchApiScopes(),
  ]);
  clients.value = clientList;
  logs.value = logList;
  scopes.value = scopeList;
}

function openCreateDialog() {
  createForm.name = '';
  createForm.clientId = '';
  createForm.clientSecret = '';
  createForm.scopes = scopes.value.length ? [scopes.value[0].code] : [];
  createDialogVisible.value = true;
}

async function submitCreateClient() {
  await createApiClient({
    name: createForm.name,
    clientId: createForm.clientId,
    clientSecret: createForm.clientSecret,
    scopes: createForm.scopes.join(','),
  });
  ElMessage.success('客户端已创建');
  createDialogVisible.value = false;
  await loadData();
}

onMounted(loadData);
</script>

<template>
  <div class="page-shell">
    <PageHeaderCard
      title="开放 API"
      tag="OAuth2 + JWT"
      description="管理客户端应用、作用域、限流策略与调用日志，为第三方患者、就诊、检验、报表查询提供接口入口。"
    >
      <template #actions>
        <el-button>查看接口目录</el-button>
        <el-button type="primary" @click="openCreateDialog">新增客户端</el-button>
      </template>
    </PageHeaderCard>

    <div class="page-grid columns-3">
      <el-card class="panel-card" shadow="never">
        <div class="api-summary">
          <strong>客户端应用</strong>
          <span>{{ clients.length }}</span>
        </div>
      </el-card>
      <el-card class="panel-card" shadow="never">
        <div class="api-summary">
          <strong>标准作用域</strong>
          <span>{{ scopes.length }}</span>
        </div>
      </el-card>
      <el-card class="panel-card" shadow="never">
        <div class="api-summary">
          <strong>限流告警</strong>
          <span>{{ rateLimitedCount }}</span>
        </div>
      </el-card>
    </div>

    <el-card class="panel-card" shadow="never">
      <template #header>
        <div class="section-title">
          <h3>作用域</h3>
        </div>
      </template>
      <div class="scope-list">
        <el-tag v-for="item in scopes" :key="item.id" class="scope-tag" effect="plain">
          {{ item.code }}
        </el-tag>
      </div>
    </el-card>

    <el-card class="panel-card" shadow="never">
      <template #header>
        <div class="section-title">
          <h3>客户端应用</h3>
        </div>
      </template>
      <el-table :data="clients">
        <el-table-column prop="name" label="应用名称" min-width="180" />
        <el-table-column prop="appKey" label="客户端标识" min-width="160" />
        <el-table-column prop="scope" label="作用域" min-width="220" />
        <el-table-column prop="rateLimit" label="限流策略" width="140" />
        <el-table-column prop="updateTime" label="更新时间" width="160" />
        <el-table-column prop="status" label="状态" width="100" />
      </el-table>
    </el-card>

    <el-card class="panel-card" shadow="never">
      <template #header>
        <div class="section-title">
          <h3>调用日志</h3>
          <el-tag type="warning">含限流返回</el-tag>
        </div>
      </template>
      <el-table :data="logs">
        <el-table-column prop="clientName" label="客户端" min-width="180" />
        <el-table-column prop="endpoint" label="接口地址" min-width="220" />
        <el-table-column prop="status" label="状态码" width="100" />
        <el-table-column prop="latency" label="耗时" width="120" />
        <el-table-column prop="timestamp" label="时间" width="180" />
      </el-table>
    </el-card>

    <el-dialog v-model="createDialogVisible" title="新增客户端" width="520px">
      <el-form label-width="96px">
        <el-form-item label="应用名称">
          <el-input v-model="createForm.name" placeholder="请输入应用名称" />
        </el-form-item>
        <el-form-item label="客户端标识">
          <el-input v-model="createForm.clientId" placeholder="请输入 clientId" />
        </el-form-item>
        <el-form-item label="客户端密钥">
          <el-input v-model="createForm.clientSecret" type="password" show-password placeholder="请输入 clientSecret" />
        </el-form-item>
        <el-form-item label="作用域">
          <el-select v-model="createForm.scopes" multiple placeholder="请选择作用域" style="width: 100%">
            <el-option
              v-for="item in scopes"
              :key="item.id"
              :label="`${item.code} - ${item.name}`"
              :value="item.code"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitCreateClient">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.api-summary {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 86px;
}

.api-summary strong {
  color: #475569;
}

.api-summary span {
  font-size: 32px;
  font-weight: 700;
}

.scope-list {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.scope-tag {
  margin-right: 0;
}
</style>
