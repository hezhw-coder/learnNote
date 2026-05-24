<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { fetchDatasets } from '@/api/modules/datasets';
import PageHeaderCard from '@/components/common/PageHeaderCard.vue';

interface DatasetRecord {
  id: string;
  name: string;
  category: string;
  owner: string;
  updateTime: string;
  description: string;
  fields: Array<{ source: string; target: string; type: string; lineage: string }>;
  samples: Array<Record<string, string>>;
}

const datasets = ref<DatasetRecord[]>([]);
const activeId = ref('');

const activeDataset = computed(() => datasets.value.find((item) => item.id === activeId.value) ?? datasets.value[0]);

async function loadData() {
  datasets.value = await fetchDatasets();
  activeId.value = datasets.value[0]?.id ?? '';
}

function handleSelect(id: string) {
  activeId.value = id;
}

onMounted(loadData);
</script>

<template>
  <div class="page-shell">
    <PageHeaderCard
      title="数据集管理"
      tag="标准层"
      description="查看标准化数据集目录、字段血缘、样本数据预览，为报表设计与开放接口提供统一口径。"
    >
      <template #actions>
        <el-button>刷新元数据</el-button>
        <el-button type="primary">申请发布数据集</el-button>
      </template>
    </PageHeaderCard>

    <div class="page-grid datasets-layout">
      <el-card class="panel-card" shadow="never">
        <template #header>
          <div class="section-title">
            <h3>数据集目录</h3>
          </div>
        </template>
        <el-menu :default-active="activeId" @select="handleSelect">
          <el-menu-item v-for="item in datasets" :key="item.id" :index="item.id">
            <div class="dataset-menu">
              <strong>{{ item.name }}</strong>
              <span>{{ item.category }}</span>
            </div>
          </el-menu-item>
        </el-menu>
      </el-card>

      <div class="page-shell">
        <el-card class="panel-card" shadow="never">
          <template #header>
            <div class="section-title">
              <h3>{{ activeDataset?.name ?? '未选择数据集' }}</h3>
              <el-tag type="success">{{ activeDataset?.owner }}</el-tag>
            </div>
          </template>
          <p class="muted-text">{{ activeDataset?.description }}</p>
          <el-descriptions :column="3" border>
            <el-descriptions-item label="所属域">{{ activeDataset?.category }}</el-descriptions-item>
            <el-descriptions-item label="维护团队">{{ activeDataset?.owner }}</el-descriptions-item>
            <el-descriptions-item label="更新时间">{{ activeDataset?.updateTime }}</el-descriptions-item>
          </el-descriptions>
        </el-card>

        <el-card class="panel-card" shadow="never">
          <template #header>
            <div class="section-title">
              <h3>字段血缘</h3>
            </div>
          </template>
          <el-table :data="activeDataset?.fields ?? []">
            <el-table-column prop="source" label="源字段" min-width="160" />
            <el-table-column prop="target" label="标准字段" min-width="160" />
            <el-table-column prop="type" label="类型" width="140" />
            <el-table-column prop="lineage" label="血缘来源" min-width="200" />
          </el-table>
        </el-card>

        <el-card class="panel-card" shadow="never">
          <template #header>
            <div class="section-title">
              <h3>样本数据预览</h3>
              <el-tag effect="plain">已脱敏</el-tag>
            </div>
          </template>
          <el-table :data="activeDataset?.samples ?? []">
            <el-table-column
              v-for="(value, key) in activeDataset?.samples?.[0] ?? {}"
              :key="key"
              :label="String(key)"
              :prop="String(key)"
              min-width="140"
            />
          </el-table>
        </el-card>
      </div>
    </div>
  </div>
</template>

<style scoped>
.datasets-layout {
  grid-template-columns: 280px minmax(0, 1fr);
}

.dataset-menu {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.dataset-menu span {
  font-size: 12px;
  color: #64748b;
}

@media (max-width: 1200px) {
  .datasets-layout {
    grid-template-columns: 1fr;
  }
}
</style>
