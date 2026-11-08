package com.yassinecharit.budget.dashboard;

import com.yassinecharit.budget.categorization.Category;

import java.math.BigDecimal;
import java.util.List;

public final class DashboardDtos {

    private DashboardDtos() {
    }

    public enum AlertLevel { OK, WARNING, EXCEEDED }

    /** Spending of one category during the month, compared to its budget (null if no budget). */
    public record CategorySpending(Category category, BigDecimal spent, BigDecimal budget,
                                   Integer percentUsed, AlertLevel alert) {
    }

    public record MonthlyTrend(String month, BigDecimal income, BigDecimal expenses) {
    }

    public record DashboardResponse(String month, BigDecimal income, BigDecimal expenses, BigDecimal balance,
                                    int transactionCount, List<CategorySpending> categories,
                                    List<MonthlyTrend> trend, List<String> alerts) {
    }
}
