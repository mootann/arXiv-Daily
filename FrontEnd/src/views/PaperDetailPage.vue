<template>
  <div class="paper-detail-page" v-loading="loading">
    <div class="detail-container" :class="{ 'full-width': activeTab === 'original' }">
      <!-- 左侧侧边栏 -->
      <div class="sidebar" v-if="activeTab !== 'original'">
        <div class="back-btn" @click="$router.back()">
          <el-icon><ArrowLeft /></el-icon>
          <span>返回</span>
        </div>
        <div class="sidebar-actions">
          <div class="sidebar-action" @click="handleShare" title="分享">
            <el-icon><Share /></el-icon>
          </div>
          <div class="sidebar-action" @click="handleCollect" :class="{ active: paper?.isCollected }" title="收藏">
            <el-icon><Star /></el-icon>
          </div>
          <div class="sidebar-action" @click="handleLike" :class="{ active: paper?.isLiked }" title="点赞">
            <el-icon><CircleCheck /></el-icon>
          </div>
        </div>
      </div>

      <!-- 主内容区 -->
      <div class="main-content">
        <!-- 顶部标签栏 -->
        <div class="tab-navigation">
          <div
            class="tab-item"
            :class="{ active: activeTab === 'overview' }"
            @click="activeTab = 'overview'"
          >
            论文概览
          </div>
          <div
            class="tab-item"
            :class="{ active: activeTab === 'original' }"
            @click="activeTab = 'original'"
          >
            论文原文
          </div>
          <div
            class="tab-item"
            :class="{ active: activeTab === 'resources' }"
            @click="activeTab = 'resources'"
          >
            相关资料/解读
          </div>
        </div>

        <!-- 内容区域 -->
        <div class="content-area" v-if="paper">
          <!-- 论文概览 -->
          <div v-show="activeTab === 'overview'" class="tab-content">
            <!-- 标题区 -->
            <div class="paper-header-section">
              <h1 class="paper-title">{{ paper.title }}</h1>
              <div class="paper-authors-section">
                <el-icon><User /></el-icon>
                <span class="authors-text">{{ formatAuthors(paper.authors) }}</span>
              </div>
              <div class="paper-category">
                <span class="category-tag">{{ paper.primaryCategory }}</span>
              </div>
            </div>

            <!-- 摘要区 -->
            <div class="abstract-section">
              <h3 class="section-title">摘要</h3>
              <div ref="abstractRef" class="abstract-text" v-html="renderLatex(paper.summary)"></div>
            </div>

            <!-- 链接区 -->
            <div class="links-section" v-if="paper.arxivUrl || paper.githubUrl">
              <a v-if="paper.arxivUrl" :href="paper.arxivUrl" target="_blank" class="link-item">
                <el-icon><Link /></el-icon>
                查看arXiv原文
              </a>
              <a v-if="paper.githubUrl" :href="paper.githubUrl" target="_blank" class="link-item">
                <el-icon><Promotion /></el-icon>
                代码仓库
              </a>
            </div>

            <!-- 互动统计 -->
            <div class="stats-section">
              <div class="stat-item">
                <span class="stat-icon">👥</span>
                <span class="stat-value">{{ paper.viewCount || 0 }}</span>
                <span class="stat-label">阅读</span>
              </div>
              <div class="stat-item" @click="handleCollect" :class="{ active: paper.isCollected }">
                <span class="stat-icon">⭐</span>
                <span class="stat-value">{{ paper.collectCount || 0 }}</span>
                <span class="stat-label">收藏</span>
              </div>
              <div class="stat-item" @click="handleLike" :class="{ active: paper.isLiked }">
                <span class="stat-icon">❤️</span>
                <span class="stat-value">{{ paper.likeCount || 0 }}</span>
                <span class="stat-label">点赞</span>
              </div>
              <div class="stat-item">
                <span class="stat-icon">💬</span>
                <span class="stat-value">{{ comments.length }}</span>
                <span class="stat-label">评论</span>
              </div>
            </div>

            <!-- 评论区 -->
            <div class="comments-section">
              <h3 class="section-title">评论</h3>
              <div v-if="userStore.isLoggedIn" class="comment-input">
                <el-input
                  v-model="newComment"
                  type="textarea"
                  :rows="3"
                  placeholder="发表您的见解..."
                />
                <el-button type="primary" @click="handleAddComment" class="submit-btn">
                  发表
                </el-button>
              </div>
              <el-empty v-else description="登录后发表评论" />
              <div class="comment-list">
                <div v-for="c in comments" :key="c.id" class="comment-item">
                  <div class="comment-header">
                    <span class="comment-username">{{ c.username }}</span>
                    <span class="comment-time">{{ formatDate(c.createdTime) }}</span>
                  </div>
                  <div class="comment-content">{{ c.content }}</div>
                </div>
                <div v-if="comments.length === 0" class="no-comments">
                  暂无评论，快来发表第一条评论吧~
                </div>
              </div>
            </div>
          </div>

          <!-- 论文原文 -->
          <div v-show="activeTab === 'original'" class="tab-content ai-tab-content">
            <div class="ai-layout">
              <!-- 左侧PDF阅读区 -->
              <div class="ai-pdf-section">
                <div class="pdf-controls compact">
                  <div class="left-controls">
                    <el-button-group size="small">
                      <el-button @click="$router.back()"><el-icon><ArrowLeft /></el-icon> 返回</el-button>
                    </el-button-group>
                    <el-divider direction="vertical" />
                    <el-button-group size="small">
                      <el-button @click="prevPage" :disabled="pdfPage <= 1"><el-icon><ArrowLeft /></el-icon></el-button>
                      <el-button disabled>{{ pdfPage }} / {{ pdfPages }}</el-button>
                      <el-button @click="nextPage" :disabled="pdfPage >= pdfPages"><el-icon><ArrowRight /></el-icon></el-button>
                    </el-button-group>
                    <el-divider direction="vertical" />
                    <el-button-group size="small">
                      <el-button @click="zoomOut" :disabled="pdfScale <= 0.5">-</el-button>
                      <el-button disabled>{{ Math.round(pdfScale * 100) }}%</el-button>
                      <el-button @click="zoomIn" :disabled="pdfScale >= 3">+</el-button>
                    </el-button-group>
                  </div>
                  <div class="right-controls">
                    <el-button 
                      type="primary" 
                      size="small" 
                      :icon="isChatVisible ? 'ArrowRight' : 'ChatDotRound'"
                      @click="toggleChat"
                    >
                      {{ isChatVisible ? '收起对话' : '展开对话' }}
                    </el-button>
                  </div>
                </div>
                
                <div class="pdf-wrapper" @mouseup="handleTextSelection" v-loading="pdfLoading">
                  <div class="pdf-render-container" :style="{ width: pdfWidth + 'px', height: pdfHeight + 'px' }">
                    <canvas ref="pdfCanvasRef"></canvas>
                    <div ref="pdfTextLayerRef" class="textLayer"></div>
                  </div>
                </div>

                <!-- 浮动菜单 -->
                <div 
                  v-if="showFloatingMenu" 
                  class="floating-menu" 
                  :style="{ top: floatingMenuPosition.top + 'px', left: floatingMenuPosition.left + 'px' }"
                >
                  <div class="menu-item" @click="handleAiAction('explain')">
                    <el-icon><Reading /></el-icon> 解释
                  </div>
                  <div class="menu-item" @click="handleAiAction('translate')">
                    <el-icon><Switch /></el-icon> 翻译
                  </div>
                  <div class="menu-item" @click="handleAiAction('chat')">
                    <el-icon><ChatDotRound /></el-icon> 对话
                  </div>
                </div>
              </div>

              <!-- 右侧聊天区 -->
              <div class="ai-chat-section" v-show="isChatVisible">
                <div class="chat-header">
                  <h3>AI 助手</h3>
                  <el-button type="text" @click="clearChat">清空</el-button>
                </div>
                <div class="chat-messages" ref="chatMessagesRef">
                  <div v-if="chatMessages.length === 0" class="chat-welcome">
                    <p>👋 您好！我是您的论文阅读助手。</p>
                    <p>您可以划选论文内容，让我为您解释、翻译，或者直接在这里提问。</p>
                  </div>
                  <div 
                    v-for="(msg, index) in chatMessages" 
                    :key="index" 
                    class="message-item"
                    :class="msg.role"
                  >
                    <div class="message-avatar">
                      <el-icon v-if="msg.role === 'ai'"><Monitor /></el-icon>
                      <el-icon v-else><User /></el-icon>
                    </div>
                    <div class="message-content">
                      <div v-if="msg.role === 'ai'" v-html="renderMarkdown(msg.content)"></div>
                      <div v-else>{{ msg.content }}</div>
                    </div>
                  </div>
                  <div v-if="chatLoading" class="message-item ai">
                    <div class="message-avatar"><el-icon><Monitor /></el-icon></div>
                    <div class="message-content loading">
                      <span></span><span></span><span></span>
                    </div>
                  </div>
                </div>
                <div class="chat-input-area">
                  <el-input
                    v-model="chatInput"
                    type="textarea"
                    :rows="3"
                    placeholder="向AI询问任何您感兴趣的问题..."
                    @keydown.enter.ctrl="sendChatMessage"
                  />
                  <div class="input-actions">
                    <el-button type="primary" size="small" @click="sendChatMessage" :loading="chatLoading">
                      发送
                    </el-button>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- 相关资料/解读 -->
          <div v-show="activeTab === 'resources'" class="tab-content">
            <div class="resources-empty">
              <el-empty description="暂无相关资料/解读" />
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧辅助内容区 -->
      <div class="right-sidebar" v-if="activeTab !== 'original'">
        <div class="related-papers">
          <h3 class="sidebar-title">相关论文推荐</h3>
          <div class="related-list">
            <div
              v-for="related in relatedPapers"
              :key="related.arxivId"
              class="related-item"
              @click="goToDetail(related.arxivId)"
            >
              <div class="related-title">{{ related.title }}</div>
              <div class="related-meta">{{ related.primaryCategory }}</div>
            </div>
            <el-empty v-if="relatedPapers.length === 0" description="暂无相关论文" :image-size="60" />
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch, nextTick, shallowRef } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import * as pdfjsLib from 'pdfjs-dist'
import 'pdfjs-dist/web/pdf_viewer.css'
import katex from 'katex'
import 'katex/dist/katex.min.css'
// @ts-ignore
import pdfWorker from 'pdfjs-dist/build/pdf.worker.min.mjs?url'
import { paperApi, userApi } from '@/api'
import { chatApi } from '@/api/chat'
import { useUserStore } from '@/stores/user'
import type { ArxivPaper } from '@/types'

