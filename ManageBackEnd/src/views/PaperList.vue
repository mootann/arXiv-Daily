<template>
  <div class="paper-list">
    <el-card class="search-card">
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="搜索关键词">
          <el-input v-model="searchForm.keyword" placeholder="输入关键词搜索" clearable />
        </el-form-item>
        <el-form-item label="分类">
          <el-select 
            v-model="searchForm.category" 
            placeholder="选择分类" 
            clearable 
            filterable
            :max-height="400"
          >
            <el-option
              v-for="cat in categories"
              :key="cat.value"
              :label="cat.label"
              :value="cat.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="开始日期">
          <el-date-picker
            v-model="searchForm.startDate"
            type="date"
            placeholder="选择开始日期"
            value-format="YYYY-MM-DD"
          />
        </el-form-item>
        <el-form-item label="结束日期">
          <el-date-picker
            v-model="searchForm.endDate"
            type="date"
            placeholder="选择结束日期"
            value-format="YYYY-MM-DD"
          />
        </el-form-item>
        <el-form-item label="Github">
          <el-checkbox v-model="searchForm.hasGithub" label="含代码" border />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch" :loading="loading">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="table-card">
      <el-table :data="paperList" style="width: 100%" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="arxivId" label="arXiv ID" width="150" />
        <el-table-column prop="title" label="标题" min-width="300">
          <template #default="{ row, $index }">
            <div :ref="el => setTableTitleRef(el, $index)" class="latex-content">
              {{ row.title }}
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="authors" label="作者" width="200" show-overflow-tooltip />
        <el-table-column prop="primaryCategory" label="分类" width="120" />
        <el-table-column prop="publishedDate" label="发布日期" width="120" />
        <el-table-column prop="version" label="版本" width="80" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleView(row)">查看</el-button>
            <el-button type="primary" link @click="handleEdit(row)">编辑</el-button>
            <el-button type="success" link @click="handleOpenLink(row.arxivUrl)">打开</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-if="total > 0"
        class="pagination"
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      />
    </el-card>

    <el-dialog
      v-model="viewDialogVisible"
      title="论文详情"
      width="80%"
      top="5vh"
    >
      <el-descriptions :column="2" border v-if="currentPaper">
        <el-descriptions-item label="arXiv ID">{{ currentPaper.arxivId }}</el-descriptions-item>
        <el-descriptions-item label="版本">{{ currentPaper.version }}</el-descriptions-item>
        <el-descriptions-item label="标题" :span="2">
          <div ref="titleContent" class="latex-content">{{ currentPaper.title }}</div>
        </el-descriptions-item>
        <el-descriptions-item label="作者" :span="2">{{ currentPaper.authors }}</el-descriptions-item>
        <el-descriptions-item label="主要分类">{{ currentPaper.primaryCategory }}</el-descriptions-item>
        <el-descriptions-item label="所有分类">{{ currentPaper.categories }}</el-descriptions-item>
        <el-descriptions-item label="发布日期">{{ currentPaper.publishedDate }}</el-descriptions-item>
        <el-descriptions-item label="更新日期">{{ currentPaper.updatedDate }}</el-descriptions-item>
        <el-descriptions-item label="DOI">{{ currentPaper.doi || '-' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ currentPaper.createdTime }}</el-descriptions-item>
        <el-descriptions-item label="摘要" :span="2">
          <div class="summary-text latex-content" ref="summaryContent">{{ currentPaper.summary }}</div>
        </el-descriptions-item>
      </el-descriptions>

      <template #footer>
        <el-button @click="viewDialogVisible = false">关闭</el-button>
        <el-button type="primary" @click="currentPaper && handleOpenLink(currentPaper.arxivUrl)">打开arXiv页面</el-button>
        <el-button type="success" @click="currentPaper && handleOpenLink(currentPaper.pdfUrl)">下载PDF</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, nextTick } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { arxivApi } from '@/api/arxiv';
import type { ArxivPaper } from '@/types/arxiv';
// @ts-ignore
import 'katex/dist/katex.min.css';
// @ts-ignore
import katex from 'katex';
// @ts-ignore
import renderMathInElement from 'katex/dist/contrib/auto-render';

const router = useRouter();

