<script setup lang="ts">
import dayjs from 'dayjs';
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { ElMessage } from 'element-plus';
import {
  createReportSchedule,
  fetchReportJobs,
  fetchReportNotifications,
  fetchReportSnapshots,
  fetchReportTemplates,
  runReportSchedule,
  updateReportSchedule,
} from '@/api/modules/reports';
import PageHeaderCard from '@/components/common/PageHeaderCard.vue';
import StatusTag from '@/components/common/StatusTag.vue';
import type {
  ReportJobItem,
  ReportInteractionContext,
  ReportNotificationItem,
  ReportParameterValue,
  ReportScheduleForm,
  ReportSnapshotItem,
  ReportTemplateSchema,
} from '@/types/report';

const jobs = ref<ReportJobItem[]>([]);
const templates = ref<ReportTemplateSchema[]>([]);
const snapshots = ref<ReportSnapshotItem[]>([]);
const notifications = ref<ReportNotificationItem[]>([]);
const loading = ref(false);
const createDialogVisible = ref(false);
const formLoading = ref(false);
const editingScheduleId = ref<string>('');
const scheduleForm = reactive<ReportScheduleForm>({
  templateId: '',
  cronExpression: '0 0 8 * * ?',
  enabled: true,
  channel: 'IN_APP',
  runtimeParams: {
    parameters: [],
    interactions: [],
  },
});

const selectedTemplate = computed(() => templates.value.find((item) => item.id === scheduleForm.templateId));
const currentTemplateParameters = computed(() => selectedTemplate.value?.parameters.filter((item) => item.field.trim()) ?? []);
const currentTemplateInteractions = computed(() => selectedTemplate.value?.interactions ?? []);

async function loadData() {
  loading.value = true;
  try {
    const [jobList, templateList, snapshotList, notificationList] = await Promise.all([
      fetchReportJobs(),
      fetchReportTemplates(),
      fetchReportSnapshots(),
      fetchReportNotifications(),
    ]);
    jobs.value = jobList;
    templates.value = templateList;
    snapshots.value = snapshotList;
    notifications.value = notificationList;
    if (!scheduleForm.templateId && templateList.length) {
      scheduleForm.templateId = templateList[0].id;
      syncScheduleRuntimeParams(templateList[0].id, [], []);
    }
  } finally {
    loading.value = false;
  }
}

function formatDateTime(value: string) {
  return value && dayjs(value).isValid() ? dayjs(value).format('YYYY-MM-DD HH:mm:ss') : '-';
}

function templateSummary(templateId: string) {
  const target = templates.value.find((item) => item.id === templateId);
  if (!target) {
    return '-';
  }
  return `${target.datasetCode} / ${target.layout.columns}列 / ${target.widgets.length}组件 / ${target.filters.length}筛选 / ${target.parameters.length}参数`;
}

function buildRuntimeParams(
  templateId: string,
  existingParameters: ReportParameterValue[] = [],
  existingInteractions: ReportInteractionContext[] = [],
) {
  const template = templates.value.find((item) => item.id === templateId);
  const parameterMap = new Map(existingParameters.map((item) => [item.parameterCode, item.value]));
  const interactionMap = new Map(existingInteractions.map((item) => [item.interactionId, item]));
  return {
    parameters: (template?.parameters ?? [])
      .filter((parameter) => parameter.field.trim())
      .map<ReportParameterValue>((parameter) => ({
        parameterCode: parameter.code,
        value: parameterMap.get(parameter.code) ?? parameter.defaultValue ?? '',
      })),
    interactions: (template?.interactions ?? []).map<ReportInteractionContext>((interaction) => ({
      interactionId: interaction.id,
      sourceWidgetId: interaction.sourceWidgetId,
      targetWidgetId: interaction.targetWidgetId,
      sourceField: interaction.sourceField,
      targetField: interaction.targetField,
      value: interactionMap.get(interaction.id)?.value ?? '',
    })),
  };
}

function syncScheduleRuntimeParams(
  templateId: string,
  existingParameters: ReportParameterValue[] = [],
  existingInteractions: ReportInteractionContext[] = [],
) {
  scheduleForm.runtimeParams = buildRuntimeParams(templateId, existingParameters, existingInteractions);
}

