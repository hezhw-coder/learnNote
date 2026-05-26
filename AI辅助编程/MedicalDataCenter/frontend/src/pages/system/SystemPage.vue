<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import PageHeaderCard from '@/components/common/PageHeaderCard.vue';
import {
  assignSystemRolePermissions,
  assignSystemUserRoles,
  createSystemRole,
  createSystemUser,
  fetchSystemAuditLogs,
  fetchSystemDictionaries,
  fetchSystemParameters,
  fetchSystemPermissions,
  fetchSystemRoles,
  fetchSystemUsers,
  type SystemParameterRecord,
  type SystemPayload,
  type SystemRoleRecord,
  updateSystemParameter,
  updateSystemRole,
  updateSystemUserStatus,
} from '@/api/modules/system';
import { usePermissionStore } from '@/stores/permission';

const permissionStore = usePermissionStore();

const payload = ref<SystemPayload>({
  users: [],
  roles: [],
  permissions: [],
  dictionaries: [],
  auditLogs: [],
  parameters: [],
});

const createDialogVisible = ref(false);
const roleDialogVisible = ref(false);
const createRoleDialogVisible = ref(false);
const editRoleDialogVisible = ref(false);
const permissionDialogVisible = ref(false);
const parameterDialogVisible = ref(false);

const roleFormUserId = ref<number | null>(null);
const editRoleId = ref<number | null>(null);
const permissionRoleId = ref<number | null>(null);
const parameterForm = reactive({
  key: '',
  value: '',
  description: '',
});
const roleForm = reactive({
  roleIds: [] as number[],
});
const createForm = reactive({
  username: '',
  password: '',
  displayName: '',
  roleIds: [] as number[],
});
const createRoleForm = reactive({
  code: '',
  name: '',
  permissionIds: [] as number[],
});
const editRoleForm = reactive({
  code: '',
  name: '',
});
const permissionForm = reactive({
  permissionIds: [] as number[],
});

const canViewUsers = computed(() => permissionStore.hasPermission('SYSTEM_USER_VIEW') || canManageUsers.value);
const canManageUsers = computed(() => permissionStore.hasPermission('SYSTEM_USER_MANAGE'));
const canViewRoles = computed(() => permissionStore.hasPermission('SYSTEM_ROLE_VIEW') || canManageRoles.value);
const canManageRoles = computed(() => permissionStore.hasPermission('SYSTEM_ROLE_MANAGE'));
const canManageParameters = computed(() => permissionStore.hasPermission('SYSTEM_PARAM_MANAGE'));
const canViewParameters = computed(() => canViewRoles.value || canManageParameters.value);
const passwordRuleText = '至少 8 位，且同时包含大写字母、小写字母、数字和特殊字符';

const roleOptions = computed(() =>
  payload.value.roles.map((item) => ({
    label: `${item.name} (${item.code})`,
    value: item.id,
    code: item.code,
  })),
);

const permissionOptions = computed(() =>
  payload.value.permissions.map((item) => ({
    label: `${item.name} (${item.code})`,
    value: item.id,
    code: item.code,
  })),
);

const permissionNameMap = computed(() =>
  Object.fromEntries(payload.value.permissions.map((item) => [item.code, item.name])),
);

async function loadData() {
  const nextPayload: SystemPayload = {
    users: canViewUsers.value ? await fetchSystemUsers() : [],
    roles: canViewRoles.value ? await fetchSystemRoles() : [],
    permissions: canViewRoles.value ? await fetchSystemPermissions() : [],
    dictionaries: canViewRoles.value ? await fetchSystemDictionaries() : [],
    auditLogs: canViewRoles.value ? await fetchSystemAuditLogs() : [],
    parameters: canViewParameters.value ? await fetchSystemParameters() : [],
  };
  payload.value = nextPayload;
}

function formatTime(value?: string) {
  return value?.replace('T', ' ').slice(0, 19) ?? '-';
}

function permissionLabel(code: string) {
  return permissionNameMap.value[code] ?? code;
}

function resetCreateForm() {
  createForm.username = '';
  createForm.password = '';
  createForm.displayName = '';
  createForm.roleIds = payload.value.roles.length ? [payload.value.roles[0].id] : [];
}

