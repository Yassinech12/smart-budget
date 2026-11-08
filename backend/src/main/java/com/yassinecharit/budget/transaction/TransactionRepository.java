package com.yassinecharit.budget.transaction;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByUserIdAndDateBetweenOrderByDateDescIdDesc(Long userId, LocalDate start, LocalDate end);

    Optional<Transaction> findByIdAndUserId(Long id, Long userId);
}
