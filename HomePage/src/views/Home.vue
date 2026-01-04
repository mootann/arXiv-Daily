<template>
  <main class="container">
    <!-- Filter Section (Teleported to Header) -->
    <Teleport to="#header-categories">
        <div class="header-tags-container">
            <button 
              v-for="tag in tags" 
              :key="tag.name" 
              class="header-tag" 
              :class="{ active: currentTag === tag.name }"
              @click="handleTagClick(tag.name)"
              :disabled="loading"
            >
              {{ tag.name }} <span class="count" v-if="tag.count > 0">{{ tag.count }}</span>
            </button>
        </div>
    </Teleport>

    <Teleport to="#header-filters">
        <div class="header-filters-container">
            <!-- Result Count (only for API) -->
            <select v-if="searchSource === 'api'" v-model="resultCount" class="header-select" :disabled="loading" title="Results per page">
                <option :value="10">10</option>
                <option :value="20">20</option>
                <option :value="50">50</option>
            </select>

            <!-- Date Picker -->
            <div class="header-date-group">
                <input 
                  type="date" 
                  v-model="startDateFilter"
                  class="header-date"
                  :disabled="loading"
                  title="Start Date"
                />
                <span style="color: #666;">-</span>
                <input 
                  type="date" 
                  v-model="endDateFilter"
                  class="header-date"
                  :disabled="loading"
                  title="End Date"
                />
            </div>

            <!-- GitHub Checkbox -->
            <label class="header-checkbox" title="Show papers with code only">
                <input 
                  type="checkbox" 
                  v-model="hasGithub"
                  :disabled="loading"
                />
                <i class="fab fa-github"></i>
            </label>

            <!-- Search -->
            <div class="header-search">
                <input 
                  type="text" 
                  placeholder="Search..." 
                  v-model="searchQuery"
                  @keyup.enter="handleSearch"
                  :disabled="loading"
                >
                <button 
                  @click="handleSearch"
                  :disabled="loading"
                >
                  <i class="fas fa-search"></i>
                </button>
            </div>
        </div>
    </Teleport>

    <!-- Loading State -->
    <div v-if="loading" class="loading-container">
        <div class="spinner"></div>
        <p>Loading papers...</p>
    </div>

    <!-- Error State -->
    <div v-else-if="error" class="error-container">
        <i class="fas fa-exclamation-circle"></i>
        <p>{{ error }}</p>
        <button @click="loadPapers" class="retry-btn">Retry</button>
    </div>

    <!-- Empty State -->
    <div v-else-if="papers.length === 0" class="empty-container">
        <i class="fas fa-file-alt"></i>
        <p>No papers found</p>
    </div>

    <!-- Cards Grid -->
    <div v-else class="cards-grid">
        <article class="paper-card" v-for="(paper, index) in papers" :key="paper.arxivId">
            <div class="paper-card-content" :class="{ 'simple-view-content': isSimpleView }">
                <div class="paper-cover-wrapper" @click="goToPaperDetail(paper.arxivId)" v-if="!isSimpleView">
                    <PaperCover :url="getPaperPdfUrl(paper)" />
                </div>
                <div class="paper-info">
                    <div class="card-header">
                        <div class="paper-index">{{ index + 1 }}</div>
                        <div class="paper-date">{{ formatDate(paper.publishedDate) }}</div>
                    </div>
                    <h2 class="paper-title">
                        <a @click="goToPaperDetail(paper.arxivId)" class="paper-link">
                            <span v-html="paper.title" :ref="el => setTitleRef(el, index)"></span>
                        </a>
                    </h2>
                    <p class="paper-authors">{{ formatAuthors(paper.authors) }}</p>
                    <div class="paper-tags">
                        <span class="paper-tag" v-for="tag in paper.categories" :key="tag">{{ tag }}</span>
                    </div>
                    <div class="paper-summary">
                        <span v-html="paper.summary" :ref="el => setSummaryRef(el, index)"></span>
                    </div>
                    <div class="card-footer">
                        <a :href="paper.arxivUrl" target="_blank" rel="noopener noreferrer" class="details-link">arXiv</a>
                        <a :href="paper.pdfUrl" target="_blank" rel="noopener noreferrer" class="details-link">PDF</a>
                        <a :href="paper.latexUrl" target="_blank" rel="noopener noreferrer" class="details-link">LaTeX</a>
                        <a v-if="paper.githubUrl" :href="paper.githubUrl" target="_blank" rel="noopener noreferrer" class="details-link github-link">
                            <i class="fab fa-github"></i> GitHub
                        </a>
                    </div>
                </div>
            </div>
        </article>

        <!-- Loading More Indicator -->
        <div v-if="loadingMore && searchSource === 'db'" class="loading-more">
            <i class="fas fa-spinner fa-spin"></i>
            <span>加载更多...</span>
        </div>

        <!-- No More Data Indicator -->
        <div v-if="!hasMore && papers.length > 0 && searchSource === 'db'" class="no-more-data">
            <i class="fas fa-check-circle"></i>
            <span>已加载全部内容</span>
        </div>
    </div>

    <!-- Floating Action Buttons -->
    <div class="fab-container">
        <!-- View Mode Toggle FAB - 固定在第一个位置 -->
        <button 
            class="fab-btn fab-view-toggle" 
            @click="toggleViewMode" 
            :title="isSimpleView ? '切换到卡片视图' : '切换到列表视图'"
        >
            <i :class="isSimpleView ? 'fas fa-th-large' : 'fas fa-list'"></i>
        </button>

        <!-- 刷新按钮 - 固定在第二个位置，只在未显示回到顶部按钮时显示 -->
        <button 
            v-if="!showBackToTop" 
            class="fab-btn fab-refresh" 
            @click="loadPapers" 
            :disabled="loading" 
            title="刷新"
        >
            <i class="fas fa-sync-alt" :class="{ 'fa-spin': loading }"></i>
        </button>
    </div>

    <!-- 返回顶部按钮 -->
    <button 
      v-if="showBackToTop" 
      class="back-to-top-btn" 
      @click="scrollToTop"
      title="返回顶部"
    >
      <i class="fas fa-arrow-up"></i>
    </button>
  </main>

  <!-- 论文详情模态框 -->
  <PaperModal 
    :show="showModal" 
    :arxiv-id="selectedArxivId" 
    @close="handleModalClose"
  />
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch, nextTick } from 'vue';
import PaperModal from '@/components/PaperModal.vue';
import PaperCover from '@/components/PaperCover.vue';
import { searchByKeyword, searchByDateRange, searchByCategoryAndDateRange, getCategoryCounts } from '@/api/arxiv';
import type { ArxivPaper, CategoryCount } from '@/types/arxiv';
import 'katex/dist/katex.min.css';
// @ts-ignore
import renderMathInElement from 'katex/dist/contrib/auto-render.mjs';

