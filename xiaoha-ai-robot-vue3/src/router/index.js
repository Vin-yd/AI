import { createRouter, createWebHashHistory } from 'vue-router'

const routes = [
    {
        path: '/login',
        name: 'LoginPage',
        component: () => import('@/views/LoginPage.vue'),
        meta: { title: '登录 - Vin-AI 机器人' }
    },
    {
        path: '/',
        name: 'Index',
        component: () => import('@/views/Index.vue'),
        meta: { title: '小哈 AI 机器人首页', requiresAuth: true }
    },
    {
        path: '/chat/:chatId',
        name: 'ChatPage',
        component: () => import('@/views/ChatPage.vue'),
        meta: { title: '对话聊天页', requiresAuth: true }
    },
    {
        path: '/customer-service/chat',
        name: 'CustomerServiceChatPage',
        component: () => import('@/views/CustomerServiceChatPage.vue'),
        meta: { title: '智能客服聊天页', requiresAuth: true }
    }
]

const router = createRouter({
    history: createWebHashHistory(),
    routes,
})

// 全局前置守卫：未登录跳转登录页
router.beforeEach((to, from, next) => {
    const token = localStorage.getItem('token')
    if (to.meta.requiresAuth && !token) {
        next({ name: 'LoginPage' })
    } else if (to.name === 'LoginPage' && token) {
        // 已登录用户访问登录页，重定向到首页
        next({ name: 'Index' })
    } else {
        next()
    }
})

// 动态设置页面标题
router.afterEach((to) => {
    if (to.meta.title) {
        document.title = to.meta.title
    }
})

export default router
