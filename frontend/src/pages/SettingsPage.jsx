import { useState } from "react";
import { api, useLoad } from "../api";
import { useAuth } from "../auth/authContext";
import { Badge, Card, ErrorBox, Field, Loading, Notice, PageHead } from "../components/ui";

const loadStatus = () => api.get("/advisor/status");

export default function SettingsPage() {
  const { user, logout, deleteAccount } = useAuth();
  const { data: status, error, loading } = useLoad(loadStatus);
  const [aiTest, setAiTest] = useState(null);
  const [testing, setTesting] = useState(false);

  async function testAi() {
    setTesting(true);
    try {
      setAiTest(await api.post("/advisor/test", {}));
    } catch (e) {
      setAiTest({ ok: false, message: e.message });
    } finally {
      setTesting(false);
    }
  }
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

      <Card title="AI advisor">
        <div className="row">
          {aiTest ? (
            aiTest.ok ? <Badge tone="good">Working - {status.provider} {status.model}</Badge> : <Badge tone="bad">Not working</Badge>
          ) : status.configured ? (
            <Badge tone="warn">Key found - not tested yet</Badge>
          ) : (
            <Badge tone="warn">Off - no key</Badge>
          )}
          <button type="button" className="btn secondary small" disabled={testing || !status.configured} onClick={testAi}>
            {testing ? "Testing..." : "Test AI connection"}
          </button>
        </div>
        {aiTest && <p className="small" style={{ marginTop: 10 }}>{aiTest.message}</p>}
        {status.configured && !aiTest && (
          <p className="muted small" style={{ marginTop: 10 }}>A key being present does not prove it works. Press Test AI connection to check.</p>
        )}
        {!status.configured && (
          <p className="muted small" style={{ marginTop: 10 }}>
            To turn it on, copy <code>backend/.env.example</code> to <code>backend/.env</code>, add your <code>ANTHROPIC_API_KEY</code> (or <code>OPENAI_API_KEY</code>), and restart the backend. The
            key stays on the server and is never committed to Git.
          </p>
        )}
      </Card>

      <Card title="What is sent to the AI?">
        <p className="small">
          Only when AI is on: the summary figures and options the app already calculated for the question being explained (for example income, free money, debt balances and rates, goal
          targets, and 401(k) percentages). Never your email, password, or name. The AI cannot change your data; it can only suggest, and every change needs your approval. If an AI
          reply contains a dollar or percent figure the app did not calculate, it is discarded. Each account can trigger a limited number of AI replies per day.
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
