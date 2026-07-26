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
          <h2>주문 상태</h2>
        </div>
        <span className="meta-pill">동기화 {lastSyncedAt ? new Date(lastSyncedAt).toLocaleTimeString() : '방금 전'}</span>
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
        <p>{order?.lastPaymentId ? `마지막 paymentId: ${order.lastPaymentId}` : '아직 승인된 결제가 없습니다.'}</p>
        <p>{order?.lastPgTransactionId ? `마지막 PG 거래번호: ${order.lastPgTransactionId}` : '첫 PG 승인을 기다리는 중입니다.'}</p>
      </div>
    </section>
  );
}
