import { useState } from "react";
import { Link } from "react-router-dom";
import { api, useLoad } from "../api";
import { money, pct } from "../format";
import { Badge, Card, ErrorBox, Loading, Notice, PageHead, Stat } from "../components/ui";

const loadRecommendation = () => api.get("/recommendation");

export default function RecommendationScreen() {
  const { data, error, loading } = useLoad(loadRecommendation);
  const [selected, setSelected] = useState(null);

  if (loading) return <Loading text="Checking your numbers..." />;
  if (error) return <ErrorBox message={error} />;

  const { facts: f, explanation, aiUsed, aiNote } = data;

  return (
    <>
      <PageHead title="401(k) vs. Debt" sub="Is extra retirement saving crowding out your high-interest debt payoff?" />

      {!f.hasData ? (
        <Notice tone="warn">
          {f.message} Go to <Link to="/paycheck">Paycheck</Link> (and <Link to="/debts">Debts</Link>).
        </Notice>
      ) : (
        <>
          <div className="grid grid-4">
            <Card>
              <Stat label="Your 401(k)" value={pct(f.contributionPercent)} />
            </Card>
            <Card>
              <Stat label="Employer match up to" value={pct(f.matchLimitPercent)} />
            </Card>
            <Card>
              <Stat label="Above the match" value={pct(f.contributionAbovePercent)} hint={`${money(f.amountAbovePerPeriod)} per paycheck`} />
            </Card>
            <Card>
              <Stat label="Highest-APR debt" value={f.debtName ? `${f.debtApr}%` : "None"} hint={f.debtName} />
            </Card>
          </div>

          <Card className={f.flagged ? "accent-alert" : "accent-teal"} title={f.flagged ? "What this means" : "Looks balanced"}>
            <p>{explanation}</p>
            <div className="row">
              {aiUsed ? <Badge tone="good">Explained by AI</Badge> : <Badge>Built-in explanation</Badge>}
              <span className="muted small">{aiNote}</span>
            </div>
            <p className="disclaimer">
              Breathing Room calculated every number above. The AI only explains them in plain language - it does not change
              anything. This is educational guidance, not financial, tax, or investment advice.
            </p>
          </Card>

          {f.flagged && (
            <Card title="Your options (you decide)">
              {f.options.map((o) => (
                <div key={o.id} className={`option ${selected === o.id ? "selected" : ""}`} onClick={() => setSelected(o.id)}>
                  <strong>{o.label}</strong>
                  <span className="muted small">{o.detail}</span>
                </div>
              ))}
              {selected && (
                <p className="muted small">
                  Noted for this visit. To act on it, update your 401(k) percentage on the <Link to="/paycheck">Paycheck</Link> page - Breathing
                  Room never changes it for you.
                </p>
              )}
            </Card>
          )}
        </>
      )}
    </>
  );
}
