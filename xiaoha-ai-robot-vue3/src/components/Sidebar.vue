<template>
    <!-- 左边栏 -->
    <div
        :class="sidebarOpen ? 'translate-x-0' : '-translate-x-full'"
        class="w-64 bg-[#f9fbff] border-r border-gray-200 fixed left-0 top-0 h-full transition-transform duration-300 ease-in-out z-10 overflow-y-auto">
        <div class="p-0 h-full flex flex-col">
            <!-- Logo 与应用名称 -->
             <div class="flex items-center justify-center p-4 cursor-pointer" @click="jumpToIndexPage">
              <SvgIcon name="ai-robot-logo" customCss="w-8 h-8 text-gray-700 mr-3" />
              <span class="text-2xl font-bold font-sans tracking-wide text-gray-800">Vin-AI机器人</span>
            </div>

             <!-- 用户区域 -->
            <div v-if="userStore.isLoggedIn" class="px-3 mb-2">
              <div @click="profileDrawerOpen = true"
                class="flex items-center gap-2 px-2 py-2 bg-white rounded-xl border border-gray-100 cursor-pointer hover:bg-gray-50 transition-colors">
                <div class="w-8 h-8 rounded-full bg-gradient-to-br from-blue-400 to-purple-500 flex items-center justify-center text-white text-xs font-bold flex-shrink-0">
                  {{ (userStore.userInfo?.nickname || userStore.userInfo?.phone || 'U')[0].toUpperCase() }}
                </div>
                <div class="flex-1 min-w-0">
                  <div class="text-sm font-medium text-gray-700 truncate">
                    {{ userStore.userInfo?.nickname || '用户' }}
                  </div>
                </div>
                <span class="text-gray-300 text-xs flex-shrink-0">▸</span>
              </div>
            </div>
            <div v-else class="px-3 mb-2">
              <button @click="jumpToLogin"
                class="w-full flex items-center justify-center gap-2 px-4 py-2.5 text-white rounded-xl cursor-pointer transition-all duration-300 hover:shadow-lg hover:-translate-y-0.5"
                style="background: linear-gradient(135deg, #4d6bfe, #8b5cf6);">
                <span class="text-sm font-medium">登 录</span>
              </button>
            </div>

             <!-- 开启新对话按钮 -->
             <button @click="jumpToIndexPage"
              class="mx-auto my-2 px-6 py-2 text-white rounded-xl transition-colors new-chat-btn w-fit cursor-pointer">
              <SvgIcon name="new-chat" customCss="w-6 h-6 mr-1.5 inline text-[#4d6bfe]" />
              开启新对话
            </button>

      <!-- 其他工具入口 -->
      <ul class="px-3 text-gray-600 mb-2">
        <li class="flex items-center py-1 px-2 hover:bg-gray-100 rounded-lg cursor-pointer" @click="jumpToCustomerServiceChatPage">
          <SvgIcon name="customer-service" customCss="w-5 h-5 mr-2 inline mb-0" />
          <span>智能客服</span>
        </li>
        <!-- 管理员：文件管理入口 -->
        <li v-if="userStore.isAdmin"
          class="flex items-center py-1 px-2 hover:bg-gray-100 rounded-lg cursor-pointer"
          @click="jumpToCustomerServiceChatPage">
          <span class="w-5 h-5 mr-2 inline-flex items-center justify-center text-xs">📁</span>
          <span>知识库管理</span>
        </li>
      </ul>

      <!-- 分割线 -->
      <div class="h-1 border-b border-gray-200 mx-4"></div>

               <!-- 历史对话区域 -->
            <div class="my-4 px-2 overflow-y-auto overflow-x-hidden flex-1" ref="historyChatContainerRef">
              <div class="space-y-1">
                <div class="text-xs px-3 py-1 text-gray-500 sticky top-0 bg-[#f9fbff] z-10">历史对话</div>
                 <div v-for="(historyChat, index) in historyChats" :key="index"
                  :class="['relative px-3 py-1 rounded-xl cursor-pointer transition-colors flex items-center justify-between',
                           selectedChatId === historyChat.uuid ? 'bg-[#e4edfd] text-[#4d6bfe]' : 'hover:bg-[rgb(239,246,255)]']"
                  @click="jumpToChatPage(historyChat.uuid)"
                  @mouseenter="showButton = historyChat.uuid" @mouseleave="showButton = null">

                    <a-tooltip placement="top">
                            <template #title>
                              <span>{{ historyChat.summary }}</span>
                            </template>

                            <p class="text-[14px] overflow-hidden whitespace-nowrap">{{ historyChat.summary }}</p>
                    </a-tooltip>
                    <a-dropdown>
                  <template #overlay>
                        <a-menu @click="handleMenuClick(historyChat.uuid, historyChat.id, historyChat.summary, $event)">
                  <a-menu-item key="rename">
                    <EditOutlined />
                    重命名
                  </a-menu-item>
                  <a-menu-item key="delete" danger>
                    <DeleteOutlined />
                    删除
                  </a-menu-item>
                </a-menu>
                  </template>
                        <button
                            class="z-10 rounded-lg outline-none justify-center items-center bg-white
                            w-6 h-6 flex absolute right-2 top-1/2 transform -translate-y-1/2 transition-all duration-300 hover:bg-gray-50"
                            :style="{ opacity: showButton === historyChat.uuid ? 1 : 0 }">
                            <EllipsisOutlined class="w-4 h-4 text-gray-500" />
                        </button>
                    </a-dropdown>
                </div>
            </div>
            </div>
        </div>
    </div>

        <!-- 侧边栏切换按钮 -->
        <a-tooltip placement="bottom">
        <template #title>
          <span>{{ sidebarOpen ? '收缩边栏' : '打开边栏'}}</span>
        </template>

        <button
          :class="sidebarOpen ? 'left-64' : 'left-0'"
          @click="toggleSidebar"
          class="fixed top-4 z-20 bg-white border border-gray-200 rounded-r-lg p-2 transition-all duration-300">
            <SvgIcon :name="sidebarOpen ? 'sidebar-open' : 'sidebar-close'" :customCss="sidebarOpen ? 'w-6 h-6 text-gray-400' : 'w-7 h-7 text-gray-400'" />
        </button>
         </a-tooltip>

          <!-- 删除对话确认框 -->
  <a-modal v-model:open="deleteChatModelOpen" width="400px" :centered=true title="永久删除对话" ok-text="确认" ok-type="danger" cancel-text="取消"
    @ok="handleDeleteChatModelOk()">
      <p>删除后，该对话将不可恢复。确认删除吗？</p>
  </a-modal>