const loading = ref(false);
const paperList = ref<ArxivPaper[]>([]);
const total = ref(0);
const currentPage = ref(1);
const pageSize = ref(20);
const viewDialogVisible = ref(false);
const currentPaper = ref<ArxivPaper | null>(null);
const summaryContent = ref<HTMLElement | null>(null);
const titleContent = ref<HTMLElement | null>(null);
const tableTitleRefs = ref<Map<number, HTMLElement>>(new Map());

const searchForm = reactive({
  keyword: '',
  category: '',
  startDate: '',
  endDate: '',
  hasGithub: false
});

const categories = [
  { value: 'UNCATEGORIZED', label: '未分类' },
  { value: 'cs.AI', label: '人工智能' },
  { value: 'cs.CL', label: '计算与语言' },
  { value: 'cs.CV', label: '计算机视觉' },
  { value: 'cs.CC', label: '计算复杂性' },
  { value: 'cs.CE', label: '计算工程、金融与科学' },
  { value: 'cs.CG', label: '计算几何' },
  { value: 'cs.GT', label: '计算机科学与博弈论' },
  { value: 'cs.CY', label: '计算机与社会' },
  { value: 'cs.DB', label: '数据库' },
  { value: 'cs.DL', label: '数字图书馆' },
  { value: 'cs.DM', label: '离散数学' },
  { value: 'cs.DS', label: '数据结构与算法' },
  { value: 'cs.ET', label: '新兴技术' },
  { value: 'cs.FL', label: '形式语言与自动机理论' },
  { value: 'cs.GL', label: '一般文献' },
  { value: 'cs.GR', label: '图形学' },
  { value: 'cs.AR', label: '硬件架构' },
  { value: 'cs.HC', label: '人机交互' },
  { value: 'cs.IR', label: '信息检索' },
  { value: 'cs.IT', label: '信息论' },
  { value: 'cs.LG', label: '机器学习' },
  { value: 'cs.LO', label: '计算逻辑' },
  { value: 'cs.MA', label: '多智能体系统' },
  { value: 'cs.MM', label: '多媒体' },
  { value: 'cs.MS', label: '数学软件' },
  { value: 'cs.NA', label: '数值分析' },
  { value: 'cs.NE', label: '神经与进化计算' },
  { value: 'cs.NI', label: '网络与互联网架构' },
  { value: 'cs.OH', label: '其他计算机科学' },
  { value: 'cs.OS', label: '操作系统' },
  { value: 'cs.PF', label: '性能' },
  { value: 'cs.PL', label: '编程语言' },
  { value: 'cs.RO', label: '机器人学' },
  { value: 'cs.SC', label: '符号计算' },
  { value: 'cs.SD', label: '声音' },
  { value: 'cs.SE', label: '软件工程' },
  { value: 'cs.SI', label: '社会与信息网络' },
  { value: 'cs.SY', label: '系统与控制' },
  { value: 'eess.AS', label: '声学' },
  { value: 'eess.SP', label: '信号处理' },
  { value: 'eess.IV', label: '图像与视频处理' }
];

const handleSearch = async () => {
  if (searchForm.startDate && searchForm.endDate && searchForm.category) {
    await handleSearchByCategoryAndDateRange();
  } else if (searchForm.startDate && searchForm.endDate) {
    await handleSearchByDateRange();
  } else if (searchForm.category && searchForm.keyword) {
    await handleSearchByCategoryAndKeyword();
  } else if (searchForm.category) {
    await handleSearchByCategory();
  } else if (searchForm.keyword) {
    await handleSearchByKeyword();
  } else {
    await handleSearchAll();
  }
};

const handleSearchAll = async () => {
  loading.value = true;
  try {
    const response = await arxivApi.getPapersFromDatabase({
      page: currentPage.value,
      size: pageSize.value,
      hasGithub: searchForm.hasGithub
    });
    const pageData = getPageData(response.data.data);
    paperList.value = convertDatabasePapers(pageData.content);
    total.value = pageData.totalElements;
    ElMessage.success('查询成功');
    renderTableLatex();
  } catch (error) {
    ElMessage.error('查询失败');
  } finally {
    loading.value = false;
  }
};

