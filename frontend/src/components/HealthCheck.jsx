import { useState } from "react";
import { Link } from "react-router-dom";
import { api, useLoad } from "../api";
import { Badge, Card, ErrorBox, Loading, Notice } from "./ui";

// Java compares the person's numbers with published benchmarks (IRS, Federal Reserve, BLS, CFPB...). No AI is used for this list.
const loadInsights = () => api.get("/insights");

const SEVERITY = {
  ALERT: { tone: "bad", label: "Needs attention" },
  WATCH: { tone: "warn", label: "Worth a look" },
  INFO: { tone: "neutral", label: "Good to know" },
  GOOD: { tone: "good", label: "On track" },
};

const dollars = new Intl.NumberFormat("en-US", { style: "currency", currency: "USD", maximumFractionDigits: 0 });
function formatBenchmark(b) {
  if (b.unit.startsWith("dollars")) return `${dollars.format(b.value)} per year`;
  if (b.unit.startsWith("percent")) return `${b.value}% ${b.unit.replace("percent", "").trim()}`.trim();
  return `${b.value} ${b.unit}`;
}

export default function HealthCheck() {
  const { data, error, loading } = useLoad(loadInsights);
  const [explain, setExplain] = useState(null);
  const [explaining, setExplaining] = useState(false);
  const [explainError, setExplainError] = useState(null);
  const [showSources, setShowSources] = useState(false);
  const [benchmarks, setBenchmarks] = useState(null);

  async function explainTop() {
    setExplaining(true);
    setExplainError(null);
    try {
      setExplain(await api.post("/insights/explain"));
    } catch (e) {
      setExplainError(e.message);
    } finally {
      setExplaining(false);
    }
  }

  async function toggleSources() {
    const next = !showSources;
    setShowSources(next);
    if (next && !benchmarks) {
      try {
        setBenchmarks(await api.get("/benchmarks"));
      } catch (e) {
        setExplainError(e.message);
      }
    }
  }

  if (loading) return <Loading text="Comparing your numbers with published benchmarks..." />;
  if (error) return <ErrorBox message={error} />;

  return (
    <Card title="Financial health check" className={data.attentionCount > 0 ? "accent-alert" : "accent-teal"}>
      <p className="muted small" style={{ marginTop: 0 }}>
        Your numbers compared with published figures from the IRS, the Federal Reserve, the Bureau of Labor Statistics, the CFPB and others. {data.dataNote}
      </p>

      {data.insights.length === 0 && (
        <p>
          Nothing to compare yet. Add your <Link to="/paycheck">paycheck</Link>, <Link to="/expenses">expenses</Link>, <Link to="/debts">debts</Link> and{" "}
          <Link to="/goals">goals</Link>, or load sample data in <Link to="/settings">Settings</Link>.
        </p>
      )}

      {data.insights.map((i) => {
        const sev = SEVERITY[i.severity] || SEVERITY.INFO;
        return (
          <div key={i.id} className="insight">
            <div className="row between">
              <strong>{i.title}</strong>
              <Badge tone={sev.tone}>{sev.label}</Badge>
            </div>
            <p style={{ margin: "6px 0" }}>{i.detail}</p>
            {i.sources.length > 0 && (
              <p className="muted small" style={{ margin: 0 }}>
                Sources:{" "}
                {i.sources.map((s, n) => (
                  <span key={s.id}>
                    {n > 0 && "; "}
                    {s.url ? (
                      <a href={s.url} target="_blank" rel="noopener noreferrer">
                        {s.label}
                      </a>
                    ) : (
                      s.label
                    )}{" "}
                    ({s.asOf})
                  </span>
                ))}
              </p>
            )}
          </div>
        );
      })}

      {data.insights.length > 0 && (
        <div className="row" style={{ marginTop: 12 }}>
          <button type="button" className="btn small" onClick={explainTop} disabled={explaining}>
            {explaining ? "Explaining..." : "Explain my top priorities"}
          </button>
          <button type="button" className="btn secondary small" onClick={toggleSources}>
            {showSources ? "Hide" : "Where do these numbers come from?"}
          </button>
        </div>
      )}
      {explainError && <p className="field-error small">{explainError}</p>}
      {explain && (
        <Notice tone="info">
          <p style={{ margin: "0 0 6px" }}>{explain.explanation}</p>
          <span className="muted small">{explain.note}</span>
        </Notice>
      )}

      {showSources && benchmarks && (
        <div className="table-wrap">
          <table className="table">
            <thead>
              <tr>
                <th>Figure</th>
                <th>Value</th>
                <th>Published by</th>
                <th>As of</th>
              </tr>
            </thead>
            <tbody>
              {benchmarks.map((b) => (
                <tr key={b.id}>
                  <td>
                    {b.label}
                    {b.kind === "ASSUMPTION" && <span className="muted small"> (assumption, not a forecast)</span>}
                  </td>
                  <td>{formatBenchmark(b)}</td>
                  <td>
                    {b.url ? (
                      <a href={b.url} target="_blank" rel="noopener noreferrer">
                        {b.source}
                      </a>
                    ) : (
                      b.source
                    )}
                  </td>
                  <td>{b.asOf}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
      <p className="disclaimer">Educational only, not financial, tax or legal advice. Benchmarks are averages and guidelines, so your situation may differ.</p>
    </Card>
  );
}
