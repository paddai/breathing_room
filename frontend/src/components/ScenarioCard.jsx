import { useEffect, useState } from "react";
import { api } from "../api";
import { duration, money, num } from "../format";
import { useDebounced } from "../hooks";
import { Card, Field, Notice } from "./ui";

// "What if I lowered my 401(k) to my employer's match and sent the difference to debt?"  Calculated by the Java backend (ScenarioMath). No AI is used here.
export default function ScenarioCard() {
  const [afterTax, setAfterTax] = useState("100");
  const settled = useDebounced(afterTax, 300);
  const [s, setS] = useState(null);
  const [error, setError] = useState(null);

  useEffect(() => {
    let stale = false;
    const keep = Math.min(100, Math.max(0, num(settled)));
    api
      .get(`/recommendation/scenario?afterTax=${keep}`)
      .then((r) => {
        if (!stale) {
          setS(r);
          setError(null);
        }
      })
      .catch((e) => {
        if (!stale) setError(e.message);
      });
    return () => {
      stale = true;
    };
  }, [settled]);

  if (error) return <Notice tone="bad">{error}</Notice>;
  if (!s) return null;
  if (!s.applicable) {
    return (
      <Card title="What if you lowered your 401(k) to the match?">
        <p className="muted">{s.note}</p>
      </Card>
    );
  }

  return (
    <Card title="What if you lowered your 401(k) to the match?" className="accent-gold">
      <p>
        You would free about <strong>{money(s.freedGrossPerMonth)}</strong> a month from your 401(k) and send <strong>{money(s.extraToDebtPerMonth)}</strong> a
        month to your debts, starting with <strong>{s.targetDebt}</strong> ({s.targetApr}% APR). Your employer match is not affected.
      </p>
      <div style={{ maxWidth: 320, marginBottom: 12 }}>
        <Field label="How much of the freed money you keep after taxes (%)" hint="If your 401(k) is pre-tax, you keep less than 100%. Try 75 to 80.">
          <input type="number" min="0" max="100" value={afterTax} onChange={(e) => setAfterTax(e.target.value)} />
        </Field>
      </div>

      <div className="grid grid-2">
        <div className="option">
          <strong>Keep things as they are</strong>
          <span className="muted small">Debt-free in {duration(s.current.months, s.current.paidOff)}</span>
          {s.current.paidOff && <span className="muted small"> Interest paid: {money(s.current.totalInterest)}</span>}
        </div>
        <div className="option selected">
          <strong>Lower 401(k) and pay debt faster</strong>
          <span className="muted small">Debt-free in {duration(s.alternative.months, s.alternative.paidOff)}</span>
          {s.alternative.paidOff && <span className="muted small"> Interest paid: {money(s.alternative.totalInterest)}</span>}
        </div>
      </div>

      {s.current.paidOff && s.alternative.paidOff && (
        <p style={{ marginTop: 12 }}>
          That is about <strong>{s.monthsSaved} months sooner</strong> and <strong>{money(s.interestSaved)}</strong> less interest. The trade-off: roughly{" "}
          <strong>{money(s.retirementGivenUp)}</strong> less goes into your 401(k) during those {s.alternative.months} months.
        </p>
      )}
      {s.note && <Notice tone="warn">{s.note}</Notice>}
      <p className="disclaimer">{s.caution}</p>
    </Card>
  );
}
