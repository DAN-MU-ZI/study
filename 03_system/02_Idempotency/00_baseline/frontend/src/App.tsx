import { startTransition, useEffect, useState } from 'react';
import { createNextOrder, getDashboard, submitPayment } from './api';
import { OrderSummary } from './components/OrderSummary';
import { PaymentControls } from './components/PaymentControls';
import { PaymentHistory } from './components/PaymentHistory';
import { RequestLogPanel } from './components/RequestLogPanel';
import { StatusBanner } from './components/StatusBanner';
import type { DashboardSnapshot, OrderResponse, PaymentAttemptRecord, RequestLogEntry } from './types';

const DEFAULT_CUSTOMER_ID = 'cust-001';
const DEFAULT_AMOUNT = 15000;

function formatIsoTime(value: string): string {
  return new Date(value).toLocaleTimeString();
}

function makeTraceId(prefix: string): string {
  const suffix = globalThis.crypto?.randomUUID?.() ?? Math.random().toString(16).slice(2);
  return `${prefix}-${suffix}`;
}

function buildPaymentPayload(orderId: string) {
  return {
    orderId,
    customerId: DEFAULT_CUSTOMER_ID,
    amount: DEFAULT_AMOUNT,
  };
}

function buildLogDetail(status: 'pending' | 'success' | 'error') {
  if (status === 'pending') {
    return 'Payment request submitted without idempotency protection';
  }

  if (status === 'success') {
    return 'Payment request finished';
  }

  return 'Payment request failed';
}

export default function App() {
  const [order, setOrder] = useState<OrderResponse | null>(null);
  const [payments, setPayments] = useState<PaymentAttemptRecord[]>([]);
  const [requestLogs, setRequestLogs] = useState<RequestLogEntry[]>([]);
  const [lastSyncedAt, setLastSyncedAt] = useState<string | null>(null);
  const [isBootstrapping, setIsBootstrapping] = useState(true);
  const [pendingRequests, setPendingRequests] = useState(0);
  const [isCreatingNextOrder, setIsCreatingNextOrder] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function syncDashboard() {
    const snapshot: DashboardSnapshot = await getDashboard();

    startTransition(() => {
      setOrder(snapshot.order);
      setPayments(snapshot.payments);
      setLastSyncedAt(new Date().toISOString());
      setError(null);
    });
  }

  async function bootstrap() {
    setIsBootstrapping(true);

    try {
      await syncDashboard();
    } catch (caughtError) {
      setError(caughtError instanceof Error ? caughtError.message : 'Failed to load dashboard');
    } finally {
      setIsBootstrapping(false);
    }
  }

  useEffect(() => {
    void bootstrap();
  }, []);

  function appendLog(entry: RequestLogEntry) {
    setRequestLogs((current) => [entry, ...current].slice(0, 12));
  }

  async function runPayment() {
    if (!order) {
      return;
    }

    const id = makeTraceId('pay');
    const startedAt = new Date().toISOString();
    appendLog({
      id,
      action: 'PAY',
      status: 'pending',
      detail: buildLogDetail('pending'),
      startedAt,
    });
    setPendingRequests((current) => current + 1);
    setError(null);

    try {
      const response = await submitPayment(buildPaymentPayload(order.orderId));
      const finishedAt = new Date().toISOString();

      setRequestLogs((current) =>
        current.map((entry) =>
          entry.id === id
            ? {
                ...entry,
                status: 'success',
                detail: buildLogDetail('success'),
                finishedAt,
                paymentId: response.paymentId,
                pgTransactionId: response.pgTransactionId,
              }
            : entry,
        ),
      );

      await syncDashboard();
    } catch (caughtError) {
      const finishedAt = new Date().toISOString();
      const message = caughtError instanceof Error ? caughtError.message : 'Payment request failed';

      setRequestLogs((current) =>
        current.map((entry) =>
          entry.id === id
            ? {
                ...entry,
                status: 'error',
                detail: message,
                finishedAt,
              }
            : entry,
        ),
      );
      setError(message);
    } finally {
      setPendingRequests((current) => Math.max(0, current - 1));
    }
  }

  async function startNextOrder() {
    setIsCreatingNextOrder(true);
    setError(null);

    try {
      const nextOrder = await createNextOrder();

      startTransition(() => {
        setOrder(nextOrder);
        setPayments([]);
        setRequestLogs([]);
        setLastSyncedAt(new Date().toISOString());
      });

      await syncDashboard();
    } catch (caughtError) {
      setError(caughtError instanceof Error ? caughtError.message : 'Failed to create next order');
    } finally {
      setIsCreatingNextOrder(false);
    }
  }

  const approvalMessages = payments.map((payment) => `${payment.pgTransactionId} approved at ${formatIsoTime(payment.approvedAt)}`);
  const duplicateMessages = approvalMessages.length >= 2 ? approvalMessages : [];

  const requestState = isBootstrapping
    ? 'bootstrapping'
    : pendingRequests > 0
      ? `submitting (${pendingRequests})`
      : isCreatingNextOrder
        ? 'creating next order'
        : error
          ? 'error'
          : 'idle';

  const canPay = order?.status === 'PENDING' && !isCreatingNextOrder;
  const canCreateNextOrder = order?.status === 'PAID' && pendingRequests === 0 && !isCreatingNextOrder;

  return (
    <main className="shell">
      <section className="page-header">
        <div>
          <p className="eyebrow">Idempotency baseline</p>
          <h1 data-testid="page-title">Duplicate payment baseline lab</h1>
          <p className="page-copy">
            This version has no idempotency protection. If the same logical order is submitted more than once while it is in
            flight, the backend can approve it more than once.
          </p>
        </div>
      </section>

      <section className="summary-strip" aria-label="baseline summary">
        <div className="summary-item">
          <span className="summary-label">Order ID</span>
          <strong data-testid="summary-order-id">{order?.orderId ?? 'loading'}</strong>
        </div>
        <div className="summary-item">
          <span className="summary-label">Order status</span>
          <strong>{order?.status ?? 'PENDING'}</strong>
        </div>
        <div className="summary-item">
          <span className="summary-label">Evidence count</span>
          <strong>{payments.length}</strong>
        </div>
        <div className="summary-item">
          <span className="summary-label">Request state</span>
          <strong>{requestState}</strong>
        </div>
      </section>

      <StatusBanner duplicateMessages={duplicateMessages} error={error} />

      {isBootstrapping ? (
        <section className="loading-panel" data-testid="loading-indicator">
          Loading baseline lab...
        </section>
      ) : null}

      <section className="grid">
        <div className="grid-main">
          <OrderSummary order={order} lastSyncedAt={lastSyncedAt} paymentCount={payments.length} />
          <PaymentControls
            requestState={requestState}
            isSubmitting={pendingRequests > 0}
            canPay={Boolean(canPay)}
            canCreateNextOrder={Boolean(canCreateNextOrder)}
            isCreatingNextOrder={isCreatingNextOrder}
            onPay={() => void runPayment()}
            onCreateNextOrder={() => void startNextOrder()}
          />
        </div>

        <div className="grid-side">
          <PaymentHistory payments={payments} />
          <RequestLogPanel logs={requestLogs} />
        </div>
      </section>
    </main>
  );
}
