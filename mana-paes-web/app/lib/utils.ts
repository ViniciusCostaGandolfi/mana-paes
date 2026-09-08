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
  ACCEPTED: "Aceito",
  CANCELLED: "Cancelado",
};

export const USER_ROLE_LABEL: Record<UserRole, string> = {
  ROLE_SYS_ADMIN: "Administrador do Sistema",
  ROLE_ADMIN: "Administrador",
};

export const USER_ROLE_BADGE_CLASS: Record<UserRole, string> = {
  ROLE_SYS_ADMIN: "badge-primary",
  ROLE_ADMIN: "badge-info",
};

/** Turnos de quantidade de um item de pedido (contrato da API). */
export const ORDER_SHIFT_FIELDS = [
  { key: "quantidadeManha", label: "Manhã", shortLabel: "M" },
  { key: "quantidadeTarde", label: "Tarde", shortLabel: "T" },
  { key: "quantidadeNoite", label: "Noite", shortLabel: "N" },
] as const;
export type OrderShiftKey = (typeof ORDER_SHIFT_FIELDS)[number]["key"];

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
  REMINDER: "Lembrete",
};

export const ORDER_STATUS_BADGE_CLASS: Record<OrderStatus, string> = {
  PENDING: "badge-warning",
  ACCEPTED: "badge-success",
  CANCELLED: "badge-error",
};

/**
 * Remove tudo que não for dígito e garante o DDI 55 (Brasil).
 * Se o valor já começar com "55", mantém; caso contrário, prefixa "55".
 * Ex.: "11900000001" → "5511900000001"; "55 11 90000-0001" → "5511900000001".
 */
export function normalizePhoneBR(value: string): string {
  const digits = value.replace(/\D/g, "");
  if (!digits) return "";
  return digits.startsWith("55") ? digits : `55${digits}`;
}

/**
 * Formata um telefone brasileiro para exibição SEM o DDI 55.
 * Aceita números com ou sem o prefixo "55". Ex.:
 * "5511900000001" → "(11) 90000-0001"; "11900000001" → "(11) 90000-0001".
 * Se não houver 10-11 dígitos nacionais, retorna os dígitos sem o 55.
 */
export function formatPhoneBR(value: string | null | undefined): string {
  const digits = (value ?? "").replace(/\D/g, "");
  if (!digits) return "";

  let national = digits;
  if (national.startsWith("55")) {
    national = national.slice(2);
  }

  if (national.length === 11) {
    return `(${national.slice(0, 2)}) ${national.slice(2, 7)}-${national.slice(7)}`;
  }
  if (national.length === 10) {
    return `(${national.slice(0, 2)}) ${national.slice(2, 6)}-${national.slice(6)}`;
  }
  return national;
}