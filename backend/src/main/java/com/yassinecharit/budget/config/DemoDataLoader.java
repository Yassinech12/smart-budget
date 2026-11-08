package com.yassinecharit.budget.config;

import com.yassinecharit.budget.budget.Budget;
import com.yassinecharit.budget.budget.BudgetRepository;
import com.yassinecharit.budget.categorization.CategorizationResult;
import com.yassinecharit.budget.categorization.CategorizationService;
import com.yassinecharit.budget.categorization.Category;
import com.yassinecharit.budget.transaction.CsvTransactionParser;
import com.yassinecharit.budget.transaction.Transaction;
import com.yassinecharit.budget.transaction.TransactionRepository;
import com.yassinecharit.budget.user.User;
import com.yassinecharit.budget.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.YearMonth;

/**
 * "demo" profile: creates a demo account (demo@smartbudget.dev / demo1234) with one month of
 * sample transactions, shifted to the current month, and a few budgets.
 */
@Component
@Profile("demo")
public class DemoDataLoader implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataLoader.class);
    static final String DEMO_EMAIL = "demo@smartbudget.dev";

    private final UserRepository users;
    private final TransactionRepository transactions;
    private final BudgetRepository budgets;
    private final CategorizationService categorization;
    private final PasswordEncoder passwordEncoder;

    public DemoDataLoader(UserRepository users, TransactionRepository transactions, BudgetRepository budgets,
                          CategorizationService categorization, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.transactions = transactions;
        this.budgets = budgets;
        this.categorization = categorization;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        if (users.existsByEmailIgnoreCase(DEMO_EMAIL)) {
            return;
        }
        User demo = users.save(new User(DEMO_EMAIL, passwordEncoder.encode("demo1234"), "Compte Démo"));
        YearMonth now = YearMonth.now();
        try (InputStream csv = new ClassPathResource("demo/releve_bancaire_exemple.csv").getInputStream()) {
            for (var row : new CsvTransactionParser().parse(csv).rows()) {
                for (int monthsAgo = 0; monthsAgo < 3; monthsAgo++) {
                    YearMonth target = now.minusMonths(monthsAgo);
                    var date = target.atDay(Math.min(row.date().getDayOfMonth(), target.lengthOfMonth()));
                    CategorizationResult result = categorization.categorize(row.label(), row.amount());
                    transactions.save(new Transaction(demo, date, row.label(), row.amount(),
                            result.category(), result.source()));
                }
            }
        }
        budgets.save(new Budget(demo, Category.ALIMENTATION, new BigDecimal("300")));
        budgets.save(new Budget(demo, Category.RESTAURANTS, new BigDecimal("80")));
        budgets.save(new Budget(demo, Category.SHOPPING, new BigDecimal("150")));
        budgets.save(new Budget(demo, Category.TRANSPORT, new BigDecimal("250")));
        log.info("Demo account created: {} / demo1234", DEMO_EMAIL);
    }
}
