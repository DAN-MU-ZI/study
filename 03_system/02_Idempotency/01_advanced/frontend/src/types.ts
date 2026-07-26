export type CartStatus = 'PENDING' | 'PAID';

export interface CartResponse {
  cartId: string;
  status: CartStatus;
  lastPaymentId: string | null;
  lastPgTransactionId: string | null;
}

export interface PaymentRequestPayload {
  cartId: string;
  customerId: string;
  amount: number;
}

export interface PaymentAttemptRecord {
  cartId: string;
  customerId: string;
  amount: number;
  paymentId: string;
  pgTransactionId: string;
  status: CartStatus;
  requestedAt: string;
  approvedAt: string;
}

export interface PaymentResponse extends PaymentAttemptRecord {
  processedAt: string;
}

export interface DashboardSnapshot {
  cart: CartResponse;
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
