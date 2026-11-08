import { describe, expect, it } from "vitest";
import { currentMonth, formatDate, formatEuro, shiftMonth } from "./format";

describe("format utils", () => {
  it("formats amounts in euros, French style", () => {
    expect(formatEuro(1234.5)).toBe("1 234,50 €");
    expect(formatEuro(-42)).toBe("-42,00 €");
  });

  it("formats ISO dates as dd/mm/yyyy", () => {
    expect(formatDate("2026-10-31")).toBe("31/10/2026");
  });

  it("moves between months across years", () => {
    expect(shiftMonth("2026-01", -1)).toBe("2025-12");
    expect(shiftMonth("2026-12", 1)).toBe("2027-01");
    expect(currentMonth(new Date(2026, 10, 8))).toBe("2026-11");
  });
});
