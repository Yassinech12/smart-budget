import { useState } from "react";
import { api } from "../api/client";
import { CATEGORIES, type Category } from "../api/types";
import CategoryBadge from "../components/CategoryBadge";
import { useAsync } from "../utils/useAsync";
import { formatEuro } from "../utils/format";

export default function BudgetsPage({ month }: { month: string }) {
  const budgets = useAsync(() => api.budgets(), []);
  const dashboard = useAsync(() => api.dashboard(month), [month]);
  const [drafts, setDrafts] = useState<Partial<Record<Category, string>>>({});
  const [error, setError] = useState<string | null>(null);

  const limitOf = (c: Category) => budgets.data?.find((b) => b.category === c)?.monthlyLimit;
  const spentOf = (c: Category) => dashboard.data?.categories.find((s) => s.category === c)?.spent ?? 0;

  const save = async (category: Category) => {
    const value = Number((drafts[category] ?? "").replace(",", "."));
    if (!value || value <= 0) return;
    setError(null);
    try {
      await api.saveBudget(category, value);
      setDrafts((d) => ({ ...d, [category]: undefined }));
      budgets.reload();
      dashboard.reload();
    } catch (e) {
      setError(e instanceof Error ? e.message : "Erreur inattendue");
    }
  };

  const remove = async (category: Category) => {
    await api.deleteBudget(category);
    budgets.reload();
    dashboard.reload();
  };

  return (
    <div className="stack">
      <h1>Budgets mensuels</h1>
      <p className="muted">Fixez une limite par catégorie : une alerte apparaît à 80 % puis en cas de dépassement.</p>
      {error && <div className="error">{error}</div>}
      <section className="card">
        <table>
          <thead>
            <tr><th>Catégorie</th><th className="num">Dépensé ce mois</th><th className="num">Limite</th><th>Nouvelle limite</th><th /></tr>
          </thead>
          <tbody>
            {CATEGORIES.filter((c) => c !== "REVENUS").map((c) => {
              const limit = limitOf(c);
              return (
                <tr key={c}>
                  <td><CategoryBadge category={c} /></td>
                  <td className="num">{formatEuro(spentOf(c))}</td>
                  <td className="num">{limit !== undefined ? formatEuro(limit) : <span className="muted">—</span>}</td>
                  <td>
                    <form className="inline-form" onSubmit={(e) => { e.preventDefault(); save(c); }}>
                      <input inputMode="decimal" placeholder="ex. 300" value={drafts[c] ?? ""}
                             onChange={(e) => setDrafts((d) => ({ ...d, [c]: e.target.value }))} aria-label={`Limite ${c}`} />
                      <button className="secondary">Enregistrer</button>
                    </form>
                  </td>
                  <td>{limit !== undefined && <button className="icon-button" aria-label="Supprimer le budget" onClick={() => remove(c)}>✕</button>}</td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </section>
    </div>
  );
}