// Configure worker
if (typeof window !== 'undefined' && 'Worker' in window) {
  pdfjsLib.GlobalWorkerOptions.workerSrc = pdfWorker
}

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const paper = ref<any>(null)
const comments = ref<any[]>([])
const newComment = ref('')
const activeTab = ref('overview')
const relatedPapers = ref<ArxivPaper[]>([])

// PDF相关
const pdfCanvasRef = ref<HTMLCanvasElement>()
const pdfTextLayerRef = ref<HTMLDivElement>()
const abstractRef = ref<HTMLDivElement>()
const pdfDoc = shallowRef<any>(null)
const pdfPage = ref(1)
const pdfPages = ref(0)
const pdfScale = ref(1.0)
const pdfRotation = ref(0)
const pdfLoading = ref(false)
const pdfWidth = ref(0)
const pdfHeight = ref(0)

// AI Chat相关
const showFloatingMenu = ref(false)
const floatingMenuPosition = ref({ top: 0, left: 0 })
const selectedText = ref('')
const chatMessages = ref<{role: 'user' | 'ai', content: string}[]>([])
const chatInput = ref('')
const chatLoading = ref(false)
const chatMessagesRef = ref<HTMLDivElement>()
const isChatVisible = ref(true)

const toggleChat = async () => {
  isChatVisible.value = !isChatVisible.value
  await nextTick()
  await fitToWidth()
  await renderPdf()
}

