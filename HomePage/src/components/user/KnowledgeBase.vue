<template>
  <div class="knowledge-container">
    <div class="toolbar">
      <div class="search-box">
        <i class="fas fa-search"></i>
        <input type="text" v-model="searchQuery" placeholder="检索知识库" />
      </div>
      <div class="actions">
        <label class="upload-btn" :class="{ disabled: isUploading }">
          <i class="fas" :class="isUploading ? 'fa-spinner fa-spin' : 'fa-plus'"></i>
          <span>{{ isUploading ? '上传中...' : '新增' }}</span>
          <input 
            type="file" 
            @change="handleUpload" 
            accept=".pdf,.doc,.docx,.md,.txt" 
            style="display: none" 
            :disabled="isUploading"
          />
        </label>
        <button class="refresh-btn" @click="fetchDocuments">
          <i class="fas fa-sync-alt"></i>
          <span>刷新</span>
        </button>
      </div>
    </div>

    <div class="file-list">
      <table>
        <thead>
          <tr>
            <th>文件名</th>
            <th>文件大小</th>
            <th>上传状态</th>
            <th>是否公开</th>
            <th>上传时间</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="loading">
            <td colspan="6" class="empty-cell">加载中...</td>
          </tr>
          <tr v-else-if="filteredDocuments.length === 0">
            <td colspan="6" class="empty-cell">暂无文件</td>
          </tr>
          <tr v-for="doc in filteredDocuments" :key="doc.id">
            <td class="file-name">
              <i :class="getFileIcon(doc.fileType)"></i>
              <span>{{ doc.fileName }}</span>
            </td>
            <td>{{ formatSize(doc.fileSize) }}</td>
            <td><span class="status-badge success">已完成</span></td>
            <td>
              <span class="privacy-badge" :class="doc.isPublic ? 'public' : 'private'">
                {{ doc.isPublic ? '公开' : '私有' }}
              </span>
            </td>
            <td>{{ formatDate(doc.createdTime) }}</td>
            <td>
              <button class="delete-btn" @click="deleteDocument(doc.id)">删除</button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { authService as request } from '@/utils/request';

interface KnowledgeDocument {
  id: number;
  fileName: string;
  fileSize: number;
  fileType: string;
  isPublic: boolean;
  createdTime: string;
}

const documents = ref<KnowledgeDocument[]>([]);
const searchQuery = ref('');
const loading = ref(false);
const isUploading = ref(false);

const filteredDocuments = computed(() => {
  if (!searchQuery.value) return documents.value;
  const query = searchQuery.value.toLowerCase();
  return documents.value.filter(doc => 
    doc.fileName.toLowerCase().includes(query)
  );
});

const fetchDocuments = async () => {
  loading.value = true;
  try {
    const res = await request.get('/api/v1/knowledge/list');
    if (res.data.code === 200) {
      documents.value = res.data.data;
    }
  } catch (error) {
    console.error('Failed to fetch documents', error);
  } finally {
    loading.value = false;
  }
};

const handleUpload = async (event: Event) => {
 const input = event.target as HTMLInputElement;
  const files = input.files;
  if (!files || files.length === 0) return;
  
  const file = files[0] as File;
  
  const formData = new FormData();
  formData.append('file', file);
  formData.append('isPublic', 'false'); // 默认私有

  isUploading.value = true;
  try {
    const res = await request.post('/api/v1/knowledge/upload', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    });
    if (res.data.code === 200) {
      await fetchDocuments();
    } else {
      alert('上传失败: ' + res.data.message);
    }
  } catch (error) {
    console.error('Upload failed', error);
    alert('上传出错');
  } finally {
    isUploading.value = false;
    input.value = ''; // 重置 input
  }
};

const deleteDocument = async (id: number) => {
  if (!confirm('确定要删除这个文件吗？')) return;
  
  try {
    const res = await request.delete(`/api/v1/knowledge/${id}`);
    if (res.data.code === 200) {
      await fetchDocuments();
    } else {
      alert('删除失败: ' + res.data.message);
    }
  } catch (error) {
    console.error('Delete failed', error);
    alert('删除出错');
  }
};

