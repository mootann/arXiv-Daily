export interface ArxivPaper {
  id: number;
  arxivId: string;
  title: string;
  summary: string;
  authors: string;
  publishedDate: string;
  updatedDate: string;
  primaryCategory: string;
  categories: string;
  pdfUrl: string;
  latexUrl: string;
  arxivUrl: string;
  doi: string;
  version: number;
  createdTime: string;
  updatedTime: string;
}

export interface ArxivPaperDTO extends ArxivPaper {}

export interface ArxivSearchRequest {
  query: string;
  maxResults?: number;
  startIndex?: number;
  sortBy?: string;
  sortOrder?: string;
}

export interface ArxivEntry {
  id: string;
  title: string;
  summary: string;
  authors: string[];
  published: string;
  updated: string;
  primaryCategory: string;
  categories: string[];
  pdfUrl: string;
  latexUrl: string;
  arxivUrl: string;
  doi: string;
  version: number;
}

export interface ArxivSearchResponse {
  totalResults: number;
  startIndex: number;
  itemsPerPage: number;
  entries: ArxivEntry[];
  actualStartDate?: string;  // 实际查询的开始日期
  actualEndDate?: string;    // 实际查询的结束日期
  categoryCounts?: CategoryCount[];  // 分类统计信息
}

export interface CategoryCount {
  category: string;
  count: number;
}

// 论文查询请求参数
export interface ArxivPaperQueryRequest {
  category?: string[];
  keyword?: string;
  startDate?: string;
  endDate?: string;
  page?: number;
  size?: number;
  hasGithub?: boolean;
}
