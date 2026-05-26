<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { fetchDataSources } from '@/api/modules/data-sources';
import { fetchEtlJobs, fetchEtlRunLogs, submitEtlJob, triggerEtlJob } from '@/api/modules/etl';
import type { DataSourceItem } from '@/types/data-source';
import type { EtlJobFormModel, EtlJobItem, EtlRunLog } from '@/types/etl';
import PageHeaderCard from '@/components/common/PageHeaderCard.vue';
import StatusTag from '@/components/common/StatusTag.vue';
import EtlJobForm from '@/components/etl/EtlJobForm.vue';

const loading = ref(false);
const drawerVisible = ref(false);
const logsVisible = ref(false);
const sourceOptions = ref<DataSourceItem[]>([]);
const jobs = ref<EtlJobItem[]>([]);
const runLogs = ref<EtlRunLog[]>([]);

async function loadData() {
  loading.value = true;
  try {
    const [sourceList, jobList, logList] = await Promise.all([fetchDataSources(), fetchEtlJobs(), fetchEtlRunLogs()]);
    sourceOptions.value = sourceList;
    jobs.value = jobList;
    runLogs.value = logList.map((log) => ({
      ...log,
      jobName: jobList.find((job) => job.id === log.jobId)?.name ?? log.jobName,
    }));
  } finally {
    loading.value = false;
  }
}

async function handleSubmit(payload: EtlJobFormModel) {
  await submitEtlJob(payload);
  ElMessage.success('ETL 任务已保存');
  drawerVisible.value = false;
  await loadData();
}

async function handleRun(id: string) {
  const result = await triggerEtlJob(id);
  ElMessage.success(result.message);
  await loadData();
}

onMounted(loadData);
</script>

<template>
  <div class="page-shell">
    <PageHeaderCard
      title="抽取集成"
      tag="ODS/CDM"
      description="支持全量/增量抽取、字段映射、清洗规则与执行日志查看，契合 ODS 到 CDM 双层建模。"
    >
      <template #actions>
        <el-button @click="logsVisible = true">查看执行日志</el-button>
        <el-button type="primary" @click="drawerVisible = true">新建任务</el-button>
      </template>
    </PageHeaderCard>

    <el-card class="panel-card" shadow="never">
      <el-alert
        title="当前已支持患者、就诊、检验、检验结果、用药医嘱五个标准主题的最小闭环，并已切换为配置驱动的主题字段绑定方式。"
        type="info"
        :closable="false"
      />
    </el-card>

    <el-card class="panel-card" shadow="never">
      <el-table :data="jobs" v-loading="loading">
        <el-table-column prop="name" label="任务名称" min-width="180" />
        <el-table-column prop="sourceName" label="来源数据源" min-width="160" />
        <el-table-column prop="datasetName" label="目标主题" min-width="160" />
        <el-table-column prop="runMode" label="抽取策略" width="110" />
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <StatusTag :status="row.status" />
          </template>
        </el-table-column>
        <el-table-column prop="schedule" label="调度计划" min-width="160" />
        <el-table-column prop="latestRun" label="最近执行" width="160" />
        <el-table-column label="操作" width="180">
          <template #default="{ row }">
            <el-button text type="primary" @click="handleRun(row.id)">立即执行</el-button>
            <el-button text @click="logsVisible = true">查看日志</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <EtlJobForm v-model="drawerVisible" :source-options="sourceOptions" :loading="loading" @submit="handleSubmit" />

    <el-drawer v-model="logsVisible" title="执行日志" size="620px">
      <el-timeline>
        <el-timeline-item
          v-for="log in runLogs"
          :key="log.id"
          :timestamp="`${log.startTime} -> ${log.endTime}`"
          :type="log.status === 'failed' ? 'danger' : 'primary'"
        >
          <div class="log-item">
            <strong>{{ log.jobName }}</strong>
            <StatusTag :status="log.status" />
          </div>
          <div class="muted-text">记录数：{{ log.records }}</div>
          <div>{{ log.message }}</div>
        </el-timeline-item>
      </el-timeline>
    </el-drawer>
  </div>
</template>

<style scoped>
.log-item {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 6px;
}
</style>
