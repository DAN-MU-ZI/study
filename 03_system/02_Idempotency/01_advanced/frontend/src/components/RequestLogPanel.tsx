import type { RequestLogEntry } from '../types';

interface RequestLogPanelProps {
  logs: RequestLogEntry[];
}

export function RequestLogPanel({ logs }: RequestLogPanelProps) {
  return (
    <section className="card log-card">
      <div className="card-header">
        <div>
          <p className="eyebrow">Frontend log</p>
          <h2>요청 로그</h2>
        </div>
        <span className="meta-pill">{logs.length}건</span>
      </div>

      <div className="request-log" data-testid="request-log">
        {logs.length === 0 ? (
          <div className="empty-state">아직 사용자 액션이 없습니다.</div>
        ) : (
          logs.map((log) => (
            <article className={`request-log-row request-log-${log.status}`} data-testid="request-log-row" key={log.id}>
              <div className="request-log-main">
                <div>
                  <p className="payment-row-title">{log.action}</p>
                  <p className="payment-row-subtitle">{log.detail}</p>
                </div>
                <span className={`state-chip state-${log.status}`}>{log.status}</span>
              </div>

              <div className="request-row-meta">
                <span>{log.startedAt}</span>
                <span>{log.finishedAt ?? 'waiting...'}</span>
                {log.paymentId ? <span>{log.paymentId}</span> : null}
              </div>
            </article>
          ))
        )}
      </div>
    </section>
  );
}
