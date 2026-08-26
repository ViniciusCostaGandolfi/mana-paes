import { api } from "~/lib/api";
import type { TenantRequest, TenantResponse } from "~/types/api";

export const getTenant = async (): Promise<TenantResponse> => {
  const { data } = await api.get<TenantResponse>("/tenant");
  return data;
};

export const updateTenant = async (data: TenantRequest): Promise<TenantResponse> => {
  const response = await api.put<TenantResponse>("/tenant", data);
  return response.data;
};