<template>
  <div class="home-page">
    <!-- 标题区域 -->
    <div class="section-header">
      <h1 class="section-title">前沿论文</h1>
    </div>

    <!-- 搜索和筛选区域 -->
    <div class="filter-section">
      <div class="search-box">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索论文标题、作者..."
          size="large"
          clearable
          @keyup.enter="handleSearch"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
          <template #append>
            <el-button @click="handleSearch">搜索</el-button>
          </template>
        </el-input>
      </div>

      <div class="category-filter">
        <el-select
          v-model="selectedCategory"
          placeholder="选择分类"
          clearable
          size="large"
          @change="handleCategoryChange"
        >
          <el-option
            v-for="cat in categories"
            :key="cat.value"
            :label="cat.label"
            :value="cat.value"
          />
        </el-select>

        <el-checkbox v-model="onlyHasGithub" @change="handleSearch">
          仅显示有代码的论文
        </el-checkbox>
      </div>
    </div>

    <!-- 论文列表 -->
    <div 
      class="paper-list" 
      v-loading="loading && papers.length === 0"
      v-infinite-scroll="loadMore"
      :infinite-scroll-disabled="disabled"
      :infinite-scroll-distance="20"
      :infinite-scroll-immediate="false"
    >
      <template v-if="papers.length > 0">
        <div
          v-for="(paper, index) in papers"
          :key="paper.arxivId"
          class="paper-card"
        >
          <!-- 左侧区域：编号 + 缩略图 -->
          <div class="paper-left">
            <div class="paper-index">{{ String(index + 1).padStart(2, '0') }}</div>
            <div class="paper-thumbnail">
              <img v-if="paper.thumbnailUrl" :src="paper.thumbnailUrl" alt="论文配图" />
              <div v-else class="thumbnail-placeholder">
                <el-icon :size="40"><Document /></el-icon>
              </div>
            </div>
          </div>

          <!-- 右侧内容区 -->
          <div class="paper-content" @click="goToDetail(paper.arxivId)">
            <div class="paper-header">
              <h3 class="paper-title">{{ paper.title }}</h3>
            </div>

            <div class="paper-authors">
              <el-icon><User /></el-icon>
              {{ formatAuthors(paper.authors) }}
            </div>

            <div class="paper-abstract">
              {{ paper.summary.substring(0, 200) }}{{ paper.summary.length > 200 ? '...' : '' }}
            </div>

            <!-- 元数据区 -->
            <div class="paper-footer">
              <div class="paper-meta">
                <span class="paper-tag">{{ paper.primaryCategory }}</span>
                <span class="publish-date">
                  发布于 {{ formatDate(paper.publishedDate) }}
                </span>
                <span class="hot-value" v-if="paper.hotValue">
                  🔥 {{ paper.hotValue }}
                </span>
              </div>

              <!-- 互动区 -->
              <div class="interaction-area">
                <span class="interaction-item" v-if="paper.githubUrl" @click.stop="openGithub(paper.githubUrl)" title="查看代码">
                   <svg height="16" aria-hidden="true" viewBox="0 0 16 16" version="1.1" width="16" data-view-component="true" class="icon" fill="currentColor">
                      <path d="M8 0c4.42 0 8 3.58 8 8a8.013 8.013 0 0 1-5.45 7.59c-.4.08-.55-.17-.55-.38 0-.27.01-1.13.01-2.2 0-.75-.25-1.23-.54-1.48 1.78-.2 3.65-.88 3.65-3.95 0-.88-.31-1.59-.82-2.15.08-.2.36-1.02-.08-2.12 0 0-.67-.22-2.2.82-.64-.18-1.32-.27-2-.27-.68 0-1.36.09-2 .27-1.53-1.03-2.2-.82-2.2-.82-.44 1.1-.16 1.92-.08 2.12-.51.56-.82 1.28-.82 2.15 0 3.06 1.86 3.75 3.64 3.95-.23.2-.44.55-.51 1.07-.46.46-1.61.55-2.33-.66-.15-.24-.6-.83-1.23-.82-.67.01-.27.38.01.53.34.19.73.9.82 1.13.16.45.68 1.31 2.69.94 0 .67.01 1.3.01 1.49 0 .21-.15.45-.55.38A7.995 7.995 0 0 1 0 8c0-4.42 3.58-8 8-8Z"></path>
                   </svg>
                </span>
                <span class="interaction-item" @click.stop="handleShare(paper)">
                  <span class="icon">👥</span>
                  <span class="count">{{ paper.viewCount || 0 }}</span>
                </span>
                <span
                  class="interaction-item"
                  :class="{ active: paper.isCollected }"
                  @click.stop="handleCollect(paper)"
                >
                  <span class="icon">⭐</span>
                  <span class="count">{{ paper.collectCount || 0 }}</span>
                </span>
                <span
                  class="interaction-item"
                  :class="{ active: paper.isLiked }"
                  @click.stop="handleLike(paper)"
                >
                  <span class="icon">❤️</span>
                  <span class="count">{{ paper.likeCount || 0 }}</span>
                </span>
                <span class="interaction-item" @click.stop="goToDetail(paper.arxivId)">
                  <span class="icon">💬</span>
                  <span class="count">{{ paper.commentCount || 0 }}</span>
                </span>
              </div>
            </div>
          </div>
        </div>
        <p v-if="loading" class="loading-text">加载中...</p>
        <p v-if="noMore" class="no-more-text">没有更多了</p>
      </template>

      <el-empty v-else-if="!loading && papers.length === 0" description="暂无论文数据" />
    </div>

    <!-- 分页已移除，改为无限滚动 -->

    <!-- 右侧浮动按钮 -->
    <div class="float-buttons">
      <div class="float-btn" @click="showAddDialog = true" title="添加论文">
        <el-icon><Plus /></el-icon>
      </div>
      <div class="float-btn" @click="goToUserCenter" title="个人中心">
        <el-icon><User /></el-icon>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { paperApi, userApi, categories } from '@/api'
