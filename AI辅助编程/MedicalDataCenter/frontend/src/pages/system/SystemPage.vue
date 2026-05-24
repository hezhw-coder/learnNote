<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { fetchSystemPayload } from '@/api/modules/system';
import PageHeaderCard from '@/components/common/PageHeaderCard.vue';

interface SystemPayload {
  users: Array<Record<string, string>>;
  roles: Array<Record<string, string | number>>;
  permissions: Array<Record<string, string>>;
  dictionaries: Array<Record<string, string | boolean>>;
  auditLogs: Array<Record<string, string>>;
  parameters: Array<Record<string, string>>;
}

const payload = ref<SystemPayload>({
  users: [],
  roles: [],
  permissions: [],
  dictionaries: [],
  auditLogs: [],
  parameters: [],
});

async function loadData() {
  payload.value = await fetchSystemPayload();
}

onMounted(loadData);
</script>

<template>
  <div class="page-shell">
    <PageHeaderCard
      title="系统管理"
      tag="RBAC"
      description="覆盖用户、角色、权限、字典项、审计日志与系统参数，为平台治理和合规审计提供入口。"
    >
      <template #actions>
        <el-button>查看审计概览</el-button>
        <el-button type="primary">新增用户</el-button>
      </template>
    </PageHeaderCard>

    <el-tabs>
      <el-tab-pane label="用户">
        <el-card class="panel-card" shadow="never">
          <el-table :data="payload.users">
            <el-table-column prop="username" label="账号" min-width="140" />
            <el-table-column prop="displayName" label="姓名" min-width="140" />
            <el-table-column prop="roles" label="角色" min-width="160" />
            <el-table-column prop="status" label="状态" width="100" />
          </el-table>
        </el-card>
      </el-tab-pane>

      <el-tab-pane label="角色">
        <el-card class="panel-card" shadow="never">
          <el-table :data="payload.roles">
            <el-table-column prop="name" label="角色名称" min-width="140" />
            <el-table-column prop="permissionCount" label="权限数" width="100" />
            <el-table-column prop="updateTime" label="更新时间" width="160" />
          </el-table>
        </el-card>
      </el-tab-pane>

      <el-tab-pane label="权限">
        <el-card class="panel-card" shadow="never">
          <el-table :data="payload.permissions">
            <el-table-column prop="module" label="模块" min-width="140" />
            <el-table-column prop="code" label="权限编码" min-width="180" />
            <el-table-column prop="description" label="说明" min-width="260" />
          </el-table>
        </el-card>
      </el-tab-pane>

      <el-tab-pane label="字典">
        <el-card class="panel-card" shadow="never">
          <el-table :data="payload.dictionaries">
            <el-table-column prop="type" label="字典类型" min-width="160" />
            <el-table-column prop="code" label="编码" min-width="140" />
            <el-table-column prop="label" label="标签" min-width="140" />
            <el-table-column prop="enabled" label="启用" width="100" />
          </el-table>
        </el-card>
      </el-tab-pane>

      <el-tab-pane label="审计日志">
        <el-card class="panel-card" shadow="never">
          <el-table :data="payload.auditLogs">
            <el-table-column prop="action" label="操作" min-width="160" />
            <el-table-column prop="operator" label="操作人" width="120" />
            <el-table-column prop="result" label="结果" width="100" />
            <el-table-column prop="timestamp" label="时间" width="180" />
          </el-table>
        </el-card>
      </el-tab-pane>

      <el-tab-pane label="系统参数">
        <el-card class="panel-card" shadow="never">
          <el-table :data="payload.parameters">
            <el-table-column prop="key" label="参数键" min-width="220" />
            <el-table-column prop="value" label="参数值" min-width="120" />
            <el-table-column prop="description" label="说明" min-width="260" />
          </el-table>
        </el-card>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>
