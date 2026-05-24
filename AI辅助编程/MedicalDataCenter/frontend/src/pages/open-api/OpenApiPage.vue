<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { fetchApiClients, fetchApiLogs } from '@/api/modules/open-api';
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

const clients = ref<ApiClientRecord[]>([]);
const logs = ref<ApiLogRecord[]>([]);

async function loadData() {
  const [clientList, logList] = await Promise.all([fetchApiClients(), fetchApiLogs()]);
  clients.value = clientList;
  logs.value = logList;
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
        <el-button type="primary">新增客户端</el-button>
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
          <span>4</span>
        </div>
      </el-card>
      <el-card class="panel-card" shadow="never">
        <div class="api-summary">
          <strong>限流告警</strong>
          <span>1</span>
        </div>
      </el-card>
    </div>

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
</style>
