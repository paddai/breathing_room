import { useState } from "react";

// Multiplier = how many times this pay period happens per year.
// This is what lets us turn a single paycheck into an annual figure.
const FREQUENCIES = {
    weekly: { label: "Weekly", periodsPerYear: 52 },
    biweekly: { label: "Biweekly", periodsPerYear: 26 },
    semimonthly: { label: "Semi-monthly", periodsPerYear: 24 },
    monthly: { label: "Monthly", periodsPerYear: 12 },
};

export default function PaycheckForm() {
    // --- 4.1: gross pay (hourly/salary) ---
    const [payType, setPayType] = useState("hourly");
    const [hourlyRate, setHourlyRate] = useState("");
    const [regularHours, setRegularHours] = useState("");
    const [overtimeHours, setOvertimeHours] = useState("");
    const [overtimeMultiplier, setOvertimeMultiplier] = useState("1.5");
    const [salaryAmount, setSalaryAmount] = useState("");

    // --- 4.2: pay frequency ---
    const [frequency, setFrequency] = useState("biweekly");

    // --- 4.3: payroll deductions (a list, same pattern as ExpenseForm) ---
    const [deductionName, setDeductionName] = useState("");
    const [deductionAmount, setDeductionAmount] = useState("");
    const [deductions, setDeductions] = useState([]);

    // --- 4.4 / 4.5: 401(k) contribution and employer match ---
    const [contribution401kPercent, setContribution401kPercent] = useState("");
    const [employerMatchRate, setEmployerMatchRate] = useState("");
    const [employerMatchLimit, setEmployerMatchLimit] = useState("");

    // ---------- derived values (4.1 gross pay) ----------
    const rate = parseFloat(hourlyRate) || 0;
    const regHours = parseFloat(regularHours) || 0;
    const otHours = parseFloat(overtimeHours) || 0;
    const otMultiplier = parseFloat(overtimeMultiplier) || 0;
    const salary = parseFloat(salaryAmount) || 0;

    const regularPay = rate * regHours;
    const overtimePay = rate * otHours * otMultiplier;
    const grossPay = payType === "hourly" ? regularPay + overtimePay : salary;

    // ---------- 4.2: annualized figures ----------
    const periodsPerYear = FREQUENCIES[frequency].periodsPerYear;
    const annualGrossPay = grossPay * periodsPerYear;

    // ---------- 4.3: deductions total ----------
    const totalDeductions = deductions.reduce((sum, d) => sum + d.amount, 0);

    function handleAddDeduction(e) {
        e.preventDefault();
        const amountNum = parseFloat(deductionAmount);
        if (!deductionName || !amountNum || amountNum <= 0) return;
        setDeductions([
            ...deductions,
            { id: crypto.randomUUID(), name: deductionName, amount: amountNum },
        ]);
        setDeductionName("");
        setDeductionAmount("");
    }

    function handleDeleteDeduction(id) {
        setDeductions(deductions.filter((d) => d.id !== id));
    }

    // ---------- 4.4 / 4.5: 401(k) and employer match ----------
    const contribution401kPct = parseFloat(contribution401kPercent) || 0;
    const matchRatePct = parseFloat(employerMatchRate) || 0;
    const matchLimitPct = parseFloat(employerMatchLimit) || 0;

    const contribution401kAmount = grossPay * (contribution401kPct / 100);

    // The employer only matches contributions up to the match limit —
    // e.g. "50% match up to 6%" means anything you put in beyond 6%
    // gets no matching money. Math.min caps the matched percentage there.
    const matchedPercent = Math.min(contribution401kPct, matchLimitPct);
    const employerMatchAmount = grossPay * (matchedPercent / 100) * (matchRatePct / 100);

    // ---------- 4.6: estimated take-home pay ----------
    const estimatedTakeHomePay = grossPay - totalDeductions - contribution401kAmount;

    function handleReset() {
        setHourlyRate("");
        setRegularHours("");
        setOvertimeHours("");
        setOvertimeMultiplier("1.5");
        setSalaryAmount("");
        setDeductions([]);
        setContribution401kPercent("");
        setEmployerMatchRate("");
        setEmployerMatchLimit("");
    }

    return (
        <div style={{ padding: 24, maxWidth: 640 }}>
            <h1>Paycheck</h1>
            <p style={{ color: "#999" }}>
                Estimate gross pay, then account for deductions and retirement
                contributions to see take-home pay.
            </p>

            {/* ---------- 4.1: Hourly / Salary ---------- */}
            <div style={{ display: "flex", gap: 8, margin: "16px 0" }}>
                <button type="button" onClick={() => setPayType("hourly")} style={toggleStyle(payType === "hourly")}>
                    Hourly
                </button>
                <button type="button" onClick={() => setPayType("salary")} style={toggleStyle(payType === "salary")}>
                    Salary
                </button>
            </div>

            {payType === "hourly" ? (
                <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 16 }}>
                    <Field label="Hourly rate">
                        <input type="number" value={hourlyRate} onChange={(e) => setHourlyRate(e.target.value)} placeholder="0.00" />
                    </Field>
                    <Field label="Regular hours">
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
                <Field label="Salary (this pay period)">
                    <input type="number" value={salaryAmount} onChange={(e) => setSalaryAmount(e.target.value)} placeholder="0.00" />
                </Field>
            )}

            {/* ---------- 4.2: Pay frequency ---------- */}
            <div style={{ marginTop: 20 }}>
                <Field label="Pay frequency">
                    <select value={frequency} onChange={(e) => setFrequency(e.target.value)}>
                        {Object.entries(FREQUENCIES).map(([key, f]) => (
                            <option key={key} value={key}>{f.label}</option>
                        ))}
                    </select>
                </Field>
            </div>

            {/* ---------- 4.3: Payroll deductions ---------- */}
            <div style={{ marginTop: 24 }}>
                <h3 style={{ marginBottom: 8 }}>Payroll deductions</h3>
                <p style={{ color: "#999", fontSize: 13, marginTop: 0 }}>
                    Enter each line from your paystub (federal tax, state tax, health
                    insurance, etc.) as its own entry.
                </p>
                <form onSubmit={handleAddDeduction} style={{ display: "grid", gridTemplateColumns: "2fr 1fr auto", gap: 12, alignItems: "end" }}>
                    <Field label="Name">
                        <input type="text" value={deductionName} onChange={(e) => setDeductionName(e.target.value)} placeholder="Federal tax" />
                    </Field>
                    <Field label="Amount">
                        <input type="number" value={deductionAmount} onChange={(e) => setDeductionAmount(e.target.value)} placeholder="0.00" />
                    </Field>
                    <button type="submit">Add</button>
                </form>

                {deductions.map((d) => (
                    <div key={d.id} style={{ display: "flex", justifyContent: "space-between", padding: "8px 0", borderBottom: "1px solid #333" }}>
                        <div>{d.name}</div>
                        <div style={{ display: "flex", gap: 12, alignItems: "center" }}>
                            <div>${d.amount.toFixed(2)}</div>
                            <button type="button" onClick={() => handleDeleteDeduction(d.id)}>Delete</button>
                        </div>
                    </div>
                ))}
            </div>

            {/* ---------- 4.4 / 4.5: 401(k) and employer match ---------- */}
            <div style={{ marginTop: 24 }}>
                <h3 style={{ marginBottom: 8 }}>Retirement (401(k))</h3>
                <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr 1fr", gap: 16 }}>
                    <Field label="Your contribution (%)">
                        <input type="number" step="0.1" value={contribution401kPercent} onChange={(e) => setContribution401kPercent(e.target.value)} placeholder="0" />
                    </Field>
                    <Field label="Employer match rate (%)">
                        <input type="number" step="0.1" value={employerMatchRate} onChange={(e) => setEmployerMatchRate(e.target.value)} placeholder="e.g. 50" />
                    </Field>
                    <Field label="Match limit (% of pay)">
                        <input type="number" step="0.1" value={employerMatchLimit} onChange={(e) => setEmployerMatchLimit(e.target.value)} placeholder="e.g. 6" />
                    </Field>
                </div>
                <p style={{ fontSize: 12, color: "#777", marginTop: 6 }}>
                    Example: a 50% match up to 6% means your employer adds 50 cents per
                    dollar you contribute, on the first 6% of your pay only.
                </p>
            </div>

            {/* ---------- Results ---------- */}
            <div style={{ marginTop: 24, padding: 16, background: "#1e2a24", borderRadius: 8, display: "grid", gridTemplateColumns: "1fr 1fr", gap: 16 }}>
                <Result label="Gross pay (this period)" value={grossPay} />
                <Result label={`Annual gross pay (${FREQUENCIES[frequency].label.toLowerCase()})`} value={annualGrossPay} />
                <Result label="Total deductions" value={totalDeductions} />
                <Result label="401(k) contribution" value={contribution401kAmount} />
                <Result label="Employer match (informational)" value={employerMatchAmount} />
                <Result label="Estimated take-home pay" value={estimatedTakeHomePay} emphasize />
            </div>

            <button type="button" onClick={handleReset} style={{ marginTop: 16 }}>
                Reset
            </button>
        </div>
    );
}

function Field({ label, children }) {
    return (
        <label style={{ display: "block" }}>
            <div style={{ fontSize: 13, color: "#aaa", marginBottom: 4 }}>{label}</div>
            {children}
        </label>
    );
}

function Result({ label, value, emphasize }) {
    return (
        <div>
            <div style={{ fontSize: 12, color: "#aaa" }}>{label}</div>
            <div style={{ fontSize: emphasize ? 22 : 18, fontWeight: 600 }}>
                ${value.toFixed(2)}
            </div>
        </div>
    );
}

function toggleStyle(active) {
    return {
        padding: "8px 16px",
        borderRadius: 6,
        border: "1px solid #444",
        background: active ? "#2d6a4f" : "transparent",
        color: "#fff",
        cursor: "pointer",
    };
}