const handleSearchByKeyword = async () => {
  if (!searchForm.keyword) {
    handleSearchAll();
    return;
  }

  loading.value = true;
  try {
    const response = await arxivApi.searchPapersFromDatabase(
      searchForm.keyword,
      currentPage.value,
      pageSize.value,
      searchForm.hasGithub
    );
    const pageData = getPageData(response.data.data);
    paperList.value = convertDatabasePapers(pageData.content);
    total.value = pageData.totalElements;
    ElMessage.success('查询成功');
    renderTableLatex();
  } catch (error) {
    ElMessage.error('查询失败');
  } finally {
    loading.value = false;
  }
};

const handleSearchByCategory = async () => {
  if (!searchForm.category) {
    handleSearchAll();
    return;
  }

  loading.value = true;
  try {
    const response = await arxivApi.getPapersByCategoryFromDatabase(
      searchForm.category,
      currentPage.value,
      pageSize.value,
      searchForm.hasGithub
    );
    const pageData = getPageData(response.data.data);
    paperList.value = convertDatabasePapers(pageData.content);
    total.value = pageData.totalElements;
    ElMessage.success('查询成功');
    renderTableLatex();
  } catch (error) {
    ElMessage.error('查询失败');
  } finally {
    loading.value = false;
  }
};

const handleSearchByDateRange = async () => {
  if (!searchForm.startDate || !searchForm.endDate) {
    handleSearchAll();
    return;
  }

  loading.value = true;
  try {
    let response;
    if (searchForm.category) {
      response = await arxivApi.getPapersByCategoryAndDateRangeFromDatabase(
        searchForm.category,
        searchForm.startDate,
        searchForm.endDate,
        currentPage.value,
        pageSize.value,
        searchForm.hasGithub
      );
    } else {
      response = await arxivApi.getPapersByDateRangeFromDatabase(
        searchForm.startDate,
        searchForm.endDate,
        currentPage.value,
        pageSize.value,
        searchForm.hasGithub
      );
    }
    const pageData = getPageData(response.data.data);
    paperList.value = convertDatabasePapers(pageData.content);
    total.value = pageData.totalElements;
    ElMessage.success('查询成功');
    renderTableLatex();
  } catch (error) {
    ElMessage.error('查询失败');
  } finally {
    loading.value = false;
  }
};

const handleSearchByCategoryAndKeyword = async () => {
  if (!searchForm.category || !searchForm.keyword) {
    if (searchForm.category) {
      handleSearchByCategory();
    } else {
      handleSearchByKeyword();
    }
    return;
  }

  loading.value = true;
  try {
    const response = await arxivApi.getPapersByCategoryAndKeywordFromDatabase(
      searchForm.category,
      searchForm.keyword,
      currentPage.value,
      pageSize.value,
      searchForm.hasGithub
    );
    const pageData = getPageData(response.data.data);
    paperList.value = convertDatabasePapers(pageData.content);
    total.value = pageData.totalElements;
    ElMessage.success('查询成功');
    renderTableLatex();
  } catch (error) {
    ElMessage.error('查询失败');
  } finally {
    loading.value = false;
  }
};

const handleSearchByCategoryAndDateRange = async () => {
  if (!searchForm.category || !searchForm.startDate || !searchForm.endDate) {
    handleSearchByDateRange();
    return;
  }

  loading.value = true;
  try {
    const response = await arxivApi.getPapersByCategoryAndDateRangeFromDatabase(
      searchForm.category,
      searchForm.startDate,
      searchForm.endDate,
      currentPage.value,
      pageSize.value,
      searchForm.hasGithub
    );
    const pageData = getPageData(response.data.data);
    paperList.value = convertDatabasePapers(pageData.content);
    total.value = pageData.totalElements;
    ElMessage.success('查询成功');
    renderTableLatex();
  } catch (error) {
    ElMessage.error('查询失败');
  } finally {
    loading.value = false;
  }
};

const handleReset = () => {
  searchForm.keyword = '';
  searchForm.category = '';
  searchForm.startDate = '';
  searchForm.endDate = '';
  searchForm.hasGithub = false;
  currentPage.value = 1;
  handleSearchAll();
};

const handleSizeChange = (size: number) => {
  pageSize.value = size;
  if (searchForm.startDate && searchForm.endDate && searchForm.category) {
    handleSearchByCategoryAndDateRange();
  } else if (searchForm.startDate && searchForm.endDate) {
    handleSearchByDateRange();
  } else if (searchForm.category && searchForm.keyword) {
    handleSearchByCategoryAndKeyword();
  } else if (searchForm.category) {
    handleSearchByCategory();
  } else if (searchForm.keyword) {
    handleSearchByKeyword();
  } else {
    handleSearchAll();
  }
};