<!-- 重命名对话弹出框 -->
  <a-modal v-model:open="renameChatModelOpen" width="600px" :centered=true title="重命名对话" ok-text="确认" cancel-text="取消"
  @ok="handleRenameChatModelOk()">
              <a-form
                ref="formRef"
                :rules="rules"
                :model="formState"
                :label-col="{ span: 2 }"
                :wrapper-col="{ span: 22 }"
                autocomplete="off"
                >
                    <a-form-item
                      label="摘要"
                      name="summary"
                    >
                      <a-input v-model:value="formState.summary" />
                    </a-form-item>
                </a-form>
  </a-modal>

  <!-- 个人中心抽屉 -->
  <ProfileDrawer :open="profileDrawerOpen" @close="profileDrawerOpen = false" />
</template>

<script setup>
import { ref, reactive, onMounted, onBeforeUnmount } from 'vue'
import SvgIcon from '@/components/SvgIcon.vue'
import ProfileDrawer from '@/components/ProfileDrawer.vue'
import { EditOutlined, EllipsisOutlined, DeleteOutlined } from '@ant-design/icons-vue'
import { useRouter, useRoute } from 'vue-router'
import { findHistoryChatPageList, deleteChat, renameChat } from '@/api/chat'
import { message } from 'ant-design-vue'
import { useUserStore } from '@/stores/userStore'

const router = useRouter()
const userStore = useUserStore()

const props = defineProps({
  sidebarOpen: { type: Boolean, required: true },
})

const emit = defineEmits(['toggle-sidebar'])

const toggleSidebar = () => { emit('toggle-sidebar') }
const route = useRoute()

// 当前选中的聊天 ID
const selectedChatId = ref(null)
const historyChats = ref([])
const historyChatContainerRef = ref(null)
const hasMore = ref(true)
const isLoadingMore = ref(false)

