import { useRef, useState, type FormEvent } from "react";
import { api } from "../api/client";
import { CATEGORIES, type Category, type ImportResult, type Transaction } from "../api/types";
import { useAsync } from "../utils/useAsync";
import { CATEGORY_LABELS, formatDate, formatEuro, SOURCE_LABELS } from "../utils/format";

export default function TransactionsPage({ month }: { month: string }) {
  const [filter, setFilter] = useState<Category | "">("");
  const { data, error, loading, reload } = useAsync(() => api.transactions(month, filter || undefined), [month, filter]);
  const [importResult, setImportResult] = useState<ImportResult | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);
  const fileInput = useRef<HTMLInputElement>(null);

  const run = async (action: () => Promise<unknown>) => {
    setActionError(null);
    try {
      await action();
      reload();
    } catch (e) {
      setActionError(e instanceof Error ? e.message : "Erreur inattendue");
    }
  };

  const onImport = async (file: File | undefined) => {
    if (!file) return;
    await run(async () => setImportResult(await api.importCsv(file)));
    if (fileInput.current) fileInput.current.value = "";
  };

  const changeCategory = (t: Transaction, category: Category) =>
    run(() => api.updateTransaction(t.id, { date: t.date, label: t.label, amount: t.amount, category }));

  return (
    <div className="stack">
      <div className="page-header">
        <h1>Transactions</h1>
        <div className="actions">
          <input ref={fileInput} type="file" accept=".csv,text/csv" hidden onChange={(e) => onImport(e.target.files?.[0])} />
          <button className="primary" onClick={() => fileInput.current?.click()}>Importer un relevé CSV</button>
        </div>
      </div>

      {importResult && (
        <div className="card info">
          <strong>{importResult.imported} transactions importées</strong> : {importResult.byRules} catégorisées par
          règles, {importResult.byLlm} par l'IA, {importResult.uncategorized} sans catégorie.
          {importResult.errors.length > 0 && (
            <details>
              <summary>{importResult.errors.length} ligne(s) ignorée(s)</summary>
              <ul>{importResult.errors.map((e) => <li key={e}>{e}</li>)}</ul>
            </details>
          )}
        </div>
      )}
      {actionError && <div className="error">{actionError}</div>}

      <AddTransactionForm onAdd={(input) => run(() => api.createTransaction(input))} />

      <section className="card">
        <div className="table-header">
          <h2>Liste</h2>
          <select value={filter} onChange={(e) => setFilter(e.target.value as Category | "")} aria-label="Filtrer par catégorie">
            <option value="">Toutes les catégories</option>
            {CATEGORIES.map((c) => <option key={c} value={c}>{CATEGORY_LABELS[c]}</option>)}
          </select>
        </div>
        {loading ? <p className="muted">Chargement…</p> : error ? <p className="error">{error}</p> : data && data.length === 0 ? (
          <p className="muted">Aucune transaction pour ce mois.</p>
        ) : (
          <div className="table-wrap">
            <table>
              <thead>
                <tr><th>Date</th><th>Libellé</th><th>Catégorie</th><th>Source</th><th className="num">Montant</th><th /></tr>
              </thead>
              <tbody>
                {data?.map((t) => (
                  <tr key={t.id}>
                    <td>{formatDate(t.date)}</td>
                    <td>{t.label}</td>
                    <td>
                      <select className="inline-select" value={t.category}
                              onChange={(e) => changeCategory(t, e.target.value as Category)} aria-label="Catégorie">
                        {CATEGORIES.map((c) => <option key={c} value={c}>{CATEGORY_LABELS[c]}</option>)}
                      </select>
                    </td>
                    <td><span className={`source ${t.categorizedBy.toLowerCase()}`}>{SOURCE_LABELS[t.categorizedBy]}</span></td>
                    <td className={`num ${t.amount < 0 ? "negative" : "positive"}`}>{formatEuro(t.amount)}</td>
                    <td>
                      <button className="icon-button" aria-label="Supprimer"
                              onClick={() => confirm("Supprimer cette transaction ?") && run(() => api.deleteTransaction(t.id))}>
                        ✕
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>
      <p className="muted small">
        Format CSV attendu : <code>date;label;amount</code>, par exemple <code>02/10/2026;CB CARREFOUR;-64,32</code>.
        Un fichier d'exemple est disponible dans <code>sample-data/</code>. {data && <CategoryHint data={data} />}
      </p>
    </div>
  );
}

function CategoryHint({ data }: { data: Transaction[] }) {
  const ai = data.filter((t) => t.categorizedBy === "LLM").length;
  return ai > 0 ? <span>{ai} transaction(s) catégorisée(s) par l'IA ce mois-ci.</span> : null;
}

function AddTransactionForm({ onAdd }: { onAdd: (input: { date: string; label: string; amount: number; category: Category | null }) => Promise<unknown> }) {
  const today = new Date().toISOString().slice(0, 10);
  const [date, setDate] = useState(today);
  const [label, setLabel] = useState("");
  const [amount, setAmount] = useState("");
  const [category, setCategory] = useState<Category | "">("");

  const submit = async (event: FormEvent) => {
    event.preventDefault();
    await onAdd({ date, label, amount: Number(amount.replace(",", ".")), category: category || null });
    setLabel("");
    setAmount("");
    setCategory("");
  };

  return (
    <form className="card add-form" onSubmit={submit}>
      <input type="date" value={date} onChange={(e) => setDate(e.target.value)} required aria-label="Date" />
      <input placeholder="Libellé (ex. CB CARREFOUR)" value={label} onChange={(e) => setLabel(e.target.value)} required />
      <input placeholder="Montant (ex. -42,50)" value={amount} onChange={(e) => setAmount(e.target.value)}
             inputMode="decimal" pattern="-?[0-9]+([.,][0-9]{1,2})?" required />
      <select value={category} onChange={(e) => setCategory(e.target.value as Category | "")} aria-label="Catégorie">
        <option value="">Catégorie automatique</option>
        {CATEGORIES.map((c) => <option key={c} value={c}>{CATEGORY_LABELS[c]}</option>)}
      </select>
      <button className="primary">Ajouter</button>
      <span className="muted small">Laissez « Catégorie automatique » pour que les règles ou l'IA choisissent.</span>
    </form>
  );
}