const handleCurrentChange = (page: number) => {
  currentPage.value = page;
  if (searchForm.startDate && searchForm.endDate && searchForm.category) {
    handleSearchByCategoryAndDateRange();
  } else if (searchForm.startDate && searchForm.endDate) {
    handleSearchByDateRange();
  } else if (searchForm.category && searchForm.keyword) {
    handleSearchByCategoryAndKeyword();
  } else if (searchForm.category) {
    handleSearchByCategory();
  } else if (searchForm.keyword) {
    handleSearchByKeyword();
  } else {
    handleSearchAll();
  }
};

const renderLatex = () => {
  nextTick(() => {
    // 渲染弹窗中的标题
    if (titleContent.value) {
      renderMathInElement(titleContent.value, {
        delimiters: [
          { left: '$$', right: '$$', display: true },
          { left: '$', right: '$', display: false },
          { left: '\\[', right: '\\]', display: true },
          { left: '\\(', right: '\\)', display: false }
        ],
        throwOnError: false
      });
    }
    
    // 渲染弹窗中的摘要
    if (summaryContent.value) {
      renderMathInElement(summaryContent.value, {
        delimiters: [
          { left: '$$', right: '$$', display: true },
          { left: '$', right: '$', display: false },
          { left: '\\[', right: '\\]', display: true },
          { left: '\\(', right: '\\)', display: false }
        ],
        throwOnError: false
      });
    }
  });
};

const setTableTitleRef = (el: any, index: number) => {
  if (el) {
    tableTitleRefs.value.set(index, el as HTMLElement);
  }
};

const renderTableLatex = () => {
  nextTick(() => {
    // 渲染表格中的标题
    tableTitleRefs.value.forEach((el) => {
      if (el) {
        renderMathInElement(el, {
          delimiters: [
            { left: '$$', right: '$$', display: true },
            { left: '$', right: '$', display: false },
            { left: '\\[', right: '\\]', display: true },
            { left: '\\(', right: '\\)', display: false }
          ],
          throwOnError: false
        });
      }
    });
  });
};

const handleView = (row: ArxivPaper) => {
  currentPaper.value = row;
  viewDialogVisible.value = true;
  renderLatex();
};

const handleEdit = (row: ArxivPaper) => {
  router.push(`/papers/${row.id}/edit`);
};

const handleOpenLink = (url?: string) => {
  if (url) {
    window.open(url, '_blank');
  }
};

// 兼容后端返回的两种分页格式：PageCacheDTO(content/totalElements) 和 IPage(records/total)
const getPageData = (data: any) => {
  return {
    content: data.content ?? data.records ?? [],
    totalElements: data.totalElements ?? data.total ?? 0
  };
};

const convertDatabasePapers = (papers: any[]): ArxivPaper[] => {
  return papers.map(paper => ({
    id: paper.id,
    arxivId: paper.arxivId,
    title: paper.title,
    summary: paper.summary,
    authors: paper.authors,
    publishedDate: paper.publishedDate,
    updatedDate: paper.updatedDate,
    primaryCategory: paper.primaryCategory,
    categories: paper.categories,
    pdfUrl: paper.pdfUrl,
    latexUrl: paper.latexUrl,
    arxivUrl: paper.arxivUrl,
    doi: paper.doi,
    version: paper.version,
    createdTime: paper.createdTime,
    updatedTime: paper.updatedTime
  }));
};

onMounted(() => {
  handleSearchAll();
});
</script>

<style scoped>
.paper-list {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.search-card {
  margin-bottom: 20px;
}

.search-form {
  margin-bottom: 0;
}

.table-card {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.table-card :deep(.el-card__body) {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.el-table {
  flex: 1;
  overflow: auto;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.summary-text {
  white-space: pre-wrap;
  word-break: break-word;
  line-height: 1.6;
  max-height: 300px;
  overflow-y: auto;
}

.latex-content {
  line-height: 1.6;
}

.latex-content .katex {
  font-size: 1em;
}

.latex-content .katex-display {
  margin: 0.5em 0;
}
</style>