import { useUserStore } from '@/stores/user'
import type { ArxivPaper } from '@/types'

const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const papers = ref<ArxivPaper[]>([])
const searchKeyword = ref('')
const selectedCategory = ref('')
const onlyHasGithub = ref(false)
const currentPage = ref(1)
const pageSize = ref(20)
const total = ref(0)
const showAddDialog = ref(false)
const isFinished = ref(false)

const noMore = computed(() => {
  return isFinished.value || (papers.value.length >= total.value && total.value > 0 && !loading.value)
})

const disabled = computed(() => loading.value || noMore.value)

// 获取今天的日期，格式：YYYY-MM-DD
const getTodayDate = () => {
  const now = new Date()
  const year = now.getFullYear()
  const month = String(now.getMonth() + 1).padStart(2, '0')
  const day = String(now.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

// 获取7天前的日期，格式：YYYY-MM-DD
const getLast7DaysDate = () => {
  const now = new Date()
  now.setDate(now.getDate() - 6) // 7天包括今天，所以往前推6天
  const year = now.getFullYear()
  const month = String(now.getMonth() + 1).padStart(2, '0')
  const day = String(now.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

// 获取论文列表
const fetchPapers = async (append = false) => {
  loading.value = true
  try {
    let res
    if (searchKeyword.value) {
      res = await paperApi.searchPapers(
        searchKeyword.value,
        currentPage.value,
        pageSize.value,
        onlyHasGithub.value || undefined
      )
    } else if (selectedCategory.value) {
      res = await paperApi.getPapersByCategory(
        selectedCategory.value,
        currentPage.value,
        pageSize.value,
        onlyHasGithub.value || undefined
      )
    } else {
      // 默认查询近7天的论文
      const today = getTodayDate()
      const last7Days = getLast7DaysDate()
      res = await paperApi.getPapers({
        startDate: last7Days,
        endDate: today,
        page: currentPage.value,
        size: pageSize.value,
        hasGithub: onlyHasGithub.value || undefined
      })
    }

    if (res.data.code === 200) {
      // 兼容 content 和 records
      // @ts-ignore
      const newPapers = res.data.data.content || res.data.data.records || []
      
      if (newPapers.length === 0) {
        isFinished.value = true
      }
      
      if (append) {
        papers.value = [...papers.value, ...newPapers]
      } else {
        papers.value = newPapers
      }
      
      // 如果返回的数据少于pageSize，说明已经是最后一页
      if (newPapers.length < pageSize.value) {
        isFinished.value = true
      } else {
        isFinished.value = false
      }

      total.value = res.data.data.totalElements || res.data.data.total || 0
      
      // 如果总数不为0但返回空，也视为结束
      if (total.value > 0 && newPapers.length === 0) {
        isFinished.value = true
      }
    }
  } catch (error) {
    console.error('获取论文列表失败:', error)
    isFinished.value = true // 出错也停止加载
  } finally {
    loading.value = false
  }
}

const loadMore = () => {
  if (disabled.value || isFinished.value) return
  currentPage.value++
  fetchPapers(true)
}

// 搜索
const handleSearch = () => {
  currentPage.value = 1
  papers.value = [] // 清空列表
  isFinished.value = false
  fetchPapers()
}

// 分类筛选
const handleCategoryChange = () => {
  searchKeyword.value = ''
  currentPage.value = 1
  papers.value = [] // 清空列表
  isFinished.value = false
  fetchPapers()
}

// 打开GitHub链接
const openGithub = (url: string) => {
  window.open(url, '_blank')
}

// 跳转详情
const goToDetail = (arxivId: string) => {
  router.push(`/paper/${arxivId}`)
}

// 收藏论文
const handleCollect = async (paper: ArxivPaper) => {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }

  try {
    if (paper.isCollected) {
      await userApi.uncollectPaper(paper.arxivId)
      paper.isCollected = false
      if (paper.collectCount) paper.collectCount--
      ElMessage.success('已取消收藏')
    } else {
      await userApi.collectPaper(paper.arxivId)
      paper.isCollected = true
      if (paper.collectCount) paper.collectCount++
      ElMessage.success('收藏成功')
    }
  } catch (error) {
    console.error('收藏操作失败:', error)
  }
}

// 点赞论文
const handleLike = async (paper: ArxivPaper) => {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }

  try {
    if (paper.isLiked) {
      await userApi.unlikePaper(paper.arxivId)
      paper.isLiked = false
      if (paper.likeCount) paper.likeCount--
      ElMessage.success('已取消点赞')
    } else {
      await userApi.likePaper(paper.arxivId)
      paper.isLiked = true
      if (paper.likeCount) paper.likeCount++
      ElMessage.success('点赞成功')
    }
  } catch (error) {
    console.error('点赞操作失败:', error)
  }
}

// 分享论文
const handleShare = (paper: ArxivPaper) => {
  const url = window.location.origin + `/paper/${paper.arxivId}`
  if (navigator.clipboard) {
    navigator.clipboard.writeText(url).then(() => {
      ElMessage.success('链接已复制到剪贴板')
    })
  } else {
    // 兼容旧浏览器
    const input = document.createElement('input')
    input.value = url
    document.body.appendChild(input)
    input.select()
    document.execCommand('copy')
    document.body.removeChild(input)
    ElMessage.success('链接已复制到剪贴板')
  }
}

// 跳转到个人中心
const goToUserCenter = () => {
  if (userStore.isLoggedIn) {
    // 已登录可以跳转到我感兴趣的页面
    router.push('/collect')
  } else {
    router.push('/login')
  }
}

// 格式化日期
const formatDate = (dateStr: string) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return date.toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit'
  })
}

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