function resetCreateRoleForm() {
  createRoleForm.code = '';
  createRoleForm.name = '';
  createRoleForm.permissionIds = [];
}

function openCreateDialog() {
  resetCreateForm();
  createDialogVisible.value = true;
}

function openAssignRoleDialog(user: SystemPayload['users'][number]) {
  roleFormUserId.value = user.id;
  roleForm.roleIds = payload.value.roles.filter((role) => user.roles.includes(role.code)).map((role) => role.id);
  roleDialogVisible.value = true;
}

function openCreateRoleDialog() {
  resetCreateRoleForm();
  createRoleDialogVisible.value = true;
}

function openEditRoleDialog(role: SystemRoleRecord) {
  editRoleId.value = role.id;
  editRoleForm.code = role.code;
  editRoleForm.name = role.name;
  editRoleDialogVisible.value = true;
}

function openPermissionDialog(role: SystemRoleRecord) {
  permissionRoleId.value = role.id;
  permissionForm.permissionIds = payload.value.permissions
    .filter((permission) => role.permissions.includes(permission.code))
    .map((permission) => permission.id);
  permissionDialogVisible.value = true;
}

function openParameterDialog(parameter: SystemParameterRecord) {
  parameterForm.key = parameter.key;
  parameterForm.value = parameter.value;
  parameterForm.description = parameter.description;
  parameterDialogVisible.value = true;
}

async function submitCreateUser() {
  if (!createForm.roleIds.length) {
    ElMessage.warning('请至少选择一个角色');
    return;
  }
  await createSystemUser({
    username: createForm.username.trim(),
    password: createForm.password,
    displayName: createForm.displayName.trim(),
    roleIds: createForm.roleIds,
  });
  ElMessage.success('用户已创建');
  createDialogVisible.value = false;
  await loadData();
}

async function submitAssignRoles() {
  if (!roleFormUserId.value || !roleForm.roleIds.length) {
    ElMessage.warning('请至少选择一个角色');
    return;
  }
  await assignSystemUserRoles(roleFormUserId.value, roleForm.roleIds);
  ElMessage.success('角色分配已更新');
  roleDialogVisible.value = false;
  await loadData();
}

async function submitCreateRole() {
  if (!createRoleForm.permissionIds.length) {
    ElMessage.warning('请至少选择一个权限');
    return;
  }
  await createSystemRole({
    code: createRoleForm.code.trim().toUpperCase(),
    name: createRoleForm.name.trim(),
    permissionIds: createRoleForm.permissionIds,
  });
  ElMessage.success('角色已创建');
  createRoleDialogVisible.value = false;
  await loadData();
}

async function submitEditRole() {
  if (!editRoleId.value) {
    return;
  }
  await updateSystemRole(editRoleId.value, {
    code: editRoleForm.code.trim().toUpperCase(),
    name: editRoleForm.name.trim(),
  });
  ElMessage.success('角色信息已更新');
  editRoleDialogVisible.value = false;
  await loadData();
}

async function submitAssignPermissions() {
  if (!permissionRoleId.value || !permissionForm.permissionIds.length) {
    ElMessage.warning('请至少选择一个权限');
    return;
  }
  await assignSystemRolePermissions(permissionRoleId.value, permissionForm.permissionIds);
  ElMessage.success('角色权限已更新');
  permissionDialogVisible.value = false;
  await loadData();
}

async function submitUpdateParameter() {
  await updateSystemParameter(parameterForm.key, parameterForm.value.trim());
  ElMessage.success('系统参数已更新');
  parameterDialogVisible.value = false;
  await loadData();
}

async function toggleUserStatus(user: SystemPayload['users'][number]) {
  await updateSystemUserStatus(user.id, !user.enabled);
  ElMessage.success(`用户已${user.enabled ? '停用' : '启用'}`);
  await loadData();
}

onMounted(async () => {
  await loadData();
  resetCreateForm();
});
</script>