// Simple markdown renderer to avoid extra dependency
const renderMarkdown = (text: string) => {
  if (!text) return ''
  return text
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
    .replace(/`([^`]+)`/g, '<code>$1</code>')
    .replace(/\n/g, '<br>')
}


const fetchPaper = async () => {
  loading.value = true
  try {
    const r = await paperApi.getPaperById(route.params.id as string)
    if (r.data.code === 200) {
      paper.value = r.data.data
      fetchComments()
      fetchRelatedPapers()
    }
  } finally {
    loading.value = false
  }
}

const fetchComments = async () => {
  try {
    const r = await userApi.getComments(route.params.id as string)
    if (r.data.code === 200) {
      comments.value = r.data.data.content || r.data.data.records || []
    }
  } catch {}
}

const fetchRelatedPapers = async () => {
  try {
    // 获取同分类的相关论文
    if (paper.value?.primaryCategory) {
      const r = await paperApi.getPapersByCategory(
        paper.value.primaryCategory,
        1,
        5
      )
      if (r.data.code === 200) {
        // 过滤掉当前论文
        const papers = r.data.data.content || r.data.data.records || []
        relatedPapers.value = papers.filter(
          (p: ArxivPaper) => p.arxivId !== paper.value.arxivId
        )
      }
    }
  } catch {}
}

const handleLike = async () => {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  try {
    if (paper.value.isLiked) {
      await userApi.unlikePaper(paper.value.arxivId)
      paper.value.isLiked = false
      if (paper.value.likeCount) paper.value.likeCount--
    } else {
      await userApi.likePaper(paper.value.arxivId)
      paper.value.isLiked = true
      if (paper.value.likeCount) paper.value.likeCount++
    }
  } catch {}
}

const handleCollect = async () => {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  try {
    if (paper.value.isCollected) {
      await userApi.uncollectPaper(paper.value.arxivId)
      paper.value.isCollected = false
      if (paper.value.collectCount) paper.value.collectCount--
    } else {
      await userApi.collectPaper(paper.value.arxivId)
      paper.value.isCollected = true
      if (paper.value.collectCount) paper.value.collectCount++
    }
  } catch {}
}

const handleShare = () => {
  const url = window.location.href
  if (navigator.clipboard) {
    navigator.clipboard.writeText(url).then(() => {
      ElMessage.success('链接已复制到剪贴板')
    })
  } else {
    const input = document.createElement('input')
    input.value = url
    document.body.appendChild(input)
    input.select()
    document.execCommand('copy')
    document.body.removeChild(input)
    ElMessage.success('链接已复制到剪贴板')
  }
}

const handleAddComment = async () => {
  if (!newComment.value.trim()) return
  await userApi.addComment(route.params.id as string, newComment.value)
  newComment.value = ''
  fetchComments()
}

const goToDetail = (arxivId: string) => {
  router.push(`/paper/${arxivId}`)
}

const formatDate = (s: string) => s ? new Date(s).toLocaleDateString('zh-CN') : ''

// 格式化作者信息，移除方括号和引号
const formatAuthors = (authors: any) => {
  if (!authors) return ''
  if (Array.isArray(authors)) {
    return authors.join(', ')
  }
  // 处理带方括号和引号的字符串格式
  if (typeof authors === 'string') {
    // 移除方括号和引号
    let formatted = authors.replace(/^\[|\]$/g, '')
    formatted = formatted.replace(/"([^"]+)"/g, '$1')
    return formatted
  }
  return authors
}

// 渲染LaTeX公式
const renderLatex = (text: string) => {
  if (!text) return ''
  
  // 首先转义HTML特殊字符
  let result = text
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
  
  // 渲染行内公式 $...$
  result = result.replace(/\$([^\$\n]+?)\$/g, (match, formula) => {
    try {
      return katex.renderToString(formula, {
        throwOnError: false,
        displayMode: false
      })
    } catch (e) {
      console.warn('LaTeX render error:', e)
      return match
    }
  })
  
  // 渲染显示公式 $$...$$
  result = result.replace(/\$\$([^\$]+?)\$\$/g, (match, formula) => {
    try {
      return katex.renderToString(formula, {
        throwOnError: false,
        displayMode: true
      })
    } catch (e) {
      console.warn('LaTeX render error:', e)
      return match
    }
  })
  
  // 处理特殊字符如 \n, \t, \textcolor 等
  result = result.replace(/\\n/g, '<br>')
  result = result.replace(/\\t/g, '    ')
  
  // 处理 \textcolor{color}{text}
  result = result.replace(/\\textcolor\{([^}]+)\}\{([^}]+)\}/g, '<span style="color: $1">$2</span>')
  
  return result
}

const loadPdf = async () => {
  if (!paper.value?.arxivId) return
  pdfLoading.value = true
  
  try {
    // Check if we have saved settings, but we might override scale for fit-to-width
    const saved = sessionStorage.getItem(`pdf_${paper.value.arxivId}`)
    let savedScale = 0
    if (saved) {
      const t = JSON.parse(saved)
      pdfPage.value = t.page || 1
      savedScale = t.scale || 0
      pdfRotation.value = t.rotation || 0
    }
    
    // Use backend proxy to avoid CORS issues
    const pdfUrl = `/api/v1/arxiv/pdf/${encodeURIComponent(paper.value.arxivId)}`
    
    const task = pdfjsLib.getDocument({
      url: pdfUrl,
      cMapUrl: `https://unpkg.com/pdfjs-dist@${pdfjsLib.version}/cmaps/`,
      cMapPacked: true,
    })
    
    pdfDoc.value = await task.promise
    pdfPages.value = pdfDoc.value.numPages
    
    // Auto fit width if no saved scale or explicitly requested
    if (!savedScale || savedScale === 0) {
      await fitToWidth()
    } else {
      pdfScale.value = savedScale
    }

    await renderPdf()
  } catch (error) {
    console.error('加载PDF失败:', error)
    ElMessage.error('加载PDF失败，请稍后重试')
  } finally {
    pdfLoading.value = false
  }
}

