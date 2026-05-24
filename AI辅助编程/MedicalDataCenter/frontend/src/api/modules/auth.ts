import type { LoginForm } from '@/types/auth';
import { request } from '@/api/http';

export function loginApi(payload: LoginForm) {
  return request<{
    accessToken: string;
    tokenType: string;
    username: string;
    displayName: string;
    authorities: string[];
  }>({
    url: '/auth/login',
    method: 'post',
    data: payload,
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
