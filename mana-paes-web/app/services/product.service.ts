import { api } from "~/lib/api";
import type { PageResponse, ProductRequest, ProductResponse } from "~/types/api";

export const listProducts = async (params: {
  active?: boolean;
  page?: number;
  size?: number;
}): Promise<PageResponse<ProductResponse>> => {
  const { data } = await api.get<PageResponse<ProductResponse>>("/products", { params });
  return data;
};

export const getProduct = async (id: string): Promise<ProductResponse> => {
  const { data } = await api.get<ProductResponse>(`/products/${id}`);
  return data;
};

export const createProduct = async (data: ProductRequest): Promise<ProductResponse> => {
  const response = await api.post<ProductResponse>("/products", data);
  return response.data;
};

export const updateProduct = async (id: string, data: ProductRequest): Promise<ProductResponse> => {
  const response = await api.put<ProductResponse>(`/products/${id}`, data);
  return response.data;
};

export const setProductActive = async (id: string, active: boolean): Promise<ProductResponse> => {
  const { data } = await api.patch<ProductResponse>(`/products/${id}/active`, { active });
  return data;
};