const fitToWidth = async () => {
  if (!pdfDoc.value) return
  
  try {
    const page = await pdfDoc.value.getPage(pdfPage.value)
    const viewport = page.getViewport({ scale: 1.0, rotation: pdfRotation.value })
    
    // Get container width based on active tab
    let containerWidth = 0
    // Use the PDF wrapper for width calculation as we now have the sidebar
    const wrapper = document.querySelector('.pdf-wrapper')
    if (wrapper) containerWidth = wrapper.clientWidth
    
    if (containerWidth > 0) {
      // Subtract padding (48px) and a small amount for scrollbar (4px)
      const availableWidth = containerWidth - 52
      pdfScale.value = availableWidth / viewport.width
    } else {
      pdfScale.value = 1.0 // Default fallback
    }
  } catch (e) {
    console.warn('Fit to width failed:', e)
    pdfScale.value = 1.0
  }
}

const renderPdf = async () => {
  if (!pdfDoc.value) return
  
  const canvas = pdfCanvasRef.value
  
  if (!canvas) return
  
  try {
    const page = await pdfDoc.value.getPage(pdfPage.value)
    const viewport = page.getViewport({ scale: pdfScale.value, rotation: pdfRotation.value })
    
    pdfWidth.value = viewport.width
    pdfHeight.value = viewport.height

    const context = canvas.getContext('2d')
    if (!context) return
    
    // High DPI rendering
    const outputScale = window.devicePixelRatio || 1
    canvas.width = Math.floor(viewport.width * outputScale)
    canvas.height = Math.floor(viewport.height * outputScale)
    canvas.style.width = Math.floor(viewport.width) + "px"
    canvas.style.height = Math.floor(viewport.height) + "px"
    
    const transform = outputScale !== 1 ? [outputScale, 0, 0, outputScale, 0, 0] : null
    
    await page.render({
      canvasContext: context,
      transform: transform,
      viewport: viewport
    }).promise
    
    // 渲染文本层
    if (pdfTextLayerRef.value) {
      const textContent = await page.getTextContent()
      const textLayerDiv = pdfTextLayerRef.value
      textLayerDiv.innerHTML = ''
      
      const pdfLib: any = pdfjsLib
      const textLayer = new pdfLib.TextLayer({
        textContentSource: textContent,
        container: textLayerDiv,
        viewport: viewport
      })
      await textLayer.render()
    }

    sessionStorage.setItem(`pdf_${paper.value.arxivId}`, JSON.stringify({
      page: pdfPage.value,
      scale: pdfScale.value,
      rotation: pdfRotation.value
    }))
  } catch (error) {
    console.error('渲染PDF失败:', error)
  }
}

