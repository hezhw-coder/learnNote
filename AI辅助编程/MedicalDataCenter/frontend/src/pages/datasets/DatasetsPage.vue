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
        <el-scrollbar max-height="520px">
          <el-menu class="dataset-directory-menu" :default-active="activeId" @select="handleSelect">
            <el-menu-item v-for="item in datasets" :key="item.id" :index="item.id">
              <div class="dataset-menu">
                <div class="dataset-menu__head">
                  <strong :title="item.name">{{ item.name }}</strong>
                  <el-tag effect="plain" round size="small">{{ item.category }}</el-tag>
                </div>
                <span class="dataset-menu__desc" :title="item.description">{{ item.description }}</span>
                <span class="dataset-menu__meta">{{ item.fields.length }} 个字段</span>
              </div>
            </el-menu-item>
          </el-menu>
        </el-scrollbar>
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
  gap: 8px;
  width: 100%;
  min-width: 0;
}

.dataset-menu__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.dataset-menu__head strong {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 14px;
  line-height: 1.5;
  color: #0f172a;
}

.dataset-menu__desc,
.dataset-menu__meta {
  font-size: 12px;
  line-height: 1.45;
  color: #64748b;
}

.dataset-menu__desc {
  display: -webkit-box;
  overflow: hidden;
  text-overflow: ellipsis;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.dataset-menu__meta {
  color: #94a3b8;
}

:deep(.dataset-directory-menu) {
  border-right: none;
}

:deep(.dataset-directory-menu .el-menu-item) {
  display: flex;
  align-items: stretch;
  height: auto;
  min-height: 96px;
  margin-bottom: 12px;
  padding: 14px 16px;
  line-height: normal;
  border: 1px solid #e2e8f0;
  border-radius: 18px;
  background: #f8fafc;
  transition: all 0.2s ease;
}

:deep(.dataset-directory-menu .el-menu-item:hover) {
  border-color: #bfdbfe;
  background: #eff6ff;
}

:deep(.dataset-directory-menu .el-menu-item.is-active) {
  border-color: #93c5fd;
  background: linear-gradient(135deg, #dbeafe 0%, #eff6ff 100%);
  box-shadow: 0 12px 24px rgba(37, 99, 235, 0.12);
}

:deep(.dataset-directory-menu .el-menu-item.is-active .dataset-menu__head strong) {
  color: #1d4ed8;
}

:deep(.dataset-directory-menu .el-menu-item.is-active .dataset-menu__desc) {
  color: #334155;
}

:deep(.dataset-directory-menu .el-menu-item.is-active .dataset-menu__meta) {
  color: #475569;
}

@media (max-width: 1200px) {
  .datasets-layout {
    grid-template-columns: 1fr;
  }
}
</style>