const formatSize = (bytes: number) => {
  if (bytes === 0) return '0 B';
  const k = 1024;
  const sizes = ['B', 'KB', 'MB', 'GB', 'TB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
};

const formatDate = (dateString: string) => {
  const date = new Date(dateString);
  return date.toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit'
  });
};

const getFileIcon = (fileType: string) => {
  if (fileType.includes('pdf')) return 'fas fa-file-pdf pdf-icon';
  if (fileType.includes('word') || fileType.includes('document')) return 'fas fa-file-word word-icon';
  if (fileType.includes('text') || fileType.includes('markdown')) return 'fas fa-file-alt text-icon';
  return 'fas fa-file file-icon';
};

onMounted(() => {
  fetchDocuments();
});
</script>

<style scoped>
.knowledge-container {
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.05);
  min-height: 600px;
  display: flex;
  flex-direction: column;
}

.toolbar {
  padding: 20px 24px;
  border-bottom: 1px solid #eee;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.search-box {
  position: relative;
  width: 300px;
}

.search-box i {
  position: absolute;
  left: 12px;
  top: 50%;
  transform: translateY(-50%);
  color: #999;
}

.search-box input {
  width: 100%;
  padding: 10px 12px 10px 36px;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  font-size: 14px;
  outline: none;
  transition: all 0.3s;
}

.search-box input:focus {
  border-color: #3498db;
  box-shadow: 0 0 0 3px rgba(52, 152, 219, 0.1);
}

.actions {
  display: flex;
  gap: 12px;
}

.upload-btn, .refresh-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 20px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s;
  border: 1px solid transparent;
}

.upload-btn {
  background: #3498db;
  color: #fff;
}

.upload-btn:hover:not(.disabled) {
  background: #2980b9;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(52, 152, 219, 0.3);
}

.upload-btn.disabled {
  background: #bdc3c7;
  cursor: not-allowed;
  transform: none;
}

.refresh-btn {
  background: #fff;
  border-color: #e0e0e0;
  color: #666;
}

.refresh-btn:hover {
  border-color: #3498db;
  color: #3498db;
}

.file-list {
  padding: 24px;
  flex: 1;
}

table {
  width: 100%;
  border-collapse: separate;
  border-spacing: 0;
}

th {
  text-align: left;
  padding: 12px 16px;
  font-size: 13px;
  font-weight: 600;
  color: #666;
  border-bottom: 2px solid #f0f0f0;
}

td {
  padding: 16px;
  font-size: 14px;
  color: #2c3e50;
  border-bottom: 1px solid #f8f8f8;
  vertical-align: middle;
}

.file-name {
  display: flex;
  align-items: center;
  gap: 12px;
  font-weight: 500;
}

.file-name i {
  font-size: 20px;
}

.pdf-icon { color: #e74c3c; }
.word-icon { color: #3498db; }
.text-icon { color: #95a5a6; }
.file-icon { color: #bdc3c7; }

.status-badge {
  display: inline-block;
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
}

.status-badge.success {
  background: rgba(46, 204, 113, 0.15);
  color: #27ae60;
}

.privacy-badge {
  display: inline-block;
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 12px;
  border: 1px solid currentColor;
}

.privacy-badge.private {
  color: #f39c12;
  background: rgba(243, 156, 18, 0.1);
}

.privacy-badge.public {
  color: #3498db;
  background: rgba(52, 152, 219, 0.1);
}

.delete-btn {
  padding: 6px 12px;
  border: 1px solid #e74c3c;
  background: #fff;
  color: #e74c3c;
  border-radius: 6px;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.3s;
}

.delete-btn:hover {
  background: #e74c3c;
  color: #fff;
}

.empty-cell {
  text-align: center;
  color: #999;
  padding: 48px;
  font-size: 15px;
}
</style>
