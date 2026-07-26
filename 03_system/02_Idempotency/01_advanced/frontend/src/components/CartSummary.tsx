import type { CartResponse } from '../types';

interface CartSummaryProps {
  cart: CartResponse | null;
  lastSyncedAt: string | null;
  paymentCount: number;
}

function formatStatus(status: string): string {
  return status.toLowerCase();
}

export function CartSummary({ cart, lastSyncedAt, paymentCount }: CartSummaryProps) {
  return (
    <section className="card order-card" data-testid="cart-card">
      <div className="card-header">
        <div>
          <p className="eyebrow">Cart</p>
          <h2>Cart status</h2>
        </div>
        <span className="meta-pill">Synced {lastSyncedAt ? new Date(lastSyncedAt).toLocaleTimeString() : 'just now'}</span>
      </div>

      <div className="order-grid">
        <div className="metric">
          <span className="metric-label">Cart ID</span>
          <strong data-testid="cart-id">{cart?.cartId ?? '1001'}</strong>
        </div>
        <div className="metric">
          <span className="metric-label">Amount</span>
          <strong data-testid="cart-amount">15,000 KRW</strong>
        </div>
        <div className="metric">
          <span className="metric-label">Status</span>
          <strong className={`status-badge status-${cart?.status?.toLowerCase() ?? 'pending'}`} data-testid="cart-status">
            {formatStatus(cart?.status ?? 'PENDING')}
          </strong>
        </div>
        <div className="metric">
          <span className="metric-label">Payment count</span>
          <strong>{paymentCount}</strong>
        </div>
      </div>

      <div className="order-notes">
        <p>{cart?.lastPaymentId ? `Last paymentId: ${cart.lastPaymentId}` : 'No approved payment yet.'}</p>
        <p>{cart?.lastPgTransactionId ? `Last PG transaction: ${cart.lastPgTransactionId}` : 'Waiting for the first PG approval.'}</p>
      </div>
    </section>
  );
}
