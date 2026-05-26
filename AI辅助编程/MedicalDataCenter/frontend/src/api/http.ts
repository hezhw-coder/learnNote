import axios, { AxiosError, type AxiosInstance, type AxiosRequestConfig } from 'axios';
import type { ApiResponse } from '@/types/common';

const storageTokenKey = 'mdc-token';
const storageRefreshTokenKey = 'mdc-refresh-token';

type RetriableRequestConfig = AxiosRequestConfig & {
  _retry?: boolean;
};

const httpClient: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? '/api',
  timeout: 10000,
});

function clearAuthStorage() {
  window.localStorage.removeItem(storageTokenKey);
  window.localStorage.removeItem(storageRefreshTokenKey);
  window.localStorage.removeItem('mdc-user');
}

function redirectToLogin() {
  if (window.location.pathname !== '/login') {
    window.location.href = '/login';
  }
}

let refreshTokenPromise: Promise<string> | null = null;

async function refreshAccessToken() {
  const refreshToken = window.localStorage.getItem(storageRefreshTokenKey);
  if (!refreshToken) {
    throw new Error('missing refresh token');
  }

  const response = await httpClient.post<ApiResponse<{ accessToken: string; refreshToken: string }>>(
    '/auth/refresh',
    { refreshToken },
    {
      headers: {
        Authorization: undefined,
      },
    },
  );

  const payload = response.data.data;
  window.localStorage.setItem(storageTokenKey, payload.accessToken);
  window.localStorage.setItem(storageRefreshTokenKey, payload.refreshToken);
  return payload.accessToken;
}

httpClient.interceptors.request.use((config) => {
  const token = window.localStorage.getItem(storageTokenKey);
  const requestUrl = config.url ?? '';
  const isLoginRequest = requestUrl === '/auth/login' || requestUrl.endsWith('/auth/login');
  const isRefreshRequest = requestUrl === '/auth/refresh' || requestUrl.endsWith('/auth/refresh');

  if (token && !isLoginRequest && !isRefreshRequest) {
    config.headers.Authorization = `Bearer ${token}`;
  }

  config.headers['X-Request-Source'] = 'medical-data-center-web';
  return config;
});

httpClient.interceptors.response.use(
  (response) => response,
  async (error: AxiosError<ApiResponse<unknown>>) => {
    const originalRequest = error.config as RetriableRequestConfig | undefined;
    const requestUrl = originalRequest?.url ?? '';
    const isLoginRequest = requestUrl === '/auth/login' || requestUrl.endsWith('/auth/login');
    const isRefreshRequest = requestUrl === '/auth/refresh' || requestUrl.endsWith('/auth/refresh');

    if (error.response?.status === 401 && originalRequest && !originalRequest._retry && !isLoginRequest && !isRefreshRequest) {
      try {
        originalRequest._retry = true;
        if (!refreshTokenPromise) {
          refreshTokenPromise = refreshAccessToken().finally(() => {
            refreshTokenPromise = null;
          });
        }
        const nextAccessToken = await refreshTokenPromise;
        originalRequest.headers = originalRequest.headers ?? {};
        originalRequest.headers.Authorization = `Bearer ${nextAccessToken}`;
        return await httpClient.request(originalRequest);
      } catch {
        clearAuthStorage();
        redirectToLogin();
      }
    }

    if (error.response?.status === 401) {
      clearAuthStorage();
      redirectToLogin();
    }

    return Promise.reject(error);
  },
);

export async function request<T>(config: AxiosRequestConfig): Promise<T> {
  const response = await httpClient.request<ApiResponse<T>>(config);
  return response.data.data;
}

export { httpClient };
