<template>
  <div class="chat-container">
    <div class="chat-header">
      <h3>AI 助手</h3>
      <span class="status" :class="{ connected: isConnected }">
        {{ isConnected ? '已连接' : '连接断开' }}
      </span>
    </div>
    
    <div class="chat-messages" ref="messagesContainer">
      <div v-if="messages.length === 0" class="empty-state">
        <i class="fas fa-comments"></i>
        <p>有什么可以帮您的吗？</p>
      </div>
      
      <div v-for="(msg, index) in messages" :key="index" class="message" :class="msg.role">
        <div class="avatar">
          <i v-if="msg.role === 'user'" class="fas fa-user"></i>
          <i v-else class="fas fa-robot"></i>
        </div>
        <div class="message-content">
          <div class="markdown-body" v-html="renderMarkdown(msg.content)"></div>
          <div v-if="msg.role === 'assistant' && msg.isStreaming" class="typing-indicator">
            <span></span><span></span><span></span>
          </div>
        </div>
      </div>
    </div>
    
    <div class="chat-input-area">
      <textarea 
        v-model="inputMessage" 
        @keydown.enter.prevent="handleEnter"
        placeholder="输入您的问题 (Shift + Enter 换行)..."
        :disabled="isGenerating"
        rows="3"
      ></textarea>
      <button class="send-btn" @click="sendMessage" :disabled="!inputMessage.trim() || isGenerating || !isConnected">
        <i class="fas fa-paper-plane"></i>
        发送
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick, watch } from 'vue';
import MarkdownIt from 'markdown-it';

const md = new MarkdownIt({
  html: true,
  linkify: true,
  typographer: true,
  breaks: true
});

interface Message {
  role: 'user' | 'assistant';
  content: string;
  isStreaming?: boolean;
}

const messages = ref<Message[]>([]);
const inputMessage = ref('');
const isConnected = ref(false);
const isGenerating = ref(false);
const messagesContainer = ref<HTMLElement | null>(null);
let ws: WebSocket | null = null;
let currentAssistantMessage = ref<Message | null>(null);

const connectWebSocket = () => {
  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
  // 修改为正确的后端端口 18081
  const host = 'localhost:18081'; 
  const wsUrl = `${protocol}//${host}/ws/chat`;
  
  ws = new WebSocket(wsUrl);
  
  ws.onopen = () => {
    console.log('WebSocket connected');
    isConnected.value = true;
  };
  
  ws.onclose = () => {
    console.log('WebSocket disconnected');
    isConnected.value = false;
    // 尝试重连
    setTimeout(connectWebSocket, 5000);
  };
  
  ws.onerror = (error) => {
    console.error('WebSocket error:', error);
    isGenerating.value = false;
    if (currentAssistantMessage.value) {
      currentAssistantMessage.value.isStreaming = false;
      currentAssistantMessage.value = null;
    }
  };
  
  ws.onmessage = (event) => {
    console.log('收到WebSocket消息:', event.data);
    const token = event.data;
    
    // 处理结束标记
    if (token === '[DONE]') {
      console.log('收到结束标记');
      isGenerating.value = false;
      if (currentAssistantMessage.value) {
        currentAssistantMessage.value.isStreaming = false;
        currentAssistantMessage.value = null;
      }
      return;
    }
    
    if (currentAssistantMessage.value) {
      console.log('追加内容到消息，当前长度:', currentAssistantMessage.value.content.length);
      currentAssistantMessage.value.content += token;
      scrollToBottom();
    } else {
      console.warn('当前没有活动的助手消息对象');
    }
  };
};

const sendMessage = () => {
  if (!inputMessage.value.trim() || !ws || ws.readyState !== WebSocket.OPEN) {
    console.warn('无法发送消息, 输入为空或WebSocket未连接');
    return;
  }
  
  const content = inputMessage.value.trim();
  console.log('发送消息:', content);
  messages.value.push({ role: 'user', content });
  inputMessage.value = '';
  isGenerating.value = true;
  
  // 创建新的助手消息占位
  const assistantMsg: Message = { role: 'assistant', content: '', isStreaming: true };
  messages.value.push(assistantMsg);
  currentAssistantMessage.value = assistantMsg;
  console.log('创建助手消息占位符, 引用:', currentAssistantMessage.value);
  
  ws.send(content);
  console.log('消息已通过WebSocket发送');
  
  scrollToBottom();
  
  // 模拟流式结束检测（实际 WebSocket 协议可能需要特定结束帧，或者简单地依靠超时/新消息）
  // 这里简化处理：假设后端不发送明确的结束信号，我们只能等待。
  // 但为了 demo 效果，我们在 ChatWebSocketHandler 中并没有发送结束信号。
  // 改进：ChatWebSocketHandler 应该在 onComplete 时发送一个特殊标记，或者前端设置超时。
  // 既然我们在后端没有改协议，我们只能假设它一直流式传输。
  // 但为了 UI 交互，我们需要知道何时结束。
  // 临时方案：每次收到消息重置超时，如果 2秒没消息则认为结束。
  resetInactivityTimer();
};

