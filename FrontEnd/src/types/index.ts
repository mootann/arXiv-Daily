// 论文类型定义
export interface ArxivPaper {
  id: number
  arxivId: string
  title: string
  summary: string
  authors: string
  publishedDate: string
  updatedDate: string
  primaryCategory: string
  categories: string
  pdfUrl: string
  latexUrl: string
  arxivUrl: string
  doi: string
  version: number
  githubUrl?: string
  likeCount?: number
  commentCount?: number
  collectCount?: number
  viewCount?: number
  hotValue?: number
  thumbnailUrl?: string
  isLiked?: boolean
  isCollected?: boolean
}

// 用户类型定义
export interface User {
  id: number
  username: string
  email: string
  role: string
}

// 登录请求
export interface LoginRequest {
  username: string
  password: string
}

// 注册请求
export interface RegisterRequest {
  username: string
  email: string
  password: string
}

// 登录响应
export interface LoginResponse {
  accessToken: string
  refreshToken: string
  userId: number
  username: string
  role: string
}

// API响应
export interface ApiResponse<T = any> {
  code: number
  message: string
  data: T
}

// 分类类型
export interface Category {
  value: string
  label: string
}

// 用户关注分类DTO
export interface UserFollowCategoryDTO {
  category: string
  todayPaperCount: number
  latestPaperDate?: string
  paperCount?: number
}

// 用户关注分类
export interface UserFollowCategory {
  id: number
  userId: number
  category: string
  createdTime: string
}

// 用户收藏论文
export interface UserCollect {
  id: number
  userId: number
  arxivId: string
  createdTime: string
}

// 论文评论
export interface PaperComment {
  id: number
  arxivId: string
  userId: number
  username: string
  content: string
  createdTime: string
}

// 分页响应
export interface PageResponse<T> {
  content?: T[]
  records?: T[]
  totalElements?: number
  total?: number
  totalPages?: number
  pages?: number
  size: number
  number?: number
  current?: number
}

// 论文查询请求参数
export interface ArxivPaperQueryRequest {
  category?: string[]
  keyword?: string
  startDate?: string
  endDate?: string
  page?: number
  size?: number
  hasGithub?: boolean
}
