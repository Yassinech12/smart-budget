package com.yassinecharit.budget.dashboard;

import com.yassinecharit.budget.budget.Budget;
import com.yassinecharit.budget.budget.BudgetRepository;
import com.yassinecharit.budget.categorization.Category;
import com.yassinecharit.budget.dashboard.DashboardDtos.AlertLevel;
import com.yassinecharit.budget.dashboard.DashboardDtos.CategorySpending;
import com.yassinecharit.budget.dashboard.DashboardDtos.DashboardResponse;
import com.yassinecharit.budget.dashboard.DashboardDtos.MonthlyTrend;
import com.yassinecharit.budget.security.CurrentUser;
import com.yassinecharit.budget.transaction.Transaction;
import com.yassinecharit.budget.transaction.TransactionRepository;
import com.yassinecharit.budget.user.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    static final int WARNING_PERCENT = 80;
    private static final int TREND_MONTHS = 6;

    private final TransactionRepository transactions;
    private final BudgetRepository budgets;
    private final CurrentUser currentUser;

    public DashboardService(TransactionRepository transactions, BudgetRepository budgets, CurrentUser currentUser) {
        this.transactions = transactions;
        this.budgets = budgets;
        this.currentUser = currentUser;
    }

    @Transactional(readOnly = true)
    public DashboardResponse build(YearMonth month) {
        User user = currentUser.get();
        YearMonth firstTrendMonth = month.minusMonths(TREND_MONTHS - 1L);
        List<Transaction> all = transactions.findByUserIdAndDateBetweenOrderByDateDescIdDesc(
                user.getId(), firstTrendMonth.atDay(1), month.atEndOfMonth());
        List<Transaction> current = all.stream().filter(t -> YearMonth.from(t.getDate()).equals(month)).toList();

        BigDecimal income = sum(current.stream().filter(t -> t.getAmount().signum() > 0).toList());
        BigDecimal expenses = sum(current.stream().filter(t -> t.getAmount().signum() < 0).toList()).negate();

        Map<Category, BigDecimal> spentByCategory = new EnumMap<>(Category.class);
        for (Transaction t : current) {
            if (t.getAmount().signum() < 0) {
                spentByCategory.merge(t.getCategory(), t.getAmount().negate(), BigDecimal::add);
            }
        }
        Map<Category, BigDecimal> limits = budgets.findByUserIdOrderByCategory(user.getId()).stream()
                .collect(Collectors.toMap(Budget::getCategory, Budget::getMonthlyLimit));

        List<CategorySpending> categories = new ArrayList<>();
        List<String> alerts = new ArrayList<>();
        for (Category category : Category.values()) {
            BigDecimal spent = spentByCategory.getOrDefault(category, BigDecimal.ZERO);
            BigDecimal limit = limits.get(category);
            if (spent.signum() == 0 && limit == null) {
                continue;
            }
            CategorySpending spending = toSpending(category, spent, limit);
            categories.add(spending);
            if (spending.alert() == AlertLevel.EXCEEDED) {
                alerts.add("Budget " + category + " exceeded: " + spent + " € spent for a limit of " + limit + " €");
            } else if (spending.alert() == AlertLevel.WARNING) {
                alerts.add("Budget " + category + " almost reached: " + spending.percentUsed() + " % used");
            }
        }
        categories.sort(Comparator.comparing(CategorySpending::spent).reversed());

        return new DashboardResponse(month.toString(), income, expenses, income.subtract(expenses),
                current.size(), categories, trend(all, firstTrendMonth, month), alerts);
    }

    static CategorySpending toSpending(Category category, BigDecimal spent, BigDecimal limit) {
        if (limit == null || limit.signum() == 0) {
            return new CategorySpending(category, spent, limit, null, AlertLevel.OK);
        }
        int percent = spent.multiply(BigDecimal.valueOf(100)).divide(limit, 0, RoundingMode.HALF_UP).intValue();
        AlertLevel level = percent > 100 ? AlertLevel.EXCEEDED
                : percent >= WARNING_PERCENT ? AlertLevel.WARNING : AlertLevel.OK;
        return new CategorySpending(category, spent, limit, percent, level);
    }

    private List<MonthlyTrend> trend(List<Transaction> all, YearMonth from, YearMonth to) {
        Map<YearMonth, List<Transaction>> byMonth = all.stream()
                .collect(Collectors.groupingBy(t -> YearMonth.from(t.getDate())));
        List<MonthlyTrend> trend = new ArrayList<>();
        for (YearMonth m = from; !m.isAfter(to); m = m.plusMonths(1)) {
            List<Transaction> list = byMonth.getOrDefault(m, List.of());
            trend.add(new MonthlyTrend(m.toString(),
                    sum(list.stream().filter(t -> t.getAmount().signum() > 0).toList()),
                    sum(list.stream().filter(t -> t.getAmount().signum() < 0).toList()).negate()));
        }
        return trend;
    }

    private static BigDecimal sum(List<Transaction> list) {
        return list.stream().map(Transaction::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
