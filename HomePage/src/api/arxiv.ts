import { arxivService } from '@/utils/request';
import type { ApiResponse, ArxivPaper, ArxivSearchResponse } from '@/types';

export const getPaperById = (arxivId: string) => {
  return arxivService<ApiResponse<ArxivPaper>>({
    url: `/paper/${arxivId}`,
    method: 'get',
  });
};

export const searchByCategory = (
  category: string,
  maxResults: number = 10,
  source: string = 'api',
  page: number = 1
) => {
  return arxivService<ApiResponse<ArxivSearchResponse>>({
    url: `/category/${category}`,
    method: 'get',
    params: { maxResults, source, page },
  });
};

export const searchByCategoryAndDateRange = (
  category: string,
  startDate: string,
  endDate: string,
  maxResults: number = 10,
  source: string = 'api',
  page: number = 1,
  hasGithub?: boolean
) => {
  if (category === 'All') {
    return searchByDateRange(startDate, endDate, maxResults, source, page, hasGithub);
  }
  return arxivService<ApiResponse<ArxivSearchResponse>>({
    url: `/category/${category}/date-range`,
    method: 'get',
    params: { startDate, endDate, maxResults, source, page, hasGithub },
  });
};

export const searchByDateRange = (
  startDate: string,
  endDate: string,
  maxResults: number = 10,
  source: string = 'api',
  page: number = 1,
  hasGithub?: boolean
) => {
  return arxivService<ApiResponse<ArxivSearchResponse>>({
    url: '/date-range',
    method: 'get',
    params: { startDate, endDate, maxResults, source, page, hasGithub },
  });
};

export const searchRecentPapersByCategory = (
  category: string,
  days: number = 7,
  maxResults: number = 10,
  source: string = 'api',
  page: number = 1,
  hasGithub?: boolean
) => {
  return arxivService<ApiResponse<ArxivSearchResponse>>({
    url: `/category/${category}/recent`,
    method: 'get',
    params: { days, maxResults, source, page, hasGithub },
  });
};

export const searchRecentPapers = (
  days: number = 7,
  maxResults: number = 10,
  page: number = 1
) => {
  return arxivService<ApiResponse<ArxivSearchResponse>>({
    url: '/recent',
    method: 'get',
    params: { days, maxResults, page },
  });
};

export const searchByKeyword = (
  keyword: string,
  maxResults: number = 10,
  source: string = 'api',
  page: number = 1,
  hasGithub?: boolean
) => {
  return arxivService<ApiResponse<ArxivSearchResponse>>({
    url: `/keyword/${keyword}`,
    method: 'get',
    params: { maxResults, source, page, hasGithub },
  });
};

export const searchByAuthor = (author: string, maxResults: number = 10) => {
  return arxivService<ApiResponse<ArxivSearchResponse>>({
    url: `/author/${author}`,
    method: 'get',
    params: { maxResults },
  });
};

export const searchByMultipleCategories = (
  categories: string[],
  maxResults: number = 10
) => {
  return arxivService<ApiResponse<ArxivSearchResponse>>({
    url: '/categories/multiple',
    method: 'post',
    data: categories,
    params: { maxResults },
  });
};

export interface CategoryCount {
  category: string;
  count: number;
}

export const getCategoryCounts = (startDate?: string, endDate?: string) => {
  return arxivService<ApiResponse<CategoryCount[]>>({
    url: '/database/stats/categories',
    method: 'get',
    params: { startDate, endDate },
  });
};

export const getLatestPublishedDate = () => {
  return arxivService<ApiResponse<string>>({
    url: '/database/latest-date',
    method: 'get',
  });
};

export const getLatestPapers = (
  page: number = 1,
  size: number = 10,
  hasGithub?: boolean
) => {
  return arxivService<ApiResponse<ArxivSearchResponse>>({
    url: '/database/latest-papers',
    method: 'get',
    params: { page, size, hasGithub },
  });
};
