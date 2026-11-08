package com.yassinecharit.budget.transaction;

import com.yassinecharit.budget.categorization.CategorizationResult;
import com.yassinecharit.budget.categorization.CategorizationService;
import com.yassinecharit.budget.categorization.CategorizationSource;
import com.yassinecharit.budget.categorization.Category;
import com.yassinecharit.budget.common.BadRequestException;
import com.yassinecharit.budget.common.NotFoundException;
import com.yassinecharit.budget.security.CurrentUser;
import com.yassinecharit.budget.transaction.CsvTransactionParser.ParseResult;
import com.yassinecharit.budget.transaction.CsvTransactionParser.ParsedRow;
import com.yassinecharit.budget.transaction.TransactionDtos.ImportResult;
import com.yassinecharit.budget.transaction.TransactionDtos.TransactionRequest;
import com.yassinecharit.budget.transaction.TransactionDtos.TransactionResponse;
import com.yassinecharit.budget.user.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

@Service
public class TransactionService {

    private final TransactionRepository transactions;
    private final CategorizationService categorization;
    private final CurrentUser currentUser;
    private final CsvTransactionParser parser = new CsvTransactionParser();

    public TransactionService(TransactionRepository transactions, CategorizationService categorization,
                              CurrentUser currentUser) {
        this.transactions = transactions;
        this.categorization = categorization;
        this.currentUser = currentUser;
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> list(YearMonth month, Category category) {
        User user = currentUser.get();
        return transactions.findByUserIdAndDateBetweenOrderByDateDescIdDesc(
                        user.getId(), month.atDay(1), month.atEndOfMonth()).stream()
                .filter(t -> category == null || t.getCategory() == category)
                .map(TransactionResponse::from)
                .toList();
    }

    @Transactional
    public TransactionResponse create(TransactionRequest request) {
        User user = currentUser.get();
        CategorizationResult result = resolveCategory(request);
        Transaction saved = transactions.save(new Transaction(user, request.date(), request.label().trim(),
                request.amount(), result.category(), result.source()));
        return TransactionResponse.from(saved);
    }

    @Transactional
    public TransactionResponse update(Long id, TransactionRequest request) {
        Transaction transaction = findOwned(id);
        CategorizationResult result = resolveCategory(request);
        transaction.update(request.date(), request.label().trim(), request.amount(), result.category(), result.source());
        return TransactionResponse.from(transaction);
    }

    @Transactional
    public void delete(Long id) {
        transactions.delete(findOwned(id));
    }

    @Transactional
    public ImportResult importCsv(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Please send a non-empty CSV file");
        }
        User user = currentUser.get();
        ParseResult parsed;
        try {
            parsed = parser.parse(file.getInputStream());
        } catch (IOException e) {
            throw new BadRequestException("Unable to read the uploaded file");
        }
        int byRules = 0, byLlm = 0, uncategorized = 0;
        List<Transaction> toSave = new ArrayList<>();
        for (ParsedRow row : parsed.rows()) {
            CategorizationResult result = categorization.categorize(row.label(), row.amount());
            switch (result.source()) {
                case RULE -> byRules++;
                case LLM -> byLlm++;
                default -> uncategorized++;
            }
            toSave.add(new Transaction(user, row.date(), row.label(), row.amount(), result.category(), result.source()));
        }
        transactions.saveAll(toSave);
        return new ImportResult(toSave.size(), byRules, byLlm, uncategorized, parsed.errors());
    }

    private CategorizationResult resolveCategory(TransactionRequest request) {
        if (request.category() != null) {
            return new CategorizationResult(request.category(), CategorizationSource.MANUAL);
        }
        return categorization.categorize(request.label(), request.amount());
    }

    private Transaction findOwned(Long id) {
        return transactions.findByIdAndUserId(id, currentUser.get().getId())
                .orElseThrow(() -> new NotFoundException("Transaction not found"));
    }
}
