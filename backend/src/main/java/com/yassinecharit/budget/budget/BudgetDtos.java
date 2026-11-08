package com.yassinecharit.budget.budget;

import com.yassinecharit.budget.categorization.Category;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public final class BudgetDtos {

    private BudgetDtos() {
    }

    public record BudgetRequest(@NotNull Category category,
                                @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal monthlyLimit) {
    }

    public record BudgetResponse(Long id, Category category, BigDecimal monthlyLimit) {
        public static BudgetResponse from(Budget b) {
            return new BudgetResponse(b.getId(), b.getCategory(), b.getMonthlyLimit());
        }
    }
}
