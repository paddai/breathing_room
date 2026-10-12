import { useState } from "react";
import { api, useLoad } from "../api";
import { money, num } from "../format";
import { Card, ErrorBox, Field, Loading, PageHead, Stat } from "../components/ui";

const loadDebts = () => api.get("/debts");

export default function DebtForm() {
  const { data: debts, error, loading, reload } = useLoad(loadDebts);
  const [name, setName] = useState("");
  const [balance, setBalance] = useState("");
  const [apr, setApr] = useState("");
  const [minPayment, setMinPayment] = useState("");
  const [formError, setFormError] = useState(null);

  async function add(e) {
    e.preventDefault();
    setFormError(null);
    try {
      await api.post("/debts", { name, balance: num(balance), apr: num(apr), minPayment: num(minPayment) });
      setName("");
      setBalance("");
      setApr("");
      setMinPayment("");
      reload();
    } catch (err) {
      setFormError(err.message);
    }
  }

  async function remove(id) {
    await api.del(`/debts/${id}`);
    reload();
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
          {formError && <ErrorBox message={formError} />}
          <form onSubmit={add} className="grid">
            <Field label="Debt name">
              <input value={name} onChange={(e) => setName(e.target.value)} placeholder="Visa credit card" />
            </Field>
            <Field label="Balance">
              <input type="number" step="0.01" value={balance} onChange={(e) => setBalance(e.target.value)} placeholder="0.00" />
            </Field>
            <Field label="APR (%)">
              <input type="number" step="0.1" value={apr} onChange={(e) => setApr(e.target.value)} placeholder="24.0" />
            </Field>
            <Field label="Minimum payment">
              <input type="number" step="0.01" value={minPayment} onChange={(e) => setMinPayment(e.target.value)} placeholder="0.00" />
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
