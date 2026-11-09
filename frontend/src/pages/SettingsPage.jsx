import { useState } from "react";
import { api, useLoad } from "../api";
import { useAuth } from "../auth/authContext";
import { Badge, Card, ErrorBox, Field, Loading, Notice, PageHead } from "../components/ui";

const loadStatus = () => api.get("/advisor/status");

export default function SettingsPage() {
  const { user, logout, deleteAccount } = useAuth();
  const { data: status, error, loading } = useLoad(loadStatus);
  const [message, setMessage] = useState(null);
  const [busy, setBusy] = useState(false);
  const [deletePassword, setDeletePassword] = useState("");
  const [deleteError, setDeleteError] = useState(null);

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

  async function removeAccount(e) {
    e.preventDefault();
    setDeleteError(null);
    if (!window.confirm("Permanently delete your account and ALL of your data? This cannot be undone.")) return;
    setBusy(true);
    try {
      await deleteAccount(deletePassword); // signs out when it succeeds
    } catch (err) {
      setDeleteError(err.message);
      setBusy(false);
    }
  }

  if (loading) return <Loading />;
  if (error) return <ErrorBox message={error} />;

  return (
    <>
      <PageHead title="Settings" sub="Your account, sample data, AI status, and privacy." />
      {message && <Notice>{message}</Notice>}

      <Card title="Your account">
        <p>
          Signed in as <strong>{user.email}</strong>
        </p>
        <div className="row">
          <button className="btn secondary" onClick={logout}>
            Sign out
          </button>
        </div>
      </Card>

      <Card title="Sample data">
        <p className="muted">Fill your account with a realistic example (paycheck, expenses, debts, goals) so you can explore every page. This replaces your current data.</p>
        <div className="row">
          <button className="btn" disabled={busy} onClick={() => run(() => api.post("/demo/seed"), "Sample data loaded. Open the Dashboard.")}>
            Load sample data
          </button>
          <button
            className="btn danger"
            disabled={busy}
            onClick={() => {
              if (window.confirm("Delete ALL of your data? Your account will stay.")) run(() => api.del("/demo"), "All of your data was deleted.");
            }}
          >
            Delete all my data
          </button>
        </div>
      </Card>

      <Card title="AI advisor (OpenAI)">
        <div className="row">{status.configured ? <Badge tone="good">On - {status.model}</Badge> : <Badge tone="warn">Off</Badge>}</div>
        {!status.configured && (
          <p className="muted small" style={{ marginTop: 10 }}>
            To turn it on, copy <code>backend/.env.example</code> to <code>backend/.env</code>, add your <code>OPENAI_API_KEY</code>, and restart the backend. The
            key stays on the server and is never committed to Git.
          </p>
        )}
      </Card>

      <Card title="What is sent to OpenAI?">
        <p className="small">
          Only when AI is on: the summary figures Java already calculated for the question being explained (for example a goal name, its target, and monthly
          amounts). Never your email, password, or name. The AI cannot change your data; it can only suggest, and every change needs your approval. If an AI
          reply contains a dollar figure the app did not calculate, it is discarded. Each account can trigger a limited number of AI replies per day.
        </p>
      </Card>

      <Card title="Delete my account" className="accent-alert">
        <p className="small">This permanently deletes your account and everything in it: paycheck, expenses, debts, goals, and decisions.</p>
        {deleteError && <Notice tone="bad">{deleteError}</Notice>}
        <form onSubmit={removeAccount} className="row" style={{ alignItems: "flex-end" }}>
          <div style={{ flex: 1, maxWidth: 280 }}>
            <Field label="Confirm with your password">
              <input type="password" autoComplete="current-password" value={deletePassword} onChange={(e) => setDeletePassword(e.target.value)} required />
            </Field>
          </div>
          <button className="btn danger" type="submit" disabled={busy}>
            Delete my account
          </button>
        </form>
      </Card>
    </>
  );
}
