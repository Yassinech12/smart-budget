import { Bar, BarChart, CartesianGrid, Cell, Legend, Pie, PieChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from "recharts";
import { api } from "../api/client";
import CategoryBadge from "../components/CategoryBadge";
import { useAsync } from "../utils/useAsync";
import { CATEGORY_COLORS, CATEGORY_LABELS, formatEuro, formatShortMonth } from "../utils/format";

export default function DashboardPage({ month }: { month: string }) {
  const { data, error, loading } = useAsync(() => api.dashboard(month), [month]);

  if (loading) return <p className="muted">Chargement…</p>;
  if (error || !data) return <p className="error">{error}</p>;

  const expensesByCategory = data.categories.filter((c) => c.spent > 0);
  const withBudget = data.categories.filter((c) => c.budget !== null);
  const alerts = data.categories.filter((c) => c.alert !== "OK");

  return (
    <div className="stack">
      <h1>Tableau de bord</h1>

      <section className="kpis">
        <Kpi label="Revenus" value={formatEuro(data.income)} tone="positive" />
        <Kpi label="Dépenses" value={formatEuro(data.expenses)} tone="negative" />
        <Kpi label="Solde du mois" value={formatEuro(data.balance)} tone={data.balance >= 0 ? "positive" : "negative"} />
        <Kpi label="Transactions" value={String(data.transactionCount)} />
      </section>

      {alerts.length > 0 && (
        <section className="card alerts">
          <h2>⚠️ Alertes budget</h2>
          <ul>
            {alerts.map((c) => (
              <li key={c.category}>
                <strong>{CATEGORY_LABELS[c.category]}</strong> :{" "}
                {c.alert === "EXCEEDED"
                  ? `budget dépassé, ${formatEuro(c.spent)} dépensés pour une limite de ${formatEuro(c.budget ?? 0)}`
                  : `${c.percentUsed} % du budget déjà utilisé`}
              </li>
            ))}
          </ul>
        </section>
      )}

      {data.transactionCount === 0 ? (
        <section className="card empty">
          Aucune transaction ce mois-ci. Importez un relevé CSV depuis la page <strong>Transactions</strong>.
        </section>
      ) : (
        <div className="grid-2">
          <section className="card">
            <h2>Dépenses par catégorie</h2>
            <ResponsiveContainer width="100%" height={280}>
              <PieChart>
                <Pie data={expensesByCategory} dataKey="spent" nameKey="category" innerRadius={60} outerRadius={100}
                     paddingAngle={2} stroke="var(--surface)" strokeWidth={2}>
                  {expensesByCategory.map((c) => <Cell key={c.category} fill={CATEGORY_COLORS[c.category]} />)}
                </Pie>
                <Tooltip formatter={(v: number, name: string) => [formatEuro(v), CATEGORY_LABELS[name as keyof typeof CATEGORY_LABELS]]} />
                <Legend formatter={(name: string) => CATEGORY_LABELS[name as keyof typeof CATEGORY_LABELS]} />
              </PieChart>
            </ResponsiveContainer>
          </section>

          <section className="card">
            <h2>Revenus et dépenses sur 6 mois</h2>
            <ResponsiveContainer width="100%" height={280}>
              <BarChart data={data.trend} barGap={2}>
                <CartesianGrid vertical={false} stroke="var(--grid)" />
                <XAxis dataKey="month" tickFormatter={formatShortMonth} tickLine={false} axisLine={false} />
                <YAxis tickFormatter={(v: number) => `${Math.round(v / 100) / 10}k`} tickLine={false} axisLine={false} width={40} />
                <Tooltip formatter={(v: number) => formatEuro(v)} labelFormatter={formatShortMonth} />
                <Legend />
                <Bar dataKey="income" name="Revenus" fill="#1baf7a" radius={[4, 4, 0, 0]} />
                <Bar dataKey="expenses" name="Dépenses" fill="#eb6834" radius={[4, 4, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </section>
        </div>
      )}

      {withBudget.length > 0 && (
        <section className="card">
          <h2>Suivi des budgets</h2>
          <div className="budget-list">
            {withBudget.map((c) => (
              <div key={c.category} className="budget-row">
                <CategoryBadge category={c.category} />
                <div className="progress" aria-label={`${c.percentUsed} % du budget utilisé`}>
                  <div className={`progress-bar ${c.alert.toLowerCase()}`} style={{ width: `${Math.min(c.percentUsed ?? 0, 100)}%` }} />
                </div>
                <span className="budget-figures">
                  {formatEuro(c.spent)} / {formatEuro(c.budget ?? 0)} <span className="muted">({c.percentUsed} %)</span>
                </span>
              </div>
            ))}
          </div>
        </section>
      )}
    </div>
  );
}

function Kpi({ label, value, tone }: { label: string; value: string; tone?: "positive" | "negative" }) {
  return (
    <div className="card kpi">
      <span className="muted">{label}</span>
      <strong className={tone}>{value}</strong>
    </div>
  );
}
