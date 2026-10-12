import { useState } from "react";
import { Link } from "react-router-dom";
import { api, useLoad } from "../api";
import { money, num } from "../format";
import { Badge, Card, ErrorBox, Field, Loading, Notice, PageHead } from "../components/ui";

const SAMPLE_DEBTS = [
  { name: "Store card", balance: 600, apr: 26, minPayment: 25 },
  { name: "Credit card", balance: 4200, apr: 24, minPayment: 90 },
  { name: "Car loan", balance: 9800, apr: 7, minPayment: 220 },
];

const loadComparison = async () => {
  const [debts, summary] = await Promise.all([api.get("/debts"), api.get("/summary")]);
  return { debts, summary };
};

// Month-by-month payoff. Every debt gets its minimum; the extra money goes to the first
// still-unpaid debt in `order`. Snowball vs. avalanche only differ in how `order` is sorted.
function simulatePayoff(debts, extraPerMonth, order) {
  const active = order.map((name) => ({ ...debts.find((d) => d.name === name) }));
  let months = 0;
  let totalInterest = 0;

  while (active.some((d) => d.balance > 0.005) && months < 600) {
    months++;
    let extra = extraPerMonth;
    const first = active.find((d) => d.balance > 0.005);

    for (const debt of active) {
      if (debt.balance <= 0.005) continue;
      const interest = debt.balance * (debt.apr / 100 / 12);
      totalInterest += interest;
      debt.balance += interest;

      let payment = debt.minPayment;
      if (debt === first && extra > 0) {
        payment += extra;
        extra = 0;
      }
      payment = Math.min(payment, debt.balance);
      debt.balance -= payment;
    }
    // if minimums alone can never pay a debt off, stop instead of looping forever
    if (months === 600) break;
  }
  return { months, totalInterest, finished: months < 600 };
}

export default function DebtComparison() {
  const { data, error, loading } = useLoad(loadComparison);
  const [extra, setExtra] = useState("100");

  if (loading) return <Loading />;
  if (error) return <ErrorBox message={error} />;

  const usingSample = data.debts.length === 0;
  const debts = usingSample ? SAMPLE_DEBTS : data.debts;
  const extraNum = num(extra);

  const avalanche = [...debts].sort((a, b) => b.apr - a.apr).map((d) => d.name);
  const snowball = [...debts].sort((a, b) => a.balance - b.balance).map((d) => d.name);
  const resA = simulatePayoff(debts, extraNum, avalanche);
  const resS = simulatePayoff(debts, extraNum, snowball);
  const saved = resS.totalInterest - resA.totalInterest;

  return (
    <>
      <PageHead title="Debt Payoff" sub="Compare paying the highest interest first (avalanche) with the smallest balance first (snowball)." />

      {usingSample && (
        <Notice tone="warn">
          Showing sample debts. Add your own on the <Link to="/debts">Debts</Link> page.
        </Notice>
      )}

      <Card>
        <div style={{ maxWidth: 280 }}>
          <Field label="Extra payment toward debt each month" hint={`Your free money each month: ${money(data.summary.monthlySurplus)}`}>
            <input type="number" value={extra} onChange={(e) => setExtra(e.target.value)} />
          </Field>
        </div>
      </Card>

      <div className="grid grid-2">
        <Strategy title="Avalanche" sub="Highest APR first" order={avalanche} result={resA} best={resA.totalInterest <= resS.totalInterest} />
        <Strategy title="Snowball" sub="Smallest balance first" order={snowball} result={resS} best={resS.totalInterest < resA.totalInterest} />
      </div>

      {saved > 0.01 && (
        <Notice>Avalanche saves about {money(saved)} in total interest compared with snowball at this payment level.</Notice>
      )}
      <p className="disclaimer">Educational estimate. Real balances and interest rules vary by lender.</p>
    </>
  );
}

function Strategy({ title, sub, order, result, best }) {
  return (
    <Card className={best ? "accent-teal" : ""}>
      <div className="row between">
        <h3>{title}</h3>
        {best && <Badge tone="good">Lowest interest</Badge>}
      </div>
      <p className="muted small">{sub}</p>
      <ol style={{ paddingLeft: 20 }}>
        {order.map((n) => (
          <li key={n}>{n}</li>
        ))}
      </ol>
      <div className="stat-label">Payoff time</div>
      <div className="stat-value" style={{ fontSize: 22 }}>
        {result.finished ? `${Math.floor(result.months / 12)}y ${result.months % 12}mo` : "50+ years"}
      </div>
      <div className="stat-label" style={{ marginTop: 8 }}>
        Total interest
      </div>
      <div className="stat-value" style={{ fontSize: 22 }}>
        {money(result.totalInterest)}
      </div>
    </Card>
  );
}
