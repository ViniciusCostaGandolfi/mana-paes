import { api } from "~/lib/api";
import type { PageResponse, UserRequest, UserResponse } from "~/types/api";

export const listUsers = async (params: {
  page?: number;
  size?: number;
}): Promise<PageResponse<UserResponse>> => {
  const { data } = await api.get<PageResponse<UserResponse>>("/users", { params });
  return data;
};

export const getUser = async (id: string): Promise<UserResponse> => {
  const { data } = await api.get<UserResponse>(`/users/${id}`);
  return data;
};

export const createUser = async (data: UserRequest): Promise<UserResponse> => {
  const response = await api.post<UserResponse>("/users", data);
  return response.data;
};

export const updateUser = async (id: string, data: UserRequest): Promise<UserResponse> => {
  const response = await api.put<UserResponse>(`/users/${id}`, data);
  return response.data;
};

export const setUserActive = async (id: string, active: boolean): Promise<UserResponse> => {
  const { data } = await api.patch<UserResponse>(`/users/${id}/status`, { active });
  return data;
};