import type {
  DashboardSnapshot,
  OrderResponse,
  PaymentAttemptRecord,
  PaymentRequestPayload,
  PaymentResponse,
} from './types';

async function readJson<T>(response: Response): Promise<T> {
  const text = await response.text();

  if (!response.ok) {
    throw new Error(text || response.statusText);
  }

  return text ? (JSON.parse(text) as T) : ({} as T);
}

async function readError(response: Response): Promise<string> {
  const text = await response.text();

  if (!text) {
    return response.statusText;
  }

  try {
    const parsed = JSON.parse(text) as { message?: string; detail?: unknown; error?: string };
    if (typeof parsed.message === 'string') {
      return parsed.message;
    }
    if (typeof parsed.error === 'string') {
      return parsed.error;
    }
    if (typeof parsed.detail === 'string') {
      return parsed.detail;
    }
  } catch {
    return text;
  }

  return response.statusText;
}

export async function getOrder(orderId: string): Promise<OrderResponse> {
  const response = await fetch(`/api/orders/${encodeURIComponent(orderId)}`);
  if (!response.ok) {
    throw new Error(await readError(response));
  }
  return readJson<OrderResponse>(response);
}

export async function getCurrentOrder(): Promise<OrderResponse> {
  const response = await fetch('/api/orders/current');
  if (!response.ok) {
    throw new Error(await readError(response));
  }
  return readJson<OrderResponse>(response);
}

export async function createNextOrder(): Promise<OrderResponse> {
  const response = await fetch('/api/orders/next', {
    method: 'POST',
  });
  if (!response.ok) {
    throw new Error(await readError(response));
  }
  return readJson<OrderResponse>(response);
}

export async function getPayments(orderId?: string): Promise<PaymentAttemptRecord[]> {
  const query = orderId ? `?orderId=${encodeURIComponent(orderId)}` : '';
  const response = await fetch(`/api/payments${query}`);
  if (!response.ok) {
    throw new Error(await readError(response));
  }
  return readJson<PaymentAttemptRecord[]>(response);
}

export async function getDashboard(): Promise<DashboardSnapshot> {
  const order = await getCurrentOrder();
  const payments = await getPayments(order.orderId);
  return { order, payments };
}

export async function submitPayment(payload: PaymentRequestPayload, idempotencyKey: string): Promise<PaymentResponse> {
  const response = await fetch('/api/payments', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Idempotency-Key': idempotencyKey,
    },
    body: JSON.stringify(payload),
  });

  if (!response.ok) {
    throw new Error(await readError(response));
  }

  return readJson<PaymentResponse>(response);
}
