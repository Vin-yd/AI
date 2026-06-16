import { createRouter, createWebHashHistory } from 'vue-router'
import { useUserStore } from '@/stores/userStore'

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

// 全局前置守卫
router.beforeEach(async (to, from, next) => {
    const token = localStorage.getItem('token')

    // 需要登录但无 token → 跳登录页
    if (to.meta.requiresAuth && !token) {
        next({ name: 'LoginPage' })
        return
    }

    // 已有 token 但用户信息为空 → 刷新页面后补全用户信息
    if (token) {
        const userStore = useUserStore()
        if (!userStore.userInfo) {
            try {
                await userStore.fetchUserInfo()
            } catch {
                // token 过期，清掉
                userStore.token = ''
                localStorage.removeItem('token')
                if (to.meta.requiresAuth) {
                    next({ name: 'LoginPage' })
                    return
                }
            }
        }
    }

    // 已登录用户访问登录页 → 重定向到首页
    if (to.name === 'LoginPage' && token) {
        next({ name: 'Index' })
        return
    }

    next()
})

// 动态设置页面标题
router.afterEach((to) => {
    if (to.meta.title) {
        document.title = to.meta.title
    }
})

export default router
