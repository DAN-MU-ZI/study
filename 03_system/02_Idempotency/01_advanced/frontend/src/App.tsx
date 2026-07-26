import { startTransition, useEffect, useState } from 'react';
import { createNextCart, getDashboard, submitPayment } from './api';
import { ulid } from './ulid';
import { CartSummary } from './components/CartSummary';
import { PaymentControls } from './components/PaymentControls';
import { PaymentHistory } from './components/PaymentHistory';
import { RequestLogPanel } from './components/RequestLogPanel';
import { StatusBanner } from './components/StatusBanner';
import type { CartResponse, DashboardSnapshot, PaymentAttemptRecord, RequestLogEntry } from './types';

const DEFAULT_CUSTOMER_ID = 'cust-001';
const DEFAULT_AMOUNT = 15000;

function formatIsoTime(value: string): string {
  return new Date(value).toLocaleTimeString();
}

function makeTraceId(prefix: string): string {
  const suffix = globalThis.crypto?.randomUUID?.() ?? Math.random().toString(16).slice(2);
  return `${prefix}-${suffix}`;
}

function makeIdempotencyKey(customerId: string): string {
  return `${customerId}:${ulid()}`;
}

function buildPaymentPayload(cartId: string) {
  return {
    cartId,
    customerId: DEFAULT_CUSTOMER_ID,
    amount: DEFAULT_AMOUNT,
  };
}

function buildLogDetail(status: 'pending' | 'success' | 'error') {
  return status === 'pending' ? 'Payment request submitted' : 'Payment request finished';
}

export default function App() {
  const [cart, setCart] = useState<CartResponse | null>(null);
  const [payments, setPayments] = useState<PaymentAttemptRecord[]>([]);
  const [requestLogs, setRequestLogs] = useState<RequestLogEntry[]>([]);
  const [lastSyncedAt, setLastSyncedAt] = useState<string | null>(null);
  const [isBootstrapping, setIsBootstrapping] = useState(true);
  const [pendingRequests, setPendingRequests] = useState(0);
  const [isCreatingNextCart, setIsCreatingNextCart] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [idempotencyKey, setIdempotencyKey] = useState(() => makeIdempotencyKey(DEFAULT_CUSTOMER_ID));

  useEffect(() => {
    if (!cart?.cartId) {
      return;
    }
    setIdempotencyKey(makeIdempotencyKey(DEFAULT_CUSTOMER_ID));
  }, [cart?.cartId]);

  async function syncDashboard() {
    const snapshot: DashboardSnapshot = await getDashboard();

    startTransition(() => {
      setCart(snapshot.cart);
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
    if (!cart) {
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
      const response = await submitPayment(buildPaymentPayload(cart.cartId), idempotencyKey);
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

  async function startNextCart() {
    setIsCreatingNextCart(true);
    setError(null);

    try {
      const nextCart = await createNextCart();

      startTransition(() => {
        setCart(nextCart);
        setPayments([]);
        setRequestLogs([]);
        setLastSyncedAt(new Date().toISOString());
      });

      await syncDashboard();
    } catch (caughtError) {
      setError(caughtError instanceof Error ? caughtError.message : 'Failed to create next cart');
    } finally {
      setIsCreatingNextCart(false);
    }
  }

  const approvalMessages = payments.map((payment) => `${payment.pgTransactionId} approved at ${formatIsoTime(payment.approvedAt)}`);
  const duplicateMessages = approvalMessages.length >= 2 ? approvalMessages : [];

  const requestState = isBootstrapping
    ? 'bootstrapping'
    : pendingRequests > 0
      ? `submitting (${pendingRequests})`
      : isCreatingNextCart
        ? 'creating next cart'
        : error
          ? 'error'
          : 'idle';

  const canPay = cart?.status === 'PENDING' && pendingRequests === 0 && !isCreatingNextCart;
  const canCreateNextCart = cart?.status === 'PAID' && pendingRequests === 0;

  return (
    <main className="shell">
      <section className="page-header">
        <div>
          <p className="eyebrow">Idempotency advanced</p>
          <h1 data-testid="page-title">Payment and cart idempotency QA lab</h1>
          <p className="page-copy">
            Reuse the same idempotency key while one cart is processing, then mint a fresh key only when the next cart starts.
          </p>
        </div>
      </section>

      <section className="summary-strip" aria-label="advanced summary">
        <div className="summary-item">
          <span className="summary-label">Cart ID</span>
          <strong data-testid="summary-cart-id">{cart?.cartId ?? 'loading'}</strong>
        </div>
        <div className="summary-item">
          <span className="summary-label">Cart status</span>
          <strong>{cart?.status ?? 'PENDING'}</strong>
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
          Loading payment lab...
        </section>
      ) : null}

      <section className="grid">
        <div className="grid-main">
          <CartSummary cart={cart} lastSyncedAt={lastSyncedAt} paymentCount={payments.length} />
          <PaymentControls
            requestState={requestState}
            isSubmitting={pendingRequests > 0}
            canPay={Boolean(canPay)}
            canCreateNextCart={Boolean(canCreateNextCart)}
            isCreatingNextCart={isCreatingNextCart}
            onPay={() => void runPayment()}
            onCreateNextCart={() => void startNextCart()}
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
