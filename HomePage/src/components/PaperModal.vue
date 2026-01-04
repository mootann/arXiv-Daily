<template>
  <div v-if="show" class="modal-overlay" @click.self="closeModal">
    <div class="modal-container">
      <button class="modal-close-btn" @click="closeModal">
        <i class="fas fa-times"></i>
      </button>
      
      <div v-if="loading && !paper" class="loading-container">
        <div class="spinner"></div>
        <p>加载论文详情...</p>
      </div>

      <div v-else-if="error" class="error-container">
        <i class="fas fa-exclamation-circle"></i>
        <p>{{ error }}</p>
        <button @click="loadPaper" class="retry-btn">重试</button>
      </div>

      <div v-else-if="paper" class="paper-detail-modal">
        <!-- 头部信息 -->
        <header class="modal-header">
          <h1 class="paper-title" v-html="renderedTitle"></h1>
          <div class="paper-meta-row">
            <span class="meta-item">
              <span class="meta-label">作者:</span>
              <span class="meta-value">{{ formatAuthors(paper.authors) }}</span>
            </span>
            <span class="meta-item">
              <span class="meta-label">发布日期:</span>
              <span class="meta-value">{{ formatDate(paper.publishedDate) }}</span>
            </span>
          </div>
        </header>

        <!-- 内容区域 -->
        <div class="modal-body">
          <!-- 左侧：摘要和信息 -->
          <div class="info-column">
            <div class="paper-meta-grid">
              <div class="meta-item">
                <span class="meta-label">分类:</span>
                <span class="category-tag" v-for="category in paper.categories" :key="category">
                  {{ category }}
                </span>
              </div>
              <div class="meta-item">
                <span class="meta-label">arXiv ID:</span>
                <span class="meta-value">{{ paper.arxivId }}</span>
              </div>
            </div>

            <div class="paper-abstract">
              <h3 class="section-title">摘要</h3>
              <div class="abstract-content" v-html="renderedSummary"></div>
            </div>

            <div class="paper-links">
              <a :href="paper.arxivUrl" target="_blank" rel="noopener noreferrer" class="link-btn">
                <i class="fas fa-external-link-alt"></i>
                arXiv
              </a>
              <a :href="paper.pdfUrl" target="_blank" rel="noopener noreferrer" class="link-btn">
                <i class="fas fa-file-pdf"></i>
                原文 PDF
              </a>
              <a :href="paper.latexUrl" target="_blank" rel="noopener noreferrer" class="link-btn" v-if="paper.latexUrl">
                <i class="fas fa-file-code"></i>
                LaTeX
              </a>
              <a :href="paper.githubUrl" target="_blank" rel="noopener noreferrer" class="link-btn github-btn" v-if="paper.githubUrl">
                <i class="fab fa-github"></i>
                GitHub
              </a>
            </div>
          </div>

          <!-- 右侧：PDF 预览 -->
          <div class="pdf-column">
            <div class="pdf-wrapper">
              <iframe 
                v-if="pdfUrl" 
                :src="pdfUrl" 
                class="pdf-iframe" 
                frameborder="0" 
                allow="autoplay; encrypted-media" 
                allowfullscreen
                title="PDF Preview"
              ></iframe>
              <div v-else class="pdf-loading">
                <div class="spinner"></div>
                <p>准备加载 PDF...</p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue';
import { getPaperById } from '@/api/arxiv';
import type { ArxivPaper } from '@/types/arxiv';
import 'katex/dist/katex.min.css';
// @ts-ignore
import renderMathInElement from 'katex/dist/contrib/auto-render';

const props = defineProps<{
  show: boolean;
  arxivId: string;
}>();

const emit = defineEmits(['close']);

const paper = ref<ArxivPaper | null>(null);
const loading = ref(false);
const error = ref('');
const pdfUrl = ref('');

const renderedTitle = ref('');
const renderedSummary = ref('');

// 监听显示状态和ID变化
watch(() => props.show, (newVal) => {
  if (newVal && props.arxivId) {
    loadPaper();
  } else {
    // 重置状态
    paper.value = null;
    pdfUrl.value = '';
    error.value = '';
  }
});

watch(() => props.arxivId, (newId) => {
  if (props.show && newId) {
    loadPaper();
  }
});

const closeModal = () => {
  emit('close');
};

const renderLatex = () => {
  if (!paper.value) return;
  
  const katexOptions = {
    delimiters: [
      { left: '$$', right: '$$', display: true },
      { left: '$', right: '$', display: false },
      { left: '\\[', right: '\\]', display: true },
      { left: '\\(', right: '\\)', display: false }
    ],
    throwOnError: false,
    strict: false,
    trust: true
  };

  const titleElement = document.createElement('div');
  titleElement.innerHTML = paper.value.title;
  renderMathInElement(titleElement, katexOptions);
  renderedTitle.value = titleElement.innerHTML;

  const summaryElement = document.createElement('div');
  summaryElement.innerHTML = paper.value.summary;
  renderMathInElement(summaryElement, katexOptions);
  renderedSummary.value = summaryElement.innerHTML;
};

