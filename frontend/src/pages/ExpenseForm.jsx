import { useState } from "react";
import { api, useLoad } from "../api";
import { money, num, todayISO } from "../format";
import { validateExpense } from "../validate";
import { Card, ErrorBox, Field, Loading, Notice, PageHead, Stat } from "../components/ui";

const CATEGORIES = ["Housing", "Groceries", "Dining", "Transportation", "Utilities", "Shopping", "Other"];

const loadBudget = async () => {
  const [expenses, summary] = await Promise.all([api.get("/expenses"), api.get("/summary")]);
  return { expenses, summary };
};

export default function ExpenseForm() {
  const { data, error, loading, reload } = useLoad(loadBudget);
  const [description, setDescription] = useState("");
  const [category, setCategory] = useState("");
  const [amount, setAmount] = useState("");
  const [date, setDate] = useState(todayISO());
  const [fieldErrors, setFieldErrors] = useState({});
  const [formError, setFormError] = useState(null);

  // typing in an input clears that input's error message
  const clear = (key) =>
    setFieldErrors((prev) => {
      if (!prev[key]) return prev;
      const { [key]: _gone, ...rest } = prev;
      return rest;
    });

  async function add(e) {
    e.preventDefault();
    setFormError(null);
    const errs = validateExpense({ description, category, amount });
    setFieldErrors(errs);
    if (Object.keys(errs).length) {
      setFormError("Please fix the highlighted fields.");
      return;
    }
    try {
      await api.post("/expenses", { description, category, amount: num(amount), expenseDate: date });
      setDescription("");
      setAmount("");
      setFieldErrors({});
      reload();
    } catch (err) {
      if (err.fieldErrors) setFieldErrors(err.fieldErrors);
      setFormError(err.message);
    }
  }

  async function remove(id) {
    setFormError(null);
    try {
      await api.del(`/expenses/${id}`);
      reload();
    } catch (err) {
      setFormError(err.message);
    }
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
          {formError && <Notice tone="bad">{formError}</Notice>}
          <form onSubmit={add} className="grid" noValidate>
            <Field label="Description" error={fieldErrors.description}>
              <input
                value={description}
                onChange={(e) => {
                  setDescription(e.target.value);
                  clear("description");
                }}
                placeholder="Whole Foods"
              />
            </Field>
            <Field label="Category" error={fieldErrors.category}>
              <select
                value={category}
                onChange={(e) => {
                  setCategory(e.target.value);
                  clear("category");
                }}
              >
                <option value="">Choose a category...</option>
                {CATEGORIES.map((c) => (
                  <option key={c}>{c}</option>
                ))}
              </select>
            </Field>
            <Field label="Amount" error={fieldErrors.amount}>
              <input
                type="number"
                step="0.01"
                value={amount}
                onChange={(e) => {
                  setAmount(e.target.value);
                  clear("amount");
                }}
                placeholder="0.00"
              />
            </Field>
            <Field label="Date" error={fieldErrors.expenseDate}>
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
