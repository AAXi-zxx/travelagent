<template>
  <div class="chat-container">
    <!-- 聊天记录区域 -->
    <div class="chat-messages" ref="messagesContainer">
      <!-- 空状态提示 -->
      <div v-if="messages.length === 0" class="empty-state">
        <div class="empty-icon" :class="aiType === 'travel' ? 'empty-travel' : 'empty-super'">
          {{ aiType === 'travel' ? '✈️' : '🤖' }}
        </div>
        <div class="empty-text">开始你的第一次对话吧</div>
      </div>

      <div v-for="(msg, index) in messages" :key="index" class="message-wrapper">
        <!-- AI消息 -->
        <div v-if="!msg.isUser" class="message ai-message" :class="[msg.type]">
          <div class="avatar ai-avatar">
            <AiAvatarFallback :type="aiType" />
          </div>
          <div class="message-bubble">
            <div class="message-content">
              <template v-for="(seg, si) in parseContent(msg.content)" :key="si">
                <img
                  v-if="seg.type === 'image'"
                  :src="seg.url"
                  :alt="'图片' + (si + 1)"
                  class="message-image"
                  loading="lazy"
                />
                <span v-else>{{ seg.text }}</span>
              </template>
              <span v-if="connectionStatus === 'connecting' && index === messages.length - 1" class="typing-indicator">▋</span>
            </div>
            <div class="message-time">{{ formatTime(msg.time) }}</div>
          </div>
        </div>

        <!-- 用户消息 -->
        <div v-else class="message user-message" :class="[msg.type]">
          <div class="message-bubble">
            <div class="message-content">{{ msg.content }}</div>
            <div class="message-time">{{ formatTime(msg.time) }}</div>
          </div>
          <div class="avatar user-avatar">
            <div class="avatar-placeholder">我</div>
          </div>
        </div>
      </div>
    </div>

    <!-- 输入区域 -->
    <div class="chat-input-container">
      <div class="chat-input">
        <textarea
          v-model="inputMessage"
          @keydown.enter.prevent="sendMessage"
          :placeholder="connectionStatus === 'connecting' ? '正在生成回答…' : '输入你的问题，回车发送…'"
          class="input-box"
          :disabled="connectionStatus === 'connecting'"
          rows="1"
        ></textarea>
        <button
          @click="sendMessage"
          class="send-button"
          :disabled="connectionStatus === 'connecting' || !inputMessage.trim()"
        >
          <svg viewBox="0 0 24 24" width="18" height="18" fill="none" aria-hidden="true">
            <path d="M3.4 20.4l17.6-8.4L3.4 3.6 3.4 10l12 2-12 2v6.4z" fill="currentColor" />
          </svg>
          <span>发送</span>
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick, watch } from 'vue'
import AiAvatarFallback from './AiAvatarFallback.vue'

const props = defineProps({
  messages: {
    type: Array,
    default: () => []
  },
  connectionStatus: {
    type: String,
    default: 'disconnected'
  },
  aiType: {
    type: String,
    default: 'default' // 'travel' 或 'super'
  }
})

const emit = defineEmits(['send-message'])

const inputMessage = ref('')
const messagesContainer = ref(null)

// 发送消息
const sendMessage = () => {
  if (!inputMessage.value.trim()) return

  emit('send-message', inputMessage.value)
  inputMessage.value = ''
}

// 格式化时间
const formatTime = (timestamp) => {
  const date = new Date(timestamp)
  return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}

// 将消息内容解析为「文本 + 图片」交替片段：优先解析 Markdown 图片语法 ![alt](url)，
// 再从剩余文本中提取裸图片 URL，二者都能渲染成缩略图
const parseContent = (content) => {
  if (!content) return []
  const segments = []
  // 预扫描：先按 Markdown 图片语法切分（")" 是明确的 URL 终止符，避免相邻链接粘连）
  const mdRegex = /!\[[^\]]*\]\((https?:\/\/[^)\s]+)\)/gi
  const urlRegex = /https?:\/\/[^\s"'<>]+?\.(?:jpg|jpeg|png|webp|gif)(?:\?[^\s"'<>]*)?/gi
  let lastIndex = 0
  let match
  while ((match = mdRegex.exec(content)) !== null) {
    const before = content.slice(lastIndex, match.index)
    pushTextWithUrls(segments, before, urlRegex)
    segments.push({ type: 'image', url: match[1] })
    lastIndex = match.index + match[0].length
  }
  const rest = content.slice(lastIndex)
  pushTextWithUrls(segments, rest, urlRegex)
  return segments
}

// 对纯文本片段做裸图片 URL 提取（兜底：模型没用 Markdown 语法时也能渲染）
const pushTextWithUrls = (segments, text, urlRegex) => {
  if (!text) return
  let last = 0
  let m
  urlRegex.lastIndex = 0
  while ((m = urlRegex.exec(text)) !== null) {
    if (m.index > last) {
      segments.push({ type: 'text', text: text.slice(last, m.index) })
    }
    segments.push({ type: 'image', url: m[0] })
    last = m.index + m[0].length
  }
  if (last < text.length) {
    segments.push({ type: 'text', text: text.slice(last) })
  }
}

// 自动滚动到底部
const scrollToBottom = async () => {
  await nextTick()
  if (messagesContainer.value) {
    messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
  }
}

// 监听消息变化与内容变化，自动滚动
watch(() => props.messages.length, () => {
  scrollToBottom()
})

watch(() => props.messages.map(m => m.content).join(''), () => {
  scrollToBottom()
})

