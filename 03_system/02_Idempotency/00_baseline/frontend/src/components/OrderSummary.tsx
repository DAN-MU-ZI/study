import type { OrderResponse } from '../types';

interface OrderSummaryProps {
  order: OrderResponse | null;
  lastSyncedAt: string | null;
  paymentCount: number;
}

function formatStatus(status: string): string {
  return status.toLowerCase();
}

export function OrderSummary({ order, lastSyncedAt, paymentCount }: OrderSummaryProps) {
  return (
    <section className="card order-card" data-testid="order-card">
      <div className="card-header">
        <div>
          <p className="eyebrow">Order</p>
          <h2>Current order</h2>
        </div>
        <span className="meta-pill">Synced at {lastSyncedAt ? new Date(lastSyncedAt).toLocaleTimeString() : 'just now'}</span>
      </div>

      <div className="order-grid">
        <div className="metric">
          <span className="metric-label">Order ID</span>
          <strong data-testid="order-id">{order?.orderId ?? '1001'}</strong>
        </div>
        <div className="metric">
          <span className="metric-label">Amount</span>
          <strong data-testid="order-amount">15,000 KRW</strong>
        </div>
        <div className="metric">
          <span className="metric-label">Status</span>
          <strong className={`status-badge status-${order?.status?.toLowerCase() ?? 'pending'}`} data-testid="order-status">
            {formatStatus(order?.status ?? 'PENDING')}
          </strong>
        </div>
        <div className="metric">
          <span className="metric-label">Evidence count</span>
          <strong>{paymentCount}</strong>
        </div>
      </div>

      <div className="order-notes">
        <p>{order?.lastPaymentId ? `Latest paymentId: ${order.lastPaymentId}` : 'No approval has been recorded yet.'}</p>
        <p>{order?.lastPgTransactionId ? `Latest PG transaction: ${order.lastPgTransactionId}` : 'Waiting for the first PG approval.'}</p>
      </div>
    </section>
  );
}
