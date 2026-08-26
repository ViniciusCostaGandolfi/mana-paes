export type UserRole = "ROLE_ADMIN" | "ROLE_REQUESTER" | "ROLE_PRODUCTION";
export type OrderStatus = "PENDING" | "IN_PRODUCTION" | "READY" | "DELIVERED" | "CANCELLED";
export type UnitMeasure = "UN" | "KG";
export type NotificationChannel = "WHATSAPP" | "EMAIL";
export type NotificationStatus = "PENDING" | "SENT" | "FAILED";
export type NotificationType = "ORDER_CONFIRMATION_REQUESTER" | "NEW_ORDER_ADMIN_ALERT" | "DAILY_REPORT" | "TEST";

export interface UserResponse {
  id: string; name: string; email: string; phone: string | null; whatsappNumber: string | null;
  role: UserRole; tenantId: string; active: boolean;
}
export interface LoginResponse {
  accessToken: string; refreshToken: string; tokenType: string; expiresIn: number; user: UserResponse;
}
export interface ProductResponse {
  id: string; name: string; description: string | null; unitPrice: number; unitMeasure: UnitMeasure; active: boolean; createdAt: string;
}
export interface ProductRequest { name: string; description?: string | null; unitPrice: number; unitMeasure: UnitMeasure; }
export interface OrderItemResponse { productId: string; productName: string; unitMeasure: UnitMeasure; quantity: number; unitPrice: number; subtotal: number; }
export interface OrderResponse {
  id: string; requesterId: string; requesterName: string; tenantId: string;
  createdAt: string; deliveryDate: string; status: OrderStatus; totalAmount: number; items: OrderItemResponse[];
}
export interface OrderItemRequest { productId: string; quantity: number; }
export interface OrderRequest { deliveryDate: string; items: OrderItemRequest[]; requesterId?: string | null; }
export interface OrderStatusUpdateRequest { status: OrderStatus; reason?: string | null; }
export interface UserRequest { name: string; email: string; phone?: string | null; whatsappNumber?: string | null; role: UserRole; }
export interface RegisterRequest { name: string; email: string; password: string; phone?: string | null; whatsappNumber?: string | null; }
export interface PageResponse<T> { content: T[]; totalElements: number; totalPages: number; number: number; size: number; first: boolean; last: boolean; empty: boolean; }
export interface NotificationConfigResponse {
  id: string; adminWhatsappNumber: string | null; adminEmail: string | null; dailyReportTime: string | null;
  whatsappEnabled: boolean; emailEnabled: boolean; evolutionApiInstanceName: string | null; evolutionApiKeyConfigured: boolean;
}
export interface NotificationConfigRequest {
  adminWhatsappNumber?: string | null; adminEmail?: string | null; dailyReportTime?: string | null;
  whatsappEnabled?: boolean; emailEnabled?: boolean; evolutionApiInstanceName?: string | null; evolutionApiKey?: string | null;
}
export interface NotificationLogResponse {
  id: string; orderId: string | null; channel: NotificationChannel; type: NotificationType; recipient: string;
  status: NotificationStatus; content: string; errorMessage: string | null; retryCount: number; sentAt: string | null; createdAt: string;
}
export interface DailyReportItemResponse { productId: string; productName: string; unitMeasure: UnitMeasure; totalQuantity: number; totalAmount: number; }
export interface DailyProductionReportResponse { date: string; items: DailyReportItemResponse[]; totalAmount: number; }
export interface DailyFinancialReportResponse { date: string; totalAmount: number; totalOrders: number; }
export interface DailyReportDispatchResponse { date: string; dispatched: boolean; whatsappSent: boolean; emailSent: boolean; whatsappMessage: string; emailMessage: string; }
export interface TenantResponse { id: string; name: string; document: string | null; phone: string | null; address: string | null; active: boolean; }
export interface TenantRequest { name: string; document?: string | null; phone?: string | null; address?: string | null; }
export interface MessageResponse { message: string; }