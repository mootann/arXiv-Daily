<template>
  <main class="paper-detail-container">
    <div class="back-btn-container">
      <button class="back-btn" @click="goBack">
        <i class="fas fa-arrow-left"></i>
        返回
      </button>
    </div>

    <div v-if="loading" class="loading-container">
      <div class="spinner"></div>
      <p>加载论文详情...</p>
    </div>

    <div v-else-if="error" class="error-container">
      <i class="fas fa-exclamation-circle"></i>
      <p>{{ error }}</p>
      <button @click="loadPaper" class="retry-btn">重试</button>
    </div>

    <div v-else-if="paper" class="paper-detail">
      <section class="paper-info">
        <h1 class="paper-title" v-html="renderedTitle"></h1>

        <div class="paper-meta">
          <div class="meta-item">
            <span class="meta-label">作者:</span>
            <span class="meta-value">{{ formatAuthors(paper.authors) }}</span>
          </div>
          <div class="meta-item">
            <span class="meta-label">分类:</span>
            <span class="meta-value">
              <span class="category-tag" v-for="category in paper.categories" :key="category">
                {{ category }}
              </span>
            </span>
          </div>
          <div class="meta-item">
            <span class="meta-label">发布日期:</span>
            <span class="meta-value">{{ formatDate(paper.publishedDate) }}</span>
          </div>
          <div class="meta-item" v-if="paper.updatedDate !== paper.publishedDate">
            <span class="meta-label">更新日期:</span>
            <span class="meta-value">{{ formatDate(paper.updatedDate) }}</span>
          </div>
          <div class="meta-item">
            <span class="meta-label">版本:</span>
            <span class="meta-value">v{{ paper.version }}</span>
          </div>
          <div class="meta-item">
            <span class="meta-label">arXiv ID:</span>
            <span class="meta-value">{{ paper.arxivId }}</span>
          </div>
        </div>

        <div class="paper-abstract">
          <h2 class="section-title">摘要</h2>
          <div class="abstract-content" v-html="renderedSummary"></div>
        </div>

        <div class="paper-links">
          <a :href="paper.arxivUrl" target="_blank" rel="noopener noreferrer" class="link-btn">
            <i class="fas fa-external-link-alt"></i>
            arXiv 链接
          </a>
          <a :href="paper.pdfUrl" target="_blank" rel="noopener noreferrer" class="link-btn">
            <i class="fas fa-file-pdf"></i>
            下载 PDF
          </a>
          <a :href="paper.latexUrl" target="_blank" rel="noopener noreferrer" class="link-btn" v-if="paper.latexUrl">
            <i class="fas fa-file-code"></i>
            源码 (LaTeX)
          </a>
          <a :href="paper.githubUrl" target="_blank" rel="noopener noreferrer" class="link-btn github-btn" v-if="paper.githubUrl">
            <i class="fab fa-github"></i>
            GitHub 仓库
          </a>
        </div>
      </section>

      <section class="pdf-preview">
        <h2 class="section-title">PDF 预览</h2>
        <div class="pdf-controls">
          <button class="control-btn" @click="prevPage" :disabled="pdfPage <= 1">
            <i class="fas fa-chevron-left"></i>
          </button>
          <span class="page-info">第 {{ pdfPage }} 页 / 共 {{ pdfPages }} 页</span>
          <button class="control-btn" @click="nextPage" :disabled="pdfPage >= pdfPages">
            <i class="fas fa-chevron-right"></i>
          </button>
          <button class="control-btn" @click="zoomOut" :disabled="pdfScale <= 0.5">
            <i class="fas fa-search-minus"></i>
          </button>
          <span class="scale-info">{{ Math.round(pdfScale * 100) }}%</span>
          <button class="control-btn" @click="zoomIn" :disabled="pdfScale >= 3">
            <i class="fas fa-search-plus"></i>
          </button>
          <button class="control-btn" @click="rotateLeft">
            <i class="fas fa-undo"></i>
          </button>
          <button class="control-btn" @click="rotateRight">
            <i class="fas fa-redo"></i>
          </button>
        </div>
        <div class="pdf-container" ref="pdfContainer">
          <div v-if="pdfLoading" class="pdf-loading">
            <div class="spinner"></div>
            <p>加载 PDF 中...</p>
          </div>
          <canvas v-show="!pdfLoading" ref="pdfCanvas"></canvas>
        </div>
      </section>
    </div>
  </main>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { getPaperById } from '@/api/arxiv';
