export function PageHead({ title, sub, children }) {
  return (
    <div className="page-head">
      <div>
        <h1>{title}</h1>
        {sub && <p className="page-sub">{sub}</p>}
      </div>
      {children}
    </div>
  );
}

export function Card({ title, children, className = "" }) {
  return (
    <section className={`card ${className}`}>
      {title && <h3>{title}</h3>}
      {children}
    </section>
  );
}

export function Stat({ label, value, tone, hint }) {
  return (
    <div className="stat">
      <div className="stat-label">{label}</div>
      <div className={`stat-value ${tone || ""}`}>{value}</div>
      {hint && <div className="stat-hint">{hint}</div>}
    </div>
  );
}

export function Field({ label, children, hint }) {
  return (
    <label className="field">
      <span>{label}</span>
      {children}
      {hint && <small>{hint}</small>}
    </label>
  );
}

export function Notice({ tone = "info", children }) {
  return <div className={`notice ${tone}`}>{children}</div>;
}

export function Badge({ tone = "neutral", children }) {
  return <span className={`badge ${tone}`}>{children}</span>;
}

export function Bar({ percent, tone }) {
  const width = Math.max(0, Math.min(100, percent || 0));
  return (
    <div className="bar">
      <span className={tone || ""} style={{ width: `${width}%` }} />
    </div>
  );
}

export function Loading({ text = "Loading..." }) {
  return <p className="muted">{text}</p>;
}

export function ErrorBox({ message }) {
  return (
    <Notice tone="bad">
      <strong>Something went wrong:</strong> {message}
      <br />
      Is the backend running on port 8080? (See the README, step 3.)
    </Notice>
  );
}

export const statusTone = (status) =>
  ({ Complete: "good", "On track": "good", "Needs attention": "warn", Overdue: "bad" })[status] || "neutral";
