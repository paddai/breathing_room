import { useState } from "react";
import { api, useLoad } from "../api";
import { money, num, todayISO } from "../format";
import { Card, ErrorBox, Field, Loading, PageHead, Stat } from "../components/ui";

const CATEGORIES = ["Housing", "Groceries", "Dining", "Transportation", "Utilities", "Shopping", "Other"];

const loadBudget = async () => {
  const [expenses, summary] = await Promise.all([api.get("/expenses"), api.get("/summary")]);
  return { expenses, summary };
};

export default function ExpenseForm() {
  const { data, error, loading, reload } = useLoad(loadBudget);
  const [description, setDescription] = useState("");
  const [category, setCategory] = useState(CATEGORIES[0]);
  const [amount, setAmount] = useState("");
  const [date, setDate] = useState(todayISO());
  const [formError, setFormError] = useState(null);

  async function add(e) {
    e.preventDefault();
    setFormError(null);
    try {
      await api.post("/expenses", { description, category, amount: num(amount), expenseDate: date });
      setDescription("");
      setAmount("");
      reload();
    } catch (err) {
      setFormError(err.message);
    }
  }

  async function remove(id) {
    await api.del(`/expenses/${id}`);
    reload();
  }

  if (loading) return <Loading />;
  if (error) return <ErrorBox message={error} />;
  const { expenses, summary: s } = data;

  return (
    <>
      <PageHead title="Budget & Expenses" sub="Log what you spend and see what is left this month." />

      <div className="grid grid-4">
        <Card>
          <Stat label="Monthly take-home" value={money(s.monthlyTakeHome)} />
        </Card>
        <Card>
          <Stat label="Spent this month" value={money(s.monthlyExpenses)} />
        </Card>
        <Card>
          <Stat label="Debt minimums" value={money(s.monthlyDebtMinimums)} />
        </Card>
        <Card>
          <Stat label="Left over" value={money(s.monthlySurplus)} tone={s.monthlySurplus < 0 ? "bad" : "good"} hint={s.monthlySurplus < 0 ? "Deficit" : "Surplus"} />
        </Card>
      </div>

      <div className="split">
        <Card title="Expenses">
          {expenses.length === 0 && <p className="muted">No expenses logged yet.</p>}
          {expenses.map((x) => (
            <div key={x.id} className="list-row">
              <div>
                <div>{x.description}</div>
                <div className="muted small">
                  {x.category} · {x.expenseDate}
                </div>
              </div>
              <div className="row">
                <strong>{money(x.amount)}</strong>
                <button type="button" className="btn danger small" onClick={() => remove(x.id)}>
                  Delete
                </button>
              </div>
            </div>
          ))}
        </Card>

        <Card title="Add an expense">
          {formError && <ErrorBox message={formError} />}
          <form onSubmit={add} className="grid">
            <Field label="Description">
              <input value={description} onChange={(e) => setDescription(e.target.value)} placeholder="Whole Foods" />
            </Field>
            <Field label="Category">
              <select value={category} onChange={(e) => setCategory(e.target.value)}>
                {CATEGORIES.map((c) => (
                  <option key={c}>{c}</option>
                ))}
              </select>
            </Field>
            <Field label="Amount">
              <input type="number" step="0.01" value={amount} onChange={(e) => setAmount(e.target.value)} placeholder="0.00" />
            </Field>
            <Field label="Date">
              <input type="date" value={date} onChange={(e) => setDate(e.target.value)} />
            </Field>
            <button className="btn" type="submit">
              Add expense
            </button>
          </form>
        </Card>
      </div>
    </>
  );
}