import type { ArxivPaper } from '@/types/arxiv';
import * as pdfjsLib from 'pdfjs-dist';
import 'katex/dist/katex.min.css';
// @ts-ignore
import renderMathInElement from 'katex/dist/contrib/auto-render';

pdfjsLib.GlobalWorkerOptions.workerSrc = `//unpkg.com/pdfjs-dist@${pdfjsLib.version}/build/pdf.worker.min.js`;

const route = useRoute();
const router = useRouter();

const paper = ref<ArxivPaper | null>(null);
const loading = ref(true);
const error = ref('');

const pdfCanvas = ref<HTMLCanvasElement | null>(null);

const pdfDoc = ref<any>(null);
const pdfPage = ref(1);
const pdfPages = ref(0);
const pdfScale = ref(1.0);
const pdfRotation = ref(0);
const pdfLoading = ref(false);

const renderedTitle = ref('');
const renderedSummary = ref('');

const renderLatex = () => {
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

  const titleElement = document.createElement('div');
  titleElement.innerHTML = paper.value!.title;
  renderMathInElement(titleElement, katexOptions);
  renderedTitle.value = titleElement.innerHTML;

  const summaryElement = document.createElement('div');
  summaryElement.innerHTML = paper.value!.summary;
  renderMathInElement(summaryElement, katexOptions);
  renderedSummary.value = summaryElement.innerHTML;
};

const loadPaper = async () => {
  loading.value = true;
  error.value = '';
  
  try {
    const arxivId = route.params.id as string;
    const response = await getPaperById(arxivId);
    
    if (response.data && response.data.data) {
      paper.value = response.data.data;
      renderLatex();
      
      if (paper.value.pdfUrl) {
        await loadPdf(paper.value.pdfUrl);
      }
    } else {
      error.value = '论文未找到';
    }
  } catch (err: any) {
    console.error('Failed to load paper:', err);
    error.value = err.message || '加载论文失败，请重试';
  } finally {
    loading.value = false;
  }
};

const loadPdf = async (pdfUrl: string) => {
  pdfLoading.value = true;
  
  try {
    const loadingTask = pdfjsLib.getDocument(pdfUrl);
    pdfDoc.value = await loadingTask.promise;
    pdfPages.value = pdfDoc.value.numPages;
    pdfPage.value = 1;
    
    await renderPdfPage();
  } catch (err: any) {
    console.error('Failed to load PDF:', err);
    error.value = 'PDF 加载失败';
  } finally {
    pdfLoading.value = false;
  }
};

const renderPdfPage = async () => {
  if (!pdfDoc.value || !pdfCanvas.value) return;

  try {
    const page = await pdfDoc.value.getPage(pdfPage.value);
    const canvas = pdfCanvas.value;
    const context = canvas.getContext('2d');
    
    if (!context) return;

    const viewport = page.getViewport({ scale: pdfScale.value, rotation: pdfRotation.value });
    
    canvas.height = viewport.height;
    canvas.width = viewport.width;

    const renderContext = {
      canvasContext: context,
      viewport: viewport
    };

    await page.render(renderContext).promise;
  } catch (err) {
    console.error('Failed to render PDF page:', err);
  }
};

const prevPage = () => {
  if (pdfPage.value > 1) {
    pdfPage.value--;
    renderPdfPage();
  }
};

const nextPage = () => {
  if (pdfPage.value < pdfPages.value) {
    pdfPage.value++;
    renderPdfPage();
  }
};

const zoomIn = () => {
  if (pdfScale.value < 3) {
    pdfScale.value += 0.25;
    renderPdfPage();
  }
};

const zoomOut = () => {
  if (pdfScale.value > 0.5) {
    pdfScale.value -= 0.25;
    renderPdfPage();
  }
};

const rotateLeft = () => {
  pdfRotation.value = (pdfRotation.value - 90 + 360) % 360;
  renderPdfPage();
};

const rotateRight = () => {
  pdfRotation.value = (pdfRotation.value + 90) % 360;
  renderPdfPage();
};

const formatDate = (dateStr: string) => {
  if (!dateStr) return '';
  const date = new Date(dateStr);
  return date.toLocaleDateString('zh-CN', { year: 'numeric', month: 'long', day: 'numeric' });
};

