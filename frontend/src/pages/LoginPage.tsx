import { useState, type FormEvent } from "react";
import { useAuth } from "../auth";

export default function LoginPage() {
  const { login, register } = useAuth();
  const [mode, setMode] = useState<"login" | "register">("login");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [fullName, setFullName] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const submit = async (event: FormEvent) => {
    event.preventDefault();
    setBusy(true);
    setError(null);
    try {
      if (mode === "login") await login(email, password);
      else await register(email, password, fullName);
    } catch (e) {
      setError(e instanceof Error ? e.message : "Erreur inattendue");
    } finally {
      setBusy(false);
    }
  };

  const fillDemo = () => {
    setMode("login");
    setEmail("demo@smartbudget.dev");
    setPassword("demo1234");
  };

  return (
    <div className="auth-page">
      <form className="card auth-card" onSubmit={submit}>
        <div className="brand">
          <img src="/favicon.svg" alt="" width={32} height={32} />
          <span>Smart Budget</span>
        </div>
        <p className="muted">Importez vos relevés bancaires, laissez l'IA les catégoriser et suivez vos budgets.</p>

        {mode === "register" && (
          <label>
            Nom complet
            <input value={fullName} onChange={(e) => setFullName(e.target.value)} required />
          </label>
        )}
        <label>
          Email
          <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required />
        </label>
        <label>
          Mot de passe
          <input type="password" value={password} onChange={(e) => setPassword(e.target.value)}
                 minLength={mode === "register" ? 8 : undefined} required />
        </label>
        {error && <div className="error">{error}</div>}
        <button className="primary" disabled={busy}>
          {busy ? "…" : mode === "login" ? "Se connecter" : "Créer mon compte"}
        </button>
        <button type="button" className="link-button"
                onClick={() => setMode(mode === "login" ? "register" : "login")}>
          {mode === "login" ? "Pas encore de compte ? Créer un compte" : "Déjà un compte ? Se connecter"}
        </button>
        <button type="button" className="secondary" onClick={fillDemo}>Utiliser le compte démo</button>
      </form>
    </div>
  );
}
