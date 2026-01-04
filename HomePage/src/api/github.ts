import { githubService } from '@/utils/request';
import type { ApiResponse } from '@/types';
import type { GitHubRepositoryInfo } from '@/types';

export const getRepositoryInfo = (): Promise<ApiResponse<GitHubRepositoryInfo>> => {
  return githubService.get('/repository').then(res => res.data);
};

