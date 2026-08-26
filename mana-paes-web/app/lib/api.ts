import axios, {
  type AxiosError,
  type AxiosInstance,
  type InternalAxiosRequestConfig,
} from "axios";
import type { LoginResponse } from "~/types/api";
import {
  clearTokens,
  getAccessToken,
  getRefreshToken,
  setStoredUser,
  setTokens,
} from "./auth";

/**
 * Corpo padrão de erro da API (record ApiError):
 * { status, message, timestamp, errors }.
 */
interface ApiErrorResponse {
  status: number;
  message: string;
  timestamp: string;
  errors?: string[] | null;
}

const baseURL = import.meta.env.VITE_API_URL ?? "/api/v1";

export const api: AxiosInstance = axios.create({
  baseURL,
  timeout: 15_000,
  headers: {
    "Content-Type": "application/json",
  },
});

api.interceptors.request.use((config) => {
  const accessToken = getAccessToken();
  if (accessToken) {
    config.headers.Authorization = `Bearer ${accessToken}`;
  }
  return config;
});

let isRefreshing = false;
let pendingQueue: Array<{
  resolve: () => void;
  reject: (reason?: unknown) => void;
}> = [];

const isAuthUrl = (url?: string): boolean =>
  !!url && (url.includes("/auth/login") || url.includes("/auth/refresh"));

interface RetriableRequestConfig extends InternalAxiosRequestConfig {
  _retry?: boolean;
}

api.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const originalRequest = error.config as RetriableRequestConfig | undefined;

    if (
      error.response?.status === 401 &&
      originalRequest &&
      !originalRequest._retry &&
      !isAuthUrl(originalRequest.url)
    ) {
      originalRequest._retry = true;

      // Single-flight: se outra requisição já está renovando o token, aguarda.
      if (isRefreshing) {
        return new Promise<void>((resolve, reject) => {
          pendingQueue.push({ resolve, reject });
        }).then(() => api(originalRequest));
      }

      isRefreshing = true;
      const refreshToken = getRefreshToken();

      try {
        // axios puro (sem interceptors) para evitar recursão.
        const { data } = await axios.post<LoginResponse>(
          `${baseURL}/auth/refresh`,
          { refreshToken },
        );

        setTokens(data.accessToken, data.refreshToken);
        pendingQueue.forEach(({ resolve }) => resolve());
        pendingQueue = [];

        // Reexecuta a requisição original uma vez (interceptor de request
        // anexa o novo access token).
        return api(originalRequest);
      } catch (refreshError) {
        pendingQueue.forEach(({ reject }) => reject(refreshError));
        pendingQueue = [];
        clearTokens();
        setStoredUser(null);
        window.location.assign("/login");
        return Promise.reject(refreshError);
      } finally {
        isRefreshing = false;
      }
    }

    return Promise.reject(error);
  },
);

export function getApiErrorMessage(error: unknown): string {
  if (axios.isAxiosError<ApiErrorResponse>(error)) {
    const data = error.response?.data;

    if (data) {
      const message = data.message?.trim();
      if (message) {
        const details = data.errors?.filter((e) => !!e?.trim()).join(" · ");
        return details ? `${message} — ${details}` : message;
      }
    }

    if (error.code === "ECONNABORTED") {
      return "A requisição excedeu o tempo limite. Tente novamente.";
    }

    if (!error.response) {
      return "Não foi possível conectar ao servidor. Verifique sua conexão.";
    }

    return `Ocorreu um erro inesperado (HTTP ${error.response.status}). Tente novamente.`;
  }

  return "Erro inesperado. Tente novamente.";
}