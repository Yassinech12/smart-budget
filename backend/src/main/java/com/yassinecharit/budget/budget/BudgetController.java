package com.yassinecharit.budget.budget;

import com.yassinecharit.budget.budget.BudgetDtos.BudgetRequest;
import com.yassinecharit.budget.budget.BudgetDtos.BudgetResponse;
import com.yassinecharit.budget.categorization.Category;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/budgets")
public class BudgetController {

    private final BudgetService service;

    public BudgetController(BudgetService service) {
        this.service = service;
    }

    @GetMapping
    public List<BudgetResponse> list() {
        return service.list();
    }

    @PutMapping
    public BudgetResponse upsert(@Valid @RequestBody BudgetRequest request) {
        return service.upsert(request);
    }

    @DeleteMapping("/{category}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Category category) {
        service.delete(category);
    }
}
