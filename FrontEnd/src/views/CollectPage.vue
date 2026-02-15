<template>
  <div class="collect-page">
    <div class="page-header">
      <h2>收藏板块</h2>
      <p class="subtitle">浏览您收藏的论文</p>
    </div>
    
    <!-- 收藏的论文列表 -->
    <div class="paper-list" v-loading="loading">
      <template v-if="collectedPapers.length > 0">
        <div
          v-for="(paper, index) in collectedPapers"
          :key="paper.arxivId"
          class="paper-card"
          @click="goToDetail(paper.arxivId)"
        >
          <div class="paper-index">{{ String(index + 1).padStart(2, '0') }}</div>
          <div class="paper-content">
            <div class="paper-header">
              <h3 class="paper-title">{{ paper.title }}</h3>
              <div class="paper-tags">
                <el-tag size="small" type="primary">{{ paper.primaryCategory }}</el-tag>
                <a v-if="paper.githubUrl" :href="paper.githubUrl" target="_blank" class="github-link" title="查看代码">
                  <svg height="16" aria-hidden="true" viewBox="0 0 16 16" version="1.1" width="16" data-view-component="true" fill="currentColor">
                    <path d="M8 0c4.42 0 8 3.58 8 8a8.013 8.013 0 0 1-5.45 7.59c-.4.08-.55-.17-.55-.38 0-.27.01-1.13.01-2.2 0-.75-.25-1.23-.54-1.48 1.78-.2 3.65-.88 3.65-3.95 0-.88-.31-1.59-.82-2.15.08-.2.36-1.02-.08-2.12 0 0-.67-.22-2.2.82-.64-.18-1.32-.27-2-.27-.68 0-1.36.09-2 .27-1.53-1.03-2.2-.82-2.2-.82-.44 1.1-.16 1.92-.08 2.12-.51.56-.82 1.28-.82 2.15 0 3.06 1.86 3.75 3.64 3.95-.23.2-.44.55-.51 1.07-.46.46-1.61.55-2.33-.66-.15-.24-.6-.83-1.23-.82-.67.01-.27.38.01.53.34.19.73.9.82 1.13.16.45.68 1.31 2.69.94 0 .67.01 1.3.01 1.49 0 .21-.15.45-.55.38A7.995 7.995 0 0 1 0 8c0-4.42 3.58-8 8-8Z"></path>
                  </svg>
                </a>
              </div>
            </div>
            
            <div class="paper-authors">
              <el-icon><User /></el-icon>
              {{ formatAuthors(paper.authors) }}
            </div>
            
            <div class="paper-abstract">
              {{ paper.summary.substring(0, 200) }}{{ paper.summary.length > 200 ? '...' : '' }}
            </div>
            
            <div class="paper-footer">
              <div class="paper-meta">
                <span class="date">
                  <el-icon><Calendar /></el-icon>
                  {{ formatDate(paper.publishedDate) }}
                </span>
                <span v-if="paper.likeCount" class="stat-item">
                  <el-icon><Star /></el-icon>
                  {{ paper.likeCount }}
                </span>
              </div>
              
              <div class="paper-actions">
                <el-button
                  type="danger"
                  size="small"
                  @click.stop="handleUncollect(paper)"
                >
                  <el-icon><Delete /></el-icon>
                  取消收藏
                </el-button>
                <el-button
                  type="primary"
                  size="small"
                  @click.stop="goToDetail(paper.arxivId)"
                >
                  查看详情
                </el-button>
              </div>
            </div>
          </div>
        </div>
      </template>
      
      <el-empty v-else-if="!loading && userStore.isLoggedIn" description="暂无收藏的论文">
        <el-button type="primary" @click="$router.push('/')">去浏览论文</el-button>
      </el-empty>
    </div>
    
    <!-- 分页 -->
    <div class="pagination" v-if="total > 0">
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 30]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleSizeChange"
        @current-change="handlePageChange"
      >
        <template #total>
          <span>共 {{ total }} 条</span>
        </template>
        <template #sizes>
          <span>{{ pageSize }} 条/页</span>
        </template>
      </el-pagination>
    </div>
    
    <!-- 未登录提示 -->
    <el-empty
      v-if="!userStore.isLoggedIn && !loading"
      description="登录后可查看收藏的论文"
    >
      <el-button type="primary" @click="$router.push('/login')">去登录</el-button>
    </el-empty>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { userApi } from '@/api'
import { useUserStore } from '@/stores/user'
import type { ArxivPaper } from '@/types'

const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const collectedPapers = ref<ArxivPaper[]>([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

// 获取收藏的论文
const fetchCollectedPapers = async () => {
  if (!userStore.isLoggedIn) {
    loading.value = false
    return
  }
  
  loading.value = true
  try {
    const res = await userApi.getCollectedPapers(currentPage.value, pageSize.value)
    if (res.data.code === 200) {
      collectedPapers.value = res.data.data.content || res.data.data.records || []
      total.value = res.data.data.totalElements || res.data.data.total || 0
    }
  } catch (error) {
    console.error('获取收藏论文失败:', error)
  } finally {
    loading.value = false
  }
}

// 取消收藏
const handleUncollect = async (paper: ArxivPaper) => {
  try {
    await userApi.uncollectPaper(paper.arxivId)
    collectedPapers.value = collectedPapers.value.filter(p => p.arxivId !== paper.arxivId)
    total.value--
    ElMessage.success('已取消收藏')
  } catch (error) {
    console.error('取消收藏失败:', error)
  }
}

// 分页
const handlePageChange = () => {
  fetchCollectedPapers()
}

const handleSizeChange = () => {
  currentPage.value = 1
  fetchCollectedPapers()
}

// 跳转详情
const goToDetail = (arxivId: string) => {
  router.push(`/paper/${arxivId}`)
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
  fetchCollectedPapers()
})
</script>

<style scoped>
.collect-page {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.page-header {
  text-align: center;
}

.page-header h2 {
  font-size: 28px;
  color: #1a1a1a;
  margin: 0 0 8px 0;
}

.subtitle {
  color: #666;
  font-size: 14px;
}

/* 论文列表 */
.paper-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.paper-card {
  display: flex;
  gap: 20px;
  background: #fff;
  padding: 24px;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
  cursor: pointer;
  transition: all 0.3s;
}

.paper-card:hover {
  box-shadow: 0 4px 20px rgba(74, 144, 226, 0.15);
  transform: translateY(-2px);
}

.paper-index {
  font-size: 32px;
  font-weight: 700;
  color: #4A90E2;
  opacity: 0.3;
  min-width: 50px;
  line-height: 1;
}

.paper-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 12px;
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
  align-items: center;
}

.github-link {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border-radius: 4px;
  background: #24292f;
  color: #fff;
  transition: all 0.3s;
}

.github-link:hover {
  background: #57606a;
}

.paper-authors {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #666;
  font-size: 14px;
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

.paper-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 8px;
}

.paper-meta {
  display: flex;
  align-items: center;
  gap: 16px;
  color: #999;
  font-size: 13px;
}

.paper-meta .date,
.paper-meta .stats {
  display: flex;
  align-items: center;
  gap: 4px;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 4px;
}

.stat-item + .stat-item {
  margin-left: 12px;
}

.paper-actions {
  display: flex;
  gap: 8px;
}

/* 分页 */
.pagination {
  display: flex;
  justify-content: center;
  padding: 20px 0;
}
</style>
