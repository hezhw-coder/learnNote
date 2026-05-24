import { createApp } from 'vue';
import ElementPlus from 'element-plus';
import * as ElementPlusIconsVue from '@element-plus/icons-vue';
import 'element-plus/dist/index.css';
import App from './App.vue';
import router from './router';
import { pinia } from './stores';
import { useAuthStore } from './stores/auth';
import './style.css';

const app = createApp(App);

Object.entries(ElementPlusIconsVue).forEach(([key, component]) => {
  app.component(key, component);
});

app.use(pinia);
app.use(router);
app.use(ElementPlus);

const authStore = useAuthStore();
if (window.localStorage.getItem('mdc-token') && !authStore.user) {
  authStore.fetchCurrentUser().catch(() => {
    authStore.logout();
  });
}

app.mount('#app');
