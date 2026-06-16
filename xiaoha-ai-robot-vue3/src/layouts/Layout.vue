<template>
    <div class="h-screen flex overflow-hidden overflow-x-hidden">
        <!-- 移动端遮罩 -->
        <div
          v-if="sidebarOpen && isMobile"
          class="fixed inset-0 bg-black/40 z-20 lg:hidden transition-opacity duration-300"
          @click="toggleSidebar"
        />

        <!-- 左边栏 -->
        <Sidebar
            :sidebarOpen="sidebarOpen"
            :isMobile="isMobile"
            @toggle-sidebar="toggleSidebar"
            />

        <!-- 主内容区域 -->
        <div :class="sidebarOpen && !isMobile ? 'ml-64' : 'ml-0'"
          class="flex flex-col flex-1 transition-all duration-300 min-w-0">
            <slot name="main-content"></slot>
        </div>

         <!-- 吸附底部的提示文字 -->
        <div v-if="showFooterText"
            :class="sidebarOpen && !isMobile ? 'ml-64' : 'ml-0'"
            class="bg-white fixed bottom-0 left-0 right-0 flex items-center justify-center text-xs text-gray-400 transition-all duration-300 py-2 z-10">
        内容由 AI 生成，请仔细甄别
        </div>
    </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import Sidebar from '@/components/Sidebar.vue'

const sidebarOpen = ref(true)
const isMobile = ref(false)

function checkMobile() {
  isMobile.value = window.innerWidth < 768
  // 移动端默认收起侧边栏
  if (isMobile.value) sidebarOpen.value = false
}

onMounted(() => {
  checkMobile()
  window.addEventListener('resize', checkMobile)
})
onBeforeUnmount(() => {
  window.removeEventListener('resize', checkMobile)
})

function toggleSidebar() {
  sidebarOpen.value = !sidebarOpen.value
}

const props = defineProps({
  showFooterText: {
    type: Boolean,
    default: true
  }
})
</script>
