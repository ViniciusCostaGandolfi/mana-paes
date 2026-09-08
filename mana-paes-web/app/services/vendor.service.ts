import { api } from "~/lib/api";
import type { VendorRequest, VendorResponse } from "~/types/api";

export const listVendors = async (): Promise<VendorResponse[]> => {
  const { data } = await api.get<VendorResponse[]>("/vendors");
  return data;
};

export const getVendor = async (id: string): Promise<VendorResponse> => {
  const { data } = await api.get<VendorResponse>(`/vendors/${id}`);
  return data;
};

export const createVendor = async (data: VendorRequest): Promise<VendorResponse> => {
  const { data: response } = await api.post<VendorResponse>("/vendors", data);
  return response;
};

export const updateVendor = async (id: string, data: VendorRequest): Promise<VendorResponse> => {
  const { data: response } = await api.put<VendorResponse>(`/vendors/${id}`, data);
  return response;
};

export const setVendorActive = async (id: string, active: boolean): Promise<VendorResponse> => {
  const { data } = await api.patch<VendorResponse>(`/vendors/${id}/status`, { active });
  return data;
};