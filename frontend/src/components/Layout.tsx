import type { ReactNode } from "react";
import { NavLink } from "react-router-dom";
import { useAuth } from "../auth";
import { formatMonth, shiftMonth } from "../utils/format";

interface Props {
  month: string;
  onMonthChange: (month: string) => void;
  children: ReactNode;
}

export default function Layout({ month, onMonthChange, children }: Props) {
  const { user, logout } = useAuth();
  return (
    <div className="layout">
      <aside className="sidebar">
        <div className="brand">
          <img src="/favicon.svg" alt="" width={28} height={28} />
          <span>Smart Budget</span>
        </div>
        <nav>
          <NavLink to="/" end>Tableau de bord</NavLink>
          <NavLink to="/transactions">Transactions</NavLink>
          <NavLink to="/budgets">Budgets</NavLink>
        </nav>
        <div className="sidebar-footer">
          <div className="user-name">{user?.fullName}</div>
          <button className="link-button" onClick={logout}>Se déconnecter</button>
        </div>
      </aside>
      <main className="content">
        <header className="topbar">
          <div className="month-picker">
            <button aria-label="Mois précédent" onClick={() => onMonthChange(shiftMonth(month, -1))}>‹</button>
            <strong>{formatMonth(month)}</strong>
            <button aria-label="Mois suivant" onClick={() => onMonthChange(shiftMonth(month, 1))}>›</button>
          </div>
        </header>
        {children}
      </main>
    </div>
  );
}