const currentTag = ref('All');
const searchSource = ref('db');
const searchQuery = ref('');
const resultCount = ref(10);
const papers = ref<ArxivPaper[]>([]);
const loading = ref(false);
const loadingMore = ref(false);
const error = ref('');
const titleRefs = ref<Map<number, HTMLElement>>(new Map());
const summaryRefs = ref<Map<number, HTMLElement>>(new Map());
const currentPage = ref(1);
const hasMore = ref(true);
const isSearching = ref(false);
const showBackToTop = ref(false);
const showModal = ref(false);
const selectedArxivId = ref('');
const isSimpleView = ref(true); // 默认使用简洁模式（不显示封面）

const toggleViewMode = () => {
  isSimpleView.value = !isSimpleView.value;
};

// 初始化日期过滤器：默认当天
const today = new Date();
const todayStr = today.toISOString().split('T')[0];

const startDateFilter = ref(todayStr);
const endDateFilter = ref(todayStr);
const hasGithub = ref(false);

const getPaperPdfUrl = (paper: ArxivPaper) => {
  if (paper.arxivUrl) {
    return paper.arxivUrl.replace('/abs/', '/pdf/');
  }
  return paper.pdfUrl || '';
};

const tags = ref([
  { name: 'All', count: 0 },
  { name: 'cs.AI', count: 0 },
  { name: 'cs.CL', count: 0 },
  { name: 'cs.CV', count: 0 },
  { name: 'cs.LG', count: 0 },
  { name: 'cs.GR', count: 0 },
  { name: 'eess.IV', count: 0 },
  { name: 'eess.SP', count: 0 },
  { name: 'eess.SY', count: 0 },
]);



