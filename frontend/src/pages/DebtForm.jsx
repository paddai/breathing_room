import { useState } from "react";
import { api, useLoad } from "../api";
import { money, num } from "../format";
import { validateDebt } from "../validate";
import { Card, ErrorBox, Field, Loading, Notice, PageHead, Stat } from "../components/ui";

const loadDebts = () => api.get("/debts");

export default function DebtForm() {
  const { data: debts, error, loading, reload } = useLoad(loadDebts);
  const [name, setName] = useState("");
  const [balance, setBalance] = useState("");
  const [apr, setApr] = useState("");
  const [minPayment, setMinPayment] = useState("");
  const [fieldErrors, setFieldErrors] = useState({});
  const [formError, setFormError] = useState(null);

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

  async function add(e) {
    e.preventDefault();
    setFormError(null);
    const errs = validateDebt({ name, balance, apr, minPayment });
    setFieldErrors(errs);
    if (Object.keys(errs).length) {
      setFormError("Please fix the highlighted fields.");
      return;
    }
    try {
      await api.post("/debts", { name, balance: num(balance), apr: num(apr), minPayment: num(minPayment) });
      setName("");
      setBalance("");
      setApr("");
      setMinPayment("");
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
      await api.del(`/debts/${id}`);
      reload();
    } catch (err) {
      setFormError(err.message);
    }
  }

  if (loading) return <Loading />;
  if (error) return <ErrorBox message={error} />;

  const total = debts.reduce((sum, d) => sum + d.balance, 0);
  const minimums = debts.reduce((sum, d) => sum + d.minPayment, 0);

  return (
    <>
      <PageHead title="Debts" sub="Enter each debt separately - this feeds the payoff comparison and recommendations." />

      <div className="grid grid-2">
        <Card>
          <Stat label="Total debt" value={money(total)} />
        </Card>
        <Card>
          <Stat label="Monthly minimums" value={money(minimums)} />
        </Card>
      </div>

      <div className="split">
        <Card title="Your debts">
          {debts.length === 0 && <p className="muted">No debts added yet.</p>}
          {debts.map((d) => (
            <div key={d.id} className="list-row">
              <div>
                <div>{d.name}</div>
                <div className="muted small">
                  {d.apr}% APR · {money(d.minPayment)} minimum / month
                </div>
              </div>
              <div className="row">
                <strong>{money(d.balance)}</strong>
                <button type="button" className="btn danger small" onClick={() => remove(d.id)}>
                  Delete
                </button>
              </div>
            </div>
          ))}
        </Card>

        <Card title="Add a debt">
          {formError && <Notice tone="bad">{formError}</Notice>}
          <form onSubmit={add} className="grid" noValidate>
            <Field label="Debt name" error={fieldErrors.name}>
              <input value={name} onChange={bind(setName, "name")} placeholder="Visa credit card" />
            </Field>
            <Field label="Balance" error={fieldErrors.balance}>
              <input type="number" step="0.01" value={balance} onChange={bind(setBalance, "balance")} placeholder="0.00" />
            </Field>
            <Field label="APR (%)" error={fieldErrors.apr}>
              <input type="number" step="0.1" value={apr} onChange={bind(setApr, "apr")} placeholder="24.0" />
            </Field>
            <Field label="Minimum payment" error={fieldErrors.minPayment}>
              <input type="number" step="0.01" value={minPayment} onChange={bind(setMinPayment, "minPayment")} placeholder="0.00" />
            </Field>
            <button className="btn" type="submit">
              Add debt
            </button>
          </form>
        </Card>
      </div>
    </>
  );
}