const prevPage = () => pdfPage.value > 1 && (pdfPage.value--, renderPdf())
const nextPage = () => pdfPage.value < pdfPages.value && (pdfPage.value++, renderPdf())
const zoomIn = () => pdfScale.value < 3 && (pdfScale.value += 0.25, renderPdf())
const zoomOut = () => pdfScale.value > 0.5 && (pdfScale.value -= 0.25, renderPdf())


// AI 交互相关方法
const handleTextSelection = () => {
  const selection = window.getSelection()
  if (selection && selection.toString().trim().length > 0) {
    // 确保选区在PDF文本层内
    const range = selection.getRangeAt(0)
    
    // 简单的位置计算，实际可能需要更复杂的判断
    // 这里使用 clientX/Y，需要配合 fixed 定位
    const rect = range.getBoundingClientRect()
    
    selectedText.value = selection.toString()
    showFloatingMenu.value = true
    floatingMenuPosition.value = {
      top: rect.top - 50,
      left: rect.left + (rect.width / 2) - 100 // 居中
    }
  } else {
    // 延迟隐藏，防止点击菜单时消失
    setTimeout(() => {
      showFloatingMenu.value = false
    }, 200)
  }
}

const handleAiAction = (type: string) => {
  const text = selectedText.value
  if (!text) return
  
  showFloatingMenu.value = false
  
  let prompt = ''
  if (type === 'explain') {
    prompt = `请解释以下内容的含义：\n"${text}"`
  } else if (type === 'translate') {
    prompt = `请将以下内容翻译成中文：\n"${text}"`
  } else if (type === 'chat') {
    prompt = `关于这段内容：\n"${text}"\n我想问：`
    chatInput.value = prompt
    // 聚焦输入框
    nextTick(() => {
      const input = document.querySelector('.chat-input-area textarea') as HTMLTextAreaElement
      if (input) input.focus()
    })
    return
  }
  
  sendChatMessage(prompt)
}

