import { computed, ref } from 'vue';
import { defineStore } from 'pinia';
import { fetchCurrentUserApi, loginApi } from '@/api/modules/auth';
import type { LoginForm, UserInfo } from '@/types/auth';

const tokenKey = 'mdc-token';
const userKey = 'mdc-user';

export const useAuthStore = defineStore('auth', () => {
  const token = ref(window.localStorage.getItem(tokenKey) ?? '');
  const user = ref<UserInfo | null>(
    window.localStorage.getItem(userKey) ? (JSON.parse(window.localStorage.getItem(userKey) as string) as UserInfo) : null,
  );

  const isAuthenticated = computed(() => Boolean(token.value));

  async function login(form: LoginForm) {
    const result = await loginApi(form);
    token.value = result.accessToken;
    window.localStorage.setItem(tokenKey, result.accessToken);
    const currentUser = await fetchCurrentUser();
    return {
      ...result,
      user: currentUser,
    };
  }

  async function fetchCurrentUser() {
    const currentUser = await fetchCurrentUserApi();
    const roles = currentUser.authorities
      .filter((item) => item.startsWith('ROLE_'))
      .map((item) => item.replace('ROLE_', ''));
    const permissions = currentUser.authorities.filter((item) => !item.startsWith('ROLE_'));
    user.value = {
      id: currentUser.username,
      username: currentUser.username,
      displayName: currentUser.displayName,
      orgName: '医疗数据中心',
      roles,
      permissions,
    };
    window.localStorage.setItem(userKey, JSON.stringify(user.value));
    return user.value;
  }

  function logout() {
    token.value = '';
    user.value = null;
    window.localStorage.removeItem(tokenKey);
    window.localStorage.removeItem(userKey);
  }

  return {
    token,
    user,
    isAuthenticated,
    login,
    fetchCurrentUser,
    logout,
  };
});
