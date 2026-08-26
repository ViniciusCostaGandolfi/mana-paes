import { api } from "~/lib/api";
import type {
  OrderRequest,
  OrderResponse,
  OrderStatusUpdateRequest,
  PageResponse,
} from "~/types/api";

export const listOrders = async (params: {
  page?: number;
  size?: number;
}): Promise<PageResponse<OrderResponse>> => {
  const { data } = await api.get<PageResponse<OrderResponse>>("/orders", { params });
  return data;
};

export const getOrder = async (id: string): Promise<OrderResponse> => {
  const { data } = await api.get<OrderResponse>(`/orders/${id}`);
  return data;
};

export const createOrder = async (data: OrderRequest): Promise<OrderResponse> => {
  const response = await api.post<OrderResponse>("/orders", data);
  return response.data;
};

export const updateOrderStatus = async (
  id: string,
  data: OrderStatusUpdateRequest,
): Promise<OrderResponse> => {
  const { data: updated } = await api.patch<OrderResponse>(`/orders/${id}/status`, data);
  return updated;
};