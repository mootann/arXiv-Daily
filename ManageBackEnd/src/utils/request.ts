import axios, { type AxiosInstance, type InternalAxiosRequestConfig, type AxiosResponse } from 'axios';
import type { ApiResponse } from '@/types';
import { ElMessage } from 'element-plus';

const service: AxiosInstance = axios.create({
  baseURL: '/api/v1',
  timeout: 30000,
});

service.interceptors.request.use(
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

service.interceptors.response.use(
  (response: AxiosResponse<ApiResponse>) => {
    const res = response.data;
    if (res.code === 200) {
      return response;
    } else {
      ElMessage.error(res.message || '请求失败');
      return Promise.reject(new Error(res.message || '请求失败'));
    }
  },
  (error) => {
    ElMessage.error(error.message || '网络错误');
    return Promise.reject(error);
  }
);

export default service;
