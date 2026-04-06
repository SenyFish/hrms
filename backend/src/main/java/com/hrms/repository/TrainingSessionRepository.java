package com.hrms.repository;

import com.hrms.entity.TrainingSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface TrainingSessionRepository extends JpaRepository<TrainingSession, Long>, JpaSpecificationExecutor<TrainingSession> {
}
