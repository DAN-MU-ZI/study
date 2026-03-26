import type { PaymentAttemptRecord } from '../types';

interface PaymentHistoryProps {
  payments: PaymentAttemptRecord[];
}

export function PaymentHistory({ payments }: PaymentHistoryProps) {
  return (
    <section className="card evidence-card">
      <div className="card-header">
        <div>
          <p className="eyebrow">Backend evidence</p>
          <h2>Payment history</h2>
        </div>
        <span className="meta-pill">{payments.length} records</span>
      </div>

      <div className="payment-history" data-testid="payment-history">
        {payments.length === 0 ? (
          <div className="empty-state">No approvals recorded yet. Submit a payment request to capture backend evidence.</div>
        ) : (
          payments.map((payment, index) => (
            <article className="payment-row" data-testid="payment-row" key={`${payment.paymentId}-${index}`}>
              <div className="payment-row-main">
                <div>
                  <p className="payment-row-title">{payment.paymentId}</p>
                  <p className="payment-row-subtitle">
                    {payment.requestedAt} to {payment.approvedAt}
                  </p>
                </div>
                <span className="status-badge status-paid" data-testid="payment-row-status">
                  paid
                </span>
              </div>

              <div className="payment-row-grid">
                <div>
                  <span className="field-label">Order</span>
                  <strong data-testid="payment-row-order-id">{payment.orderId}</strong>
                </div>
                <div>
                  <span className="field-label">PG transaction</span>
                  <strong data-testid="payment-row-pg-transaction-id">{payment.pgTransactionId}</strong>
                </div>
                <div>
                  <span className="field-label">Thread</span>
                  <strong>{payment.threadName}</strong>
                </div>
              </div>
            </article>
          ))
        )}
      </div>
    </section>
  );
}