function runtimeParamSummary(job: ReportJobItem) {
  const parameterSummary = job.runtimeParams.parameters
    .filter((item) => item.value.trim())
    .map((item) => {
      const template = templates.value.find((current) => current.id === job.templateId);
      const parameter = template?.parameters.find((current) => current.code === item.parameterCode);
      return `${parameter?.label || item.parameterCode}=${item.value}`;
    });
  const interactionSummary = job.runtimeParams.interactions
    .filter((item) => item.value.trim())
    .map((item) => `${item.targetField}=${item.value}`);
  const activeItems = [...parameterSummary, ...interactionSummary];
  if (!activeItems.length) {
    return '未配置运行时参数';
  }
  return activeItems.join(' / ');
}

async function handleCreateSchedule() {
  if (!scheduleForm.templateId) {
    ElMessage.warning('请选择报表模板');
    return;
  }
  formLoading.value = true;
  try {
    if (editingScheduleId.value) {
      await updateReportSchedule(editingScheduleId.value, scheduleForm);
      ElMessage.success('调度任务已更新');
    } else {
      await createReportSchedule(scheduleForm);
      ElMessage.success('调度任务已创建');
    }
    createDialogVisible.value = false;
    editingScheduleId.value = '';
    await loadData();
  } finally {
    formLoading.value = false;
  }
}

async function handleRunJob(job: ReportJobItem) {
  await runReportSchedule(job.id);
  ElMessage.success(`任务 ${job.templateName} 已执行，快照和站内通知已刷新`);
  await loadData();
}

function handleEditJob(job: ReportJobItem) {
  editingScheduleId.value = job.id;
  scheduleForm.templateId = job.templateId;
  scheduleForm.channel = job.pushChannel;
  scheduleForm.cronExpression = job.cron;
  scheduleForm.enabled = job.status === 'enabled';
  syncScheduleRuntimeParams(job.templateId, job.runtimeParams.parameters, job.runtimeParams.interactions);
  createDialogVisible.value = true;
}

function handleCreateDialogOpen() {
  editingScheduleId.value = '';
  scheduleForm.templateId = templates.value[0]?.id ?? '';
  scheduleForm.cronExpression = '0 0 8 * * ?';
  scheduleForm.enabled = true;
  scheduleForm.channel = 'IN_APP';
  syncScheduleRuntimeParams(scheduleForm.templateId, [], []);
  createDialogVisible.value = true;
}

watch(
  () => scheduleForm.templateId,
  (value, previous) => {
    if (!value || value === previous) {
      return;
    }
    syncScheduleRuntimeParams(value, scheduleForm.runtimeParams.parameters, scheduleForm.runtimeParams.interactions);
  },
);

onMounted(loadData);
</script>

