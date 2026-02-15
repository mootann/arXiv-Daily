<template>
  <div class="follow-page">
    <div class="page-header">
      <h2>关注板块</h2>
      <p class="subtitle">查看您关注分类的最新论文</p>
    </div>

    <!-- 未登录提示 -->
    <el-empty
      v-if="!userStore.isLoggedIn"
      description="请先登录以使用关注功能"
    >
      <el-button type="primary" @click="$router.push('/login')">去登录</el-button>
    </el-empty>

    <div v-else class="follow-content">
      <!-- 侧边栏：关注列表 -->
      <div class="sidebar">
        <div class="sidebar-header">
          <h3>我的关注</h3>
          <el-button type="primary" link @click="showManage = true">
            <el-icon><Setting /></el-icon> 管理
          </el-button>
        </div>
        
        <div v-loading="loadingCategories" class="category-list">
          <div
            v-for="item in followedCategories"
            :key="item.category"
            class="category-item"
            :class="{ active: activeCategory === item.category }"
            @click="selectCategory(item)"
          >
            <span class="category-name">{{ getCategoryLabel(item.category) }}</span>
            <el-badge
              v-if="item.paperCount && item.paperCount > 0"
              :value="item.paperCount"
              class="count-badge"
              type="danger"
            />
            <span v-else class="no-update">无更新</span>
          </div>
          
          <div v-if="followedCategories.length === 0 && !loadingCategories" class="empty-follow">
            <p>暂无关注分类</p>
            <el-button type="primary" size="small" @click="showManage = true">去添加</el-button>
          </div>
        </div>
      </div>

      <!-- 主内容区：论文列表 -->
      <div class="main-content">
        <div v-if="activeCategory" class="paper-section">
          <div class="section-header">
            <h3>{{ getCategoryLabel(activeCategory) }} ({{ activeCategory }}) - {{ displayDate ? displayDate + ' 更新' : '暂无数据' }}</h3>
            <div class="header-right">
              <el-switch
                v-model="filterHasGithub"
                active-text="仅显示含Github代码"
                @change="handleGithubFilterChange"
                style="margin-right: 15px; --el-switch-on-color: #2563EB;"
              />
              <span v-if="displayDate" class="date-tag">{{ displayDate }}</span>
            </div>
          </div>
          
          <div v-loading="loadingPapers" class="paper-list">
            <el-empty v-if="papers.length === 0 && !loadingPapers" description="该分类暂无论文数据" />
            
            <div
              v-for="(paper, index) in papers"
              :key="paper.arxivId"
              class="paper-card"
              @click="goToDetail(paper.arxivId)"
            >
              <div class="paper-index">{{ String((currentPage - 1) * pageSize + index + 1).padStart(2, '0') }}</div>
              <div class="paper-content">
                <div class="paper-header-row">
                  <h3 class="paper-title" v-html="highlightKeyword(paper.title)"></h3>
                  <div class="paper-tags">
                    <el-tag size="small" effect="plain">{{ paper.primaryCategory }}</el-tag>
                    <el-tag v-if="paper.pdfUrl" size="small" type="success">PDF</el-tag>
                    <a 
                      v-if="paper.githubUrl" 
                      :href="paper.githubUrl" 
                      target="_blank" 
                      @click.stop
                      class="github-link"
                      title="查看代码"
                    >
                      <svg height="16" aria-hidden="true" viewBox="0 0 16 16" version="1.1" width="16" data-view-component="true" class="icon" fill="currentColor">
                        <path d="M8 0c4.42 0 8 3.58 8 8a8.013 8.013 0 0 1-5.45 7.59c-.4.08-.55-.17-.55-.38 0-.27.01-1.13.01-2.2 0-.75-.25-1.23-.54-1.48 1.78-.2 3.65-.88 3.65-3.95 0-.88-.31-1.59-.82-2.15.08-.2.36-1.02-.08-2.12 0 0-.67-.22-2.2.82-.64-.18-1.32-.27-2-.27-.68 0-1.36.09-2 .27-1.53-1.03-2.2-.82-2.2-.82-.44 1.1-.16 1.92-.08 2.12-.51.56-.82 1.28-.82 2.15 0 3.06 1.86 3.75 3.64 3.95-.23.2-.44.55-.51 1.07-.46.46-1.61.55-2.33-.66-.15-.24-.6-.83-1.23-.82-.67.01-.27.38.01.53.34.19.73.9.82 1.13.16.45.68 1.31 2.69.94 0 .67.01 1.3.01 1.49 0 .21-.15.45-.55.38A7.995 7.995 0 0 1 0 8c0-4.42 3.58-8 8-8Z"></path>
                      </svg>
                    </a>
                  </div>
                </div>
                <div class="paper-authors">{{ formatAuthors(paper.authors) }}</div>
                <div class="paper-abstract">
                  {{ paper.summary.length > 200 ? paper.summary.substring(0, 200) + '...' : paper.summary }}
                </div>
                <div class="paper-footer">
                  <span class="info-item">
                    <el-icon><Calendar /></el-icon>
                    {{ formatDate(paper.publishedDate) }}
                  </span>
                  <div class="interactions">
                    <span class="info-item">
                      <el-icon><View /></el-icon> {{ paper.viewCount || 0 }}
                    </span>
                    <span class="info-item">
                      <el-icon><Star /></el-icon> {{ paper.collectCount || 0 }}
                    </span>
                    <span class="info-item">
                      <el-icon><ChatLineRound /></el-icon> {{ paper.commentCount || 0 }}
                    </span>
                  </div>
                </div>
              </div>
            </div>
          </div>
          
          <!-- 分页组件 -->
          <div v-if="papers.length > 0" class="pagination-container">
            <el-pagination
              v-model:current-page="currentPage"
              v-model:page-size="pageSize"
              :page-sizes="[10, 20, 50, 100]"
              layout="total, sizes, prev, pager, next, jumper"
              :total="totalPapers"
              @size-change="handleSizeChange"
              @current-change="handleCurrentChange"
            />
          </div>
        </div>
        <el-empty v-else description="请选择左侧分类查看论文" />
      </div>
    </div>

    <!-- 管理关注弹窗 -->
    <el-dialog
      v-model="showManage"
      title="管理关注分类"
      width="80%"
      destroy-on-close
    >
      <div class="manage-grid">
        <div
          v-for="cat in allCategories"
          :key="cat.value"
          class="manage-card"
          :class="{ active: isFollowed(cat.value) }"
          @click="toggleFollow(cat.value)"
        >
          <div class="card-header">
            <span class="code">{{ cat.value }}</span>
            <el-icon v-if="isFollowed(cat.value)" class="check-icon"><Select /></el-icon>
          </div>
          <div class="name">{{ cat.label }}</div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { userApi, paperApi, categories } from '@/api'
