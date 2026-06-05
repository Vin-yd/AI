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

const app = createApp(App)

// 应用路由
app.use(router)

app.mount('#app')
// 应用 Pinia
app.use(pinia)
pinia.use(piniaPluginPersistedstate)
