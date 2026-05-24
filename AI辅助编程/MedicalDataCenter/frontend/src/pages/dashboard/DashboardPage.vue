<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { fetchDashboard } from '@/api/modules/dashboard';
import type { DashboardPayload } from '@/types/dashboard';
import PageHeaderCard from '@/components/common/PageHeaderCard.vue';
import StatCard from '@/components/common/StatCard.vue';
import TrendChart from '@/components/charts/TrendChart.vue';

const loading = ref(false);
const payload = ref<DashboardPayload | null>(null);

async function loadData() {
  loading.value = true;
  try {
    payload.value = await fetchDashboard();
  } finally {
    loading.value = false;
  }
}

onMounted(loadData);
</script>

<template>
  <div class="page-shell">
    <PageHeaderCard
      title="工作台"
      tag="总览"
      description="统一查看数据源健康度、抽取任务运行、报表交付与开放 API 调用趋势。"
    >
      <template #actions>
        <el-button @click="loadData">刷新数据</el-button>
        <el-button type="primary">进入今日巡检</el-button>
      </template>
    </PageHeaderCard>

    <el-skeleton :loading="loading" animated :rows="8">
      <template #default>
        <div class="page-grid columns-4 dashboard-stats">
          <StatCard label="数据源总数" :value="payload?.summary.dataSourceTotal ?? 0" icon="Coin" trend="含 1 个异常源" />
          <StatCard label="运行中任务" :value="payload?.summary.runningJobs ?? 0" icon="Connection" color="#f59e0b" />
          <StatCard label="今日报表运行" :value="payload?.summary.reportRuns ?? 0" icon="Histogram" color="#10b981" />
          <StatCard label="累计 API 调用" :value="payload?.summary.apiCalls ?? 0" icon="Link" color="#8b5cf6" />
        </div>

        <div class="page-grid columns-2">
          <TrendChart title="ETL 任务执行趋势" :points="payload?.etlTrend ?? []" />
          <TrendChart title="开放 API 调用分布" type="bar" :points="payload?.apiTrend ?? []" color="#8b5cf6" />
        </div>

        <div class="page-grid columns-2">
          <el-card class="panel-card" shadow="never">
            <template #header>
              <div class="section-title">
                <h3>运行告警</h3>
                <el-tag type="warning">{{ payload?.alerts.length ?? 0 }} 条</el-tag>
              </div>
            </template>
            <el-timeline>
              <el-timeline-item
                v-for="(alert, index) in payload?.alerts ?? []"
                :key="index"
                type="warning"
                :timestamp="`优先级 P${index + 1}`"
              >
                {{ alert }}
              </el-timeline-item>
            </el-timeline>
          </el-card>

          <el-card class="panel-card" shadow="never">
            <template #header>
              <div class="section-title">
                <h3>待办事项</h3>
                <el-button text>查看全部</el-button>
              </div>
            </template>
            <el-empty v-if="!(payload?.todoItems.length)" description="暂无待办" :image-size="88" />
            <el-steps v-else direction="vertical" :active="payload?.todoItems.length">
              <el-step
                v-for="(todo, index) in payload?.todoItems ?? []"
                :key="index"
                :title="todo"
                description="待联调/待确认"
              />
            </el-steps>
          </el-card>
        </div>
      </template>
    </el-skeleton>
  </div>
</template>

<style scoped>
.dashboard-stats {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

@media (max-width: 1400px) {
  .dashboard-stats {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .dashboard-stats {
    grid-template-columns: 1fr;
  }
}
</style>