const goToPaperDetail = (arxivId: string) => {
  selectedArxivId.value = arxivId;
  showModal.value = true;
};

const handleModalClose = () => {
  showModal.value = false;
  selectedArxivId.value = '';
};

const formatDate = (dateStr: string) => {
  if (!dateStr) return '';
  const date = new Date(dateStr);
  return date.toLocaleDateString('en-US', { month: 'numeric', day: 'numeric', year: 'numeric' });
};

const formatAuthors = (authors: string[]) => {
  if (!authors || authors.length === 0) return '';
  if (authors.length <= 3) {
    return authors.join(', ');
  }
  return `${authors.slice(0, 3).join(', ')} et al.`;
};

const setTitleRef = (el: any, index: number) => {
  if (el) {
    titleRefs.value.set(index, el as HTMLElement);
  }
};

watch(searchSource, () => {
  if (searchQuery.value.trim()) {
    handleSearch();
  } else {
    loadPapers();
  }
});

watch(hasGithub, () => {
  loadPapers();
});

const setSummaryRef = (el: any, index: number) => {
  if (el) {
    summaryRefs.value.set(index, el as HTMLElement);
  }
};

const renderLatex = () => {
  nextTick(() => {
    setTimeout(() => {
      const katexOptions = {
        delimiters: [
          { left: '$$', right: '$$', display: true },
          { left: '$', right: '$', display: false },
          { left: '\\[', right: '\\]', display: true },
          { left: '\\(', right: '\\)', display: false }
        ],
        throwOnError: false,
        strict: false,
        trust: true,
        displayMode: false
      };
      
      titleRefs.value.forEach((el) => {
        if (el) {
          try {
            renderMathInElement(el, katexOptions);
          } catch (error) {
            console.error('渲染标题LaTeX失败:', error);
          }
        }
      });
      
      summaryRefs.value.forEach((el) => {
        if (el) {
          try {
            renderMathInElement(el, katexOptions);
          } catch (error) {
            console.error('渲染摘要LaTeX失败:', error);
          }
        }
      });
    }, 100);
  });
};

const loadPapers = async () => {
  loading.value = true;
  error.value = '';
  currentPage.value = 1;
  hasMore.value = true;
  isSearching.value = false;

  try {
    let response;
    
    // 使用日期过滤器
    const endDate = endDateFilter.value || '';
    const startDate = startDateFilter.value || '';
    
    if (currentTag.value === 'All') {
      response = await searchByDateRange(
        startDate,
        endDate,
        resultCount.value,
        searchSource.value,
        1,
        hasGithub.value ? true : undefined
      );
    } else {
      response = await searchByCategoryAndDateRange(
        currentTag.value,
        startDate,
        endDate,
        resultCount.value,
        searchSource.value,
        1,
        hasGithub.value ? true : undefined
      );
    }

    if (response.data && response.data.data && response.data.data.papers) {
      papers.value = response.data.data.papers;
      hasMore.value = response.data.data.papers.length >= resultCount.value;

      // 如果返回的实际日期与请求的日期不同,更新日期过滤器
      if (response.data.data.actualStartDate && response.data.data.actualEndDate) {
        if (response.data.data.actualStartDate !== startDate || response.data.data.actualEndDate !== endDate) {
          console.log('自动调整日期范围从', startDate, '-', endDate, '到', response.data.data.actualStartDate, '-', response.data.data.actualEndDate);
          startDateFilter.value = response.data.data.actualStartDate;
          endDateFilter.value = response.data.data.actualEndDate;
        }
      }

      // 如果返回了分类统计信息,更新标签计数
      if (response.data.data.categoryCounts) {
        tags.value.forEach(t => t.count = 0);
        response.data.data.categoryCounts.forEach((item: any) => {
          const tag = tags.value.find(t => t.name === item.category);
          if (tag) {
            tag.count = item.count;
          }
        });
      }

      // 渲染LaTeX公式
      renderLatex();
    } else {
      papers.value = [];
      hasMore.value = false;
    }
  } catch (err: any) {
    console.error('Failed to load papers:', err);
    error.value = err.message || 'Failed to load papers. Please try again.';
    papers.value = [];
    hasMore.value = false;
  } finally {
    loading.value = false;
  }
};

