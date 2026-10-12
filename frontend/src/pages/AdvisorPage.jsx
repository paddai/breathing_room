import { useState } from "react";
import { Link } from "react-router-dom";
import { api, useLoad } from "../api";
import { money } from "../format";
import { Badge, Bar, Card, ErrorBox, Field, Loading, Notice, PageHead, statusTone } from "../components/ui";

// Java builds every number and option; the AI (if a key is set) only explains / picks one option.
const loadAdvisor = async () => {
  const [status, recommendation, goals, decisions] = await Promise.all([
    api.get("/advisor/status"),
    api.get("/recommendation"),
    api.get("/goals"),
    api.get("/goal-decisions"),
  ]);
  const advice = await Promise.all(goals.filter((g) => g.status !== "Complete").map((g) => api.get(`/goals/${g.id}/advice`)));
  return { status, recommendation, advice, decisions };
};

export default function AdvisorPage() {
  const { data, error, loading, reload } = useLoad(loadAdvisor);
  const [picked, setPicked] = useState({}); // goalId -> optionId the user selected
  const [busy, setBusy] = useState(false);
  const [actionError, setActionError] = useState(null);
  const [question, setQuestion] = useState("");
  const [answer, setAnswer] = useState(null);
  const [asking, setAsking] = useState(false);

  async function decide(goalId, optionId, status) {
    setBusy(true);
    setActionError(null);
    try {
      await api.post(`/goals/${goalId}/decision`, { optionId, status });
      await reload();
    } catch (e) {
      setActionError(e.message);
    } finally {
      setBusy(false);
    }
  }

  async function ask(e) {
    e.preventDefault();
    if (!question.trim()) return;
    setAsking(true);
    setAnswer(null);
    try {
      setAnswer(await api.post("/advisor/ask", { question }));
    } catch (err) {
      setAnswer({ answer: err.message, aiUsed: false, note: "" });
    } finally {
      setAsking(false);
    }
  }

  if (loading) return <Loading text="Your advisor is reviewing your numbers..." />;
  if (error) return <ErrorBox message={error} />;

  const { status, recommendation: r, advice, decisions } = data;

  return (
    <>
      <PageHead title="Advisor" sub="Suggestions you can approve or decline. Nothing changes without your OK." />

      <Notice tone={status.configured ? "info" : "warn"}>
        {status.configured
          ? `AI explanations are on (${status.model}). Java calculates every number; the AI only explains and picks from options Java created.`
          : "AI explanations are off on this computer (no OPENAI_API_KEY). You still get built-in suggestions - see Settings."}
      </Notice>
      {actionError && <ErrorBox message={actionError} />}

      {r.facts.hasData && (
        <Card className={r.facts.flagged ? "accent-alert" : "accent-teal"} title="Your 401(k) and debt">
          <p>{r.explanation}</p>
          <Link to="/recommendation">See details</Link>
        </Card>
      )}

      <h2 style={{ fontSize: 22, margin: "22px 0 12px" }}>Goal suggestions</h2>
      {advice.length === 0 && (
        <Card>
          <p className="muted">
            No active goals. <Link to="/goals">Add a goal</Link> to get suggestions.
          </p>
        </Card>
      )}

      {advice.map((a) => {
        const g = a.goal;
        const selected = picked[g.id] || a.recommendedOptionId;
        return (
          <Card key={g.id} className="accent-gold">
            <div className="row between">
              <h3 style={{ margin: 0 }}>{g.name}</h3>
              <Badge tone={statusTone(g.status)}>{g.status}</Badge>
            </div>
            <div className="row between small" style={{ margin: "10px 0 4px" }}>
              <span>
                {money(g.savedAmount)} of {money(g.targetAmount)} by {g.targetDate}
              </span>
              <span className="muted">Free money available for this goal: {money(a.availableForGoal)}/month</span>
            </div>
            <Bar percent={g.percentComplete} />

            <p style={{ marginTop: 14 }}>{a.explanation}</p>
            <div className="row" style={{ marginBottom: 12 }}>
              {a.aiUsed ? <Badge tone="good">Explained by AI</Badge> : <Badge>Built-in suggestion</Badge>}
              <span className="muted small">{a.aiNote}</span>
            </div>

            {a.options.map((o) => (
              <div key={o.id} className={`option ${selected === o.id ? "selected" : ""}`} onClick={() => setPicked({ ...picked, [g.id]: o.id })}>
                <strong>
                  {o.label} {o.id === a.recommendedOptionId && <Badge tone="good">Suggested</Badge>}
                </strong>
                <span className="muted small">{o.projection}</span>
              </div>
            ))}

            <div className="row">
              <button type="button" className="btn" disabled={busy} onClick={() => decide(g.id, selected, "APPROVED")}>
                Approve selected option
              </button>
              <button type="button" className="btn secondary" disabled={busy} onClick={() => decide(g.id, selected, "DECLINED")}>
                Not now
              </button>
            </div>
            <p className="disclaimer">Approving updates this goal&apos;s monthly amount (and date, if the option changes it). It never moves real money.</p>
          </Card>
        );
      })}

      <h2 style={{ fontSize: 22, margin: "22px 0 12px" }}>Ask a question</h2>
      <Card>
        <form onSubmit={ask}>
          <Field label="About your own numbers" hint="Example: Can I afford to save more each month?">
            <input value={question} onChange={(e) => setQuestion(e.target.value)} maxLength={300} placeholder="Ask about your budget, debt, or goals" />
          </Field>
          <div className="row" style={{ marginTop: 10 }}>
            <button className="btn" type="submit" disabled={asking}>
              {asking ? "Thinking..." : "Ask"}
            </button>
          </div>
        </form>
        {answer && (
          <div style={{ marginTop: 14 }}>
            <p>{answer.answer}</p>
            {answer.note && <span className="muted small">{answer.note}</span>}
          </div>
        )}
        <p className="disclaimer">Educational guidance based on the numbers you entered - not financial, tax, or investment advice.</p>
      </Card>

      <h2 style={{ fontSize: 22, margin: "22px 0 12px" }}>Your decisions</h2>
      <Card>
        {decisions.length === 0 && <p className="muted">Nothing approved or declined yet.</p>}
        {decisions.map((d) => (
          <div key={d.id} className="list-row small">
            <div>
              <strong>{d.goalName}</strong> - {d.label}
              <div className="muted">{String(d.createdAt).replace("T", " ").slice(0, 16)}</div>
            </div>
            <Badge tone={d.status === "APPROVED" ? "good" : "neutral"}>{d.status === "APPROVED" ? "Approved" : "Declined"}</Badge>
          </div>
        ))}
      </Card>
    </>
  );
}
