import { api } from "~/lib/api";
import type {
  MessageResponse,
  OrderRequest,
  OrderResponse,
  OrderStatusUpdateRequest,
  PageResponse,
  PublicOrderRequest,
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

export const createPublicOrder = async (
  data: PublicOrderRequest,
): Promise<OrderResponse> => {
  const response = await api.post<OrderResponse>("/public/orders", data);
  return response.data;
};

export const updateOrderStatus = async (
  id: string,
  data: OrderStatusUpdateRequest,
): Promise<OrderResponse> => {
  const { data: updated } = await api.patch<OrderResponse>(`/orders/${id}/status`, data);
  return updated;
};

export const acceptAllOrders = async (): Promise<MessageResponse> => {
  const { data } = await api.post<MessageResponse>("/orders/accept-all");
  return data;
};