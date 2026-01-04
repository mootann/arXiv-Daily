<template>
  <div class="data-sync">
    <el-row :gutter="20">
      <el-col :span="12">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>按分类同步</span>
            </div>
          </template>

          <el-form :model="syncByCategoryForm" label-width="120px">
            <el-form-item label="选择分类">
              <el-select v-model="syncByCategoryForm.category" placeholder="选择分类" style="width: 100%">
                <el-option
                  v-for="cat in categories"
                  :key="cat.value"
                  :label="cat.label"
                  :value="cat.value"
                />
              </el-select>
            </el-form-item>

            <el-form-item label="获取数量">
              <el-input-number v-model="syncByCategoryForm.maxResults" :min="1" />
            </el-form-item>

            <el-form-item>
              <el-button type="primary" @click="handleSyncByCategory" :loading="syncing">
                开始同步
              </el-button>
              <el-button @click="resetCategoryForm">重置</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>

      <el-col :span="12">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>按日期范围同步</span>
            </div>
          </template>

          <el-form :model="syncByDateForm" label-width="120px">
            <el-form-item label="选择分类">
              <el-select v-model="syncByDateForm.category" placeholder="选择分类（可选）" clearable style="width: 100%">
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
                v-model="syncByDateForm.startDate"
                type="date"
                placeholder="选择开始日期"
                value-format="YYYY-MM-DD"
                style="width: 100%"
              />
            </el-form-item>

            <el-form-item label="结束日期">
              <el-date-picker
                v-model="syncByDateForm.endDate"
                type="date"
                placeholder="选择结束日期"
                value-format="YYYY-MM-DD"
                style="width: 100%"
              />
            </el-form-item>

            <el-alert
              title="数据同步说明"
              type="info"
              description="系统将根据日期范围自动计算获取数量。单日最多获取1000篇论文，多日查询最多获取天数×1000篇论文。例如：7天最多获取7000篇论文。"
              :closable="false"
              style="margin-bottom: 20px"
            />

            <el-form-item>
              <el-button type="primary" @click="handleSyncByDateRange" :loading="syncing">
                开始同步
              </el-button>
              <el-button @click="resetDateForm">重置</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="12">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>按关键词同步</span>
            </div>
          </template>

          <el-form :model="syncByKeywordForm" label-width="120px">
            <el-form-item label="关键词">
              <el-input v-model="syncByKeywordForm.keyword" placeholder="输入关键词" />
            </el-form-item>

            <el-form-item label="选择分类">
              <el-select v-model="syncByKeywordForm.category" placeholder="选择分类（可选）" clearable style="width: 100%">
                <el-option
                  v-for="cat in categories"
                  :key="cat.value"
                  :label="cat.label"
                  :value="cat.value"
                />
              </el-select>
            </el-form-item>

            <el-form-item label="获取数量">
              <el-input-number v-model="syncByKeywordForm.maxResults" :min="1" />
            </el-form-item>

            <el-form-item>
              <el-button type="primary" @click="handleSyncByKeyword" :loading="syncing">
                开始同步
              </el-button>
              <el-button @click="resetKeywordForm">重置</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>

      <el-col :span="12">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>最近论文同步</span>
            </div>
          </template>

          <el-form :model="syncRecentForm" label-width="120px">
            <el-form-item label="天数">
              <el-input-number v-model="syncRecentForm.days" :min="1" :max="30" />
            </el-form-item>

            <el-form-item label="选择分类">
              <el-select v-model="syncRecentForm.category" placeholder="选择分类（可选）" clearable style="width: 100%">
                <el-option
                  v-for="cat in categories"
                  :key="cat.value"
                  :label="cat.label"
                  :value="cat.value"
                />
              </el-select>
            </el-form-item>

            <el-alert
              title="数据同步说明"
              type="info"
              description="系统将根据天数自动计算获取数量，最多获取天数×1000篇论文。例如：7天最多获取7000篇论文。"
              :closable="false"
              style="margin-bottom: 20px"
            />

            <el-form-item>
              <el-button type="primary" @click="handleSyncRecent" :loading="syncing">
                开始同步
              </el-button>
              <el-button @click="resetRecentForm">重置</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>
    </el-row>

    <el-card style="margin-top: 20px">
      <template #header>
        <div class="card-header">
          <span>同步日志</span>
          <el-button type="danger" @click="clearLogs" :disabled="logs.length === 0">清空日志</el-button>
        </div>
      </template>

      <div class="log-container">
        <div v-if="logs.length === 0" class="empty-logs">暂无同步日志</div>
        <div v-for="(log, index) in logs" :key="index" class="log-item">
          <el-tag :type="log.type" size="small">{{ log.time }}</el-tag>
          <span :class="['log-message', log.type]">{{ log.message }}</span>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue';
import { ElMessage } from 'element-plus';
import { arxivApi } from '@/api/arxiv';

const syncing = ref(false);
const logs = ref<Array<{ time: string; message: string; type: 'success' | 'error' | 'info' }>>([]);

const syncByCategoryForm = reactive({
  category: '',
  maxResults: 10
});

const syncByDateForm = reactive({
  category: '',
  startDate: '',
  endDate: ''
});

const syncByKeywordForm = reactive({
  keyword: '',
  category: '',
  maxResults: 10
});

const syncRecentForm = reactive({
  days: 7,
  category: ''
});

