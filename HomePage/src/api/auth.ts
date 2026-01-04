import { authService } from '@/utils/request';
import type { ApiResponse } from '@/types';

export interface LoginRequest {
  username: string;
  password: string;
}

export interface RegisterRequest {
  username: string;
  email: string;
  password: string;
}

export interface LoginResponse {
  token: string;
  userId: number;
  username: string;
  role: string;
  orgTags: Set<string>;
  primaryOrg: string;
}

export interface UserInfo {
  id: number;
  username: string;
  email: string;
  role: string;
  orgTags: Set<string>;
  primaryOrg: string;
  createdTime: string;
  updatedTime: string;
}

export const login = (data: LoginRequest) => {
  return authService.post<ApiResponse<LoginResponse>>('/auth/login', data)
    .then(res => res.data.data!);
};

export const register = (data: RegisterRequest) => {
  return authService.post<ApiResponse<UserInfo>>('/auth/register', data)
    .then(res => res.data.data!);
};

export const logout = (token: string) => {
  return authService.post<ApiResponse<void>>('/auth/logout', {}, {
    headers: {
      'Authorization': `Bearer ${token}`
    }
  });
};

export const getCurrentUser = () => {
  return authService.get<ApiResponse<UserInfo>>('/auth/current');
};

export const joinOrganization = (tagId: string) => {
  return authService.post<ApiResponse<UserInfo>>('/auth/organizations/join', null, {
    params: { tagId }
  });
};

export const leaveOrganization = (tagId: string) => {
  return authService.post<ApiResponse<UserInfo>>('/auth/organizations/leave', null, {
    params: { tagId }
  });
};

export const setPrimaryOrganization = (tagId: string) => {
  return authService.post<ApiResponse<UserInfo>>('/auth/organizations/primary', null, {
    params: { tagId }
  });
};