import type { UserFollowCategoryDTO, ArxivPaper } from '@/types'
import { ElMessage } from 'element-plus'
import { Calendar, View, Star, ChatLineRound, Setting, Select } from '@element-plus/icons-vue'
import dayjs from 'dayjs'

const router = useRouter()
const userStore = useUserStore()

const loadingCategories = ref(false)
const loadingPapers = ref(false)
const followedCategories = ref<UserFollowCategoryDTO[]>([])
const papers = ref<ArxivPaper[]>([])
const activeCategory = ref('')
const showManage = ref(false)
const displayDate = ref('')
const filterHasGithub = ref(false)

// 分页相关
const currentPage = ref(1)
const pageSize = ref(10)
const totalPapers = ref(0)

const allCategories = categories
const currentDate = dayjs().format('YYYY-MM-DD')

// 获取关注列表
const fetchFollowedCategories = async () => {
  if (!userStore.isLoggedIn) return
  
  loadingCategories.value = true
  try {
    const res = await userApi.getFollowCategories()
    if (res.data.code === 200) {
      followedCategories.value = res.data.data
      // 如果没有选中的分类且有关注列表，默认选中第一个
      if (!activeCategory.value && followedCategories.value.length > 0) {
        selectCategory(followedCategories.value[0])
      } else if (followedCategories.value.length === 0) {
        activeCategory.value = ''
        papers.value = []
        // 如果没有关注，自动弹出管理界面
        // showManage.value = true
      }
    }
  } catch (error) {
    console.error('获取关注列表失败', error)
    ElMessage.error('获取关注列表失败')
  } finally {
    loadingCategories.value = false
  }
}

// 选择分类
const selectCategory = async (item: UserFollowCategoryDTO) => {
  activeCategory.value = item.category
  currentPage.value = 1 // 切换分类重置页码
  // 如果有最新日期，使用最新日期；否则默认使用今天（虽然可能无数据）
  const dateToQuery = item.latestPaperDate || currentDate
  displayDate.value = item.latestPaperDate || ''
  await fetchPapers(item.category, dateToQuery)
}

// 处理Github筛选变化
const handleGithubFilterChange = async () => {
  if (activeCategory.value) {
    currentPage.value = 1 // 筛选条件变化重置页码
    // 重新获取论文，使用当前显示的日期
    const dateToQuery = displayDate.value || currentDate
    await fetchPapers(activeCategory.value, dateToQuery)
  }
}

// 获取论文
const fetchPapers = async (category: string, date: string) => {
  loadingPapers.value = true
  try {
    // 查询指定日期的该分类论文
    const res = await paperApi.getPapers({
      page: currentPage.value,
      size: pageSize.value,
      category: [category],
      startDate: date,
      endDate: date,
      hasGithub: filterHasGithub.value ? true : undefined
    })
    
    if (res.data.code === 200 && res.data.data.records) {
      papers.value = res.data.data.records
      totalPapers.value = res.data.data.total || 0
    } else {
      papers.value = []
      totalPapers.value = 0
    }
  } catch (error) {
    console.error('获取论文失败', error)
    ElMessage.error('获取论文列表失败')
  } finally {
    loadingPapers.value = false
  }
}

