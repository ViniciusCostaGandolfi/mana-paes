import { api } from "~/lib/api";
import type { LoginResponse, MessageResponse, RegisterRequest } from "~/types/api";

export const login = async (email: string, password: string): Promise<LoginResponse> => {
  const { data } = await api.post<LoginResponse>("/auth/login", { email, password });
  return data;
};

export const register = async (data: RegisterRequest): Promise<LoginResponse> => {
  const { data: response } = await api.post<LoginResponse>("/auth/register", data);
  return response;
};

export const forgotPassword = async (email: string): Promise<MessageResponse> => {
  const { data } = await api.post<MessageResponse>("/auth/forgot-password", { email });
  return data;
};

export const resetPassword = async (token: string, password: string): Promise<MessageResponse> => {
  const { data } = await api.post<MessageResponse>("/auth/reset-password", { token, password });
  return data;
};

export const refreshToken = async (refreshToken: string): Promise<LoginResponse> => {
  const { data } = await api.post<LoginResponse>("/auth/refresh", { refreshToken });
  return data;
};