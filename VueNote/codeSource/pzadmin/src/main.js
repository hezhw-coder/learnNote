import { createApp } from 'vue'
import './style.css'
import App from './App.vue'
import router  from './router'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import store from './store'

const app = createApp(App)
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

app.use(store)

app.use(router).mount('#app')