// 分页处理
const handleSizeChange = (val: number) => {
  pageSize.value = val
  currentPage.value = 1
  if (activeCategory.value) {
    const dateToQuery = displayDate.value || currentDate
    fetchPapers(activeCategory.value, dateToQuery)
  }
}

const handleCurrentChange = (val: number) => {
  currentPage.value = val
  if (activeCategory.value) {
    const dateToQuery = displayDate.value || currentDate
    fetchPapers(activeCategory.value, dateToQuery)
  }
}

// 判断是否已关注
const isFollowed = (category: string) => {
  return followedCategories.value.some(item => item.category === category)
}

// 切换关注状态
const toggleFollow = async (category: string) => {
  try {
    if (isFollowed(category)) {
      await userApi.unfollowCategory(category)
      ElMessage.success('已取消关注')
    } else {
      await userApi.followCategory(category)
      ElMessage.success('已关注')
    }
    // 刷新列表
    await fetchFollowedCategories()
  } catch (error) {
    console.error('操作失败', error)
    ElMessage.error('操作失败')
  }
}

const getCategoryLabel = (value: string) => {
  const cat = allCategories.find(c => c.value === value)
  return cat ? cat.label : value
}

const goToDetail = (arxivId: string) => {
  router.push(`/paper/${arxivId}`)
}

const formatDate = (date: string) => {
  return dayjs(date).format('YYYY-MM-DD')
}

const formatAuthors = (authors: any) => {
  if (Array.isArray(authors)) {
    return authors.join(', ')
  }
  return authors
}

const highlightKeyword = (text: string) => {
  return text
}

onMounted(() => {
  if (userStore.isLoggedIn) {
    fetchFollowedCategories()
  }
})
</script>

<style scoped>
.follow-page {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}

.page-header {
  margin-bottom: 30px;
  text-align: center;
}

.subtitle {
  color: #666;
  margin-top: 10px;
}

.follow-content {
  display: flex;
  gap: 20px;
  min-height: 600px;
}

.sidebar {
  width: 280px;
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
  height: fit-content;
}

.sidebar-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 10px;
  border-bottom: 1px solid #eee;
}

.category-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.category-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.3s;
  background: #f9f9f9;
}

.category-item:hover {
  background: #f0f7ff;
}

.category-item.active {
  background: #e6f7ff;
  color: #2563EB;
  font-weight: 500;
  border-right: 3px solid #2563EB;
}

.no-update {
  font-size: 12px;
  color: #999;
}

.main-content {
  flex: 1;
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 15px;
  border-bottom: 1px solid #eee;
}

.pagination-container {
  display: flex;
  justify-content: center;
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid #eee;
}

.header-right {
  display: flex;
  align-items: center;
}

.date-tag {
  background: #f0f7ff;
  color: #2563EB;
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 13px;
}

.paper-list {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.paper-card {
  display: flex;
  gap: 15px;
  padding: 20px;
  border: 1px solid #eee;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s;
}

.paper-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
  border-color: #2563EB;
}

.paper-index {
  font-size: 24px;
  font-weight: bold;
  color: #eee;
  min-width: 40px;
  text-align: center;
}

.paper-content {
  flex: 1;
}

.paper-header-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 8px;
}

.paper-title {
  margin: 0;
  font-size: 16px;
  line-height: 1.4;
  color: #333;
  flex: 1;
  padding-right: 15px;
}

.paper-tags {
  display: flex;
  gap: 8px;
  align-items: center;
}

.github-link {
  color: #333;
  font-size: 18px;
  transition: color 0.3s;
  display: flex;
  align-items: center;
}

.github-link:hover {
  color: #2563EB;
}

.paper-authors {
  font-size: 13px;
  color: #666;
  margin-bottom: 10px;
}

.paper-abstract {
  font-size: 13px;
  color: #888;
  line-height: 1.5;
  margin-bottom: 15px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.paper-footer {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: #999;
}

.info-item {
  display: flex;
  align-items: center;
  gap: 5px;
}

.interactions {
  display: flex;
  gap: 15px;
}

.empty-follow {
  text-align: center;
  padding: 40px 0;
  color: #999;
}

.manage-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
  gap: 15px;
}

.manage-card {
  border: 1px solid #eee;
  border-radius: 6px;
  padding: 15px;
  cursor: pointer;
  transition: all 0.3s;
}

.manage-card:hover {
  border-color: #2563EB;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.manage-card.active {
  background: #f0f7ff;
  border-color: #2563EB;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.code {
  font-weight: bold;
  color: #333;
}

.check-icon {
  color: #2563EB;
}

.name {
  font-size: 13px;
  color: #666;
}
</style>