onMounted(() => {
  fetchPapers()
})
</script>

<style scoped>
.home-page {
  display: flex;
  flex-direction: column;
  gap: 24px;
  position: relative;
}

.loading-text,
.no-more-text {
  text-align: center;
  padding: 10px;
  color: #999;
  font-size: 14px;
}
.section-header {
  text-align: center;
  padding: 20px 0;
}

.section-title {
  font-size: 28px;
  font-weight: 700;
  color: #1a1a1a;
  margin: 0 0 8px 0;
}

.section-subtitle {
  font-size: 14px;
  color: #999;
  margin: 0;
  letter-spacing: 2px;
}

/* 搜索和筛选区域 */
.filter-section {
  background: #fff;
  padding: 20px 24px;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
}

.search-box {
  margin-bottom: 16px;
}

.search-box :deep(.el-input-group__append) {
  background: #1a1a1a;
  border-color: #1a1a1a;
  color: #fff;
}

.category-filter {
  display: flex;
  align-items: center;
  gap: 16px;
}

.category-filter .el-select {
  width: 200px;
}

/* 论文列表 */
.paper-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.paper-card {
  display: flex;
  gap: 16px;
  background: #fff;
  padding: 24px;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
  transition: all 0.3s;
}

.paper-card:hover {
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
  transform: translateY(-2px);
}

