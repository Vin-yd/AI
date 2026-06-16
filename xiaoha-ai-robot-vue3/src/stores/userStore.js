import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login as loginApi, logout as logoutApi, getCurrentUser, updateProfile } from '@/api/auth'

export const useUserStore = defineStore('user', () => {
    // 状态
    const token = ref(localStorage.getItem('token') || '')
    const userInfo = ref(null)
    const isLoggedIn = computed(() => !!token.value)
    const isAdmin = computed(() => userInfo.value?.role === 'admin')

    // 登录
    async function login(phone, code) {
        const res = await loginApi(phone, code)
        if (res.data.success) {
            const data = res.data.data
            token.value = data.token
            userInfo.value = data.userInfo
            localStorage.setItem('token', data.token)
            return data
        } else {
            throw new Error(res.data.message || '登录失败')
        }
    }

    // 退出
    async function logout() {
        try {
            await logoutApi()
        } finally {
            token.value = ''
            userInfo.value = null
            localStorage.removeItem('token')
        }
    }

    // 获取当前用户信息
    async function fetchUserInfo() {
        const res = await getCurrentUser()
        if (res.data.success) {
            userInfo.value = res.data.data
        }
        return userInfo.value
    }

    // 更新昵称
    async function updateNickname(nickname) {
        const res = await updateProfile(nickname)
        if (res.data.success && userInfo.value) {
            userInfo.value.nickname = nickname
        }
        return res.data
    }

    return {
        token,
        userInfo,
        isLoggedIn,
        isAdmin,
        login,
        logout,
        fetchUserInfo,
        updateNickname,
    }
})
