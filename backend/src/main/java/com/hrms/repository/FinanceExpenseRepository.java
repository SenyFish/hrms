package com.hrms.repository;

import com.hrms.entity.FinanceExpense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface FinanceExpenseRepository extends JpaRepository<FinanceExpense, Long>, JpaSpecificationExecutor<FinanceExpense> {
    Optional<FinanceExpense> findTopByOrderByIdDesc();
}
