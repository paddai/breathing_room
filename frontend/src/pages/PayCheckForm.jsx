import { useEffect, useState } from "react";
import { api } from "../api";
import { money, num } from "../format";
import { useDebounced } from "../hooks";
import { Card, ErrorBox, Field, Notice, PageHead } from "../components/ui";

const FREQUENCIES = {
  weekly: { label: "Weekly", perYear: 52 },
  biweekly: { label: "Biweekly", perYear: 26 },
  semimonthly: { label: "Semi-monthly", perYear: 24 },
  monthly: { label: "Monthly", perYear: 12 },
};

const show = (v, fallback = "") => (v ? String(v) : fallback);

export default function PaycheckForm() {
  const [payType, setPayType] = useState("hourly");
  const [hourlyRate, setHourlyRate] = useState("");
  const [regularHours, setRegularHours] = useState("");
  const [overtimeHours, setOvertimeHours] = useState("");
  const [overtimeMultiplier, setOvertimeMultiplier] = useState("1.5");
  const [salaryAmount, setSalaryAmount] = useState("");
  const [frequency, setFrequency] = useState("biweekly");
  const [deductions, setDeductions] = useState([]);
  const [dName, setDName] = useState("");
  const [dAmount, setDAmount] = useState("");
  const [k401, setK401] = useState("");
  const [matchRate, setMatchRate] = useState("");
  const [matchLimit, setMatchLimit] = useState("");

  const [message, setMessage] = useState(null);
  const [error, setError] = useState(null);

  // Load what was saved last time (task 4.7 - saved in PostgreSQL, so it survives a refresh)
  useEffect(() => {
    api
      .get("/paycheck")
      .then((p) => {
        setPayType(p.payType);
        setHourlyRate(show(p.hourlyRate));
        setRegularHours(show(p.regularHours));
        setOvertimeHours(show(p.overtimeHours));
        setOvertimeMultiplier(show(p.overtimeMultiplier, "1.5"));
        setSalaryAmount(show(p.salaryAmount));
        setFrequency(p.frequency);
        setK401(show(p.contribution401kPercent));
        setMatchRate(show(p.employerMatchRate));
        setMatchLimit(show(p.employerMatchLimit));
        setDeductions(p.deductions.map((d) => ({ id: crypto.randomUUID(), name: d.name, amount: d.amount })));
      })
      .catch((e) => setError(e.message));
  }, []);

  // ---- the numbers on the right are calculated by the Java backend (nothing is calculated in the browser) ----
  const payload = {
    payType,
    hourlyRate: num(hourlyRate),
    regularHours: num(regularHours),
    overtimeHours: num(overtimeHours),
    overtimeMultiplier: num(overtimeMultiplier),
    salaryAmount: num(salaryAmount),
    frequency,
    contribution401kPercent: num(k401),
    employerMatchRate: num(matchRate),
    employerMatchLimit: num(matchLimit),
    deductions: deductions.map((d) => ({ name: d.name, amount: d.amount })),
  };
  const settledKey = useDebounced(JSON.stringify(payload), 300);
  const [calc, setCalc] = useState(null);
  const [calcError, setCalcError] = useState(null);
  useEffect(() => {
    let stale = false;
    api
      .post("/paycheck/preview", JSON.parse(settledKey))
      .then((r) => {
        if (!stale) {
          setCalc(r);
          setCalcError(null);
        }
      })
      .catch((e) => {
        if (!stale) setCalcError(e.message);
      });
    return () => {
      stale = true;
    };
  }, [settledKey]);
  const c = calc ?? { grossPerPeriod: 0, periodsPerYear: FREQUENCIES[frequency].perYear, grossPerYear: 0, totalDeductions: 0, k401PerPeriod: 0, employerMatchPerPeriod: 0, takeHomePerPeriod: 0, takeHomePerMonth: 0 };

  function addDeduction(e) {
    e.preventDefault();
    const amount = num(dAmount);
    if (!dName.trim() || amount <= 0) return;
    setDeductions([...deductions, { id: crypto.randomUUID(), name: dName.trim(), amount }]);
    setDName("");
    setDAmount("");
  }

  async function save() {
    setMessage(null);
    setError(null);
    try {
      await api.put("/paycheck", payload);
      setMessage("Saved. Your dashboard and recommendations now use these numbers.");
    } catch (e) {
      setError(e.message);
    }
  }

  return (
    <>
      <PageHead title="Paycheck" sub="From gross pay to take-home pay." />
      {error && <ErrorBox message={error} />}
      {message && <Notice>{message}</Notice>}

      <div className="split">
        <div>
          <Card title="Gross pay">
            <div className="toggle">
              <button type="button" className={`btn ${payType === "hourly" ? "on" : ""}`} onClick={() => setPayType("hourly")}>
                Hourly
              </button>
              <button type="button" className={`btn ${payType === "salary" ? "on" : ""}`} onClick={() => setPayType("salary")}>
                Salary
              </button>
            </div>

            {payType === "hourly" ? (
              <div className="grid grid-2">
                <Field label="Hourly rate">
                  <input type="number" value={hourlyRate} onChange={(e) => setHourlyRate(e.target.value)} placeholder="0.00" />
                </Field>
                <Field label="Regular hours (per pay period)">
                  <input type="number" value={regularHours} onChange={(e) => setRegularHours(e.target.value)} placeholder="0" />
                </Field>
                <Field label="Overtime hours">
                  <input type="number" value={overtimeHours} onChange={(e) => setOvertimeHours(e.target.value)} placeholder="0" />
                </Field>
                <Field label="Overtime multiplier">
                  <input type="number" step="0.1" value={overtimeMultiplier} onChange={(e) => setOvertimeMultiplier(e.target.value)} />
                </Field>
              </div>
            ) : (
              <Field label="Salary for this pay period">
                <input type="number" value={salaryAmount} onChange={(e) => setSalaryAmount(e.target.value)} placeholder="0.00" />
              </Field>
            )}

            <div style={{ marginTop: 16 }}>
              <Field label="Pay frequency">
                <select value={frequency} onChange={(e) => setFrequency(e.target.value)}>
                  {Object.entries(FREQUENCIES).map(([key, f]) => (
                    <option key={key} value={key}>
                      {f.label}
                    </option>
                  ))}
                </select>
              </Field>
            </div>
          </Card>

          <Card title="Payroll deductions">
            <p className="muted small">Enter each line from your paystub (federal tax, state tax, health insurance...).</p>
            <form onSubmit={addDeduction} className="row" style={{ alignItems: "flex-end" }}>
              <div style={{ flex: 2 }}>
                <Field label="Name">
                  <input value={dName} onChange={(e) => setDName(e.target.value)} placeholder="Federal tax" />
                </Field>
              </div>
              <div style={{ flex: 1 }}>
                <Field label="Amount">
                  <input type="number" value={dAmount} onChange={(e) => setDAmount(e.target.value)} placeholder="0.00" />
                </Field>
              </div>
              <button className="btn" type="submit">
                Add
              </button>
            </form>
            {deductions.map((d) => (
              <div key={d.id} className="list-row">
                <span>{d.name}</span>
                <span className="row">
                  {money(d.amount)}
                  <button type="button" className="btn danger small" onClick={() => setDeductions(deductions.filter((x) => x.id !== d.id))}>
                    Delete
                  </button>
                </span>
              </div>
            ))}
          </Card>

          <Card title="Retirement (401(k))">
            <div className="grid grid-3">
              <Field label="Your contribution (%)">
                <input type="number" step="0.1" value={k401} onChange={(e) => setK401(e.target.value)} placeholder="0" />
              </Field>
              <Field label="Employer match rate (%)">
                <input type="number" step="0.1" value={matchRate} onChange={(e) => setMatchRate(e.target.value)} placeholder="e.g. 50" />
              </Field>
              <Field label="Match limit (% of pay)">
                <input type="number" step="0.1" value={matchLimit} onChange={(e) => setMatchLimit(e.target.value)} placeholder="e.g. 6" />
              </Field>
            </div>
            <p className="disclaimer">
              Example: a 50% match up to 6% means your employer adds 50 cents per dollar you contribute, on the first 6% of pay only.
            </p>
          </Card>
        </div>

        <Card title="Your paycheck">
          <div className="result-box">
            <Stat2 label="Gross pay (this paycheck)" value={money(c.grossPerPeriod)} />
            <Stat2 label={`Annual gross (${FREQUENCIES[frequency].label.toLowerCase()})`} value={money(c.grossPerYear)} />
            <Stat2 label="Total deductions" value={money(c.totalDeductions)} />
            <Stat2 label="401(k) contribution" value={money(c.k401PerPeriod)} />
            <Stat2 label="Employer match (free money)" value={money(c.employerMatchPerPeriod)} />
            <Stat2 label="Pay periods per year" value={String(c.periodsPerYear)} />
            <Stat2 label="Estimated take-home (this paycheck)" value={money(c.takeHomePerPeriod)} strong />
            <Stat2 label="Take-home per month" value={money(c.takeHomePerMonth)} strong />
          </div>
          {calcError && <p className="disclaimer">Check your numbers: {calcError}</p>}
          <div className="row" style={{ marginTop: 16 }}>
            <button className="btn" type="button" onClick={save}>
              Save paycheck
            </button>
          </div>
          <p className="disclaimer">Educational estimate based on the numbers you enter. It is not tax advice.</p>
        </Card>
      </div>
    </>
  );
}

function Stat2({ label, value, strong }) {
  return (
    <div>
      <div className="stat-label">{label}</div>
      <div className="stat-value" style={{ fontSize: strong ? 26 : 19 }}>
        {value}
      </div>
    </div>
  );
}
