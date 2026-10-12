import { useState } from "react";
import { api, useLoad } from "../api";
import { Badge, Card, ErrorBox, Loading, Notice, PageHead } from "../components/ui";

const loadStatus = () => api.get("/advisor/status");

export default function SettingsPage() {
  const { data: status, error, loading } = useLoad(loadStatus);
  const [message, setMessage] = useState(null);
  const [busy, setBusy] = useState(false);

  async function run(action, doneMessage) {
    setBusy(true);
    setMessage(null);
    try {
      await action();
      setMessage(doneMessage);
    } catch (e) {
      setMessage(`Error: ${e.message}`);
    } finally {
      setBusy(false);
    }
  }

  if (loading) return <Loading />;
  if (error) return <ErrorBox message={error} />;

  return (
    <>
      <PageHead title="Settings" sub="Sample data, AI status, and privacy." />
      {message && <Notice>{message}</Notice>}

      <Card title="Sample data">
        <p className="muted">Fill the app with a realistic example (paycheck, expenses, debts, goals) so you can explore every page.</p>
        <div className="row">
          <button className="btn" disabled={busy} onClick={() => run(() => api.post("/demo/seed"), "Sample data loaded. Open the Dashboard.")}>
            Load sample data
          </button>
          <button
            className="btn danger"
            disabled={busy}
            onClick={() => {
              if (window.confirm("Delete ALL data in this app?")) run(() => api.del("/demo"), "All data deleted.");
            }}
          >
            Delete all data
          </button>
        </div>
      </Card>

      <Card title="AI advisor (OpenAI)">
        <div className="row">
          {status.configured ? <Badge tone="good">On - {status.model}</Badge> : <Badge tone="warn">Off</Badge>}
        </div>
        {!status.configured && (
          <p className="muted small" style={{ marginTop: 10 }}>
            To turn it on, copy <code>backend/.env.example</code> to <code>backend/.env</code>, add your <code>OPENAI_API_KEY</code>, and restart the
            backend. The key stays on the server and is never committed to Git.
          </p>
        )}
      </Card>

      <Card title="What is sent to OpenAI?">
        <p className="small">
          Only when AI is on: the summary figures Java already calculated for the question being explained (for example your goal name, target, and
          monthly amounts). Never your name, email, or raw account details - this app does not collect them. The AI cannot change your data; it can
          only suggest, and every change needs your approval. If an AI reply contains a dollar figure the app did not calculate, it is discarded.
        </p>
      </Card>
    </>
  );
}