const loadMorePapers = async () => {
  // 只在数据库查询时允许无限滚动加载
  if (loadingMore.value || !hasMore.value || searchSource.value !== 'db') return;

  loadingMore.value = true;

  try {
    currentPage.value++;
    let response;
    
    // 使用日期过滤器
    const endDate = endDateFilter.value || '';
    const startDate = startDateFilter.value || '';

    if (currentTag.value === 'All') {
      response = await searchByDateRange(
        startDate,
        endDate,
        resultCount.value,
        searchSource.value,
        currentPage.value,
        hasGithub.value ? true : undefined
      );
    } else {
      response = await searchByCategoryAndDateRange(
        currentTag.value,
        startDate,
        endDate,
        resultCount.value,
        searchSource.value,
        currentPage.value,
        hasGithub.value ? true : undefined
      );
    }

    if (response.data && response.data.data && response.data.data.papers) {
      const newPapers = response.data.data.papers;
      papers.value = [...papers.value, ...newPapers];
      hasMore.value = newPapers.length >= resultCount.value;

      // 渲染新加载论文的LaTeX公式
      renderLatex();
    } else {
      hasMore.value = false;
    }
  } catch (err: any) {
    console.error('Failed to load more papers:', err);
    hasMore.value = false;
  } finally {
    loadingMore.value = false;
  }
};

const handleTagClick = (tagName: string) => {
  currentTag.value = tagName;
  loadPapers();
};

const handleSearch = async () => {
  if (!searchQuery.value.trim()) {
    loadPapers();
    return;
  }

  loading.value = true;
  error.value = '';
  currentPage.value = 1;
  hasMore.value = true;
  isSearching.value = true;

  try {
    const response = await searchByKeyword(
      searchQuery.value,
      resultCount.value,
      searchSource.value,
      1,
      hasGithub.value ? true : undefined
    );

    if (response.data && response.data.data && response.data.data.papers) {
      papers.value = response.data.data.papers;
      hasMore.value = response.data.data.papers.length >= resultCount.value;

      renderLatex();
    } else {
      papers.value = [];
      hasMore.value = false;
    }
  } catch (err: any) {
    console.error('Failed to search papers:', err);
    error.value = err.message || 'Failed to search papers. Please try again.';
    papers.value = [];
    hasMore.value = false;
  } finally {
    loading.value = false;
  }
};

const loadMoreSearchResults = async () => {
  // 只在数据库查询且搜索时允许无限滚动
  if (loadingMore.value || !hasMore.value || !isSearching.value || searchSource.value !== 'db') return;

  loadingMore.value = true;

  try {
    currentPage.value++;
    const response = await searchByKeyword(
      searchQuery.value,
      resultCount.value,
      searchSource.value,
      currentPage.value,
      hasGithub.value ? true : undefined
    );

    if (response.data && response.data.data && response.data.data.papers) {
      const newPapers = response.data.data.papers;
      papers.value = [...papers.value, ...newPapers];
      hasMore.value = newPapers.length >= resultCount.value;

      // 渲染新加载论文的LaTeX公式
      renderLatex();
    } else {
      hasMore.value = false;
    }
  } catch (err: any) {
    console.error('Failed to load more search results:', err);
    hasMore.value = false;
  } finally {
    loadingMore.value = false;
  }
};