const loadPaper = async () => {
  if (!props.arxivId) return;
  
  loading.value = true;
  error.value = '';
  
  try {
    const response = await getPaperById(props.arxivId);
    
    if (response.data && response.data.data) {
      paper.value = response.data.data;
      renderLatex();
      
      if (paper.value.arxivUrl) {
        // 直接使用 arXiv PDF URL (将 abs 替换为 pdf)
        pdfUrl.value = paper.value.arxivUrl.replace('/abs/', '/pdf/');
      } else if (paper.value.pdfUrl) {
        pdfUrl.value = paper.value.pdfUrl;
      }
    } else {
      error.value = '论文未找到';
    }
  } catch (err: any) {
    console.error('Failed to load paper:', err);
    error.value = err.message || '加载论文失败';
  } finally {
    loading.value = false;
  }
};

const formatDate = (dateStr: string) => {
  if (!dateStr) return '';
  const date = new Date(dateStr);
  return date.toLocaleDateString('zh-CN', { year: 'numeric', month: 'long', day: 'numeric' });
};

const formatAuthors = (authors: string[]) => {
  if (!authors || authors.length === 0) return '';
  // 限制显示的作者数量，避免过长
  if (authors.length > 5) {
    return authors.slice(0, 5).join(', ') + ' 等';
  }
  return authors.join(', ');
};
</script>

<style scoped>
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.75);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
  backdrop-filter: blur(5px);
}

.modal-container {
  background-color: #1e1e1e;
  width: 95%;
  height: 90vh;
  border-radius: 12px;
  position: relative;
  display: flex;
  flex-direction: column;
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.5);
  max-width: 1600px;
  overflow: hidden;
}

.modal-close-btn {
  position: absolute;
  top: 15px;
  right: 15px;
  background: transparent;
  border: none;
  color: #a0aec0;
  font-size: 24px;
  cursor: pointer;
  z-index: 10;
  padding: 5px;
  transition: color 0.2s;
}

.modal-close-btn:hover {
  color: #fff;
}

.loading-container, .error-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  color: #e2e8f0;
}

.spinner {
  width: 40px;
  height: 40px;
  border: 3px solid rgba(255, 255, 255, 0.3);
  border-radius: 50%;
  border-top-color: #3182ce;
  animation: spin 1s ease-in-out infinite;
  margin-bottom: 15px;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.paper-detail-modal {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.modal-header {
  padding: 20px 30px;
  border-bottom: 1px solid #2d3748;
  background-color: #1a202c;
}

.paper-title {
  font-size: 20px;
  font-weight: 700;
  color: #fff;
  margin: 0 0 10px 0;
  line-height: 1.4;
  padding-right: 30px;
}

.paper-meta-row {
  display: flex;
  gap: 20px;
  font-size: 14px;
  color: #a0aec0;
  align-items: center;
}

.code-link {
  color: #63b3ed;
  text-decoration: none;
  transition: color 0.2s;
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.code-link:hover {
  color: #3182ce;
  text-decoration: underline;
}

.code-link i {
  font-size: 12px;
}

.meta-label {
  color: #718096;
  margin-right: 5px;
}

.modal-body {
  flex: 1;
  display: flex;
  overflow: hidden;
}

.info-column {
  width: 35%;
  padding: 20px;
  overflow-y: auto;
  border-right: 1px solid #2d3748;
  background-color: #1a202c;
}

.pdf-column {
  flex: 1;
  display: flex;
  flex-direction: column;
  background-color: #2d3748;
  position: relative;
}

.paper-meta-grid {
  margin-bottom: 20px;
}

.meta-item {
  margin-bottom: 8px;
  font-size: 14px;
}

.category-tag {
  display: inline-block;
  background-color: #2b6cb0;
  color: white;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
  margin-right: 5px;
  margin-bottom: 5px;
}

.section-title {
  font-size: 16px;
  color: #63b3ed;
  margin: 0 0 10px 0;
  border-bottom: 1px solid #4a5568;
  padding-bottom: 5px;
}

.abstract-content {
  font-size: 14px;
  line-height: 1.6;
  color: #cbd5e0;
  margin-bottom: 25px;
  text-align: justify;
}

.paper-links {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.link-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  background-color: #4a5568;
  color: white;
  border-radius: 6px;
  text-decoration: none;
  font-size: 13px;
  transition: background-color 0.2s;
}

.link-btn:hover {
  background-color: #2b6cb0;
}

.link-btn.github-btn {
  background-color: #2d3748;
  border: 1px solid #4a5568;
}

.link-btn.github-btn:hover {
  background-color: #4a5568;
  border-color: #718096;
}

.pdf-wrapper {
  flex: 1;
  overflow: hidden;
  display: flex;
  justify-content: center;
  position: relative;
  width: 100%;
  height: 100%;
}

.pdf-iframe {
  width: 100%;
  height: 100%;
  border: none;
  background-color: white; /* Ensure iframe has white background for PDF visibility */
}

.pdf-loading {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  text-align: center;
  color: #e2e8f0;
}

@media (max-width: 1024px) {
  .modal-body {
    flex-direction: column;
    overflow-y: auto;
  }
  
  .info-column {
    width: 100%;
    border-right: none;
    border-bottom: 1px solid #2d3748;
    max-height: 40%;
  }
  
  .pdf-column {
    flex: 1;
    min-height: 400px;
  }
}
</style>