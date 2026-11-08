package com.yassinecharit.budget.budget;

import com.yassinecharit.budget.budget.BudgetDtos.BudgetRequest;
import com.yassinecharit.budget.budget.BudgetDtos.BudgetResponse;
import com.yassinecharit.budget.categorization.Category;
import com.yassinecharit.budget.common.NotFoundException;
import com.yassinecharit.budget.security.CurrentUser;
import com.yassinecharit.budget.user.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class BudgetService {

    private final BudgetRepository budgets;
    private final CurrentUser currentUser;

    public BudgetService(BudgetRepository budgets, CurrentUser currentUser) {
        this.budgets = budgets;
        this.currentUser = currentUser;
    }

    @Transactional(readOnly = true)
    public List<BudgetResponse> list() {
        return budgets.findByUserIdOrderByCategory(currentUser.get().getId()).stream()
                .map(BudgetResponse::from).toList();
    }

    /** Creates the budget of a category, or updates its limit if it already exists. */
    @Transactional
    public BudgetResponse upsert(BudgetRequest request) {
        User user = currentUser.get();
        Budget budget = budgets.findByUserIdAndCategory(user.getId(), request.category())
                .orElseGet(() -> new Budget(user, request.category(), request.monthlyLimit()));
        budget.setMonthlyLimit(request.monthlyLimit());
        return BudgetResponse.from(budgets.save(budget));
    }

    @Transactional
    public void delete(Category category) {
        Budget budget = budgets.findByUserIdAndCategory(currentUser.get().getId(), category)
                .orElseThrow(() -> new NotFoundException("No budget for this category"));
        budgets.delete(budget);
    }
}