const sendChatMessage = async (initialPrompt?: string) => {
  const msg = typeof initialPrompt === 'string' ? initialPrompt : chatInput.value
  if (!msg.trim() || chatLoading.value) return
  
  // 添加用户消息
  chatMessages.value.push({ role: 'user', content: msg })
  chatInput.value = ''
  
  // 滚动到底部
  scrollToBottom()
  
  chatLoading.value = true
  const aiMsgIndex = chatMessages.value.push({ role: 'ai', content: '' }) - 1
  
  try {
    const response = await chatApi.chatStream(msg, selectedText.value)
    const reader = response.body?.getReader()
    const decoder = new TextDecoder()
    
    if (reader) {
      while (true) {
        const { done, value } = await reader.read()
        if (done) break
        const chunk = decoder.decode(value)
        chatMessages.value[aiMsgIndex].content += chunk
        scrollToBottom()
      }
    }
  } catch (error) {
    chatMessages.value[aiMsgIndex].content += '\n(抱歉，AI响应出错，请稍后重试)'
  } finally {
    chatLoading.value = false
    scrollToBottom()
  }
}

const clearChat = () => {
  chatMessages.value = []
}

const scrollToBottom = () => {
  nextTick(() => {
    if (chatMessagesRef.value) {
      chatMessagesRef.value.scrollTop = chatMessagesRef.value.scrollHeight
    }
  })
}

// 监听标签页切换
watch(activeTab, async (newTab) => {
  if (newTab === 'original' && paper.value?.arxivId) {
    await nextTick()
    if (!pdfDoc.value) {
      await loadPdf()
    } else {
      // Re-calculate fit width when switching tabs as container size changes
      await fitToWidth()
      await renderPdf()
    }
  }
})

// 监听路由参数变化
watch(() => route.params.id, () => {
  if (route.params.id) {
    paper.value = null
    relatedPapers.value = []
    comments.value = []
    activeTab.value = 'overview'
    pdfDoc.value = null
    chatMessages.value = []
    fetchPaper()
  }
})

onMounted(() => {
  fetchPaper()
})
</script>

<style scoped>
/* 保持原有样式... */
.paper-detail-page {
  min-height: calc(100vh - 64px);
  background: #f8f9fa;
}

.detail-container {
  display: flex;
  max-width: 1400px;
  margin: 0 auto;
  padding: 20px;
  gap: 20px;
}

.detail-container.full-width {
  max-width: 100%;
  padding: 0;
  height: calc(100vh - 64px);
  overflow: hidden;
}

.detail-container.full-width .content-area {
  padding: 0;
  height: 100%;
  overflow: hidden;
}

.detail-container.full-width .ai-tab-content {
  height: 100%;
}

/* 左侧侧边栏 */
.sidebar {
  width: 80px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  gap: 20px;
  /* Make sidebar sticky or ensure it doesn't break layout */
}

/* Hide sidebar in full-width mode is handled by v-if in template */

.back-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  color: #6B5CE7;
  cursor: pointer;
  padding: 8px 12px;
  border-radius: 8px;
  transition: all 0.3s;
  font-size: 14px;
}

