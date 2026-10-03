<template>
  <div class="super-agent-container">
    <div class="header">
      <button class="back-button" @click="goBack">
        <span class="back-arrow">←</span>
        <span>返回</span>
      </button>
      <h1 class="title">AI超级智能体</h1>
      <div class="status-badge">
        <span class="status-dot"></span>
        <span>AI 在线</span>
      </div>
    </div>
    
    <div class="content-wrapper">
      <div class="chat-area">
        <ChatRoom
          :messages="messages"
          :connection-status="connectionStatus"
          ai-type="super"
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
import { chatWithManus } from '../api'

// 设置页面标题和元数据
useHead({
  title: 'AI超级智能体 - AI 智能体应用平台',
  meta: [
    {
      name: 'description',
      content: 'AI超级智能体是AI 智能体应用平台的全能助手，能解答各类专业问题，提供精准建议和解决方案'
    },
    {
      name: 'keywords',
      content: 'AI超级智能体,智能助手,专业问答,AI问答,专业建议,AI智能体'
    }
  ]
})

const router = useRouter()
const messages = ref([])
const connectionStatus = ref('disconnected')
let eventSource = null

// 添加消息到列表
const addMessage = (content, isUser, type = '') => {
  messages.value.push({
    content,
    isUser,
    type,
    time: new Date().getTime()
  })
}

// 发送消息
const sendMessage = (message) => {
  addMessage(message, true, 'user-question')
  
  // 连接SSE
  if (eventSource) {
    eventSource.close()
  }
  
  // 设置连接状态
  connectionStatus.value = 'connecting'
  
  // 临时存储
  let messageBuffer = []; // 用于存储SSE消息的缓冲区
  let lastBubbleTime = Date.now(); // 上一个气泡的创建时间
  let isFirstResponse = true; // 是否是第一次响应
  
  const chineseEndPunctuation = ['。', '！', '？', '…']; // 中文句子结束标点
  const minBubbleInterval = 800; // 气泡最小间隔时间(毫秒)
  
  // 创建消息气泡的函数
  const createBubble = (content, type = 'ai-answer') => {
    if (!content.trim()) return;
    
    // 添加适当的延迟，使消息显示更自然
    const now = Date.now();
    const timeSinceLastBubble = now - lastBubbleTime;
    
    if (isFirstResponse) {
      // 第一条消息立即显示
      addMessage(content, false, type);
      isFirstResponse = false;
    } else if (timeSinceLastBubble < minBubbleInterval) {
      // 如果与上一气泡间隔太短，添加一个延迟
      setTimeout(() => {
        addMessage(content, false, type);
      }, minBubbleInterval - timeSinceLastBubble);
    } else {
      // 正常添加消息
      addMessage(content, false, type);
    }
    
    lastBubbleTime = now;
    messageBuffer = []; // 清空缓冲区
  };
  
  eventSource = chatWithManus(message)
  
  // 监听SSE消息
  eventSource.onmessage = (event) => {
    const data = event.data
    
    if (data && data !== '[DONE]') {
      messageBuffer.push(data);
      
      // 检查是否应该创建新气泡
      const combinedText = messageBuffer.join('');
      
      // 句子结束或消息长度达到阈值
      const lastChar = data.charAt(data.length - 1);
      const hasCompleteSentence = chineseEndPunctuation.includes(lastChar) || data.includes('\n\n');
      // 含图片链接时，不按长度拆分（避免一张图被拆到多个气泡），等句子结束或流结束再整体成泡
      const hasImage = /!\[[^\]]*\]\(https?:\/\/[^)]+\)/.test(combinedText);
      const isLongEnough = combinedText.length > 40 && !hasImage;
      
      if (hasCompleteSentence || isLongEnough) {
        createBubble(combinedText);
      }
    }
    
    if (data === '[DONE]') {
      // 如果还有未显示的内容，创建最后一个气泡
      if (messageBuffer.length > 0) {
        const remainingContent = messageBuffer.join('');
        createBubble(remainingContent, 'ai-final');
      }
      
      // 完成后关闭连接
      connectionStatus.value = 'disconnected'
      eventSource.close()
    }
  }
  
  // 监听SSE错误
  eventSource.onerror = (error) => {
    console.error('SSE Error:', error)
    connectionStatus.value = 'error'
    eventSource.close()
    
    // 如果出错时有未显示的内容，也创建气泡
    if (messageBuffer.length > 0) {
      const remainingContent = messageBuffer.join('');
      createBubble(remainingContent, 'ai-error');
    }
  }
}

// 返回主页
const goBack = () => {
  router.push('/')
}

// 页面加载时添加欢迎消息
onMounted(() => {
  // 添加欢迎消息
  addMessage('你好，我是AI超级智能体。我可以解答各类问题，提供专业建议，请问有什么可以帮助你的吗？', false)
})

// 组件销毁前关闭SSE连接
onBeforeUnmount(() => {
  if (eventSource) {
    eventSource.close()
  }
})
</script>

<style scoped>
.super-agent-container {
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

.status-badge {
  justify-self: end;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: var(--text-secondary);
  background: #f1f5f9;
  border-radius: 999px;
  padding: 6px 12px;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #22c55e;
  box-shadow: 0 0 0 0 rgba(34, 197, 94, 0.5);
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0% {
    box-shadow: 0 0 0 0 rgba(34, 197, 94, 0.5);
  }
  70% {
    box-shadow: 0 0 0 6px rgba(34, 197, 94, 0);
  }
  100% {
    box-shadow: 0 0 0 0 rgba(34, 197, 94, 0);
  }
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

  .status-badge span:last-child {
    display: none;
  }

  .chat-area {
    padding: 8px;
  }
}
</style> 