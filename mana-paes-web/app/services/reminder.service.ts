import { api } from "~/lib/api";
import type {
  PageResponse,
  ScheduledReminderRequest,
  ScheduledReminderResponse,
  VendorReminderResultResponse,
} from "~/types/api";

export const listReminders = async (params: {
  page?: number;
  size?: number;
}): Promise<PageResponse<ScheduledReminderResponse>> => {
  const { data } = await api.get<PageResponse<ScheduledReminderResponse>>("/reminders", {
    params,
  });
  return data;
};

export const getReminder = async (id: string): Promise<ScheduledReminderResponse> => {
  const { data } = await api.get<ScheduledReminderResponse>(`/reminders/${id}`);
  return data;
};

export const createReminder = async (
  data: ScheduledReminderRequest,
): Promise<ScheduledReminderResponse> => {
  const { data: response } = await api.post<ScheduledReminderResponse>("/reminders", data);
  return response;
};

export const updateReminder = async (
  id: string,
  data: ScheduledReminderRequest,
): Promise<ScheduledReminderResponse> => {
  const { data: response } = await api.put<ScheduledReminderResponse>(
    `/reminders/${id}`,
    data,
  );
  return response;
};

export const deleteReminder = async (id: string): Promise<void> => {
  await api.delete(`/reminders/${id}`);
};

export const testReminder = async (
  id: string,
): Promise<VendorReminderResultResponse> => {
  const { data } = await api.post<VendorReminderResultResponse>(`/reminders/${id}/test`);
  return data;
};