interface StatusBannerProps {
  duplicateMessages: string[];
  error: string | null;
}

export function StatusBanner({ duplicateMessages, error }: StatusBannerProps) {
  return (
    <div className="banner-stack">
      {duplicateMessages.length > 0 ? (
        <section className="banner banner-warning" data-testid="duplicate-warning" role="status">
          <div>
            <p className="eyebrow">Warning</p>
            <strong>Duplicate approvals detected for this order: {duplicateMessages.length} records</strong>
          </div>
          <div className="banner-details">
            {duplicateMessages.map((message) => (
              <span key={message}>{message}</span>
            ))}
          </div>
        </section>
      ) : null}

      {error ? (
        <section className="banner banner-error" data-testid="error-banner" role="alert">
          <div>
            <p className="eyebrow">Error</p>
            <strong>{error}</strong>
          </div>
        </section>
      ) : null}
    </div>
  );
}
