package com.nexushr.repository;

import com.nexushr.entity.InterviewSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InterviewScheduleRepository extends JpaRepository<InterviewSchedule, Long> {
    List<InterviewSchedule> findByApplicationIdOrderByCreatedAtDesc(Long applicationId);
    Optional<InterviewSchedule> findFirstByApplicationIdOrderByCreatedAtDesc(Long applicationId);
}