const categories = [
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

const addLog = (message: string, type: 'success' | 'error' | 'info') => {
  const now = new Date();
  const time = `${now.getHours().toString().padStart(2, '0')}:${now.getMinutes().toString().padStart(2, '0')}:${now.getSeconds().toString().padStart(2, '0')}`;
  logs.value.unshift({ time, message, type });
  if (logs.value.length > 100) {
    logs.value.pop();
  }
};

const handleSyncByCategory = async () => {
  if (!syncByCategoryForm.category) {
    ElMessage.warning('请选择分类');
    return;
  }

  syncing.value = true;
  addLog(`开始同步分类 ${syncByCategoryForm.category} 的论文...`, 'info');

  try {
    const response = await arxivApi.searchByCategory(
      syncByCategoryForm.category,
      syncByCategoryForm.maxResults
    );
    const data = response.data.data;
    addLog(`成功获取 ${data.totalResults} 篇论文`, 'success');
    ElMessage.success(`成功获取 ${data.totalResults} 篇论文`);
  } catch (error) {
    addLog(`同步失败: ${error}`, 'error');
    ElMessage.error('同步失败');
  } finally {
    syncing.value = false;
  }
};

const handleSyncByDateRange = async () => {
  if (!syncByDateForm.startDate || !syncByDateForm.endDate) {
    ElMessage.warning('请选择开始日期和结束日期');
    return;
  }

  syncing.value = true;
  addLog(`开始同步 ${syncByDateForm.startDate} 到 ${syncByDateForm.endDate} 的论文（自动计算最大获取数量）...`, 'info');

  try {
    let response;
    if (syncByDateForm.category) {
      // 不传递maxResults参数，让后端自动根据日期范围计算
      response = await arxivApi.searchByCategoryAndDateRange(
        syncByDateForm.category,
        syncByDateForm.startDate,
        syncByDateForm.endDate
      );
    } else {
      // 不传递maxResults参数，让后端自动根据日期范围计算
      response = await arxivApi.searchByDateRange(
        syncByDateForm.startDate,
        syncByDateForm.endDate
      );
    }
    const data = response.data.data;
    addLog(`成功获取 ${data.papers.length} 篇论文（总共 ${data.totalResults} 篇）`, 'success');
    ElMessage.success(`成功获取 ${data.papers.length} 篇论文`);
  } catch (error) {
    addLog(`同步失败: ${error}`, 'error');
    ElMessage.error('同步失败');
  } finally {
    syncing.value = false;
  }
};

const handleSyncByKeyword = async () => {
  if (!syncByKeywordForm.keyword) {
    ElMessage.warning('请输入关键词');
    return;
  }

  syncing.value = true;
  addLog(`开始同步关键词 "${syncByKeywordForm.keyword}" 的论文...`, 'info');

  try {
    let response;
    if (syncByKeywordForm.category) {
      response = await arxivApi.searchByCategoryAndKeyword(
        syncByKeywordForm.category,
        syncByKeywordForm.keyword,
        syncByKeywordForm.maxResults
      );
    } else {
      response = await arxivApi.searchByKeyword(
        syncByKeywordForm.keyword,
        syncByKeywordForm.maxResults
      );
    }
    const data = response.data.data;
    addLog(`成功获取 ${data.totalResults} 篇论文`, 'success');
    ElMessage.success(`成功获取 ${data.totalResults} 篇论文`);
  } catch (error) {
    addLog(`同步失败: ${error}`, 'error');
    ElMessage.error('同步失败');
  } finally {
    syncing.value = false;
  }
};

const handleSyncRecent = async () => {
  syncing.value = true;
  addLog(`开始同步最近 ${syncRecentForm.days} 天的论文（自动计算最大获取数量）...`, 'info');

  try {
    let response;
    if (syncRecentForm.category) {
      // 不传递maxResults参数，让后端自动根据天数计算
      response = await arxivApi.searchRecentPapersByCategory(
        syncRecentForm.category,
        syncRecentForm.days
      );
    } else {
      // 不传递maxResults参数，让后端自动根据天数计算
      response = await arxivApi.searchRecentPapers(
        syncRecentForm.days
      );
    }
    const data = response.data.data;
    addLog(`成功获取 ${data.papers.length} 篇论文（总共 ${data.totalResults} 篇）`, 'success');
    ElMessage.success(`成功获取 ${data.papers.length} 篇论文`);
  } catch (error) {
    addLog(`同步失败: ${error}`, 'error');
    ElMessage.error('同步失败');
  } finally {
    syncing.value = false;
  }
};

const resetCategoryForm = () => {
  syncByCategoryForm.category = '';
  syncByCategoryForm.maxResults = 10;
};

const resetDateForm = () => {
  syncByDateForm.category = '';
  syncByDateForm.startDate = '';
  syncByDateForm.endDate = '';
};

const resetKeywordForm = () => {
  syncByKeywordForm.keyword = '';
  syncByKeywordForm.category = '';
  syncByKeywordForm.maxResults = 10;
};

const resetRecentForm = () => {
  syncRecentForm.days = 7;
  syncRecentForm.category = '';
};

const clearLogs = () => {
  logs.value = [];
  ElMessage.success('日志已清空');
};
</script>

<style scoped>
.data-sync {
  height: 100%;
  overflow-y: auto;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 500;
}

.log-container {
  max-height: 400px;
  overflow-y: auto;
  border: 1px solid #e6e6e6;
  border-radius: 4px;
  padding: 10px;
}

.empty-logs {
  text-align: center;
  color: #909399;
  padding: 40px 0;
}

.log-item {
  display: flex;
  align-items: center;
  padding: 8px 0;
  border-bottom: 1px solid #f0f0f0;
}

.log-item:last-child {
  border-bottom: none;
}

.log-message {
  margin-left: 10px;
  flex: 1;
}

.log-message.success {
  color: #67c23a;
}

.log-message.error {
  color: #f56c6c;
}

.log-message.info {
  color: #909399;
}
</style>
