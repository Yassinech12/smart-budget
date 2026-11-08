import type { Category, CategorizationSource } from "../api/types";

const euro = new Intl.NumberFormat("fr-FR", { style: "currency", currency: "EUR" });

export function formatEuro(value: number): string {
  // Intl uses narrow no-break spaces: normalise them so the text is easy to test and copy
  return euro.format(value).replace(/[  ]/g, " ");
}

export function formatMonth(month: string): string {
  const [year, m] = month.split("-").map(Number);
  const label = new Date(year, m - 1, 1).toLocaleDateString("fr-FR", { month: "long", year: "numeric" });
  return label.charAt(0).toUpperCase() + label.slice(1);
}

export function formatShortMonth(month: string): string {
  const [year, m] = month.split("-").map(Number);
  return new Date(year, m - 1, 1).toLocaleDateString("fr-FR", { month: "short" }).replace(".", "");
}

export function formatDate(iso: string): string {
  const [y, m, d] = iso.split("-");
  return `${d}/${m}/${y}`;
}

export function currentMonth(date = new Date()): string {
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}`;
}

export function shiftMonth(month: string, delta: number): string {
  const [year, m] = month.split("-").map(Number);
  return currentMonth(new Date(year, m - 1 + delta, 1));
}

export const CATEGORY_LABELS: Record<Category, string> = {
  ALIMENTATION: "Alimentation",
  LOGEMENT: "Logement",
  TRANSPORT: "Transport",
  SANTE: "Santé",
  LOISIRS: "Loisirs",
  SHOPPING: "Shopping",
  ABONNEMENTS: "Abonnements",
  RESTAURANTS: "Restaurants",
  REVENUS: "Revenus",
  AUTRE: "Autre",
};

/** Fixed colour per category, so a category keeps its colour on every chart. */
export const CATEGORY_COLORS: Record<Category, string> = {
  ALIMENTATION: "#2a78d6",
  LOGEMENT: "#eb6834",
  TRANSPORT: "#1baf7a",
  SANTE: "#d6497f",
  LOISIRS: "#8a5cd6",
  SHOPPING: "#e0a31b",
  ABONNEMENTS: "#2fa4b8",
  RESTAURANTS: "#b5652b",
  REVENUS: "#4c8f3a",
  AUTRE: "#8a8985",
};

export const SOURCE_LABELS: Record<CategorizationSource, string> = {
  RULE: "Règle",
  LLM: "IA",
  MANUAL: "Manuel",
  DEFAULT: "Par défaut",
};
