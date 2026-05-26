import type { LoginForm } from '@/types/auth';
import { request } from '@/api/http';

export interface AuthTokenPayload {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  refreshExpiresIn: number;
  username: string;
  displayName: string;
  authorities: string[];
}

export function loginApi(payload: LoginForm) {
  return request<AuthTokenPayload>({
    url: '/auth/login',
    method: 'post',
    data: payload,
  });
}

export function refreshTokenApi(refreshToken: string) {
  return request<AuthTokenPayload>({
    url: '/auth/refresh',
    method: 'post',
    data: { refreshToken },
  });
}

export function logoutApi() {
  return request<void>({
    url: '/auth/logout',
    method: 'post',
  });
}

export function fetchCurrentUserApi() {
  return request<{
    username: string;
    displayName: string;
    tokenKind: string;
    authorities: string[];
  }>({
    url: '/auth/me',
    method: 'get',
  });
}
