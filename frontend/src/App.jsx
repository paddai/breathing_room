import { useEffect, useState } from "react";
import { BrowserRouter, NavLink, Route, Routes } from "react-router-dom";
import Dashboard from "./pages/Dashboard";
import PaycheckForm from "./pages/PaycheckForm";
import ExpenseForm from "./pages/ExpenseForm";
import DebtForm from "./pages/DebtForm";
import DebtComparison from "./pages/DebtComparison";
import RecommendationScreen from "./pages/RecommendationScreen";
import GoalsPage from "./pages/GoalsPage";
import AdvisorPage from "./pages/AdvisorPage";
import SettingsPage from "./pages/SettingsPage";

const NAV = [
  ["/", "Dashboard"],
  ["/paycheck", "Paycheck"],
  ["/expenses", "Budget & Expenses"],
  ["/debts", "Debts"],
  ["/debt-comparison", "Debt Payoff"],
  ["/recommendation", "401(k) vs Debt"],
  ["/goals", "Goals"],
  ["/advisor", "Advisor"],
  ["/settings", "Settings"],
];

export default function App() {
  const [apiStatus, setApiStatus] = useState("checking");

  useEffect(() => {
    fetch("/api/health")
      .then((r) => setApiStatus(r.ok ? "online" : "offline"))
      .catch(() => setApiStatus("offline"));
  }, []);

  return (
    <BrowserRouter>
      <div className="shell">
        <aside className="sidebar">
          <div className="brand">
            <strong>Breathing Room</strong>
            <span>From paycheck pressure to financial clarity</span>
          </div>
          <nav>
            {NAV.map(([to, label]) => (
              <NavLink
                key={to}
                to={to}
                end={to === "/"}
                className={({ isActive }) => "nav-link" + (isActive ? " active" : "")}
              >
                {label}
              </NavLink>
            ))}
          </nav>
          <div className="sidebar-foot">
            <span className={`dot ${apiStatus}`} />
            API {apiStatus}
          </div>
        </aside>

        <main className="content">
          <Routes>
            <Route path="/" element={<Dashboard />} />
            <Route path="/paycheck" element={<PaycheckForm />} />
            <Route path="/expenses" element={<ExpenseForm />} />
            <Route path="/debts" element={<DebtForm />} />
            <Route path="/debt-comparison" element={<DebtComparison />} />
            <Route path="/recommendation" element={<RecommendationScreen />} />
            <Route path="/goals" element={<GoalsPage />} />
            <Route path="/advisor" element={<AdvisorPage />} />
            <Route path="/settings" element={<SettingsPage />} />
          </Routes>
        </main>
      </div>
    </BrowserRouter>
  );
}
