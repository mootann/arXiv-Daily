<template>
  <div class="paper-cover" ref="containerRef">
    <canvas ref="canvasRef" v-show="loaded"></canvas>
    <div v-if="loading" class="loading-cover">
      <i class="fas fa-spinner fa-spin"></i>
    </div>
    <div v-if="error" class="error-cover">
      <div class="error-content">
        <i class="fas fa-file-pdf"></i>
        <span>PDF</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch } from 'vue';
import * as pdfjsLib from 'pdfjs-dist';
// @ts-ignore
import pdfWorker from 'pdfjs-dist/build/pdf.worker.mjs?url';

pdfjsLib.GlobalWorkerOptions.workerSrc = pdfWorker;

const props = defineProps<{
  url: string;
  title?: string;
}>();

const containerRef = ref<HTMLElement | null>(null);
const canvasRef = ref<HTMLCanvasElement | null>(null);
const loaded = ref(false);
const loading = ref(false);
const error = ref(false);
let observer: IntersectionObserver | null = null;
let loadingTask: any = null;

const loadCover = async () => {
  if (!props.url || loaded.value || loading.value) return;
  
  try {
    loading.value = true;
    error.value = false;
    
    // Check if URL is valid (simple check)
    if (!props.url.startsWith('http')) {
        // Only throw if it's not a valid URL structure we expect
        // But relative URLs might be valid in some contexts. 
        // Here we expect full URLs for arXiv.
        if (props.url && !props.url.startsWith('/')) {
             throw new Error('Invalid URL');
        }
    }

    loadingTask = pdfjsLib.getDocument({
      url: props.url,
      cMapUrl: 'https://unpkg.com/pdfjs-dist@5.4.530/cmaps/',
      cMapPacked: true,
      disableAutoFetch: true,
      disableStream: true,
      rangeChunkSize: 65536 * 2 // 128KB
    });

    const pdf = await loadingTask.promise;
    const page = await pdf.getPage(1);
    
    const canvas = canvasRef.value;
    if (!canvas) return;

    // Adjust scale to fit width/height of container, or fixed scale
    // Let's use a fixed reasonable scale for quality, CSS will downscale
    const viewport = page.getViewport({ scale: 1.0 }); 
    const context = canvas.getContext('2d');
    
    if (context) {
        canvas.height = viewport.height;
        canvas.width = viewport.width;

        await page.render({
            canvasContext: context,
            viewport: viewport
        }).promise;
        
        loaded.value = true;
    }
  } catch (e) {
    console.error('Error loading PDF cover:', e);
    error.value = true;
  } finally {
    loading.value = false;
    loadingTask = null;
  }
};

onMounted(() => {
  observer = new IntersectionObserver((entries) => {
    if (entries[0] && entries[0].isIntersecting) {
      loadCover();
      if (observer && containerRef.value) {
        observer.unobserve(containerRef.value);
      }
    }
  }, { rootMargin: '200px' });

  if (containerRef.value) {
    observer.observe(containerRef.value);
  }
});

onUnmounted(() => {
  if (observer) {
    observer.disconnect();
  }
  if (loadingTask) {
    loadingTask.destroy();
  }
});

watch(() => props.url, () => {
  loaded.value = false;
  error.value = false;
  // Re-observe if needed, or just load if already visible
  if (containerRef.value && observer) {
      observer.observe(containerRef.value);
  }
});
</script>

<style scoped>
.paper-cover {
  width: 100%;
  height: 100%;
  aspect-ratio: 210/297; /* A4 ratio */
  background: #f8fafc;
  border-radius: 8px;
  overflow: hidden;
  position: relative;
  flex-shrink: 0;
  border: 1px solid #e2e8f0;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 2px 4px rgba(0,0,0,0.05);
}

canvas {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.loading-cover {
  color: #94a3b8;
  font-size: 24px;
}

.error-cover {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #f1f5f9;
  color: #cbd5e1;
}

.error-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.error-content i {
  font-size: 32px;
}

.error-content span {
  font-size: 12px;
  font-weight: 600;
}
</style>