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

export function Field({ label, children, hint, error }) {
  return (
    <label className={"field" + (error ? " has-error" : "")}>
      <span>{label}</span>
      {children}
      {error ? <small className="field-error">{error}</small> : hint && <small>{hint}</small>}
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
  const network = /Cannot reach the server/i.test(message || "");
  return (
    <Notice tone="bad">
      <strong>{network ? "The app cannot reach the server." : "Something went wrong."}</strong> {message}
      {network && (
        <>
          <br />
          Start the backend (see the README, step 3) and try again.
        </>
      )}
    </Notice>
  );
}

export const statusTone = (status) =>
  ({ Complete: "good", "On track": "good", "Needs attention": "warn", Overdue: "bad" })[status] || "neutral";
