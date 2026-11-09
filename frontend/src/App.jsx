import { useEffect, useState } from "react";
import { BrowserRouter, Navigate, NavLink, Route, Routes } from "react-router-dom";
import AuthProvider from "./auth/AuthProvider";
import { useAuth } from "./auth/authContext";
import Avatar from "./components/Avatar";
import Dashboard from "./pages/Dashboard";
import PaycheckForm from "./pages/PaycheckForm";
import ExpenseForm from "./pages/ExpenseForm";
import DebtForm from "./pages/DebtForm";
import DebtComparison from "./pages/DebtComparison";
import RecommendationScreen from "./pages/RecommendationScreen";
import GoalsPage from "./pages/GoalsPage";
import AdvisorPage from "./pages/AdvisorPage";
import SettingsPage from "./pages/SettingsPage";
import ProfilePage from "./pages/ProfilePage";
import LoginPage from "./pages/LoginPage";

const NAV = [
  ["/", "Dashboard"],
  ["/paycheck", "Paycheck"],
  ["/expenses", "Budget & Expenses"],
  ["/debts", "Debts"],
  ["/debt-comparison", "Debt Payoff"],
  ["/recommendation", "401(k) vs Debt"],
  ["/goals", "Goals"],
  ["/advisor", "Advisor"],
  ["/profile", "Profile"],
  ["/settings", "Settings"],
];

function Shell() {
  const { user, profile, avatarUrl, logout } = useAuth();
  const [apiStatus, setApiStatus] = useState("checking");

  useEffect(() => {
    fetch("/api/health")
      .then((r) => setApiStatus(r.ok ? "online" : "offline"))
      .catch(() => setApiStatus("offline"));
  }, []);

  // Not signed in: only the login page exists
  if (!user) {
    return (
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route path="*" element={<Navigate to="/login" replace />} />
      </Routes>
    );
  }

  const shownName = profile?.displayName || user.email;

  return (
    <div className="shell">
      <aside className="sidebar">
        <div className="brand">
          <strong>Breathing Room</strong>
          <span>From paycheck pressure to financial clarity</span>
        </div>

        <NavLink to="/profile" className="user-badge" title="Open your profile">
          <Avatar url={avatarUrl} name={shownName} size={40} />
          <div className="who">
            <strong>{shownName}</strong>
            <span>View profile</span>
          </div>
        </NavLink>
        <button type="button" className="signout-btn" onClick={logout}>
          Sign out
        </button>

        <nav>
          {NAV.map(([to, label]) => (
            <NavLink key={to} to={to} end={to === "/"} className={({ isActive }) => "nav-link" + (isActive ? " active" : "")}>
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
          <Route path="/profile" element={<ProfilePage />} />
          <Route path="/settings" element={<SettingsPage />} />
          <Route path="/login" element={<Navigate to="/" replace />} />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </main>
    </div>
  );
}

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <Shell />
      </AuthProvider>
    </BrowserRouter>
  );
}