.back-btn:hover {
  background: #f0f0f0;
}

.sidebar-actions {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: 20px;
}

.sidebar-action {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: #fff;
  border: 1px solid #e0e0e0;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.3s;
  color: #666;
}

.sidebar-action:hover {
  border-color: #2563EB;
  color: #2563EB;
}

.sidebar-action.active {
  background: #2563EB;
  border-color: #2563EB;
  color: #fff;
}

/* 主内容区 */
.main-content {
  flex: 1;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

/* 顶部标签栏 */
.tab-navigation {
  display: flex;
  border-bottom: 1px solid #eee;
  padding: 0 24px;
  background: #fff;
}

.tab-item {
  padding: 16px 20px;
  color: #888;
  cursor: pointer;
  font-size: 14px;
  position: relative;
  transition: all 0.3s;
}

.tab-item:hover {
  color: #333;
}

.tab-item.active {
  color: #333;
  font-weight: 500;
}

.tab-item.active::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 2px;
  background: #2563EB;
}

/* 内容区域 */
.content-area {
  padding: 24px;
  flex: 1;
  display: flex;
  flex-direction: column;
}

.tab-content {
  animation: fadeIn 0.3s ease;
  flex: 1;
}

.ai-tab-content {
  padding: 0;
  height: calc(100vh - 150px);
  display: flex;
  flex-direction: column;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

/* 标题区 */
.paper-header-section {
  margin-bottom: 24px;
}

.paper-title {
  font-size: 24px;
  font-weight: 600;
  color: #1a1a1a;
  line-height: 1.4;
  margin: 0 0 16px 0;
}

.paper-authors-section {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  color: #666;
  font-size: 14px;
  margin-bottom: 12px;
}

.authors-text {
  line-height: 1.6;
}

.paper-category {
  margin-top: 8px;
}

.category-tag {
  display: inline-block;
  background: #2563EB;
  color: #fff;
  padding: 4px 12px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
}

/* 摘要区 */
.abstract-section {
  margin-bottom: 24px;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: #333;
  margin: 0 0 12px 0;
}

.abstract-text {
  color: #444;
  font-size: 14px;
  line-height: 1.8;
  text-align: justify;
}

/* 链接区 */
.links-section {
  display: flex;
  gap: 12px;
  margin-bottom: 24px;
}

.link-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  background: #f5f5f5;
  border-radius: 6px;
  color: #666;
  text-decoration: none;
  font-size: 13px;
  transition: all 0.3s;
}

.link-item:hover {
  background: #eee;
  color: #6B5CE7;
}

/* 互动统计 */
.stats-section {
  display: flex;
  gap: 24px;
  padding: 20px;
  background: #f9f9f9;
  border-radius: 8px;
  margin-bottom: 24px;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  padding: 6px 12px;
  border-radius: 6px;
  transition: all 0.3s;
}

.stat-item:hover {
  background: #f0f0f0;
}

.stat-item.active .stat-icon {
  color: #2563EB;
}

.stat-icon {
  font-size: 16px;
}

.stat-value {
  font-size: 14px;
  font-weight: 500;
  color: #333;
}

.stat-label {
  font-size: 12px;
  color: #999;
}

/* 评论区 */
.comments-section {
  border-top: 1px solid #eee;
  padding-top: 24px;
}

.comment-input {
  margin-bottom: 20px;
}

.submit-btn {
  margin-top: 12px;
  background: #2563EB;
  border-color: #2563EB;
}

.submit-btn:hover {
  background: #1d4ed8;
  border-color: #1d4ed8;
}

.comment-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.comment-item {
  padding: 16px;
  background: #f9f9f9;
  border-radius: 8px;
}

.comment-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
}

.comment-username {
  font-weight: 500;
  color: #333;
  font-size: 14px;
}

.comment-time {
  color: #999;
  font-size: 12px;
}

.comment-content {
  color: #666;
  font-size: 14px;
  line-height: 1.6;
}

.no-comments {
  text-align: center;
  color: #999;
  padding: 20px;
  font-size: 14px;
}

/* PDF预览 */
.pdf-controls {
  display: flex;
  justify-content: center;
  gap: 16px;
  padding: 12px;
  background: #f5f5f5;
  border-radius: 8px;
  margin-bottom: 16px;
}