const handleScroll = () => {
  // 更新返回顶部按钮显示状态
  const scrollTop = window.pageYOffset || document.documentElement.scrollTop;
  showBackToTop.value = scrollTop > 300;

  // 只在数据库查询时触发滚动加载，减少触发深度到40px
  if (searchSource.value !== 'db') return;

  const windowHeight = window.innerHeight;
  const documentHeight = document.documentElement.scrollHeight;

  // 当滚动到距离底部40px时加载更多
  if (scrollTop + windowHeight >= documentHeight - 40 && !loading.value && !loadingMore.value) {
    if (isSearching.value) {
      loadMoreSearchResults();
    } else {
      loadMorePapers();
    }
  }
};

let scrollTimer: number | null = null;
const throttledHandleScroll = () => {
  if (scrollTimer === null) {
    scrollTimer = window.setTimeout(() => {
      handleScroll();
      scrollTimer = null;
    }, 200);
  }
};

const scrollToTop = () => {
  window.scrollTo({
    top: 0,
    behavior: 'smooth'
  });
};

watch([currentTag, searchSource, resultCount, startDateFilter, endDateFilter], () => {
  loadPapers();
});

watch([startDateFilter, endDateFilter], () => {
  fetchCategoryCounts();
});

const fetchCategoryCounts = async () => {
  try {
    const response = await getCategoryCounts(startDateFilter.value, endDateFilter.value);
    console.log('Category counts response:', response);
    if (response.data && response.data.code === 200 && response.data.data) {
      const counts = response.data.data; // Array of CategoryCount
      
      // Reset counts
      tags.value.forEach(t => t.count = 0);
      
      counts.forEach((item: any) => {
         const cat = item.category;
         const count = item.count;
         const tag = tags.value.find(t => t.name === cat);
         if (tag) {
           tag.count = count;
         }
      });
      console.log('Updated tags with counts:', tags.value);
    }
  } catch (error) {
    console.error('Failed to fetch category counts:', error);
  }
};

onMounted(() => {
  fetchCategoryCounts();
  loadPapers();
  window.addEventListener('scroll', throttledHandleScroll);
});

onUnmounted(() => {
  window.removeEventListener('scroll', throttledHandleScroll);
  if (scrollTimer) {
    clearTimeout(scrollTimer);
  }
});
</script>

<style scoped>
.simple-view-content {
    /* 可以在这里添加针对简洁模式的特殊样式，目前保持默认即可 */
}

.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  text-align: center;
}

.spinner {
  width: 40px;
  height: 40px;
  border: 4px solid #f3f3f3;
  border-top: 4px solid #3498db;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: 20px;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.error-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  text-align: center;
  color: #e74c3c;
}

.error-container i {
  font-size: 48px;
  margin-bottom: 20px;
}

.retry-btn {
  margin-top: 20px;
  padding: 10px 20px;
  background-color: #3498db;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  transition: background-color 0.3s;
}

.retry-btn:hover {
  background-color: #2980b9;
}

.empty-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  text-align: center;
  color: #7f8c8d;
}

.empty-container i {
  font-size: 48px;
  margin-bottom: 20px;
}

.search-btn {
  padding: 10px 20px;
  background-color: #3498db;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  transition: background-color 0.3s;
  margin-left: 10px;
}

.search-btn:hover:not(:disabled) {
  background-color: #2980b9;
}

.search-btn:disabled {
  background-color: #bdc3c7;
  cursor: not-allowed;
}

.source-selector {
  margin-right: 15px;
}

