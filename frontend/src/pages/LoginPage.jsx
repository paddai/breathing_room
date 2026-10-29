import { useState } from "react";
import { Navigate, useNavigate } from "react-router-dom";
import { useAuth } from "../auth/authContext";
import { Field, Notice } from "../components/ui";

export default function LoginPage() {
  const { user, login, register } = useAuth();
  const navigate = useNavigate();
  const [mode, setMode] = useState("signin"); // "signin" | "signup"
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState(null);
  const [busy, setBusy] = useState(false);

  if (user) return <Navigate to="/" replace />;

  const signup = mode === "signup";

  async function submit(e) {
    e.preventDefault();
    setError(null);
    if (signup && password.length < 10) {
      setError("Use at least 10 characters for your password.");
      return;
    }
    setBusy(true);
    try {
      await (signup ? register(email, password) : login(email, password));
      navigate("/", { replace: true });
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="auth-wrap">
      <div className="auth-card">
        <h1>Breathing Room</h1>
        <p className="page-sub">From paycheck pressure to financial clarity.</p>

        <div className="toggle" style={{ marginTop: 18 }}>
          <button type="button" className={`btn ${!signup ? "on" : ""}`} onClick={() => { setMode("signin"); setError(null); }}>
            Sign in
          </button>
          <button type="button" className={`btn ${signup ? "on" : ""}`} onClick={() => { setMode("signup"); setError(null); }}>
            Create account
          </button>
        </div>

        {error && <Notice tone="bad">{error}</Notice>}

        <form onSubmit={submit} className="grid">
          <Field label="Email">
            <input type="email" autoComplete="email" value={email} onChange={(e) => setEmail(e.target.value)} required />
          </Field>
          <Field label="Password" hint={signup ? "At least 10 characters. A short phrase only you would know works well." : undefined}>
            <input
              type="password"
              autoComplete={signup ? "new-password" : "current-password"}
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
            />
          </Field>
          <button className="btn" type="submit" disabled={busy}>
            {busy ? "One moment..." : signup ? "Create account" : "Sign in"}
          </button>
        </form>

        <p className="disclaimer">
          Your numbers are private to your account, and your password is stored only as a one-way hash. Breathing Room is an educational
          planning tool, not financial, tax, or investment advice.
        </p>
      </div>
    </div>
  );
}
