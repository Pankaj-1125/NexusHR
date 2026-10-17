package com.nexushr.repository;

import com.nexushr.entity.PerformanceGoal;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PerformanceGoalRepository extends JpaRepository<PerformanceGoal, Long> {
    List<PerformanceGoal> findByEmployeeIdOrderByTargetDateAsc(Long employeeId);
    List<PerformanceGoal> findByStatus(PerformanceGoal.GoalStatus status);
}
