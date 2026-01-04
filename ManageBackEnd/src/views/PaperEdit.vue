<template>
  <div class="paper-edit">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>编辑论文</span>
          <el-button type="primary" @click="handleSave" :loading="saving">保存</el-button>
        </div>
      </template>

      <el-form :model="form" :rules="rules" ref="formRef" label-width="120px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="arXiv ID" prop="arxivId">
              <el-input v-model="form.arxivId" disabled />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="版本" prop="version">
              <el-input-number v-model="form.version" :min="0" :disabled="true" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" type="textarea" :rows="2" />
        </el-form-item>

        <el-form-item label="作者" prop="authors">
          <el-input v-model="form.authors" type="textarea" :rows="2" />
        </el-form-item>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="主要分类" prop="primaryCategory">
              <el-select v-model="form.primaryCategory" placeholder="选择分类" style="width: 100%">
                <el-option
                  v-for="cat in categories"
                  :key="cat.value"
                  :label="cat.label"
                  :value="cat.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="所有分类" prop="categories">
              <el-input v-model="form.categories" placeholder="多个分类用逗号分隔" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="发布日期" prop="publishedDate">
              <el-date-picker
                v-model="form.publishedDate"
                type="date"
                placeholder="选择发布日期"
                value-format="YYYY-MM-DD"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="更新日期" prop="updatedDate">
              <el-date-picker
                v-model="form.updatedDate"
                type="date"
                placeholder="选择更新日期"
                value-format="YYYY-MM-DD"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="DOI" prop="doi">
          <el-input v-model="form.doi" placeholder="输入DOI（可选）" />
        </el-form-item>

        <el-form-item label="摘要" prop="summary">
          <el-input
            v-model="form.summary"
            type="textarea"
            :rows="6"
            placeholder="输入论文摘要"
          />
        </el-form-item>

        <el-form-item label="arXiv链接" prop="arxivUrl">
          <el-input v-model="form.arxivUrl" placeholder="输入arXiv链接" />
        </el-form-item>

        <el-form-item label="PDF链接" prop="pdfUrl">
          <el-input v-model="form.pdfUrl" placeholder="输入PDF链接" />
        </el-form-item>

        <el-form-item label="LaTeX链接" prop="latexUrl">
          <el-input v-model="form.latexUrl" placeholder="输入LaTeX源码链接" />
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import type { FormInstance, FormRules } from 'element-plus';
import { arxivApi } from '@/api/arxiv';
import type { ArxivPaper } from '@/types/arxiv';

const router = useRouter();

const formRef = ref<FormInstance>();
const saving = ref(false);

const form = reactive<ArxivPaper>({
  id: 0,
  arxivId: '',
  title: '',
  summary: '',
  authors: '',
  publishedDate: '',
  updatedDate: '',
  primaryCategory: '',
  categories: '',
  pdfUrl: '',
  latexUrl: '',
  arxivUrl: '',
  doi: '',
  version: 0,
  createdTime: '',
  updatedTime: ''
});

const rules: FormRules = {
  arxivId: [
    { required: true, message: 'arXiv ID不能为空', trigger: 'blur' }
  ],
  title: [
    { required: true, message: '标题不能为空', trigger: 'blur' }
  ],
  authors: [
    { required: true, message: '作者不能为空', trigger: 'blur' }
  ],
  primaryCategory: [
    { required: true, message: '主要分类不能为空', trigger: 'change' }
  ],
  publishedDate: [
    { required: true, message: '发布日期不能为空', trigger: 'change' }
  ]
};

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

const loadPaperData = async () => {
  try {
    const response = await arxivApi.getPaperById('');
    const data = response.data.data as ArxivPaper;
    if (data) {
      Object.assign(form, data);
    }
  } catch (error) {
    ElMessage.error('加载论文数据失败');
  }
};

const handleSave = async () => {
  if (!formRef.value) return;

  await formRef.value.validate(async (valid) => {
    if (valid) {
      saving.value = true;
      try {
        ElMessage.success('保存成功');
        router.push('/papers');
      } catch (error) {
        ElMessage.error('保存失败');
      } finally {
        saving.value = false;
      }
    } else {
      ElMessage.warning('请填写必填项');
    }
  });
};

onMounted(() => {
  loadPaperData();
});
</script>

<style scoped>
.paper-edit {
  height: 100%;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
