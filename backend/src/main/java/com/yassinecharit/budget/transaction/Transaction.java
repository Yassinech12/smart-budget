package com.yassinecharit.budget.transaction;

import com.yassinecharit.budget.categorization.Category;
import com.yassinecharit.budget.categorization.CategorizationSource;
import com.yassinecharit.budget.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A bank transaction. Negative amount = expense, positive amount = income. */
@Entity
@Table(name = "transactions", indexes = @Index(name = "idx_transactions_user_date", columnList = "user_id, date"))
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    private String label;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Category category;

    @Enumerated(EnumType.STRING)
    @Column(name = "categorized_by", nullable = false, length = 20)
    private CategorizationSource categorizedBy;

    protected Transaction() {
    }

    public Transaction(User user, LocalDate date, String label, BigDecimal amount,
                       Category category, CategorizationSource categorizedBy) {
        this.user = user;
        this.date = date;
        this.label = label;
        this.amount = amount;
        this.category = category;
        this.categorizedBy = categorizedBy;
    }

    public void update(LocalDate date, String label, BigDecimal amount, Category category,
                       CategorizationSource categorizedBy) {
        this.date = date;
        this.label = label;
        this.amount = amount;
        this.category = category;
        this.categorizedBy = categorizedBy;
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public LocalDate getDate() { return date; }
    public String getLabel() { return label; }
    public BigDecimal getAmount() { return amount; }
    public Category getCategory() { return category; }
    public CategorizationSource getCategorizedBy() { return categorizedBy; }
}
