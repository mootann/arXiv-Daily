import { githubService } from '@/utils/request';
import type { ApiResponse } from '@/types';
import type { GitHubRepositoryInfo, GitHubStats } from '@/types';

export const getRepositoryInfo = (): Promise<ApiResponse<GitHubRepositoryInfo>> => {
  return githubService.get('/repository').then(res => res.data);
};

export const getRepositoryStats = (): Promise<ApiResponse<GitHubStats>> => {
  return githubService.get('/stats').then(res => res.data);
};

export const getStarsCount = (): Promise<ApiResponse<number>> => {
  return githubService.get('/stars').then(res => res.data);
};

export const getForksCount = (): Promise<ApiResponse<number>> => {
  return githubService.get('/forks').then(res => res.data);
};
