import { useState } from "react";
import { Link } from "react-router-dom";
import { api, useLoad } from "../api";
import { money, num } from "../format";
import { Badge, Bar, Card, ErrorBox, Field, Loading, PageHead, statusTone } from "../components/ui";

const loadGoals = () => api.get("/goals");

export default function GoalsPage() {
  const { data: goals, error, loading, reload } = useLoad(loadGoals);
  const [name, setName] = useState("");
  const [target, setTarget] = useState("");
  const [saved, setSaved] = useState("");
  const [date, setDate] = useState("");
  const [monthly, setMonthly] = useState("");
  const [formError, setFormError] = useState(null);
  const [addAmounts, setAddAmounts] = useState({});

  async function create(e) {
    e.preventDefault();
    setFormError(null);
    try {
      await api.post("/goals", {
        name,
        targetAmount: num(target),
        savedAmount: num(saved),
        targetDate: date,
        monthlyContribution: num(monthly),
      });
      setName("");
      setTarget("");
      setSaved("");
      setDate("");
      setMonthly("");
      reload();
    } catch (err) {
      setFormError(err.message);
    }
  }

  async function addSavings(id) {
    const amount = num(addAmounts[id]);
    if (amount <= 0) return;
    await api.post(`/goals/${id}/savings`, { amount });
    setAddAmounts({ ...addAmounts, [id]: "" });
    reload();
  }

  async function remove(id) {
    await api.del(`/goals/${id}`);
    reload();
  }

  if (loading) return <Loading />;
  if (error) return <ErrorBox message={error} />;

  return (
    <>
      <PageHead title="Goals" sub="Set a savings goal. The Advisor can suggest a plan - but only you approve changes." />

      <div className="split">
        <div>
          {goals.length === 0 && (
            <Card>
              <p className="muted">No goals yet. Add your first one on the right.</p>
            </Card>
          )}
          {goals.map((g) => (
            <Card key={g.id}>
              <div className="row between">
                <h3 style={{ margin: 0 }}>{g.name}</h3>
                <Badge tone={statusTone(g.status)}>{g.status}</Badge>
              </div>
              <div className="row between small" style={{ margin: "10px 0 4px" }}>
                <span>
                  {money(g.savedAmount)} of {money(g.targetAmount)}
                </span>
                <span className="muted">{g.percentComplete.toFixed(0)}%</span>
              </div>
              <Bar percent={g.percentComplete} tone={g.status === "Needs attention" ? "warn" : g.status === "Overdue" ? "bad" : ""} />
              <p className="muted small" style={{ marginTop: 10 }}>
                Target date {g.targetDate} ({g.monthsLeft} months left) · Saving {money(g.monthlyContribution)}/month · Needs{" "}
                {money(g.requiredMonthly)}/month to finish on time
              </p>
              <div className="row">
                <input
                  type="number"
                  style={{ width: 130 }}
                  placeholder="Add savings"
                  value={addAmounts[g.id] || ""}
                  onChange={(e) => setAddAmounts({ ...addAmounts, [g.id]: e.target.value })}
                />
                <button type="button" className="btn small" onClick={() => addSavings(g.id)}>
                  Add
                </button>
                <button type="button" className="btn danger small" onClick={() => remove(g.id)}>
                  Delete
                </button>
              </div>
            </Card>
          ))}
          {goals.length > 0 && (
            <p className="small">
              <Link to="/advisor">Get Advisor suggestions for these goals</Link>
            </p>
          )}
        </div>

        <Card title="Add a goal">
          {formError && <ErrorBox message={formError} />}
          <form onSubmit={create} className="grid">
            <Field label="Goal name">
              <input value={name} onChange={(e) => setName(e.target.value)} placeholder="Emergency fund" />
            </Field>
            <Field label="Target amount">
              <input type="number" value={target} onChange={(e) => setTarget(e.target.value)} placeholder="6000" />
            </Field>
            <Field label="Saved so far">
              <input type="number" value={saved} onChange={(e) => setSaved(e.target.value)} placeholder="0" />
            </Field>
            <Field label="Target date">
              <input type="date" value={date} onChange={(e) => setDate(e.target.value)} />
            </Field>
            <Field label="Monthly amount you plan to save" hint="Optional - the Advisor can suggest one.">
              <input type="number" value={monthly} onChange={(e) => setMonthly(e.target.value)} placeholder="0" />
            </Field>
            <button className="btn" type="submit">
              Add goal
            </button>
          </form>
        </Card>
      </div>
    </>
  );
}
