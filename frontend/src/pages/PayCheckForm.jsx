import { useEffect, useState } from "react";
import { api } from "../api";
import { money, num } from "../format";
import { useDebounced } from "../hooks";
import { validatePaycheck } from "../validate";
import { Card, ErrorBox, Field, Notice, PageHead } from "../components/ui";

const FREQUENCIES = {
  weekly: { label: "Weekly", perYear: 52 },
  biweekly: { label: "Biweekly", perYear: 26 },
  semimonthly: { label: "Semi-monthly", perYear: 24 },
  monthly: { label: "Monthly", perYear: 12 },
};

// These rows are always on the page. Leave one blank if it does not apply to you.
const DEFAULT_DEDUCTIONS = ["Federal tax", "State tax", "Social Security + Medicare", "Health insurance"];
const blankDefaults = () => Object.fromEntries(DEFAULT_DEDUCTIONS.map((n) => [n, ""]));
const show = (v, fallback = "") => (v ? String(v) : fallback);

export default function PaycheckForm() {
  const [payType, setPayType] = useState("hourly");
  const [hourlyRate, setHourlyRate] = useState("");
  const [regularHours, setRegularHours] = useState("");
  const [overtimeHours, setOvertimeHours] = useState("");
  const [overtimeMultiplier, setOvertimeMultiplier] = useState("1.5");
  const [salaryAmount, setSalaryAmount] = useState("");
  const [frequency, setFrequency] = useState("biweekly");
  const [defaultAmounts, setDefaultAmounts] = useState(blankDefaults);
  const [custom, setCustom] = useState([]); // extra deductions the person added: { id, name, amount }
  const [cName, setCName] = useState("");
  const [cAmount, setCAmount] = useState("");
  const [customError, setCustomError] = useState(null);
  const [k401, setK401] = useState("");
  const [matchRate, setMatchRate] = useState("");
  const [matchLimit, setMatchLimit] = useState("");

  const [fieldErrors, setFieldErrors] = useState({});
  const [message, setMessage] = useState(null);
  const [saveError, setSaveError] = useState(null);
  const [loadError, setLoadError] = useState(null);

  const clear = (key) =>
    setFieldErrors((prev) => {
      if (!prev[key]) return prev;
      const { [key]: _gone, ...rest } = prev;
      return rest;
    });
  const bind = (setter, key) => (e) => {
    setter(e.target.value);
    if (key) clear(key);
  };

  // Load what was saved last time (so it survives a refresh)
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
        const defaults = blankDefaults();
        const extras = [];
        p.deductions.forEach((d) => {
          if (Object.hasOwn(defaults, d.name)) defaults[d.name] = String(d.amount);
          else extras.push({ id: crypto.randomUUID(), name: d.name, amount: d.amount });
        });
        setDefaultAmounts(defaults);
        setCustom(extras);
      })
      .catch((e) => setLoadError(e.message));
  }, []);

  // ---- the numbers on the right are calculated by the Java backend (nothing is calculated in the browser) ----
  const deductions = [
    ...DEFAULT_DEDUCTIONS.filter((n) => num(defaultAmounts[n]) > 0).map((n) => ({ name: n, amount: num(defaultAmounts[n]) })),
    ...custom.map((d) => ({ name: d.name, amount: d.amount })),
  ];
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
    deductions,
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

  function addCustom(e) {
    e.preventDefault();
    setCustomError(null);
    if (!cName.trim()) return setCustomError("Enter a name for this deduction.");
    if (!(num(cAmount) > 0)) return setCustomError("Enter an amount greater than 0.");
    setCustom([...custom, { id: crypto.randomUUID(), name: cName.trim(), amount: num(cAmount) }]);
    setCName("");
    setCAmount("");
  }

  async function save() {
    setMessage(null);
    setSaveError(null);
    const errs = validatePaycheck({ payType, hourlyRate, regularHours, overtimeHours, overtimeMultiplier, salaryAmount, k401, matchRate, matchLimit, deductions: defaultAmounts });
    setFieldErrors(errs);
    if (Object.keys(errs).length) {
      setSaveError("Your paycheck was not saved. Please fix the highlighted fields.");
      return;
    }
    try {
      await api.put("/paycheck", payload);
      setMessage("Saved. Your dashboard and recommendations now use these numbers.");
    } catch (e) {
      if (e.fieldErrors) setFieldErrors(e.fieldErrors);
      setSaveError(e.fieldErrors ? "Your paycheck was not saved. Please fix the highlighted fields." : e.message);
    }
  }

  return (
    <>
      <PageHead title="Paycheck" sub="From gross pay to take-home pay." />
      {loadError && <ErrorBox message={loadError} />}

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
                <Field label="Hourly rate" error={fieldErrors.hourlyRate}>
                  <input type="number" value={hourlyRate} onChange={bind(setHourlyRate, "hourlyRate")} placeholder="0.00" />
                </Field>
                <Field label="Regular hours (per pay period)" error={fieldErrors.regularHours}>
                  <input type="number" value={regularHours} onChange={bind(setRegularHours, "regularHours")} placeholder="0" />
                </Field>
                <Field label="Overtime hours" hint="Optional" error={fieldErrors.overtimeHours}>
                  <input type="number" value={overtimeHours} onChange={bind(setOvertimeHours, "overtimeHours")} placeholder="0" />
                </Field>
                <Field label="Overtime multiplier" error={fieldErrors.overtimeMultiplier}>
                  <input type="number" step="0.1" value={overtimeMultiplier} onChange={bind(setOvertimeMultiplier, "overtimeMultiplier")} />
                </Field>
              </div>
            ) : (
              <Field label="Salary for this pay period" error={fieldErrors.salaryAmount}>
                <input type="number" value={salaryAmount} onChange={bind(setSalaryAmount, "salaryAmount")} placeholder="0.00" />
              </Field>
            )}

            <div style={{ marginTop: 16 }}>
              <Field label="Pay frequency" error={fieldErrors.frequency}>
                <select value={frequency} onChange={bind(setFrequency, "frequency")}>
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
            <p className="muted small">Fill in what comes out of each paycheck (see your pay stub). Leave a row blank if it does not apply to you.</p>
            <div className="grid grid-2">
              {DEFAULT_DEDUCTIONS.map((n) => (
                <Field key={n} label={n}>
                  <input
                    type="number"
                    min="0"
                    step="0.01"
                    value={defaultAmounts[n]}
                    onChange={(e) => {
                      setDefaultAmounts({ ...defaultAmounts, [n]: e.target.value });
                      clear("deductions");
                    }}
                    placeholder="Optional"
                  />
                </Field>
              ))}
            </div>
            {fieldErrors.deductions && <p className="field-error small">{fieldErrors.deductions}</p>}

            <h3 style={{ marginTop: 18, fontSize: 15 }}>Other deductions</h3>
            {custom.length === 0 && <p className="muted small">Nothing else yet. Add things like union dues or a parking fee below.</p>}
            {custom.map((d) => (
              <div key={d.id} className="list-row">
                <span>{d.name}</span>
                <span className="row">
                  {money(d.amount)}
                  <button type="button" className="btn danger small" onClick={() => setCustom(custom.filter((x) => x.id !== d.id))}>
                    Delete
                  </button>
                </span>
              </div>
            ))}
            <form onSubmit={addCustom} className="row" style={{ alignItems: "flex-start", marginTop: 8 }} noValidate>
              <div style={{ flex: 2 }}>
                <Field label="Name">
                  <input value={cName} onChange={(e) => setCName(e.target.value)} placeholder="Union dues" />
                </Field>
              </div>
              <div style={{ flex: 1 }}>
                <Field label="Amount">
                  <input type="number" value={cAmount} onChange={(e) => setCAmount(e.target.value)} placeholder="0.00" />
                </Field>
              </div>
              <button className="btn secondary" type="submit" style={{ marginTop: 22 }}>
                Add
              </button>
            </form>
            {customError && <p className="field-error small">{customError}</p>}
          </Card>

          <Card title="Retirement (401(k))">
            <div className="grid grid-3">
              <Field label="Your contribution (%)" error={fieldErrors.contribution401kPercent}>
                <input type="number" step="0.1" value={k401} onChange={bind(setK401, "contribution401kPercent")} placeholder="0" />
              </Field>
              <Field label="Employer match rate (%)" error={fieldErrors.employerMatchRate}>
                <input type="number" step="0.1" value={matchRate} onChange={bind(setMatchRate, "employerMatchRate")} placeholder="e.g. 50" />
              </Field>
              <Field label="Match limit (% of pay)" error={fieldErrors.employerMatchLimit}>
                <input type="number" step="0.1" value={matchLimit} onChange={bind(setMatchLimit, "employerMatchLimit")} placeholder="e.g. 6" />
              </Field>
            </div>
            <p className="disclaimer">
              Example: a 50% match up to 6% means your employer adds 50 cents per dollar you contribute, on the first 6% of pay only. No match? Leave both match boxes
              blank.
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

          <div style={{ marginTop: 16 }}>
            {saveError && <Notice tone="bad">{saveError}</Notice>}
            {message && <Notice>{message}</Notice>}
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
