<script setup lang="ts">
import { reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { useAuthStore } from '@/stores/auth';

const router = useRouter();
const route = useRoute();
const authStore = useAuthStore();
const loading = ref(false);

const form = reactive({
  username: 'admin',
  password: 'Admin@123',
});

async function handleLogin() {
  loading.value = true;
  try {
    await authStore.login(form);
    ElMessage.success('登录成功');
    router.push((route.query.redirect as string) || '/dashboard');
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <div class="login-page">
    <div class="login-hero">
      <div class="hero-badge">MVP 一期</div>
      <h1>医疗数据中心</h1>
      <p>
        面向数据源管理、ETL 集成、标准化数据集、报表设计与开放 API
        的一体化平台前端骨架。
      </p>
      <ul class="hero-points">
        <li>支持统一认证与 RBAC 权限扩展</li>
        <li>预置报表设计 JSON Schema 模型</li>
        <li>覆盖多数据源接入、抽取与开放接口管理</li>
      </ul>
    </div>

    <el-card class="login-card panel-card" shadow="never">
      <div class="card-header">
        <h2>欢迎登录</h2>
        <span>已接入真实认证接口与会话续期</span>
      </div>

      <el-form label-position="top" @submit.prevent="handleLogin">
        <el-form-item label="账号">
          <el-input v-model="form.username" placeholder="请输入账号" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" show-password placeholder="请输入密码" />
        </el-form-item>
        <el-button type="primary" size="large" class="login-button" :loading="loading" @click="handleLogin">
          登录系统
        </el-button>
      </el-form>

      <div class="login-tips">
        <span>默认演示账号：admin / Admin@123</span>
        <span>登录后会自动续期，会话退出后旧令牌将失效</span>
      </div>
    </el-card>
  </div>
</template>

<style scoped>
.login-page {
  display: grid;
  grid-template-columns: 1.1fr 420px;
  gap: 32px;
  align-items: center;
  min-height: 100vh;
  padding: 32px 5vw;
}

.login-hero {
  padding: 36px;
  color: #0f172a;
}

.hero-badge {
  display: inline-flex;
  padding: 6px 10px;
  border-radius: 999px;
  background: rgba(59, 130, 246, 0.1);
  color: #2563eb;
  font-weight: 600;
}

h1 {
  margin: 18px 0 14px;
  font-size: 52px;
  line-height: 1.1;
}

p {
  max-width: 640px;
  font-size: 18px;
  color: #475569;
}

.hero-points {
  display: grid;
  gap: 14px;
  padding: 0;
  margin: 28px 0 0;
  list-style: none;
}

.hero-points li {
  padding: 14px 16px;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.66);
  border: 1px solid rgba(15, 23, 42, 0.06);
}

.login-card {
  border-radius: 24px;
}

.login-card :deep(.el-card__body) {
  padding: 30px;
}

.card-header {
  margin-bottom: 18px;
}

.card-header h2 {
  margin: 0 0 6px;
}

.card-header span,
.login-tips {
  color: #64748b;
}

.login-button {
  width: 100%;
  margin-top: 8px;
}

.login-tips {
  display: grid;
  gap: 8px;
  margin-top: 18px;
  font-size: 13px;
}

@media (max-width: 1100px) {
  .login-page {
    grid-template-columns: 1fr;
  }
}
</style>
