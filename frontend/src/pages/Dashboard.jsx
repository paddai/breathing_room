import { Link } from "react-router-dom";
import { api, useLoad } from "../api";
import { money } from "../format";
import { Badge, Bar, Card, ErrorBox, Loading, Notice, PageHead, Stat } from "../components/ui";

const loadDashboard = async () => {
  // the health check is a bonus: if it fails, the rest of the dashboard still shows
  const [summary, recommendation, health] = await Promise.all([api.get("/summary"), api.get("/recommendation"), api.get("/insights").catch(() => null)]);
  return { summary, recommendation, health };
};

export default function Dashboard() {
  const { data, error, loading } = useLoad(loadDashboard);

  if (loading) return <Loading />;
  if (error) return <ErrorBox message={error} />;

  const { summary: s, recommendation: r, health } = data;
  const attention = health ? health.insights.filter((i) => i.severity === "ALERT" || i.severity === "WATCH").slice(0, 3) : [];
  const categories = Object.entries(s.spendingByCategory).sort((a, b) => b[1] - a[1]);
  const goalPercent = s.goalsTarget > 0 ? (s.goalsSaved / s.goalsTarget) * 100 : 0;

  return (
    <>
      <PageHead title="Dashboard" sub="Where your money stands this month." />

      {!s.hasPaycheck && (
        <Notice tone="warn">
          Welcome! Start by entering your <Link to="/paycheck">paycheck</Link>, or load sample data from{" "}
          <Link to="/settings">Settings</Link> to explore the app.
        </Notice>
      )}

      <div className="grid grid-4">
        <Card>
          <Stat
            label="Free money each month"
            value={money(s.monthlySurplus)}
            tone={s.monthlySurplus < 0 ? "bad" : "good"}
            hint="After expenses and debt minimums"
          />
        </Card>
        <Card>
          <Stat label="Monthly take-home" value={money(s.monthlyTakeHome)} hint={`${money(s.takeHomePerPeriod)} per paycheck`} />
        </Card>
        <Card>
          <Stat label="Spent this month" value={money(s.monthlyExpenses)} />
        </Card>
        <Card>
          <Stat label="Total debt" value={money(s.totalDebt)} hint={`${money(s.monthlyDebtMinimums)} minimums / month`} />
        </Card>
      </div>

      <div className="split">
        <Card title="Spending by category">
          {categories.length === 0 && <p className="muted">No expenses logged this month yet.</p>}
          {categories.map(([name, amount]) => (
            <div key={name} style={{ marginBottom: 12 }}>
              <div className="row between small">
                <span>{name}</span>
                <span className="muted">{money(amount)}</span>
              </div>
              <Bar percent={s.monthlyExpenses > 0 ? (amount / s.monthlyExpenses) * 100 : 0} />
            </div>
          ))}
        </Card>

        <div>
          <Card title="Savings goals">
            {s.goalCount === 0 ? (
              <p className="muted">
                No goals yet. <Link to="/goals">Add one</Link>.
              </p>
            ) : (
              <>
                <div className="row between small">
                  <span>{money(s.goalsSaved)} saved</span>
                  <span className="muted">of {money(s.goalsTarget)}</span>
                </div>
                <Bar percent={goalPercent} />
                <p className="small" style={{ marginTop: 10 }}>
                  <Link to="/advisor">See Advisor suggestions</Link>
                </p>
              </>
            )}
          </Card>

          <Card title="Recent expenses">
            {s.recentExpenses.length === 0 && <p className="muted">Nothing logged yet.</p>}
            {s.recentExpenses.map((e) => (
              <div key={e.id} className="list-row small">
                <div>
                  <div>{e.description}</div>
                  <div className="muted">
                    {e.category} · {e.expenseDate}
                  </div>
                </div>
                <strong>{money(e.amount)}</strong>
              </div>
            ))}
          </Card>
        </div>
      </div>

      {attention.length > 0 && (
        <Card className="accent-alert" title="Financial health check">
          {attention.map((i) => (
            <div key={i.id} className="list-row">
              <span>{i.title}</span>
              <Badge tone={i.severity === "ALERT" ? "bad" : "warn"}>{i.severity === "ALERT" ? "Needs attention" : "Worth a look"}</Badge>
            </div>
          ))}
          <p className="small" style={{ marginBottom: 0 }}>
            <Link to="/advisor">See the full health check, with sources</Link>
          </p>
        </Card>
      )}

      {r.facts.flagged && (
        <Card className="accent-alert" title="Worth a look: 401(k) vs. high-interest debt">
          <p>{r.explanation}</p>
          <Link to="/recommendation">See your options</Link>
        </Card>
      )}
    </>
  );
}
