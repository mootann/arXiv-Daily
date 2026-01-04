export interface ArxivPaper {
  arxivId: string;
  title: string;
  summary: string;
  authors: string[];
  publishedDate: string;
  updatedDate: string;
  primaryCategory: string;
  categories: string[];
  pdfUrl: string;
  latexUrl: string;
  arxivUrl: string;
  doi?: string;
  version: number;
  githubUrl?: string;
}

export interface ArxivSearchResponse {
  totalResults: number;
  startIndex: number;
  itemsPerPage: number;
  papers: ArxivPaper[];
  actualStartDate?: string;  // 实际查询的开始日期
  actualEndDate?: string;    // 实际查询的结束日期
  categoryCounts?: CategoryCount[];  // 分类统计信息
}

export interface CategoryCount {
  category: string;
  count: number;
}

export interface ArxivSearchParams {
  category?: string;
  startDate?: string;
  endDate?: string;
  days?: number;
  keyword?: string;
  author?: string;
  maxResults?: number;
  categories?: string[];
}