.source-select {
  padding: 10px 15px;
  border: none;
  border-radius: 8px;
  background: linear-gradient(135deg, #1e1e1e 0%, #2a2a2a 100%);
  color: #fff;
  font-size: 14px;
  font-weight: 600;
  outline: none;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 2px 8px rgba(52, 152, 219, 0.2);
}

.source-select option {
  background-color: #1e1e1e;
  color: #fff;
  padding: 10px;
}

.source-select:hover {
  box-shadow: 0 4px 12px rgba(52, 152, 219, 0.3);
}

.source-select:focus {
  box-shadow: 0 0 0 3px rgba(52, 152, 219, 0.2);
}

.result-count-selector {
  margin-right: 15px;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: linear-gradient(135deg, #2c2c2c 0%, #3a3a3a 100%);
  border-radius: 8px;
  border: 2px solid #3498db;
  box-shadow: 0 2px 8px rgba(52, 152, 219, 0.2);
}

.github-filter {
  margin-right: 15px;
  display: flex;
  align-items: center;
}

.github-checkbox-label {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #ecf0f1;
  font-weight: 600;
  cursor: pointer;
  padding: 8px 12px;
  border-radius: 8px;
  border: 2px solid #2ecc71;
  background: linear-gradient(135deg, #27ae60 0%, #2ecc71 100%);
  transition: all 0.3s ease;
  user-select: none;
}

.github-checkbox-label:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(46, 204, 113, 0.3);
}

.github-checkbox-label input[type="checkbox"] {
  width: 16px;
  height: 16px;
  cursor: pointer;
}

.github-checkbox-label i {
  font-size: 1.2em;
}

.count-label {
  font-size: 12px;
  color: #3498db;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.result-count-select {
  padding: 8px 12px;
  border: 1px solid #3498db;
  border-radius: 6px;
  background-color: #1e1e1e;
  color: #fff;
  font-size: 14px;
  font-weight: 600;
  outline: none;
  cursor: pointer;
  min-width: 70px;
  transition: all 0.3s ease;
}

.result-count-select option {
  background-color: #1e1e1e;
  color: #fff;
  padding: 8px;
}

.result-count-select:hover {
  background-color: #2a2a2a;
  border-color: #2980b9;
}

.result-count-select:focus {
  border-color: #3498db;
  box-shadow: 0 0 0 2px rgba(52, 152, 219, 0.2);
}

.refresh-controls {
  margin-right: 15px;
}

.refresh-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 20px;
  border: 2px solid #e74c3c;
  border-radius: 8px;
  background: linear-gradient(135deg, #c0392b 0%, #e74c3c 100%);
  color: white;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 2px 8px rgba(231, 76, 60, 0.3);
}

.refresh-btn:hover:not(:disabled) {
  background: linear-gradient(135deg, #a93226 0%, #c0392b 100%);
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(231, 76, 60, 0.4);
}

.refresh-btn:active:not(:disabled) {
  transform: translateY(0);
  box-shadow: 0 2px 6px rgba(231, 76, 60, 0.3);
}

.refresh-btn:disabled {
  cursor: not-allowed;
  opacity: 0.6;
  background: linear-gradient(135deg, #7f8c8d 0%, #95a5a6 100%);
  border-color: #95a5a6;
}

.refresh-btn i {
  font-size: 16px;
}

.date-picker-wrapper {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-right: 15px;
  padding: 8px 12px;
  background: linear-gradient(135deg, #2c2c2c 0%, #3a3a3a 100%);
  border-radius: 8px;
  border: none;
  box-shadow: 0 2px 8px rgba(52, 152, 219, 0.2);
}

.date-picker {
  padding: 8px 12px;
  border: none;
  border-radius: 6px;
  background-color: #2d3748;
  color: #e2e8f0;
  font-family: inherit;
  font-size: 14px;
  outline: none;
  cursor: pointer;
  transition: all 0.3s ease;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
}

.date-picker:hover {
  background-color: #374151;
}

.date-picker:focus {
  box-shadow: 0 0 0 2px rgba(52, 152, 219, 0.2);
  background-color: #1a202c;
}

.date-separator {
  color: #3498db;
  font-weight: 600;
}

.paper-title a {
  color: inherit;
  text-decoration: none;
  transition: color 0.3s;
}

.paper-link {
  cursor: pointer;
}

.paper-title a:hover {
  color: #3498db;
}

.card-footer {
  display: flex;
  gap: 10px;
}



.fa-spin {
  animation: spin 1s linear infinite;
}

button:disabled {
  cursor: not-allowed;
  opacity: 0.6;
}

.loading-more {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 30px 20px;
  color: #7f8c8d;
  font-size: 14px;
  animation: fadeIn 0.3s ease-in;
}

.loading-more i {
  color: #3498db;
  font-size: 20px;
}

.no-more-data {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 30px 20px;
  color: #7f8c8d;
  font-size: 14px;
  animation: fadeIn 0.3s ease-in;
}

.no-more-data i {
  color: #27ae60;
  font-size: 18px;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(-10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.cards-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(600px, 1fr));
  gap: 20px;
  margin-bottom: 30px;
}

.paper-card-content {
  display: flex;
  gap: 20px;
}

.paper-cover-wrapper {
  cursor: pointer;
  flex-shrink: 0;
  padding-top: 10px;
  width: 200px; /* Increase width for larger cover */
}

.paper-title {
  margin: 0 0 10px 0;
  font-size: 20px; /* Increase font size */
  line-height: 1.4;
  font-weight: 700;
}

.paper-link {
  color: #e2e8f0;
  text-decoration: none;
  transition: color 0.2s;
  cursor: pointer;
}

.paper-link:hover {
  color: #63b3ed;
}

.paper-authors {
  color: #a0aec0;
  font-size: 15px; /* Increase font size */
  margin-bottom: 12px;
  line-height: 1.5;
}

.paper-summary {
  color: #cbd5e1;
  font-size: 15px; /* Increase font size */
  line-height: 1.6;
  margin-bottom: 15px;
  max-height: 96px; /* 大约4行的高度 (15px * 1.6 * 4 ≈ 96px) */
  overflow-y: auto;
  overflow-x: hidden;
  /* 隐藏滚动条样式 */
  scrollbar-width: thin;
  scrollbar-color: rgba(52, 152, 219, 0.3) transparent;
}

/* Webkit浏览器滚动条样式 */
.paper-summary::-webkit-scrollbar {
  width: 6px;
}

.paper-summary::-webkit-scrollbar-track {
  background: transparent;
}

.paper-summary::-webkit-scrollbar-thumb {
  background-color: rgba(52, 152, 219, 0.3);
  border-radius: 3px;
  transition: background-color 0.3s;
}

.paper-summary::-webkit-scrollbar-thumb:hover {
  background-color: rgba(52, 152, 219, 0.5);
}

.paper-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

@media (max-width: 768px) {
  .paper-card-content {
    flex-direction: column;
  }
  
  .paper-cover-wrapper {
    align-self: center;
    padding-top: 0;
    margin-bottom: 15px;
  }
}

/* Header Teleport Styles */
.header-tags-container {
    display: flex;
    flex-wrap: wrap;
    gap: 4px;
    max-width: 100%;
    max-height: 48px;
    overflow-y: auto;
    scrollbar-width: none;
    -ms-overflow-style: none;
}
.header-tags-container::-webkit-scrollbar {
    display: none;
}

.header-tag {
    padding: 2px 8px;
    border-radius: 4px;
    background: rgba(255, 255, 255, 0.1);
    color: #ccc;
    border: 1px solid rgba(255, 255, 255, 0.1);
    cursor: pointer;
    font-size: 11px;
    white-space: nowrap;
    transition: all 0.2s;
    display: flex;
    align-items: center;
    gap: 2px;
    height: 20px;
}
.header-tag:hover {
    background: rgba(255, 255, 255, 0.2);
    color: #fff;
    border-color: rgba(255, 255, 255, 0.3);
}
.header-tag.active {
    background: #3498db;
    color: #fff;
    border-color: #3498db;
}
.header-tag .count {
    font-size: 10px;
    opacity: 0.8;
    background: rgba(0,0,0,0.2);
    padding: 0 4px;
    border-radius: 4px;
}

.header-filters-container {
    display: flex;
    align-items: center;
    gap: 12px;
}

.header-select {
    background: #2c2c2c;
    color: #fff;
    border: 1px solid #444;
    border-radius: 4px;
    padding: 4px 8px;
    font-size: 12px;
    outline: none;
    cursor: pointer;
}
.header-select:hover {
    border-color: #3498db;
}

.header-icon-btn {
    background: #e74c3c;
    border: none;
    color: white;
    cursor: pointer;
    font-size: 12px;
    padding: 5px 10px;
    border-radius: 4px;
    display: flex;
    align-items: center;
    justify-content: center;
    transition: all 0.2s;
}
.header-icon-btn:hover:not(:disabled) {
    background: #c0392b;
    transform: translateY(-1px);
}
.header-icon-btn:disabled {
    opacity: 0.6;
    cursor: not-allowed;
    background: #7f8c8d;
}

.header-date-group {
    display: flex;
    align-items: center;
    gap: 6px;
    background: #2c2c2c;
    padding: 2px 8px;
    border-radius: 4px;
    border: 1px solid #444;
}
.header-date {
    background: transparent;
    border: none;
    color: #aaa;
    font-size: 12px;
    width: 85px;
    padding: 0;
    font-family: inherit;
}
.header-date::-webkit-calendar-picker-indicator {
    filter: invert(0.6);
    cursor: pointer;
    width: 12px;
    height: 12px;
}
.header-date:focus {
    color: #fff;
    outline: none;
}

.header-checkbox {
    display: flex;
    align-items: center;
    gap: 6px;
    cursor: pointer;
    color: #ccc;
    font-size: 14px;
    padding: 2px 6px;
    border-radius: 4px;
    transition: all 0.2s;
}
.header-checkbox input {
    accent-color: #2ecc71;
    width: 14px;
    height: 14px;
    cursor: pointer;
}
.header-checkbox:hover {
    background: rgba(255,255,255,0.1);
    color: #fff;
}
.header-checkbox i {
    font-size: 16px;
}

.header-search {
    display: flex;
    align-items: center;
    background: #2c2c2c;
    border: 1px solid #444;
    border-radius: 4px;
    padding: 2px 4px;
}
.header-search:focus-within {
    border-color: #3498db;
}
.header-search input {
    background: transparent;
    border: none;
    color: #fff;
    font-size: 12px;
    width: 120px;
    padding: 4px;
    outline: none;
}
.header-search button {
    background: #3498db;
    border: none;
    color: white;
    cursor: pointer;
    font-size: 12px;
    padding: 4px 8px;
    border-radius: 2px;
    margin-left: 4px;
}
.header-search button:hover {
    background: #2980b9;
}

/* 浮动按钮容器重写 - 使用绝对定位 */
.fab-container {
    position: fixed;
    bottom: 2rem;
    right: 2rem;
    z-index: 1000;
}

/* 视图切换按钮 - 固定在最上方 */
.fab-view-toggle {
    position: absolute;
    bottom: 70px; /* 离容器底部 70px，为刷新/回顶按钮留空间 */
    right: 0;
}

/* 刷新按钮 - 固定在底部 */
.fab-refresh {
    position: absolute;
    bottom: 0;
    right: 0;
}

/* 回到顶部按钮 - 也固定在底部，与刷新按钮相同位置 */
.back-to-top-btn {
    position: fixed;
    bottom: 30px; /* 与 fab-container 的 bottom 对齐 */
    right: 30px; /* 与 fab-container 的 right 对齐 */
    width: 50px;
    height: 50px;
    background: linear-gradient(135deg, #3498db 0%, #2980b9 100%);
    color: #fff;
    border: none;
    border-radius: 50%;
    cursor: pointer;
    box-shadow: 0 4px 12px rgba(52, 152, 219, 0.4);
    transition: all 0.3s ease;
    z-index: 1000;
    animation: fadeIn 0.3s ease-in;
}

.back-to-top-btn:hover {
    transform: translateY(-5px) scale(1.1);
    box-shadow: 0 6px 16px rgba(52, 152, 219, 0.5);
}

.back-to-top-btn:active {
    transform: translateY(-2px) scale(1.05);
}

.back-to-top-btn i {
    font-size: 20px;
    font-weight: bold;
}
</style>

<style>
/* Global styles for hiding scrollbar as requested */
html::-webkit-scrollbar {
  display: none;
}
html {
  -ms-overflow-style: none;
  scrollbar-width: none;
}
</style>
