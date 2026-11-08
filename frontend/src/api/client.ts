import type {
  AuthResponse,
  Budget,
  Category,
  Dashboard,
  ImportResult,
  Transaction,
  TransactionInput,
  User,
} from "./types";

const BASE_URL = import.meta.env.VITE_API_URL ?? "";
const TOKEN_KEY = "smart-budget-token";

export class ApiError extends Error {
  constructor(public status: number, message: string) {
    super(message);
  }
}

export const tokenStore = {
  get: () => {
    try {
      return localStorage.getItem(TOKEN_KEY);
    } catch {
      return null;
    }
  },
  set: (token: string) => {
    try {
      localStorage.setItem(TOKEN_KEY, token);
    } catch {
      /* private mode: token kept for this page only */
    }
  },
  clear: () => {
    try {
      localStorage.removeItem(TOKEN_KEY);
    } catch {
      /* ignore */
    }
  },
};

async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  const headers = new Headers(options.headers);
  const token = tokenStore.get();
  if (token) headers.set("Authorization", `Bearer ${token}`);
  if (options.body && !(options.body instanceof FormData)) headers.set("Content-Type", "application/json");

  const response = await fetch(`${BASE_URL}${path}`, { ...options, headers });
  if (response.status === 401 && token) {
    tokenStore.clear();
    window.dispatchEvent(new Event("auth:logout"));
  }
  if (!response.ok) {
    let message = `Erreur ${response.status}`;
    try {
      const problem = await response.json();
      message = problem.detail ?? message;
    } catch {
      /* no JSON body */
    }
    throw new ApiError(response.status, message);
  }
  return response.status === 204 ? (undefined as T) : response.json();
}

export const api = {
  register: (email: string, password: string, fullName: string) =>
    request<AuthResponse>("/api/auth/register", {
      method: "POST",
      body: JSON.stringify({ email, password, fullName }),
    }),
  login: (email: string, password: string) =>
    request<AuthResponse>("/api/auth/login", { method: "POST", body: JSON.stringify({ email, password }) }),
  me: () => request<User>("/api/auth/me"),

  dashboard: (month: string) => request<Dashboard>(`/api/dashboard?month=${month}`),

  transactions: (month: string, category?: Category) =>
    request<Transaction[]>(`/api/transactions?month=${month}${category ? `&category=${category}` : ""}`),
  createTransaction: (input: TransactionInput) =>
    request<Transaction>("/api/transactions", { method: "POST", body: JSON.stringify(input) }),
  updateTransaction: (id: number, input: TransactionInput) =>
    request<Transaction>(`/api/transactions/${id}`, { method: "PUT", body: JSON.stringify(input) }),
  deleteTransaction: (id: number) => request<void>(`/api/transactions/${id}`, { method: "DELETE" }),
  importCsv: (file: File) => {
    const form = new FormData();
    form.append("file", file);
    return request<ImportResult>("/api/transactions/import", { method: "POST", body: form });
  },

  budgets: () => request<Budget[]>("/api/budgets"),
  saveBudget: (category: Category, monthlyLimit: number) =>
    request<Budget>("/api/budgets", { method: "PUT", body: JSON.stringify({ category, monthlyLimit }) }),
  deleteBudget: (category: Category) => request<void>(`/api/budgets/${category}`, { method: "DELETE" }),
};
