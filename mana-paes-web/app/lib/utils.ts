import clsx from "clsx";
import type {
  NotificationChannel,
  NotificationStatus,
  NotificationType,
  OrderStatus,
  UnitMeasure,
  UserRole,
} from "~/types/api";

export function cn(...inputs: Array<string | false | null | undefined>): string {
  return clsx(...inputs);
}

export function formatCurrency(value: number): string {
  return new Intl.NumberFormat("pt-BR", {
    style: "currency",
    currency: "BRL",
  }).format(value);
}

/**
 * Converte um valor ISO para Date local. Strings apenas com data ("yyyy-MM-dd")
 * são interpretadas como meia-noite no fuso local (evita deslocamento de dia);
 * demais valores (instantes ISO com hora) são parseados pelo Date nativo.
 */
function toLocalDate(iso: string): Date {
  if (/^\d{4}-\d{2}-\d{2}$/.test(iso)) {
    const [year, month, day] = iso.split("-").map(Number);
    return new Date(year, month - 1, day);
  }
  return new Date(iso);
}

export function formatDate(iso: string): string {
  const date = toLocalDate(iso);
  if (Number.isNaN(date.getTime())) return "";
  const day = date.getDate().toString().padStart(2, "0");
  const month = (date.getMonth() + 1).toString().padStart(2, "0");
  return `${day}/${month}/${date.getFullYear()}`;
}

export function formatDateTime(iso: string): string {
  const date = toLocalDate(iso);
  if (Number.isNaN(date.getTime())) return "";
  const day = date.getDate().toString().padStart(2, "0");
  const month = (date.getMonth() + 1).toString().padStart(2, "0");
  const hours = date.getHours().toString().padStart(2, "0");
  const minutes = date.getMinutes().toString().padStart(2, "0");
  return `${day}/${month}/${date.getFullYear()} às ${hours}:${minutes}`;
}

export function formatTime(time: string | null): string {
  if (!time) return "";
  const match = /^(\d{2}):(\d{2})/.exec(time);
  return match ? `${match[1]}:${match[2]}` : time;
}

export const ORDER_STATUS_LABEL: Record<OrderStatus, string> = {
  PENDING: "Pendente",
  IN_PRODUCTION: "Em produção",
  READY: "Pronto",
  DELIVERED: "Entregue",
  CANCELLED: "Cancelado",
};

export const USER_ROLE_LABEL: Record<UserRole, string> = {
  ROLE_ADMIN: "Administrador",
  ROLE_REQUESTER: "Solicitante",
  ROLE_PRODUCTION: "Produção",
};

export const UNIT_MEASURE_LABEL: Record<UnitMeasure, string> = {
  UN: "Unidade(s)",
  KG: "Kg",
};

export const NOTIFICATION_CHANNEL_LABEL: Record<NotificationChannel, string> = {
  WHATSAPP: "WhatsApp",
  EMAIL: "E-mail",
};

export const NOTIFICATION_STATUS_LABEL: Record<NotificationStatus, string> = {
  PENDING: "Pendente",
  SENT: "Enviado",
  FAILED: "Falhou",
};

export const NOTIFICATION_TYPE_LABEL: Record<NotificationType, string> = {
  ORDER_CONFIRMATION_REQUESTER: "Confirmação de pedido",
  NEW_ORDER_ADMIN_ALERT: "Novo pedido (admin)",
  DAILY_REPORT: "Relatório diário",
  TEST: "Teste",
};

export const ORDER_STATUS_BADGE_CLASS: Record<OrderStatus, string> = {
  PENDING: "badge-warning",
  IN_PRODUCTION: "badge-info",
  READY: "badge-success",
  DELIVERED: "badge-neutral",
  CANCELLED: "badge-error",
};