onMounted(async () => {
  // 用户信息
  if (userStore.isLoggedIn && !userStore.userInfo) {
    try { await userStore.fetchUserInfo() } catch {}
  }

  fetchHistoryChats()

  const currentChatId = route.params.chatId
  if (currentChatId) { selectedChatId.value = currentChatId }

  if (historyChatContainerRef.value) {
    historyChatContainerRef.value.addEventListener('scroll', handleScroll)
  }
})

onBeforeUnmount(() => {
  if (historyChatContainerRef.value) {
    historyChatContainerRef.value.removeEventListener('scroll', handleScroll);
  }
})

const handleScroll = () => {
  if (historyChatContainerRef.value) {
    const { scrollTop, scrollHeight, clientHeight } = historyChatContainerRef.value
    const scrollPosition = scrollHeight - scrollTop - clientHeight
    if (scrollPosition <= 20 && hasMore.value && !isLoadingMore.value) {
      loadMoreHistoryChats()
    }
  }
}

const loadMoreHistoryChats = async () => {
  if (!hasMore.value || isLoadingMore.value) return
  isLoadingMore.value = true
  const nextPageNo = current.value + 1
  const currentTemp = current.value
  current.value = nextPageNo
  try { await fetchHistoryChats() }
  catch { current.value = currentTemp }
}

const current = ref(1)
const size = ref(20)

const fetchHistoryChats = async () => {
  findHistoryChatPageList(current.value, size.value).then(res => {
    isLoadingMore.value = false
    if (res.data.success) {
      const data = res.data.data
      hasMore.value = res.data.pages > current.value
      if (data && data.length > 0) {
        historyChats.value = [...historyChats.value, ...data]
      }
    }
  }).catch(() => { isLoadingMore.value = false })
}

const showButton = ref(null)
const deleteChatModelOpen = ref(false)
const deleteChatUUID = ref(null)
const renameChatModelOpen = ref(false)
const formState = reactive({ id: null, summary: '' })
const formRef = ref(null)
const rules = { summary: [{ required: true, message: '请输入对话摘要', trigger: 'change' }] }

const handleMenuClick = (uuid, id, summary, e) => {
  if (e.key === 'delete') {
    deleteChatUUID.value = uuid
    deleteChatModelOpen.value = true
  } else if (e.key === 'rename') {
    formState.id = id
    formState.summary = summary
    renameChatModelOpen.value = true
  }
}

const handleDeleteChatModelOk = () => {
  deleteChat(deleteChatUUID.value).then((res) => {
    if (res.data.success) {
      message.success('删除成功')
      const index = historyChats.value.findIndex(chat => chat.uuid === deleteChatUUID.value)
      if (index !== -1) historyChats.value.splice(index, 1)
      router.push({ name: 'Index'})
    } else {
      message.error(res.data.message)
    }
  }).finally(() => { deleteChatModelOpen.value = false })
}

const handleRenameChatModelOk = () => {
  formRef.value.validate().then(() => {
    renameChat(formState.id, formState.summary).then(res => {
      if (!res.data.success) { message.error(res.data.message); return }
      message.success('操作成功！')
      const chatIndex = historyChats.value.findIndex(chat => chat.id === formState.id)
      if (chatIndex !== -1) historyChats.value[chatIndex].summary = formState.summary
      renameChatModelOpen.value = false
    })
  }).catch(() => {})
}

const jumpToIndexPage = () => router.push('/')
const jumpToChatPage = (chatId) => {
  selectedChatId.value = chatId
  router.push({ name: 'ChatPage', params: { chatId } })
}
const jumpToCustomerServiceChatPage = () => router.push({ name: 'CustomerServiceChatPage'})
const jumpToLogin = () => router.push({ name: 'LoginPage' })

// 个人中心抽屉
const profileDrawerOpen = ref(false)
</script>

<style scoped>
.overflow-y-auto {
  scrollbar-color: rgba(0, 0, 0, 0.2) transparent;
}

.new-chat-btn {
  background-color: rgb(219 234 254);
  color: #4d6bfe;
}

.new-chat-btn:hover {
  background-color: #c6dcf8;
}
</style>