/* 左侧区域 */
.paper-left {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  min-width: 100px;
}

.paper-index {
  font-size: 36px;
  font-weight: 700;
  color: #e91e63;
  opacity: 0.8;
  line-height: 1;
}

.paper-thumbnail {
  width: 80px;
  height: 80px;
  border-radius: 8px;
  overflow: hidden;
  background: #f5f5f5;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.paper-thumbnail img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.thumbnail-placeholder {
  color: #ccc;
}

/* 右侧内容区 */
.paper-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 12px;
  cursor: pointer;
}

.paper-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
}

.paper-title {
  font-size: 18px;
  font-weight: 600;
  color: #1a1a1a;
  margin: 0;
  line-height: 1.4;
  flex: 1;
}

.paper-tags {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}

.paper-authors {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #666;
  font-size: 13px;
}

.paper-abstract {
  color: #666;
  font-size: 14px;
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

/* 元数据区 */
.paper-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 8px;
  flex-wrap: wrap;
  gap: 12px;
}

.paper-meta {
  display: flex;
  align-items: center;
  gap: 12px;
}

.paper-tag {
  background: #2563EB;
  color: #fff;
  padding: 4px 10px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
}

.publish-date {
  color: #999;
  font-size: 13px;
}

.hot-value {
  color: #ff6b35;
  font-size: 13px;
  font-weight: 500;
}

/* 互动区 */
.interaction-area {
  display: flex;
  align-items: center;
  gap: 16px;
}

.interaction-item {
  display: flex;
  align-items: center;
  gap: 4px;
  color: #999;
  font-size: 13px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 4px;
  transition: all 0.2s;
}

.interaction-item:hover {
  background: #f5f5f5;
}

.interaction-item.active {
  color: #6B5CE7;
}

.interaction-item .icon {
  font-size: 16px;
  display: flex;
  align-items: center;
}

.interaction-item svg {
  width: 16px;
  height: 16px;
}

.interaction-item .count {
  min-width: 16px;
}

/* 分页 - 已移除 */

/* 右侧浮动按钮 */
.float-buttons {
  position: fixed;
  right: 40px;
  top: 50%;
  transform: translateY(-50%);
  display: flex;
  flex-direction: column;
  gap: 16px;
  z-index: 100;
}

.float-btn {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: #fff;
  border: 2px solid #e91e63;
  color: #e91e63;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.3s;
  box-shadow: 0 2px 8px rgba(233, 30, 99, 0.2);
}

.float-btn:hover {
  background: #e91e63;
  color: #fff;
  transform: scale(1.1);
}

.float-btn .el-icon {
  font-size: 20px;
}
</style>
