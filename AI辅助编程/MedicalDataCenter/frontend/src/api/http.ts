import axios, { AxiosError, type AxiosInstance, type AxiosRequestConfig } from 'axios';
import type { ApiResponse } from '@/types/common';

const storageTokenKey = 'mdc-token';

const httpClient: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? '/api',
  timeout: 10000,
});

httpClient.interceptors.request.use((config) => {
  const token = window.localStorage.getItem(storageTokenKey);
  const requestUrl = config.url ?? '';
  const isLoginRequest = requestUrl === '/auth/login' || requestUrl.endsWith('/auth/login');

  if (token && !isLoginRequest) {
    config.headers.Authorization = `Bearer ${token}`;
  }

  config.headers['X-Request-Source'] = 'medical-data-center-web';
  return config;
});

httpClient.interceptors.response.use(
  (response) => response,
  (error: AxiosError<ApiResponse<unknown>>) => {
    if (error.response?.status === 401) {
      window.localStorage.removeItem(storageTokenKey);
      if (window.location.pathname !== '/login') {
        window.location.href = '/login';
      }
    }
    return Promise.reject(error);
  },
);

export async function request<T>(config: AxiosRequestConfig): Promise<T> {
  const response = await httpClient.request<ApiResponse<T>>(config);
  return response.data.data;
}

export { httpClient };
