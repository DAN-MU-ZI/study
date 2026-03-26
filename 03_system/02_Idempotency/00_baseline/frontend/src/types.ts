export type OrderStatus = 'PENDING' | 'PAID';

export interface OrderResponse {
  orderId: string;
  status: OrderStatus;
  lastPaymentId: string | null;
  lastPgTransactionId: string | null;
}

export interface PaymentRequestPayload {
  orderId: string;
  customerId: string;
  amount: number;
}

export interface PaymentAttemptRecord {
  orderId: string;
  customerId: string;
  amount: number;
  paymentId: string;
  pgTransactionId: string;
  status: OrderStatus;
  threadName: string;
  requestedAt: string;
  approvedAt: string;
}

export interface PaymentResponse extends PaymentAttemptRecord {
  processedAt: string;
}

export interface DashboardSnapshot {
  order: OrderResponse;
  payments: PaymentAttemptRecord[];
}

export interface RequestLogEntry {
  id: string;
  action: 'PAY' | 'RETRY' | 'REFRESH';
  status: 'pending' | 'success' | 'error';
  detail: string;
  startedAt: string;
  finishedAt?: string;
  paymentId?: string;
  pgTransactionId?: string;
}