.pdf-controls.compact {
  padding: 8px 16px;
  margin-bottom: 0;
  background: #fff;
  border-bottom: 1px solid #eee;
  display: flex;
  justify-content: space-between;
  align-items: center;
  box-shadow: 0 2px 4px rgba(0,0,0,0.05);
  z-index: 10;
}

.left-controls {
  display: flex;
  align-items: center;
  gap: 12px;
}

.pdf-container {
  overflow: auto;
  padding: 0;
  background: #525659;
  display: flex;
  justify-content: center;
  border-radius: 8px;
  min-height: 600px;
}

/* AI阅读布局 */
.ai-layout {
  display: flex;
  height: 100%;
  overflow: hidden;
}

.ai-pdf-section {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: #f2f4f7; /* Light gray background like Moonlight */
  position: relative;
  overflow: hidden;
}

.pdf-wrapper {
  flex: 1;
  overflow: auto;
  display: flex;
  justify-content: center;
  padding: 24px; /* Add some padding around the PDF page */
  background: #f2f4f7;
}

.pdf-render-container {
  position: relative;
  box-shadow: 0 0 16px rgba(0,0,0,0.1); /* Softer shadow */
  background: #fff;
}

/* Remove manual textLayer styles as we now import pdf_viewer.css */

.floating-menu {
  position: fixed;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 4px 16px rgba(0,0,0,0.15);
  display: flex;
  padding: 6px;
  gap: 4px;
  z-index: 1000;
  animation: fadeIn 0.2s ease;
}

.menu-item {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 6px 12px;
  border-radius: 6px;
  cursor: pointer;
  font-size: 13px;
  color: #333;
  transition: all 0.2s;
}

.menu-item:hover {
  background: #f0f0f0;
  color: #2563EB;
}

.ai-chat-section {
  width: 350px;
  background: #fff;
  border-left: 1px solid #eee;
  display: flex;
  flex-direction: column;
}

.chat-header {
  padding: 16px;
  border-bottom: 1px solid #eee;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.chat-header h3 {
  margin: 0;
  font-size: 16px;
  color: #333;
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.chat-welcome {
  text-align: center;
  color: #888;
  font-size: 14px;
  margin-top: 40px;
}

.message-item {
  display: flex;
  gap: 10px;
}

.message-item.user {
  flex-direction: row-reverse;
}

.message-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: #f0f0f0;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #666;
  flex-shrink: 0;
}

.message-item.ai .message-avatar {
  background: #e6f0ff;
  color: #2563EB;
}

.message-item.user .message-avatar {
  background: #f0f0f0;
  color: #666;
}

.message-content {
  background: #f5f5f5;
  padding: 10px 14px;
  border-radius: 10px;
  font-size: 14px;
  line-height: 1.6;
  max-width: 80%;
  word-wrap: break-word;
}

.message-item.ai .message-content {
  background: #fff;
  border: 1px solid #eee;
}

.message-item.user .message-content {
  background: #2563EB;
  color: #fff;
}

.loading span {
  display: inline-block;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #ccc;
  margin: 0 2px;
  animation: bounce 1.4s infinite ease-in-out both;
}

.loading span:nth-child(1) { animation-delay: -0.32s; }
.loading span:nth-child(2) { animation-delay: -0.16s; }

@keyframes bounce {
  0%, 80%, 100% { transform: scale(0); }
  40% { transform: scale(1); }
}

.chat-input-area {
  padding: 16px;
  border-top: 1px solid #eee;
}

.input-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 8px;
}

/* 相关资料/解读 */
.resources-empty {
  padding: 60px 0;
}

/* 右侧辅助区 */
.right-sidebar {
  width: 280px;
  flex-shrink: 0;
}

.related-papers {
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
  padding: 20px;
}

.sidebar-title {
  font-size: 14px;
  font-weight: 600;
  color: #333;
  margin: 0 0 16px 0;
  padding-bottom: 12px;
  border-bottom: 1px solid #eee;
}

.related-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.related-item {
  padding: 12px;
  background: #f9f9f9;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s;
}

.related-item:hover {
  background: #f0f0f0;
}

.related-title {
  font-size: 14px;
  color: #333;
  line-height: 1.4;
  margin-bottom: 4px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.related-meta {
  font-size: 12px;
  color: #999;
}
</style>
