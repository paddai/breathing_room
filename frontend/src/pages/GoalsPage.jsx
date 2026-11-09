import { useState } from "react";
import { Link } from "react-router-dom";
import { api, useLoad } from "../api";
import { money, num } from "../format";
import { validateGoal } from "../validate";
import { Badge, Bar, Card, ErrorBox, Field, Loading, Notice, PageHead, statusTone } from "../components/ui";

const loadGoals = () => api.get("/goals");

export default function GoalsPage() {
  const { data: goals, error, loading, reload } = useLoad(loadGoals);
  const [name, setName] = useState("");
  const [target, setTarget] = useState("");
  const [saved, setSaved] = useState("");
  const [date, setDate] = useState("");
  const [monthly, setMonthly] = useState("");
  const [fieldErrors, setFieldErrors] = useState({});
  const [formError, setFormError] = useState(null);
  const [addAmounts, setAddAmounts] = useState({});
  const [rowErrors, setRowErrors] = useState({}); // goal id -> message shown under that goal

  const clear = (key) =>
    setFieldErrors((prev) => {
      if (!prev[key]) return prev;
      const { [key]: _gone, ...rest } = prev;
      return rest;
    });
  const bind = (setter, key) => (e) => {
    setter(e.target.value);
    clear(key);
  };

  async function create(e) {
    e.preventDefault();
    setFormError(null);
    const errs = validateGoal({ name, target, saved, date, monthly });
    setFieldErrors(errs);
    if (Object.keys(errs).length) {
      setFormError("Please fix the highlighted fields.");
      return;
    }
    try {
      await api.post("/goals", { name, targetAmount: num(target), savedAmount: num(saved), targetDate: date, monthlyContribution: num(monthly) });
      setName("");
      setTarget("");
      setSaved("");
      setDate("");
      setMonthly("");
      setFieldErrors({});
      reload();
    } catch (err) {
      if (err.fieldErrors) setFieldErrors(err.fieldErrors);
      setFormError(err.message);
    }
  }

  async function addSavings(id) {
    const raw = addAmounts[id];
    if (raw === undefined || String(raw).trim() === "" || !(num(raw) > 0)) {
      setRowErrors({ ...rowErrors, [id]: "Enter the amount you saved, more than 0." });
      return;
    }
    try {
      await api.post(`/goals/${id}/savings`, { amount: num(raw) });
      setAddAmounts({ ...addAmounts, [id]: "" });
      setRowErrors({ ...rowErrors, [id]: null });
      reload();
    } catch (err) {
      setRowErrors({ ...rowErrors, [id]: err.message });
    }
  }

  async function remove(id) {
    try {
      await api.del(`/goals/${id}`);
      reload();
    } catch (err) {
      setRowErrors({ ...rowErrors, [id]: err.message });
    }
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
                Target date {g.targetDate} ({g.monthsLeft} months left) · Saving {money(g.monthlyContribution)}/month · Needs {money(g.requiredMonthly)}/month to
                finish on time
              </p>
              <div className="row">
                <input
                  type="number"
                  style={{ width: 130 }}
                  placeholder="Add savings"
                  value={addAmounts[g.id] || ""}
                  onChange={(e) => {
                    setAddAmounts({ ...addAmounts, [g.id]: e.target.value });
                    setRowErrors({ ...rowErrors, [g.id]: null });
                  }}
                />
                <button type="button" className="btn small" onClick={() => addSavings(g.id)}>
                  Add
                </button>
                <button type="button" className="btn danger small" onClick={() => remove(g.id)}>
                  Delete
                </button>
              </div>
              {rowErrors[g.id] && <p className="field-error small" style={{ margin: "6px 0 0" }}>{rowErrors[g.id]}</p>}
            </Card>
          ))}
          {goals.length > 0 && (
            <p className="small">
              <Link to="/advisor">Get Advisor suggestions for these goals</Link>
            </p>
          )}
        </div>

        <Card title="Add a goal">
          {formError && <Notice tone="bad">{formError}</Notice>}
          <form onSubmit={create} className="grid" noValidate>
            <Field label="Goal name" error={fieldErrors.name}>
              <input value={name} onChange={bind(setName, "name")} placeholder="Emergency fund" />
            </Field>
            <Field label="Target amount" error={fieldErrors.targetAmount}>
              <input type="number" value={target} onChange={bind(setTarget, "targetAmount")} placeholder="6000" />
            </Field>
            <Field label="Saved so far" hint="Optional. Leave blank if you are starting from zero." error={fieldErrors.savedAmount}>
              <input type="number" value={saved} onChange={bind(setSaved, "savedAmount")} placeholder="0" />
            </Field>
            <Field label="Target date" error={fieldErrors.targetDate}>
              <input type="date" value={date} onChange={bind(setDate, "targetDate")} />
            </Field>
            <Field label="Monthly amount you plan to save" hint="Optional - the Advisor can suggest one." error={fieldErrors.monthlyContribution}>
              <input type="number" value={monthly} onChange={bind(setMonthly, "monthlyContribution")} placeholder="0" />
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