<template>
  <div class="page-shell">
    <PageHeaderCard
      title="报表任务"
      tag="调度推送"
      description="用于配置定时报表生成、触发快照落库并回写站内通知，邮件通道当前仍保留占位能力。"
    >
      <template #actions>
        <el-button @click="loadData">刷新</el-button>
        <el-button type="primary" @click="handleCreateDialogOpen">新建调度</el-button>
      </template>
    </PageHeaderCard>

    <el-card class="panel-card" shadow="never">
      <el-table v-loading="loading" :data="jobs">
        <el-table-column prop="templateName" label="报表模板" min-width="220" />
        <el-table-column label="模板版本" width="100">
          <template #default="{ row }">
            v{{ row.templateVersion || 0 }}
          </template>
        </el-table-column>
          <el-table-column label="模板摘要" min-width="220">
            <template #default="{ row }">
              {{ templateSummary(row.templateId) }}
            </template>
          </el-table-column>
        <el-table-column prop="cron" label="Cron" min-width="180" />
        <el-table-column prop="pushChannel" label="推送通道" width="120" />
        <el-table-column label="运行参数" min-width="220">
          <template #default="{ row }">
            {{ runtimeParamSummary(row) }}
          </template>
        </el-table-column>
        <el-table-column label="最近执行" min-width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.latestRun) }}
          </template>
        </el-table-column>
        <el-table-column label="下一次执行" min-width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.nextRun) }}
          </template>
        </el-table-column>
        <el-table-column prop="owner" label="归属人" min-width="120" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <StatusTag :status="row.status" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180">
          <template #default="{ row }">
            <el-button text type="primary" @click="handleRunJob(row)">立即执行</el-button>
            <el-button text @click="handleEditJob(row)">编辑</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <div class="jobs-grid">
      <el-card class="panel-card" shadow="never">
        <template #header>
          <div class="section-title">
            <h3>最近快照</h3>
          </div>
        </template>
        <el-table :data="snapshots" max-height="320">
          <el-table-column prop="templateName" label="报表模板" min-width="180" />
          <el-table-column label="版本" width="90">
            <template #default="{ row }">
              v{{ row.templateVersion || 0 }}
            </template>
          </el-table-column>
          <el-table-column label="生成时间" min-width="180">
            <template #default="{ row }">
              {{ formatDateTime(row.createdAt) }}
            </template>
          </el-table-column>
          <el-table-column prop="exportPath" label="导出路径" min-width="280" show-overflow-tooltip />
        </el-table>
      </el-card>

      <el-card class="panel-card" shadow="never">
        <template #header>
          <div class="section-title">
            <h3>站内通知</h3>
          </div>
        </template>
        <el-timeline>
          <el-timeline-item v-for="item in notifications" :key="item.id" :timestamp="formatDateTime(item.createdAt)">
            <strong>{{ item.title }}</strong>
            <div class="notify-content">{{ item.content }}</div>
          </el-timeline-item>
        </el-timeline>
      </el-card>
    </div>

    <el-dialog v-model="createDialogVisible" :title="editingScheduleId ? '编辑报表调度' : '新建报表调度'" width="520px">
      <el-form label-width="88px">
        <el-form-item label="报表模板">
          <el-select v-model="scheduleForm.templateId" style="width: 100%">
            <el-option v-for="item in templates" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-alert
          :title="`当前调度会复用模板的布局、组件绑定、筛选与运行时参数：${templateSummary(scheduleForm.templateId)}`"
          type="info"
          :closable="false"
          style="margin-bottom: 16px"
        />
        <el-form-item label="Cron">
          <el-input v-model="scheduleForm.cronExpression" placeholder="例如 0 0 8 * * ?" />
        </el-form-item>
        <el-form-item label="通道">
          <el-select v-model="scheduleForm.channel" style="width: 100%">
            <el-option label="站内消息" value="IN_APP" />
            <el-option label="邮件预留" value="EMAIL" />
          </el-select>
        </el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="scheduleForm.enabled" />
        </el-form-item>
        <el-form-item label="运行参数">
          <div style="width: 100%">
            <el-empty
              v-if="!currentTemplateParameters.length && !currentTemplateInteractions.length"
              description="当前模板未声明运行参数或联动规则"
              :image-size="56"
            />
            <div v-else class="runtime-param-list">
              <div
                v-for="parameter in scheduleForm.runtimeParams.parameters"
                :key="parameter.parameterCode"
                class="runtime-param-row"
              >
                <div class="runtime-param-meta">
                  <strong>{{ currentTemplateParameters.find((item) => item.code === parameter.parameterCode)?.label || parameter.parameterCode }}</strong>
                  <div class="notify-content">
                    {{
                      `${currentTemplateParameters.find((item) => item.code === parameter.parameterCode)?.field || '-'} / ${
                        currentTemplateParameters.find((item) => item.code === parameter.parameterCode)?.operator || 'eq'
                      }`
                    }}
                  </div>
                </div>
                <el-input
                  v-model="parameter.value"
                  :disabled="currentTemplateParameters.find((item) => item.code === parameter.parameterCode)?.operator === 'not_empty'"
                  placeholder="输入固定运行参数值"
                />
              </div>
              <div
                v-for="interaction in scheduleForm.runtimeParams.interactions"
                :key="interaction.interactionId"
                class="runtime-param-row"
              >
                <div class="runtime-param-meta">
                  <strong>{{ currentTemplateInteractions.find((item) => item.id === interaction.interactionId)?.name || interaction.interactionId }}</strong>
                  <div class="notify-content">
                    {{ interaction.sourceField }} -> {{ interaction.targetField }}
                  </div>
                </div>
                <el-input v-model="interaction.value" placeholder="输入固定联动值，例如 女 / 门诊 / 心内科" />
              </div>
            </div>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="formLoading" @click="handleCreateSchedule">
          {{ editingScheduleId ? '更新' : '保存' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.jobs-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
  margin-top: 16px;
}

.notify-content {
  margin-top: 4px;
  color: #475569;
}

.runtime-param-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.runtime-param-row {
  display: grid;
  grid-template-columns: minmax(0, 180px) minmax(0, 1fr);
  gap: 12px;
  align-items: center;
}

.runtime-param-meta {
  color: #1f2937;
}

@media (max-width: 1200px) {
  .jobs-grid {
    grid-template-columns: 1fr;
  }

  .runtime-param-row {
    grid-template-columns: 1fr;
  }
}
</style>
