import { api } from "~/lib/api";
import type { MessageResponse, WhatsAppStatusResponse } from "~/types/api";

export const connectWhatsapp = async (): Promise<WhatsAppStatusResponse> => {
  const { data } = await api.post<WhatsAppStatusResponse>("/whatsapp/connect");
  return data;
};

export const getWhatsappStatus = async (): Promise<WhatsAppStatusResponse> => {
  const { data } = await api.get<WhatsAppStatusResponse>("/whatsapp/status");
  return data;
};

export const disconnectWhatsapp = async (): Promise<MessageResponse> => {
  const { data } = await api.post<MessageResponse>("/whatsapp/disconnect");
  return data;
};

export const testWhatsapp = async (): Promise<MessageResponse> => {
  const { data } = await api.post<MessageResponse>("/whatsapp/test");
  return data;
};

export const simulateScan = async (): Promise<WhatsAppStatusResponse> => {
  const { data } = await api.post<WhatsAppStatusResponse>("/whatsapp/simulate-scan");
  return data;
};