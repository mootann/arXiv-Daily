import request from '@/utils/request';
import type { ApiResponse, ArxivPaperDTO, ArxivSearchRequest, ArxivSearchResponse } from '@/types';

export const arxivApi = {
  getPaperById(arxivId: string) {
    return request.get<ApiResponse<ArxivPaperDTO>>(`/arxiv/paper/${arxivId}`);
  },

  getPapersByIds(arxivIds: string[]) {
    return request.post<ApiResponse<ArxivPaperDTO[]>>('/arxiv/papers/batch', arxivIds);
  },

  searchPapers(data: ArxivSearchRequest) {
    return request.post<ApiResponse<ArxivSearchResponse>>('/arxiv/search', data);
  },

  searchByCategory(category: string, maxResults: number = 10) {
    return request.get<ApiResponse<ArxivSearchResponse>>(`/arxiv/category/${category}`, {
      params: { maxResults }
    });
  },

  searchByKeyword(keyword: string, maxResults: number = 10) {
    return request.get<ApiResponse<ArxivSearchResponse>>(`/arxiv/keyword/${keyword}`, {
      params: { maxResults }
    });
  },

  searchByAuthor(author: string, maxResults: number = 10) {
    return request.get<ApiResponse<ArxivSearchResponse>>(`/arxiv/author/${author}`, {
      params: { maxResults }
    });
  },

  searchByCategoryAndDateRange(category: string, startDate: string, endDate: string, maxResults?: number) {
    return request.get<ApiResponse<ArxivSearchResponse>>(`/arxiv/category/${category}/date-range`, {
      params: { startDate, endDate, maxResults }
    });
  },

  searchByCategoryFromDate(category: string, startDate: string, maxResults: number = 10) {
    return request.get<ApiResponse<ArxivSearchResponse>>(`/arxiv/category/${category}/from-date`, {
      params: { startDate, maxResults }
    });
  },

  searchByCategoryToDate(category: string, endDate: string, maxResults: number = 10) {
    return request.get<ApiResponse<ArxivSearchResponse>>(`/arxiv/category/${category}/to-date`, {
      params: { endDate, maxResults }
    });
  },

  searchByDateRange(startDate: string, endDate: string, maxResults?: number) {
    return request.get<ApiResponse<ArxivSearchResponse>>('/arxiv/date-range', {
      params: { startDate, endDate, maxResults }
    });
  },

  searchByDate(date: string, maxResults?: number) {
    return request.get<ApiResponse<ArxivSearchResponse>>('/arxiv/date', {
      params: { date, maxResults }
    });
  },

  searchRecentPapers(days: number = 7, maxResults?: number) {
    return request.get<ApiResponse<ArxivSearchResponse>>('/arxiv/recent', {
      params: { days, maxResults }
    });
  },

  searchRecentPapersByCategory(category: string, days: number = 7, maxResults?: number) {
    return request.get<ApiResponse<ArxivSearchResponse>>(`/arxiv/category/${category}/recent`, {
      params: { days, maxResults }
    });
  },

  searchByMultipleCategories(categories: string[], maxResults: number = 10) {
    return request.post<ApiResponse<ArxivSearchResponse>>('/arxiv/categories/multiple', categories, {
      params: { maxResults }
    });
  },

  searchByMultipleCategoriesAndDateRange(categories: string[], startDate: string, endDate: string, maxResults: number = 10) {
    return request.post<ApiResponse<ArxivSearchResponse>>('/arxiv/categories/multiple/date-range', categories, {
      params: { startDate, endDate, maxResults }
    });
  },

  searchByCategoryAndKeyword(category: string, keyword: string, maxResults: number = 10) {
    return request.get<ApiResponse<ArxivSearchResponse>>(`/arxiv/category/${category}/keyword/${keyword}`, {
      params: { maxResults }
    });
  },

  // ==================== 数据库查询API ====================

  getPapersFromDatabase(page: number = 1, size: number = 10, hasGithub?: boolean) {
    return request.get<ApiResponse<any>>('/arxiv/database/papers', {
      params: { page, size, hasGithub }
    });
  },

  searchPapersFromDatabase(keyword: string, page: number = 1, size: number = 10, hasGithub?: boolean) {
    return request.get<ApiResponse<any>>('/arxiv/database/search', {
      params: { keyword, page, size, hasGithub }
    });
  },

  getPapersByCategoryFromDatabase(category: string, page: number = 1, size: number = 10, hasGithub?: boolean) {
    return request.get<ApiResponse<any>>(`/arxiv/database/category/${category}`, {
      params: { page, size, hasGithub }
    });
  },

  getPapersByDateRangeFromDatabase(startDate: string, endDate: string, page: number = 1, size: number = 10, hasGithub?: boolean) {
    return request.get<ApiResponse<any>>('/arxiv/database/date-range', {
      params: { startDate, endDate, page, size, hasGithub }
    });
  },

  getPapersByCategoryAndDateRangeFromDatabase(category: string, startDate: string, endDate: string, page: number = 1, size: number = 10, hasGithub?: boolean) {
    return request.get<ApiResponse<any>>(`/arxiv/database/category/${category}/date-range`, {
      params: { startDate, endDate, page, size, hasGithub }
    });
  },

  getPapersByCategoryAndKeywordFromDatabase(category: string, keyword: string, page: number = 1, size: number = 10, hasGithub?: boolean) {
    return request.get<ApiResponse<any>>(`/arxiv/database/category/${category}/keyword/${keyword}`, {
      params: { page, size, hasGithub }
    });
  },

  getPaperFromDatabase(arxivId: string) {
    return request.get<ApiResponse<any>>(`/arxiv/database/paper/${arxivId}`);
  },

  getPapersByIdsFromDatabase(arxivIds: string[]) {
    return request.post<ApiResponse<any>>('/arxiv/database/papers/batch', arxivIds);
  }
};
