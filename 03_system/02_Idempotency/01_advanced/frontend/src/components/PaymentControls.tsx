interface PaymentControlsProps {
  requestState: string;
  isSubmitting: boolean;
  canPay: boolean;
  canCreateNextCart: boolean;
  isCreatingNextCart: boolean;
  onPay: () => void;
  onCreateNextCart: () => void;
}

export function PaymentControls({
  requestState,
  isSubmitting,
  canPay,
  canCreateNextCart,
  isCreatingNextCart,
  onPay,
  onCreateNextCart,
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
          Pay cart
        </button>
        <button
          className="primary-button"
          data-testid="new-cart-button"
          type="button"
          onClick={onCreateNextCart}
          disabled={!canCreateNextCart || isCreatingNextCart}
        >
          {isCreatingNextCart ? 'Creating next cart...' : 'Start next cart'}
        </button>
      </div>

      <p className="controls-help">
        Reuse one idempotency key while the same cart is in flight, then issue a new key only when a new cart starts.
      </p>
    </section>
  );
}
