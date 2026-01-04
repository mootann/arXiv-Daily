import axios from 'axios';
import type { AxiosInstance, AxiosResponse, InternalAxiosRequestConfig } from 'axios';
import type { ApiResponse } from '@/types';

// 创建 arxiv API 的独立实例
const arxivService: AxiosInstance = axios.create({
  baseURL: '/api/v1/arxiv',
  timeout: 30000, // Arxiv API 可能需要更长时间
});

// Arxiv API request interceptor
arxivService.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = localStorage.getItem('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// Arxiv API response interceptor
arxivService.interceptors.response.use(
  (response: AxiosResponse<ApiResponse>) => {
    const res = response.data;
    if (res.code === 200) {
      return response;
    } else {
      console.error('Arxiv API Error:', res.message || 'Error');
      return Promise.reject(new Error(res.message || 'Error'));
    }
  },
  (error) => {
    console.error('Arxiv Request Error:', error);
    const message = error.response?.data?.message || error.message || '请求失败';
    return Promise.reject(new Error(message));
  }
);

// 创建 GitHub API 的独立实例
const githubService: AxiosInstance = axios.create({
  baseURL: '/api/v1/github',
  timeout: 10000,
});

// GitHub API request interceptor
githubService.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = localStorage.getItem('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// GitHub API response interceptor
githubService.interceptors.response.use(
  (response: AxiosResponse<ApiResponse>) => {
    const res = response.data;
    if (res.code === 200) {
      return response;
    } else {
      console.error('GitHub API Error:', res.message || 'Error');
      return Promise.reject(new Error(res.message || 'Error'));
    }
  },
  (error) => {
    console.error('GitHub Request Error:', error);
    const message = error.response?.data?.message || error.message || '请求失败';
    return Promise.reject(new Error(message));
  }
);

// 创建 auth API 的独立实例
const authService: AxiosInstance = axios.create({
  baseURL: '/api/v1',
  timeout: 10000,
});

// Auth API request interceptor
authService.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = localStorage.getItem('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// Auth API response interceptor
authService.interceptors.response.use(
  (response: AxiosResponse<ApiResponse>) => {
    const res = response.data;
    if (res.code === 200) {
      return response;
    } else {
      console.error('Auth API Error:', res.message || 'Error');
      return Promise.reject(new Error(res.message || 'Error'));
    }
  },
  (error) => {
    console.error('Auth Request Error:', error);
    const message = error.response?.data?.message || error.message || '请求失败';
    return Promise.reject(new Error(message));
  }
);

export { arxivService, githubService, authService };
