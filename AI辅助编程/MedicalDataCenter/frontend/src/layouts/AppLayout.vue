<script setup lang="ts">
import { computed, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useAuthStore } from '@/stores/auth';
import { usePermissionStore } from '@/stores/permission';

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();
const permissionStore = usePermissionStore();
const isCollapse = ref(false);

const breadcrumb = computed(() => route.meta.title ?? '工作台');

function handleLogout() {
  authStore.logout();
  router.push('/login');
}
</script>

<template>
  <el-container class="app-layout">
    <el-aside :width="isCollapse ? '80px' : '240px'" class="layout-aside">
      <div class="brand-card">
        <div class="brand-logo">MDC</div>
        <div v-if="!isCollapse" class="brand-text">
          <strong>医疗数据中心</strong>
          <span>Medical Data Center</span>
        </div>
      </div>

      <el-menu
        :collapse="isCollapse"
        :collapse-transition="false"
        :default-active="route.path"
        class="layout-menu"
        router
      >
        <el-menu-item v-for="menu in permissionStore.visibleMenus" :key="menu.path" :index="menu.path">
          <el-icon><component :is="menu.icon" /></el-icon>
          <span>{{ menu.title }}</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="layout-header">
        <div class="header-left">
          <el-button text @click="isCollapse = !isCollapse">
            <el-icon><Fold v-if="!isCollapse" /><Expand v-else /></el-icon>
          </el-button>
          <div>
            <div class="header-title">{{ breadcrumb }}</div>
            <div class="header-subtitle">{{ authStore.user?.orgName }}</div>
          </div>
        </div>

        <div class="header-right">
          <el-tag type="success">MVP 骨架</el-tag>
          <el-dropdown>
            <span class="user-dropdown">
              {{ authStore.user?.displayName ?? '未登录' }}
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item>个人中心</el-dropdown-item>
                <el-dropdown-item @click="handleLogout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <el-main class="layout-main">
        <RouterView />
      </el-main>
    </el-container>
  </el-container>
</template>

<style scoped>
.app-layout {
  min-height: 100vh;
}

.layout-aside {
  padding: 16px 12px;
  background: linear-gradient(180deg, #0f172a 0%, #172554 100%);
  color: #fff;
  transition: width 0.2s ease;
}

.brand-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  margin-bottom: 16px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.08);
}

.brand-logo {
  display: grid;
  place-items: center;
  width: 44px;
  height: 44px;
  border-radius: 14px;
  background: linear-gradient(135deg, #38bdf8, #2563eb);
  color: #fff;
  font-weight: 700;
  letter-spacing: 1px;
}

.brand-text {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.brand-text span {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.7);
}

.layout-menu {
  border: none;
  background: transparent;
}

:deep(.el-menu-item) {
  margin-bottom: 8px;
  border-radius: 12px;
  color: rgba(255, 255, 255, 0.86);
}

:deep(.el-menu-item.is-active) {
  background: rgba(59, 130, 246, 0.22);
  color: #fff;
}

:deep(.el-menu-item:hover) {
  background: rgba(255, 255, 255, 0.08);
}

.layout-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  background: rgba(255, 255, 255, 0.76);
  backdrop-filter: blur(16px);
  border-bottom: 1px solid rgba(15, 23, 42, 0.06);
}

.layout-main {
  padding: 20px 24px 28px;
}

.header-left,
.header-right,
.user-dropdown {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-title {
  font-size: 20px;
  font-weight: 600;
}

.header-subtitle {
  font-size: 12px;
  color: #64748b;
}

.user-dropdown {
  cursor: pointer;
}
</style>
