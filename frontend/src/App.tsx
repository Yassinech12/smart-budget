import { useState } from "react";
import { Navigate, Route, Routes } from "react-router-dom";
import { useAuth } from "./auth";
import Layout from "./components/Layout";
import BudgetsPage from "./pages/BudgetsPage";
import DashboardPage from "./pages/DashboardPage";
import LoginPage from "./pages/LoginPage";
import TransactionsPage from "./pages/TransactionsPage";
import { currentMonth } from "./utils/format";

export default function App() {
  const { user, loading } = useAuth();
  const [month, setMonth] = useState(currentMonth());

  if (loading) return <div className="center muted">Chargement…</div>;
  if (!user) return <LoginPage />;

  return (
    <Layout month={month} onMonthChange={setMonth}>
      <Routes>
        <Route path="/" element={<DashboardPage month={month} />} />
        <Route path="/transactions" element={<TransactionsPage month={month} />} />
        <Route path="/budgets" element={<BudgetsPage month={month} />} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </Layout>
  );
}