<template>
  <div class="page-shell">
    <PageHeaderCard
      title="系统管理"
      tag="RBAC"
      description="补齐用户、角色、角色权限分配、关键系统参数维护与审计追踪，形成最小可运营后台闭环。"
    >
      <template #actions>
        <el-button v-if="canManageRoles" @click="openCreateRoleDialog">新增角色</el-button>
        <el-button v-if="canManageUsers" type="primary" @click="openCreateDialog">新增用户</el-button>
      </template>
    </PageHeaderCard>

    <el-tabs>
      <el-tab-pane v-if="canViewUsers" label="用户">
        <el-card class="panel-card" shadow="never">
          <el-table :data="payload.users">
            <el-table-column prop="username" label="账号" min-width="140" />
            <el-table-column prop="displayName" label="姓名" min-width="140" />
            <el-table-column label="角色" min-width="200">
              <template #default="{ row }">
                {{ row.roles.join(', ') || '-' }}
              </template>
            </el-table-column>
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="row.enabled ? 'success' : 'info'">
                  {{ row.enabled ? '启用' : '停用' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="创建时间" min-width="180">
              <template #default="{ row }">
                {{ formatTime(row.createdAt) }}
              </template>
            </el-table-column>
            <el-table-column v-if="canManageUsers" label="操作" width="220" fixed="right">
              <template #default="{ row }">
                <div class="table-actions">
                  <el-button link type="primary" @click="openAssignRoleDialog(row)">分配角色</el-button>
                  <el-button link @click="toggleUserStatus(row)">
                    {{ row.enabled ? '停用' : '启用' }}
                  </el-button>
                </div>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-tab-pane>

      <el-tab-pane v-if="canViewRoles" label="角色">
        <el-card class="panel-card" shadow="never">
          <el-table :data="payload.roles">
            <el-table-column prop="name" label="角色名称" min-width="140" />
            <el-table-column prop="code" label="角色编码" min-width="160" />
            <el-table-column label="已分配权限" min-width="360">
              <template #default="{ row }">
                <div class="permission-tags">
                  <el-tag v-for="permission in row.permissions" :key="permission" size="small" effect="plain">
                    {{ permissionLabel(permission) }}
                  </el-tag>
                  <span v-if="!row.permissions.length">-</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="权限数" width="100">
              <template #default="{ row }">
                {{ row.permissions.length }}
              </template>
            </el-table-column>
            <el-table-column v-if="canManageRoles" label="操作" width="220" fixed="right">
              <template #default="{ row }">
                <div class="table-actions">
                  <el-button link type="primary" :disabled="row.code === 'ADMIN'" @click="openEditRoleDialog(row)">
                    编辑角色
                  </el-button>
                  <el-button link :disabled="row.code === 'ADMIN'" @click="openPermissionDialog(row)">
                    分配权限
                  </el-button>
                </div>
              </template>
            </el-table-column>
          </el-table>
          <div class="panel-tip">内置 `ADMIN` 角色默认保留为系统兜底角色，不支持直接修改。</div>
        </el-card>
      </el-tab-pane>

      <el-tab-pane v-if="canViewRoles" label="权限">
        <el-card class="panel-card" shadow="never">
          <el-table :data="payload.permissions">
            <el-table-column prop="module" label="模块" min-width="140" />
            <el-table-column prop="code" label="权限编码" min-width="180" />
            <el-table-column prop="name" label="说明" min-width="260" />
          </el-table>
        </el-card>
      </el-tab-pane>

      <el-tab-pane v-if="canViewRoles" label="字典">
        <el-card class="panel-card" shadow="never">
          <el-table :data="payload.dictionaries">
            <el-table-column prop="type" label="字典类型" min-width="160" />
            <el-table-column prop="code" label="编码" min-width="140" />
            <el-table-column prop="label" label="标签" min-width="140" />
            <el-table-column prop="enabled" label="启用" width="100" />
          </el-table>
        </el-card>
      </el-tab-pane>

      <el-tab-pane v-if="canViewRoles" label="审计日志">
        <el-card class="panel-card" shadow="never">
          <el-table :data="payload.auditLogs">
            <el-table-column prop="action" label="操作" min-width="180" />
            <el-table-column prop="actor" label="操作人" width="120" />
            <el-table-column prop="targetType" label="目标类型" min-width="120" />
            <el-table-column prop="targetId" label="目标标识" min-width="120" />
            <el-table-column prop="detail" label="明细" min-width="300" />
            <el-table-column label="时间" width="180">
              <template #default="{ row }">
                {{ formatTime(row.createdAt) }}
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-tab-pane>

      <el-tab-pane v-if="canViewParameters" label="系统参数">
        <el-card class="panel-card" shadow="never">
          <el-table :data="payload.parameters">
            <el-table-column prop="key" label="参数键" min-width="240" />
            <el-table-column prop="value" label="参数值" min-width="160" />
            <el-table-column prop="description" label="说明" min-width="220" />
            <el-table-column prop="updatedBy" label="最后修改人" min-width="120" />
            <el-table-column label="最后修改时间" min-width="180">
              <template #default="{ row }">
                {{ formatTime(row.updatedAt) }}
              </template>
            </el-table-column>
            <el-table-column v-if="canManageParameters" label="操作" width="120" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="openParameterDialog(row)">修改</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="createDialogVisible" title="新增用户" width="520px">
      <el-form label-width="88px">
        <el-form-item label="账号">
          <el-input v-model="createForm.username" placeholder="请输入账号" />
        </el-form-item>
        <el-form-item label="姓名">
          <el-input v-model="createForm.displayName" placeholder="请输入姓名" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="createForm.password" type="password" show-password placeholder="请输入初始密码" />
          <div class="form-tip">{{ passwordRuleText }}</div>
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="createForm.roleIds" multiple placeholder="请选择角色" style="width: 100%">
            <el-option v-for="item in roleOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitCreateUser">确认</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="roleDialogVisible" title="分配角色" width="420px">
      <el-form label-width="88px">
        <el-form-item label="角色">
          <el-select v-model="roleForm.roleIds" multiple placeholder="请选择角色" style="width: 100%">
            <el-option v-for="item in roleOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="roleDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitAssignRoles">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="createRoleDialogVisible" title="新增角色" width="560px">
      <el-form label-width="96px">
        <el-form-item label="角色编码">
          <el-input v-model="createRoleForm.code" placeholder="请输入英文编码，如 OPS_MANAGER" />
        </el-form-item>
        <el-form-item label="角色名称">
          <el-input v-model="createRoleForm.name" placeholder="请输入角色名称" />
        </el-form-item>
        <el-form-item label="角色权限">
          <el-select v-model="createRoleForm.permissionIds" multiple placeholder="请选择权限" style="width: 100%">
            <el-option
              v-for="item in permissionOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createRoleDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitCreateRole">确认</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="editRoleDialogVisible" title="编辑角色" width="520px">
      <el-form label-width="96px">
        <el-form-item label="角色编码">
          <el-input v-model="editRoleForm.code" placeholder="请输入英文编码" />
        </el-form-item>
        <el-form-item label="角色名称">
          <el-input v-model="editRoleForm.name" placeholder="请输入角色名称" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editRoleDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitEditRole">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="permissionDialogVisible" title="分配权限" width="560px">
      <el-form label-width="96px">
        <el-form-item label="权限">
          <el-select v-model="permissionForm.permissionIds" multiple placeholder="请选择权限" style="width: 100%">
            <el-option
              v-for="item in permissionOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="permissionDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitAssignPermissions">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="parameterDialogVisible" title="修改系统参数" width="520px">
      <el-form label-width="96px">
        <el-form-item label="参数键">
          <el-input :model-value="parameterForm.key" disabled />
        </el-form-item>
        <el-form-item label="说明">
          <el-input :model-value="parameterForm.description" disabled />
        </el-form-item>
        <el-form-item label="参数值">
          <el-input v-model="parameterForm.value" placeholder="请输入参数值" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="parameterDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitUpdateParameter">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.panel-card {
  margin-bottom: 16px;
}

.table-actions {
  display: flex;
  gap: 8px;
}

.permission-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.panel-tip {
  margin-top: 12px;
  font-size: 12px;
  color: #64748b;
}

.form-tip {
  margin-top: 6px;
  font-size: 12px;
  line-height: 1.4;
  color: #64748b;
}
</style>
