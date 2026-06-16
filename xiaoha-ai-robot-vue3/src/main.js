import { createApp } from 'vue'
import './assets/main.css'
import App from './App.vue'
// 导入路由
import router from './router'
// 注册 SVG Icon
import 'virtual:svg-icons-register'
// 引入全局状态管理 Pinia
import { createPinia } from 'pinia'
import piniaPluginPersistedstate from 'pinia-plugin-persistedstate'

const pinia = createPinia()
pinia.use(piniaPluginPersistedstate)

const app = createApp(App)

// 应用路由和 Pinia（必须在 mount 之前）
app.use(pinia)
app.use(router)

app.mount('#app')
