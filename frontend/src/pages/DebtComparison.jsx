import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { api, useLoad } from "../api";
import { duration, money, num } from "../format";
import { useDebounced } from "../hooks";
import { Badge, Card, ErrorBox, Field, Loading, Notice, PageHead } from "../components/ui";

const SAMPLE_DEBTS = [
  { name: "Medical bill", balance: 800, apr: 0, minPayment: 40 },
  { name: "Credit card", balance: 4200, apr: 24, minPayment: 90 },
  { name: "Car loan", balance: 9800, apr: 7, minPayment: 220 },
];

const loadContext = async () => {
  const [debts, summary] = await Promise.all([api.get("/debts"), api.get("/summary")]);
  return { debts, summary };
};

export default function DebtComparison() {
  const { data, error, loading } = useLoad(loadContext);
  const [extra, setExtra] = useState("100");
  const settledExtra = useDebounced(extra, 300);
  const [result, setResult] = useState(null);
  const [calcError, setCalcError] = useState(null);

  // The payoff plan is calculated by the Java backend (DebtPayoff). With no saved debts we send sample debts so the page still shows how it works.
  useEffect(() => {
    if (!data) return undefined;
    let stale = false;
    const body = { extra: num(settledExtra) };
    if (data.debts.length === 0) body.debts = SAMPLE_DEBTS;
    api
      .post("/debts/payoff", body)
      .then((r) => {
        if (!stale) {
          setResult(r);
          setCalcError(null);
        }
      })
      .catch((e) => {
        if (!stale) setCalcError(e.message);
      });
    return () => {
      stale = true;
    };
  }, [data, settledExtra]);

  if (loading) return <Loading />;
  if (error) return <ErrorBox message={error} />;

  const usingSample = data.debts.length === 0;
  const best = result && result.avalanche && result.snowball && result.avalanche.paidOff && result.snowball.paidOff
    ? (result.avalanche.totalInterest <= result.snowball.totalInterest ? "avalanche" : "snowball")
    : null;

  return (
    <>
      <PageHead title="Debt Payoff" sub="Compare paying the highest interest first (avalanche) with the smallest balance first (snowball)." />

      {usingSample && (
        <Notice tone="warn">
          Showing sample debts. Add your own on the <Link to="/debts">Debts</Link> page.
        </Notice>
      )}
      {calcError && <ErrorBox message={calcError} />}

      <Card>
        <div style={{ maxWidth: 300 }}>
          <Field label="Extra payment toward debt each month" hint={`Your free money each month: ${money(data.summary.monthlySurplus)}`}>
            <input type="number" value={extra} onChange={(e) => setExtra(e.target.value)} />
          </Field>
        </div>
      </Card>

      {result && result.debtCount > 0 && (
        <>
          <div className="grid grid-3">
            <Strategy title="Avalanche" sub="Highest APR first" r={result.avalanche} best={best === "avalanche"} />
            <Strategy title="Snowball" sub="Smallest balance first" r={result.snowball} best={best === "snowball"} />
            <Strategy title="Minimums only" sub="Each debt on its own, no extra" r={result.minimumsOnly} />
          </div>
          <Notice>{result.message}</Notice>
        </>
      )}
      {result && result.debtCount === 0 && <Notice tone="warn">{result.message}</Notice>}
      <p className="disclaimer">Calculated by the Breathing Room server. Educational estimate: real lenders round and charge fees differently.</p>
    </>
  );
}

function Strategy({ title, sub, r, best }) {
  return (
    <Card className={best ? "accent-teal" : ""}>
      <div className="row between">
        <h3>{title}</h3>
        {best && <Badge tone="good">Lowest interest</Badge>}
      </div>
      <p className="muted small">{sub}</p>
      <div className="stat-label">Debt-free in</div>
      <div className="stat-value" style={{ fontSize: 22 }}>
        {duration(r.months, r.paidOff)}
      </div>
      {r.paidOff && (
        <>
          <div className="stat-label" style={{ marginTop: 8 }}>
            Total interest
          </div>
          <div className="stat-value" style={{ fontSize: 22 }}>
            {money(r.totalInterest)}
          </div>
        </>
      )}
      <div className="stat-label" style={{ marginTop: 10 }}>
        Paid off in this order
      </div>
      <ol style={{ paddingLeft: 20, margin: "4px 0 0" }}>
        {r.order.map((s) => (
          <li key={s.name} className="small">
            {s.name} <span className="muted">(month {s.month})</span>
          </li>
        ))}
      </ol>
    </Card>
  );
}
