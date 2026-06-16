<template>
  <a-drawer
    title="个人中心"
    placement="left"
    :width="360"
    :open="open"
    @close="$emit('close')"
    :closable="true"
  >
    <div class="flex flex-col items-center pt-4">
      <!-- 头像 -->
      <div class="w-20 h-20 rounded-full bg-gradient-to-br from-blue-400 to-purple-500 flex items-center justify-center text-white text-3xl font-bold mb-3 shadow-lg">
        {{ avatarChar }}
      </div>

      <!-- 角色标签 -->
      <a-tag :color="userStore.isAdmin ? 'purple' : 'blue'" class="mb-6">
        {{ userStore.isAdmin ? '管理员' : '普通用户' }}
      </a-tag>

      <!-- 用户信息 -->
      <div class="w-full space-y-4 px-2">
        <!-- 昵称 -->
        <div class="bg-gray-50 rounded-xl p-4">
          <div class="text-xs text-gray-400 mb-2">昵称</div>
          <template v-if="editingNickname">
            <div class="flex items-center gap-2">
              <a-input
                v-model:value="nicknameValue"
                size="middle"
                style="flex: 1"
                @pressEnter="saveNickname"
                ref="nicknameInput"
                :maxlength="50"
              />
              <a-button size="small" type="primary" @click="saveNickname">保存</a-button>
            </div>
          </template>
          <template v-else>
            <div class="flex items-center justify-between">
              <span class="text-base text-gray-800 font-medium">
                {{ userStore.userInfo?.nickname || '未设置' }}
              </span>
              <span class="text-blue-500 text-sm cursor-pointer hover:text-blue-600 transition-colors" @click="startEditNickname">
                编辑
              </span>
            </div>
          </template>
        </div>

        <!-- 手机号 -->
        <div class="bg-gray-50 rounded-xl p-4">
          <div class="text-xs text-gray-400 mb-2">手机号</div>
          <div class="text-base text-gray-800 font-medium">{{ userStore.userInfo?.phone }}</div>
        </div>

        <!-- 注册时间 -->
        <div class="bg-gray-50 rounded-xl p-4">
          <div class="text-xs text-gray-400 mb-2">注册时间</div>
          <div class="text-base text-gray-800 font-medium">{{ userStore.userInfo?.createTime || '-' }}</div>
        </div>
      </div>

      <!-- 退出登录 -->
      <div class="w-full px-2 mt-8">
        <a-button
          danger
          type="primary"
          block
          size="large"
          @click="handleLogout"
        >
          退出登录
        </a-button>
      </div>
    </div>
  </a-drawer>
</template>

<script setup>
import { ref, computed, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { useUserStore } from '@/stores/userStore'

defineProps({
  open: { type: Boolean, required: true },
})
const emit = defineEmits(['close'])

const router = useRouter()
const userStore = useUserStore()

const avatarChar = computed(() =>
  (userStore.userInfo?.nickname || userStore.userInfo?.phone || 'U')[0].toUpperCase()
)

const editingNickname = ref(false)
const nicknameValue = ref('')
const nicknameInput = ref(null)

function startEditNickname() {
  nicknameValue.value = userStore.userInfo?.nickname || ''
  editingNickname.value = true
  nextTick(() => nicknameInput.value?.focus())
}

async function saveNickname() {
  if (!editingNickname.value) return
  editingNickname.value = false
  const val = nicknameValue.value.trim()
  if (!val || val === userStore.userInfo?.nickname) return
  try {
    await userStore.updateNickname(val)
    message.success('昵称已更新')
  } catch {
    message.error('更新失败')
  }
}

async function handleLogout() {
  await userStore.logout()
  emit('close')
  message.success('已退出登录')
  router.push({ name: 'LoginPage' })
}
</script>
