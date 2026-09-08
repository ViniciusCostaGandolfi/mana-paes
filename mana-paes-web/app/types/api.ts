export type UserRole = "ROLE_SYS_ADMIN" | "ROLE_ADMIN";
export type OrderStatus = "PENDING" | "ACCEPTED" | "CANCELLED";
export type UnitMeasure = "UN" | "KG";
export type NotificationChannel = "WHATSAPP" | "EMAIL";
export type NotificationStatus = "PENDING" | "SENT" | "FAILED";
export type NotificationType =
  | "ORDER_CONFIRMATION_REQUESTER"
  | "NEW_ORDER_ADMIN_ALERT"
  | "DAILY_REPORT"
  | "TEST"
  | "REMINDER";

export interface UserResponse {
  id: string;
  name: string;
  email: string | null;
  phone: string | null;
  whatsappNumber: string | null;
  role: UserRole;
  tenantId: string;
  active: boolean;
}
export interface LoginResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  user: UserResponse;
}

export interface UserRequest {
  name: string;
  email: string | null;
  phone?: string | null;
  whatsappNumber?: string | null;
  role: UserRole | null;
  password?: string | null;
}

export interface VendorResponse {
  id: string;
  name: string;
  phone: string;
  whatsappNumber: string | null;
  active: boolean;
  tenantId: string;
}
export interface VendorRequest {
  name: string;
  phone: string;
  whatsappNumber?: string | null;
}

export interface ProductOptionResponse {
  id: string;
  name: string;
  priceAdjustment: number;
}
export interface ProductOptionGroupResponse {
  id: string;
  name: string;
  required: boolean;
  options: ProductOptionResponse[];
}
export interface ProductResponse {
  id: string;
  name: string;
  description: string | null;
  unitPrice: number;
  unitMeasure: UnitMeasure;
  active: boolean;
  createdAt: string;
  optionGroups: ProductOptionGroupResponse[];
}
export interface ProductOptionRequest {
  name: string;
  priceAdjustment: number;
}
export interface ProductOptionGroupRequest {
  name: string;
  required?: boolean;
  options: ProductOptionRequest[];
}
export interface ProductRequest {
  name: string;
  description?: string | null;
  unitPrice: number;
  unitMeasure: UnitMeasure;
  optionGroups?: ProductOptionGroupRequest[] | null;
}

export interface OrderSelectionRequest {
  groupId: string;
  optionId: string;
}
export interface OrderItemRequest {
  productId: string;
  quantidadeManha: number;
  quantidadeTarde: number;
  quantidadeNoite: number;
  selections?: OrderSelectionRequest[] | null;
}
export interface OrderRequest {
  deliveryDate: string;
  items: OrderItemRequest[];
  vendorId?: string | null;
}
export interface PublicOrderRequest {
  vendorPhone: string;
  vendorName?: string | null;
  deliveryDate: string;
  items: OrderItemRequest[];
}
export interface OrderStatusUpdateRequest {
  status: OrderStatus;
  reason?: string | null;
}

export interface OrderItemOptionResponse {
  optionId: string;
  optionName: string;
  priceAdjustment: number;
}
export interface OrderItemResponse {
  productId: string;
  productName: string;
  unitMeasure: UnitMeasure;
  quantidadeManha: number;
  quantidadeTarde: number;
  quantidadeNoite: number;
  totalQuantity: number;
  unitPrice: number;
  subtotal: number;
  options: OrderItemOptionResponse[];
}
export interface OrderResponse {
  id: string;
  requesterId: string | null;
  requesterName: string | null;
  vendorId: string | null;
  vendorName: string | null;
  tenantId: string;
  createdAt: string;
  deliveryDate: string;
  status: OrderStatus;
  totalAmount: number;
  items: OrderItemResponse[];
}

export interface ScheduledReminderRequest {
  name: string;
  message: string;
  reminderTime: string;
  enabled?: boolean;
  whatsappEnabled?: boolean;
  emailEnabled?: boolean;
  deliveryDayOffset?: number;
}
export interface ScheduledReminderResponse {
  id: string;
  name: string;
  message: string;
  reminderTime: string;
  enabled: boolean;
  whatsappEnabled: boolean;
  emailEnabled: boolean;
  deliveryDayOffset: number;
  lastSentDate: string | null;
  tenantId: string;
  createdById: string | null;
}
export interface VendorReminderResultResponse {
  recipients: number;
  whatsappSent: number;
}

export interface PageInfo {
  size: number;
  number: number;
  totalElements: number;
  totalPages: number;
  first?: boolean;
  last?: boolean;
  empty?: boolean;
}

export interface PageResponse<T> {
  content: T[];
  page: PageInfo;
}

export interface NotificationConfigResponse {
  id: string;
  adminWhatsappNumber: string | null;
  adminEmail: string | null;
  dailyReportTime: string | null;
  dailyReportEnabled: boolean;
  whatsappEnabled: boolean;
  emailEnabled: boolean;
}
export interface NotificationConfigRequest {
  adminWhatsappNumber?: string | null;
  adminEmail?: string | null;
  dailyReportTime?: string | null;
  dailyReportEnabled?: boolean;
  whatsappEnabled?: boolean;
  emailEnabled?: boolean;
}
export interface NotificationLogResponse {
  id: string;
  orderId: string | null;
  channel: NotificationChannel;
  type: NotificationType;
  recipient: string;
  status: NotificationStatus;
  content: string;
  errorMessage: string | null;
  retryCount: number;
  sentAt: string | null;
  createdAt: string;
}

export interface DailyReportItemResponse {
  productId: string;
  productName: string;
  unitMeasure: UnitMeasure;
  quantidadeManha: number;
  quantidadeTarde: number;
  quantidadeNoite: number;
  totalQuantity: number;
  totalAmount: number;
}
export interface DailyProductionReportResponse {
  date: string;
  items: DailyReportItemResponse[];
  totalAmount: number;
  vendorNames: string[];
}
export interface DailyFinancialReportResponse {
  date: string;
  totalAmount: number;
  totalOrders: number;
}
export interface DailyReportDispatchResponse {
  date: string;
  dispatched: boolean;
  whatsappSent: boolean;
  emailSent: boolean;
  whatsappMessage: string;
  emailMessage: string;
}

export interface TenantResponse {
  id: string;
  name: string;
  document: string | null;
  phone: string | null;
  address: string | null;
  active: boolean;
}
export interface TenantRequest {
  name: string;
  document?: string | null;
  phone?: string | null;
  address?: string | null;
}

export interface MessageResponse {
  message: string;
}

export type WhatsAppConnectionState = "CLOSE" | "CONNECTING" | "OPEN";
export interface WhatsAppStatusResponse {
  state: WhatsAppConnectionState;
  qrCodeBase64?: string | null;
  connectedNumber?: string | null;
  updatedAt?: string | null;
}