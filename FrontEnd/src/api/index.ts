import request from '@/utils/request'
import type { ApiResponse, ArxivPaper, ArxivPaperQueryRequest, LoginRequest, LoginResponse, RegisterRequest, User, PageResponse, UserFollowCategoryDTO } from '@/types'

export const authApi = {
  login(data: LoginRequest) {
    return request.post<ApiResponse<LoginResponse>>('/auth/login', data)
  },

  register(data: RegisterRequest) {
    return request.post<ApiResponse<User>>('/auth/register', data)
  },

  logout() {
    return request.post<ApiResponse<void>>('/auth/logout')
  },

  getCurrentUser() {
    return request.get<ApiResponse<User>>('/auth/current')
  }
}

export const paperApi = {
  // 获取论文列表（数据库）- POST请求
  getPapers(params: ArxivPaperQueryRequest = {}) {
    return request.post<ApiResponse<PageResponse<ArxivPaper>>>('/arxiv/database/papers', params)
  },

  // 搜索论文
  searchPapers(keyword: string, page: number = 1, size: number = 10, hasGithub?: boolean) {
    return request.get<ApiResponse<PageResponse<ArxivPaper>>>('/arxiv/database/search', {
      params: { keyword, page, size, hasGithub }
    })
  },

  // 按分类获取论文
  getPapersByCategory(category: string, page: number = 1, size: number = 10, hasGithub?: boolean) {
    return request.get<ApiResponse<PageResponse<ArxivPaper>>>(`/arxiv/database/category/${category}`, {
      params: { page, size, hasGithub }
    })
  },

  // 按日期范围获取论文
  getPapersByDateRange(startDate: string, endDate: string, page: number = 1, size: number = 10) {
    return request.get<ApiResponse<PageResponse<ArxivPaper>>>('/arxiv/database/date-range', {
      params: { startDate, endDate, page, size }
    })
  },

  // 获取单篇论文详情
  getPaperById(arxivId: string) {
    return request.get<ApiResponse<ArxivPaper>>(`/arxiv/database/paper/${arxivId}`)
  },

  // 批量获取论文
  getPapersByIds(arxivIds: string[]) {
    return request.post<ApiResponse<ArxivPaper[]>>('/arxiv/database/papers/batch', arxivIds)
  }
}

// 用户相关API（需要后端支持）
export const userApi = {
  // 获取用户关注的分类
  getFollowCategories() {
    return request.get<ApiResponse<UserFollowCategoryDTO[]>>('/user/follow/categories')
  },

  // 关注分类
  followCategory(category: string) {
    return request.post<ApiResponse<void>>('/user/follow/category', null, {
      params: { category }
    })
  },

  // 取消关注分类
  unfollowCategory(category: string) {
    return request.delete<ApiResponse<void>>(`/user/follow/category/${category}`)
  },

  // 获取用户收藏的论文
  getCollectedPapers(page: number = 1, size: number = 10) {
    return request.get<ApiResponse<PageResponse<ArxivPaper>>>('/user/collect/papers', {
      params: { page, size }
    })
  },

  // 收藏论文
  collectPaper(arxivId: string) {
    return request.post<ApiResponse<void>>('/user/collect/paper', null, {
      params: { arxivId }
    })
  },

  // 取消收藏论文
  uncollectPaper(arxivId: string) {
    return request.delete<ApiResponse<void>>(`/user/collect/paper/${arxivId}`)
  },

  // 点赞论文
  likePaper(arxivId: string) {
    return request.post<ApiResponse<void>>('/user/like/paper', null, {
      params: { arxivId }
    })
  },

  // 取消点赞论文
  unlikePaper(arxivId: string) {
    return request.delete<ApiResponse<void>>(`/user/like/paper/${arxivId}`)
  },

  // 获取论文评论
  getComments(arxivId: string, page: number = 1, size: number = 10) {
    return request.get<ApiResponse<PageResponse<any>>>(`/paper/comment/${arxivId}`, {
      params: { page, size }
    })
  },

  // 添加评论
  addComment(arxivId: string, content: string) {
    return request.post<ApiResponse<void>>('/paper/comment', {
      arxivId,
      content
    })
  }
}

// GitHub相关API
export const githubApi = {
  // 获取仓库信息
  getRepositoryInfo() {
    return request.get<ApiResponse<any>>('/github/repository')
  }
}

// 分类列表
export const categories = [
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
]
