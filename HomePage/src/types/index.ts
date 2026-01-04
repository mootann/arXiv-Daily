export interface ApiResponse<T = any> {
  code: number;
  message: string;
  data: T;
}

export interface User {
  id: number;
  username: string;
  token?: string;
}

export interface GitHubRepositoryInfo {
  full_name: string;
  description: string;
  stargazers_count: number;
  forks_count: number;
  subscribers_count: number;
  open_issues_count: number;
  html_url: string;
  created_at: string;
  updated_at: string;
}

export interface GitHubStats {
  stars: number;
  forks: number;
}

export * from './arxiv';