onMounted(() => {
  scrollToBottom()
})
</script>

<style scoped>
.chat-container {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-height: 480px;
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-md);
  overflow: hidden;
}

/* 消息区域 */
.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
  background: linear-gradient(180deg, #f8fafc 0%, #eef2f7 100%);
  display: flex;
  flex-direction: column;
}

.empty-state {
  margin: auto;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 14px;
  color: var(--text-muted);
}

.empty-icon {
  width: 72px;
  height: 72px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 32px;
  box-shadow: var(--shadow-md);
}

.empty-travel {
  background: linear-gradient(135deg, #3b82f6, #06b6d4);
}

.empty-super {
  background: linear-gradient(135deg, #2563eb, #4f46e5);
}

.empty-text {
  font-size: 15px;
}

.message-wrapper {
  margin-bottom: 20px;
  display: flex;
  flex-direction: column;
  width: 100%;
}

.message {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  max-width: 78%;
  animation: messageIn 0.25s ease-out;
}

.user-message {
  margin-left: auto;
  flex-direction: row;
  justify-content: flex-end;
}

.ai-message {
  margin-right: auto;
}

.avatar {
  width: 38px;
  height: 38px;
  border-radius: 50%;
  overflow: hidden;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}

.ai-avatar {
  box-shadow: 0 0 0 2px #fff, 0 2px 6px rgba(15, 23, 42, 0.12);
}

.avatar-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #2563eb, #3b82f6);
  color: #fff;
  font-weight: 600;
  font-size: 14px;
}

.message-bubble {
  padding: 12px 16px;
  border-radius: 16px;
  position: relative;
  word-wrap: break-word;
  overflow-wrap: break-word;
  min-width: 60px;
  max-width: 100%;
}

.user-message .message-bubble {
  background: linear-gradient(135deg, #2563eb, #3b82f6);
  color: #fff;
  border-bottom-right-radius: 4px;
  box-shadow: 0 4px 14px rgba(37, 99, 235, 0.28);
}

.ai-message .message-bubble {
  background: #fff;
  color: var(--text-primary);
  border: 1px solid var(--border);
  border-bottom-left-radius: 4px;
  box-shadow: var(--shadow-sm);
}

.message-content {
  font-size: 15px;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
}

.message-image {
  display: block;
  max-width: 240px;
  max-height: 180px;
  width: auto;
  border-radius: 10px;
  margin: 8px 0;
  box-shadow: 0 2px 8px rgba(15, 23, 42, 0.12);
}

.message-time {
  font-size: 11px;
  margin-top: 6px;
  text-align: right;
  letter-spacing: 0.3px;
}

.user-message .message-time {
  color: rgba(255, 255, 255, 0.78);
}

.ai-message .message-time {
  color: var(--text-muted);
}

/* 输入区域 */
.chat-input-container {
  background: #fff;
  border-top: 1px solid var(--border);
  padding: 14px 16px;
}

.chat-input {
  display: flex;
  align-items: flex-end;
  gap: 10px;
}

.input-box {
  flex: 1;
  border: 1px solid var(--border);
  border-radius: 14px;
  padding: 12px 16px;
  font-size: 15px;
  line-height: 1.5;
  font-family: inherit;
  resize: none;
  min-height: 24px;
  max-height: 120px;
  outline: none;
  background: #f8fafc;
  transition: border-color 0.2s, box-shadow 0.2s, background 0.2s;
  color: var(--text-primary);
}

.input-box::placeholder {
  color: var(--text-muted);
}

.input-box:focus {
  border-color: var(--brand-500);
  background: #fff;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.15);
}

.send-button {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  background: linear-gradient(135deg, #2563eb, #06b6d4);
  color: #fff;
  border: none;
  border-radius: 14px;
  padding: 12px 18px;
  font-size: 15px;
  font-weight: 500;
  cursor: pointer;
  transition: transform 0.15s, box-shadow 0.2s, opacity 0.2s;
  height: 46px;
  flex-shrink: 0;
  box-shadow: 0 4px 12px rgba(37, 99, 235, 0.3);
}

.send-button:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 6px 18px rgba(37, 99, 235, 0.4);
}

.send-button:active:not(:disabled) {
  transform: translateY(0);
}

.typing-indicator {
  display: inline-block;
  animation: blink 0.7s infinite;
  margin-left: 2px;
  color: var(--brand-500);
}

@keyframes blink {
  0% { opacity: 0; }
  50% { opacity: 1; }
  100% { opacity: 0; }
}

@keyframes messageIn {
  from {
    opacity: 0;
    transform: translateY(6px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.input-box:disabled,
.send-button:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

/* 连续 AI 气泡收窄 */
.ai-message + .ai-message {
  margin-top: 4px;
}

.ai-message + .ai-message .avatar {
  visibility: hidden;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .chat-container {
    min-height: 420px;
    border-radius: var(--radius-md);
  }

  .chat-messages {
    padding: 16px;
  }

  .message {
    max-width: 92%;
  }

  .message-content {
    font-size: 14px;
  }
}

@media (max-width: 480px) {
  .chat-container {
    min-height: 360px;
  }

  .avatar {
    width: 32px;
    height: 32px;
  }

  .message-bubble {
    padding: 10px 12px;
  }

  .message-content {
    font-size: 14px;
  }

  .chat-input-container {
    padding: 10px 12px;
  }

  .input-box {
    padding: 10px 12px;
  }

  .send-button {
    padding: 10px 14px;
    height: 42px;
  }
}
</style>
