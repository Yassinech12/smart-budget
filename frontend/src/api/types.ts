export const CATEGORIES = [
  "ALIMENTATION",
  "LOGEMENT",
  "TRANSPORT",
  "SANTE",
  "LOISIRS",
  "SHOPPING",
  "ABONNEMENTS",
  "RESTAURANTS",
  "REVENUS",
  "AUTRE",
] as const;

export type Category = (typeof CATEGORIES)[number];
export type CategorizationSource = "RULE" | "LLM" | "MANUAL" | "DEFAULT";

export interface User {
  id: number;
  email: string;
  fullName: string;
}

export interface AuthResponse {
  token: string;
  user: User;
}

export interface Transaction {
  id: number;
  date: string;
  label: string;
  amount: number;
  category: Category;
  categorizedBy: CategorizationSource;
}

export interface TransactionInput {
  date: string;
  label: string;
  amount: number;
  category: Category | null;
}

export interface ImportResult {
  imported: number;
  byRules: number;
  byLlm: number;
  uncategorized: number;
  errors: string[];
}

export interface Budget {
  id: number;
  category: Category;
  monthlyLimit: number;
}

export type AlertLevel = "OK" | "WARNING" | "EXCEEDED";

export interface CategorySpending {
  category: Category;
  spent: number;
  budget: number | null;
  percentUsed: number | null;
  alert: AlertLevel;
}

export interface Dashboard {
  month: string;
  income: number;
  expenses: number;
  balance: number;
  transactionCount: number;
  categories: CategorySpending[];
  trend: { month: string; income: number; expenses: number }[];
  alerts: string[];
}
