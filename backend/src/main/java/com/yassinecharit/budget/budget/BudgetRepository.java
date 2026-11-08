package com.yassinecharit.budget.budget;

import com.yassinecharit.budget.categorization.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BudgetRepository extends JpaRepository<Budget, Long> {

    List<Budget> findByUserIdOrderByCategory(Long userId);

    Optional<Budget> findByUserIdAndCategory(Long userId, Category category);
}
