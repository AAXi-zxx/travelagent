<template>
  <div class="travel-master-container">
    <div class="header">
      <button class="back-button" @click="goBack">
        <span class="back-arrow">←</span>
        <span>返回</span>
      </button>
      <h1 class="title">AI旅游管理大师</h1>
      <div class="chat-id">会话 {{ chatId }}</div>
    </div>
    
    <div class="content-wrapper">
      <div class="chat-area">
        <ChatRoom
          :messages="messages"
          :connection-status="connectionStatus"
          ai-type="travel"
          @send-message="sendMessage"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { useHead } from '@vueuse/head'
import ChatRoom from '../components/ChatRoom.vue'
import { chatWithTravelApp } from '../api'

// 设置页面标题和元数据
useHead({
  title: 'AI旅游管理大师 - AI 智能体应用平台',
  meta: [
    {
      name: 'description',
      content: 'AI旅游管理大师是AI 智能体应用平台的专业旅游顾问，帮你制定各种旅行方案，提供出行建议'
    },
    {
      name: 'keywords',
      content: 'AI旅游管理大师,旅游顾问,旅行规划,旅游方案,出行建议,AI智能体'
    }
  ]
})

const router = useRouter()
const messages = ref([])
const chatId = ref('')
const connectionStatus = ref('disconnected')
let eventSource = null

// 生成随机会话ID
const generateChatId = () => {
  return 'travel_' + Math.random().toString(36).substring(2, 10)
}

// 添加消息到列表
const addMessage = (content, isUser) => {
  messages.value.push({
    content,
    isUser,
    time: new Date().getTime()
  })
}

// 发送消息（统一智能体：大模型自主决定工具调用与知识库检索）
const sendMessage = (message) => {
  addMessage(message, true)
  
  // 连接SSE
  if (eventSource) {
    eventSource.close()
  }
  
  // 创建一个空的AI回复消息
  const aiMessageIndex = messages.value.length
  addMessage('', false)
  
  connectionStatus.value = 'connecting'
  eventSource = chatWithTravelApp(message, chatId.value)
  
  // 监听SSE消息
  eventSource.onmessage = (event) => {
    const data = event.data
    if (data && data !== '[DONE]') {
      // 更新最新的AI消息内容
      if (aiMessageIndex < messages.value.length) {
        messages.value[aiMessageIndex].content += data
      }
    }
    
    if (data === '[DONE]') {
      connectionStatus.value = 'disconnected'
      eventSource.close()
    }
  }
  
  // 监听SSE错误
  eventSource.onerror = (error) => {
    console.error('SSE Error:', error)
    connectionStatus.value = 'error'
    eventSource.close()
  }
}

// 返回主页
const goBack = () => {
  router.push('/')
}

// 页面加载时添加欢迎消息
onMounted(() => {
  // 生成聊天ID
  chatId.value = generateChatId()
  
  // 添加欢迎消息
  addMessage('欢迎来到AI旅游管理大师，请告诉我你的旅行计划，我会帮你制定详细的旅游方案。', false)
})

// 组件销毁前关闭SSE连接
onBeforeUnmount(() => {
  if (eventSource) {
    eventSource.close()
  }
})
</script>

<style scoped>
.travel-master-container {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: var(--bg-page);
}

.header {
  display: grid;
  grid-template-columns: 1fr auto 1fr;
  align-items: center;
  gap: 12px;
  padding: 14px 24px;
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border-bottom: 1px solid var(--border);
  box-shadow: 0 1px 3px rgba(15, 23, 42, 0.04);
  position: sticky;
  top: 0;
  z-index: 10;
}

.back-button {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  justify-self: start;
  font-size: 14px;
  color: var(--text-secondary);
  background: #fff;
  border: 1px solid var(--border);
  border-radius: 999px;
  padding: 8px 16px;
  cursor: pointer;
  transition: color 0.2s, border-color 0.2s, box-shadow 0.2s, transform 0.15s;
}

.back-button:hover {
  color: var(--brand-600);
  border-color: var(--brand-400);
  box-shadow: 0 2px 8px rgba(37, 99, 235, 0.15);
  transform: translateX(-2px);
}

.back-arrow {
  font-size: 16px;
  line-height: 1;
}

.title {
  font-size: 20px;
  font-weight: 700;
  margin: 0;
  text-align: center;
  justify-self: center;
  background: linear-gradient(120deg, var(--brand-700), var(--brand-500) 60%, var(--accent));
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
  letter-spacing: 1px;
}

.chat-id {
  justify-self: end;
  font-size: 13px;
  color: var(--text-muted);
  background: #f1f5f9;
  border-radius: 999px;
  padding: 6px 12px;
  font-variant-numeric: tabular-nums;
}

.content-wrapper {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-height: 0;
}

.chat-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
  padding: 20px;
  overflow: hidden;
}

/* 响应式样式 */
@media (max-width: 768px) {
  .header {
    padding: 12px 16px;
  }

  .title {
    font-size: 18px;
  }

  .chat-id {
    font-size: 12px;
  }

  .chat-area {
    padding: 12px;
  }
}

@media (max-width: 480px) {
  .header {
    padding: 10px 12px;
  }

  .back-button {
    font-size: 13px;
    padding: 7px 12px;
  }

  .back-button span:last-child {
    display: none;
  }

  .title {
    font-size: 16px;
  }

  .chat-id {
    display: none;
  }

  .chat-area {
    padding: 8px;
  }
}
</style> 