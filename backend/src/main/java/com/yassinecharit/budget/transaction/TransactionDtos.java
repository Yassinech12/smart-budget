package com.yassinecharit.budget.transaction;

import com.yassinecharit.budget.categorization.Category;
import com.yassinecharit.budget.categorization.CategorizationSource;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class TransactionDtos {

    private TransactionDtos() {
    }

    /** When category is null, the API categorizes the transaction automatically. */
    public record TransactionRequest(
            @NotNull LocalDate date,
            @NotBlank @Size(max = 255) String label,
            @NotNull BigDecimal amount,
            Category category) {
    }

    public record TransactionResponse(Long id, LocalDate date, String label, BigDecimal amount,
                                      Category category, CategorizationSource categorizedBy) {
        public static TransactionResponse from(Transaction t) {
            return new TransactionResponse(t.getId(), t.getDate(), t.getLabel(), t.getAmount(),
                    t.getCategory(), t.getCategorizedBy());
        }
    }

    public record ImportResult(int imported, int byRules, int byLlm, int uncategorized, List<String> errors) {
    }
}
