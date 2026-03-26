interface PaymentControlsProps {
  requestState: string;
  isSubmitting: boolean;
  canPay: boolean;
  canCreateNextOrder: boolean;
  isCreatingNextOrder: boolean;
  onPay: () => void;
  onCreateNextOrder: () => void;
}

export function PaymentControls({
  requestState,
  isSubmitting,
  canPay,
  canCreateNextOrder,
  isCreatingNextOrder,
  onPay,
  onCreateNextOrder,
}: PaymentControlsProps) {
  return (
    <section className="card controls-card">
      <div className="card-header">
        <div>
          <p className="eyebrow">Action</p>
          <h2>Payment flow</h2>
        </div>
        <span className={`state-pill ${isSubmitting ? 'state-live' : 'state-idle'}`} data-testid="request-state">
          {requestState}
        </span>
      </div>

      <div className="controls-actions">
        <button className="primary-button" data-testid="pay-button" type="button" onClick={onPay} disabled={!canPay}>
          Pay order
        </button>
        <button
          className="primary-button"
          data-testid="new-order-button"
          type="button"
          onClick={onCreateNextOrder}
          disabled={!canCreateNextOrder || isCreatingNextOrder}
        >
          {isCreatingNextOrder ? 'Creating next order...' : 'Start next order'}
        </button>
      </div>

      <p className="controls-help">
        This baseline keeps the pay action available while the order is still in flight, so repeated clicks can race into
        duplicate approvals.
      </p>
    </section>
  );
}