const formatAuthors = (authors: string[]) => {
  if (!authors || authors.length === 0) return '';
  return authors.join(', ');
};

const goBack = () => {
  router.back();
};

onMounted(() => {
  loadPaper();
});
</script>

<style scoped>
.paper-detail-container {
  max-width: 1400px;
  margin: 0 auto;
  padding: 20px;
}

.back-btn-container {
  margin-bottom: 20px;
}

.back-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 10px 20px;
  background-color: #3498db;
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 2px 8px rgba(52, 152, 219, 0.3);
}

.back-btn:hover {
  background-color: #2980b9;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(52, 152, 219, 0.4);
}

.loading-container,
.error-container {
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

.paper-detail {
  display: grid;
  grid-template-columns: 1fr;
  gap: 30px;
}

.paper-info {
  background: linear-gradient(135deg, #1e1e1e 0%, #2a2a2a 100%);
  border-radius: 16px;
  padding: 30px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.3);
}

.paper-title {
  font-size: 28px;
  font-weight: 700;
  margin: 0 0 24px 0;
  line-height: 1.4;
  color: #fff;
}

.paper-meta {
  margin-bottom: 24px;
}

.meta-item {
  display: flex;
  margin-bottom: 12px;
  font-size: 14px;
}

.meta-label {
  min-width: 80px;
  font-weight: 600;
  color: #3498db;
  margin-right: 12px;
}

.meta-value {
  flex: 1;
  color: #e0e0e0;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}

.category-tag {
  display: inline-block;
  padding: 4px 12px;
  background: linear-gradient(135deg, #3498db 0%, #2980b9 100%);
  color: white;
  border-radius: 16px;
  font-size: 12px;
  font-weight: 600;
}

.paper-abstract {
  margin-bottom: 24px;
}

.section-title {
  font-size: 20px;
  font-weight: 600;
  margin: 0 0 16px 0;
  color: #3498db;
}

.abstract-content {
  background: linear-gradient(135deg, #2c2c2c 0%, #3a3a3a 100%);
  padding: 20px;
  border-radius: 12px;
  color: #e0e0e0;
  line-height: 1.6;
  font-size: 14px;
}

.paper-links {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.link-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 10px 20px;
  background: linear-gradient(135deg, #3498db 0%, #2980b9 100%);
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 2px 8px rgba(52, 152, 219, 0.3);
  text-decoration: none;
}

.link-btn:hover {
  background: linear-gradient(135deg, #2980b9 0%, #1a5276 100%);
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(52, 152, 219, 0.4);
}

.link-btn.github-btn {
    background: linear-gradient(135deg, #2c2c2c 0%, #3a3a3a 100%);
    border: 1px solid #404040;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
}

.link-btn.github-btn:hover {
    background: linear-gradient(135deg, #3a3a3a 0%, #4a4a4a 100%);
    border-color: #606060;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.4);
}

.pdf-preview {
  background: linear-gradient(135deg, #1e1e1e 0%, #2a2a2a 100%);
  border-radius: 16px;
  padding: 30px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.3);
}

.pdf-controls {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}

.control-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  background: linear-gradient(135deg, #3498db 0%, #2980b9 100%);
  color: white;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s ease;
  font-size: 14px;
}

.control-btn:hover:not(:disabled) {
  background: linear-gradient(135deg, #2980b9 0%, #1a5276 100%);
  transform: translateY(-2px);
}

.control-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.page-info,
.scale-info {
  font-size: 14px;
  font-weight: 600;
  color: #e0e0e0;
  min-width: 120px;
  text-align: center;
}

.pdf-container {
  display: flex;
  justify-content: center;
  align-items: flex-start;
  min-height: 500px;
  background: linear-gradient(135deg, #2c2c2c 0%, #3a3a3a 100%);
  border-radius: 12px;
  padding: 20px;
  overflow: auto;
}

.pdf-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px;
  color: #e0e0e0;
}

.pdf-loading p {
  margin-top: 16px;
}

canvas {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.5);
  max-width: 100%;
}

@media (min-width: 1024px) {
  .paper-detail {
    grid-template-columns: 1fr 1fr;
  }
  
  .pdf-preview {
    position: sticky;
    top: 20px;
    height: fit-content;
  }
}
</style>
