import { api } from "~/lib/api";
import type {
  MessageResponse,
  NotificationChannel,
  NotificationConfigRequest,
  NotificationConfigResponse,
  NotificationLogResponse,
  NotificationStatus,
  PageResponse,
} from "~/types/api";

export const getConfig = async (): Promise<NotificationConfigResponse> => {
  const { data } = await api.get<NotificationConfigResponse>("/notifications/config");
  return data;
};

export const updateConfig = async (
  data: NotificationConfigRequest,
): Promise<NotificationConfigResponse> => {
  const response = await api.put<NotificationConfigResponse>("/notifications/config", data);
  return response.data;
};

export const testWhatsapp = async (): Promise<MessageResponse> => {
  const { data } = await api.post<MessageResponse>("/notifications/whatsapp/test");
  return data;
};

export const listLogs = async (params: {
  status?: NotificationStatus;
  channel?: NotificationChannel;
  page?: number;
  size?: number;
}): Promise<PageResponse<NotificationLogResponse>> => {
  const { data } = await api.get<PageResponse<NotificationLogResponse>>("/notifications/logs", {
    params,
  });
  return data;
};