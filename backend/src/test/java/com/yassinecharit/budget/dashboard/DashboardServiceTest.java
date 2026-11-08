package com.yassinecharit.budget.dashboard;

import com.yassinecharit.budget.categorization.Category;
import com.yassinecharit.budget.dashboard.DashboardDtos.AlertLevel;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class DashboardServiceTest {

    @Test
    void computesBudgetAlertLevels() {
        assertThat(DashboardService.toSpending(Category.SHOPPING, new BigDecimal("50"), new BigDecimal("100")).alert())
                .isEqualTo(AlertLevel.OK);
        assertThat(DashboardService.toSpending(Category.SHOPPING, new BigDecimal("85"), new BigDecimal("100")).alert())
                .isEqualTo(AlertLevel.WARNING);
        var exceeded = DashboardService.toSpending(Category.SHOPPING, new BigDecimal("130"), new BigDecimal("100"));
        assertThat(exceeded.alert()).isEqualTo(AlertLevel.EXCEEDED);
        assertThat(exceeded.percentUsed()).isEqualTo(130);
    }

    @Test
    void noBudgetMeansNoPercentage() {
        var spending = DashboardService.toSpending(Category.LOISIRS, new BigDecimal("40"), null);
        assertThat(spending.percentUsed()).isNull();
        assertThat(spending.alert()).isEqualTo(AlertLevel.OK);
    }
}
