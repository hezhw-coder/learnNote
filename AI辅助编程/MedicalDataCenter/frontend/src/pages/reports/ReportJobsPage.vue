<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { fetchReportJobs } from '@/api/modules/reports';
import type { ReportJobItem } from '@/types/report';
import PageHeaderCard from '@/components/common/PageHeaderCard.vue';
import StatusTag from '@/components/common/StatusTag.vue';

const jobs = ref<ReportJobItem[]>([]);

async function loadData() {
  jobs.value = await fetchReportJobs();
}

onMounted(loadData);
</script>

<template>
  <div class="page-shell">
    <PageHeaderCard
      title="报表任务"
      tag="调度推送"
      description="用于配置定时报表生成、消息推送与执行记录，邮件通道当前仅保留占位能力。"
    >
      <template #actions>
        <el-button>查看消息中心</el-button>
        <el-button type="primary">新建调度</el-button>
      </template>
    </PageHeaderCard>

    <el-card class="panel-card" shadow="never">
      <el-table :data="jobs">
        <el-table-column prop="templateName" label="报表模板" min-width="220" />
        <el-table-column prop="cron" label="Cron" min-width="180" />
        <el-table-column prop="pushChannel" label="推送通道" width="120" />
        <el-table-column prop="latestRun" label="最近执行" width="160" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <StatusTag :status="row.status" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180">
          <template #default>
            <el-button text type="primary">编辑</el-button>
            <el-button text>查看记录</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>