let inactivityTimer: any = null;
const resetInactivityTimer = () => {
  if (inactivityTimer) clearTimeout(inactivityTimer);
  inactivityTimer = setTimeout(() => {
    if (isGenerating.value) {
      isGenerating.value = false;
      if (currentAssistantMessage.value) {
        currentAssistantMessage.value.isStreaming = false;
        currentAssistantMessage.value = null;
      }
    }
  }, 2000); // 2秒无响应认为结束
};

// 监听消息接收以重置定时器
watch(() => currentAssistantMessage.value?.content, () => {
  if (isGenerating.value) {
    resetInactivityTimer();
  }
});

const handleEnter = (e: KeyboardEvent) => {
  if (!e.shiftKey) {
    sendMessage();
  }
};

const renderMarkdown = (text: string) => {
  return md.render(text);
};

const scrollToBottom = () => {
  nextTick(() => {
    if (messagesContainer.value) {
      messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight;
    }
  });
};

onMounted(() => {
  connectWebSocket();
});

onUnmounted(() => {
  if (ws) ws.close();
});
</script>

<style scoped>
.chat-container {
  display: flex;
  flex-direction: column;
  height: 600px;
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.05);
  overflow: hidden;
}

.chat-header {
  padding: 16px 24px;
  border-bottom: 1px solid #eee;
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #f8f9fa;
}

.chat-header h3 {
  margin: 0;
  font-size: 16px;
  color: #2c3e50;
}

.status {
  font-size: 12px;
  color: #e74c3c;
  display: flex;
  align-items: center;
  gap: 6px;
}

.status::before {
  content: '';
  display: block;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: currentColor;
}

.status.connected {
  color: #2ecc71;
}

.chat-messages {
  flex: 1;
  padding: 24px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 20px;
  background: #fcfcfc;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #bdc3c7;
  gap: 16px;
}

.empty-state i {
  font-size: 48px;
}

.message {
  display: flex;
  gap: 16px;
  max-width: 80%;
}

.message.user {
  align-self: flex-end;
  flex-direction: row-reverse;
}

.message.assistant {
  align-self: flex-start;
}

.avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  color: #fff;
  font-size: 14px;
}

.message.user .avatar {
  background: #3498db;
}

.message.assistant .avatar {
  background: #9b59b6;
}

.message-content {
  background: #fff;
  padding: 12px 16px;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  font-size: 14px;
  line-height: 1.6;
  color: #2c3e50;
  overflow-wrap: break-word;
}

.message.user .message-content {
  background: #3498db;
  color: #fff;
  border-bottom-right-radius: 2px;
}

.message.assistant .message-content {
  background: #fff;
  border-bottom-left-radius: 2px;
  border: 1px solid #eee;
}

.chat-input-area {
  padding: 20px;
  background: #fff;
  border-top: 1px solid #eee;
  display: flex;
  gap: 12px;
  align-items: flex-end;
}

textarea {
  flex: 1;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  padding: 12px;
  font-family: inherit;
  font-size: 14px;
  resize: none;
  outline: none;
  transition: border-color 0.3s;
}

textarea:focus {
  border-color: #3498db;
}

.send-btn {
  padding: 0 24px;
  height: 46px;
  background: #3498db;
  color: #fff;
  border: none;
  border-radius: 8px;
  font-weight: 600;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 8px;
  transition: all 0.3s;
}

.send-btn:disabled {
  background: #bdc3c7;
  cursor: not-allowed;
}

.send-btn:hover:not(:disabled) {
  background: #2980b9;
}

/* Markdown Styles */
:deep(.markdown-body p) {
  margin-bottom: 1em;
}

:deep(.markdown-body p:last-child) {
  margin-bottom: 0;
}

:deep(.markdown-body pre) {
  background: #f8f9fa;
  padding: 12px;
  border-radius: 6px;
  overflow-x: auto;
  margin: 10px 0;
}

:deep(.markdown-body code) {
  background: rgba(0, 0, 0, 0.05);
  padding: 2px 4px;
  border-radius: 4px;
  font-family: monospace;
}

.message.user :deep(.markdown-body code) {
  background: rgba(255, 255, 255, 0.2);
}
</style>
