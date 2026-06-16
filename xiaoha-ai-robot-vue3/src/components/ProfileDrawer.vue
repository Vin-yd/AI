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
      <div class="w-20 h-20 rounded-full bg-gradient-to-br from-blue-400 to-purple-500 flex items-center justify-center text-white text-3xl font-bold mb-4 shadow-lg">
        {{ avatarChar }}
      </div>

      <!-- 角色标签 -->
      <a-tag :color="userStore.isAdmin ? 'purple' : 'blue'" class="mb-4">
        {{ userStore.isAdmin ? '管理员' : '普通用户' }}
      </a-tag>

      <!-- 信息表格 -->
      <a-descriptions :column="1" bordered size="middle" class="w-full mb-6">
        <a-descriptions-item label="用户 ID">
          {{ userStore.userInfo?.id }}
        </a-descriptions-item>
        <a-descriptions-item label="手机号">
          {{ userStore.userInfo?.phone }}
        </a-descriptions-item>
        <a-descriptions-item label="昵称">
          <template v-if="editingNickname">
            <a-input
              v-model:value="nicknameValue"
              size="small"
              style="width: 120px"
              @pressEnter="saveNickname"
              @blur="saveNickname"
              ref="nicknameInput"
            />
          </template>
          <template v-else>
            <span class="cursor-pointer hover:text-blue-500 transition-colors" @click="startEditNickname">
              {{ userStore.userInfo?.nickname || '未设置' }}
              <span class="text-gray-300 text-xs ml-1">✎</span>
            </span>
          </template>
        </a-descriptions-item>
        <a-descriptions-item label="注册时间">
          {{ userStore.userInfo?.createTime || '-' }}
        </a-descriptions-item>
      </a-descriptions>

      <!-- 退出登录 -->
      <a-button
        danger
        type="primary"
        block
        size="large"
        @click="handleLogout"
      >
        <template #icon>
          <span style="font-size: 16px;">⏻</span>
        </template>
        退出登录
      </a-button>
    </div>
  </a-drawer>
</template>

<script setup>
import { ref, computed, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { useUserStore } from '@/stores/userStore'

const props = defineProps({
  open: { type: Boolean, required: true },
})
const emit = defineEmits(['close'])

const router = useRouter()
const userStore = useUserStore()

const avatarChar = computed(() =>
  (userStore.userInfo?.nickname || userStore.userInfo?.phone || 'U')[0].toUpperCase()
)

// 编辑昵称
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
