package com.nexushr.repository;

import com.nexushr.entity.Leave;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface LeaveRepository
        extends JpaRepository<Leave, Long> {

    List<Leave> findByEmployeeId(
            Long employeeId
    );

    List<Leave> findByStatus(
            String status
    );

    List<Leave> findByLeaveType(
            String leaveType
    );

    List<Leave> findByEmployeeIdAndStatus(
            Long employeeId,
            String status
    );

    List<Leave> findByStartDateBetween(
            LocalDate startDate,
            LocalDate endDate
    );

    List<Leave> findByEmployeeIdAndStartDateBetween(
            Long employeeId,
            LocalDate startDate,
            LocalDate endDate
    );

    List<Leave> findByStatusOrderByAppliedAtDesc(
            String status
    );

    List<Leave> findAllByOrderByAppliedAtDesc();

    long countByStatus(String status);

    List<Leave> findByEmployeeIdAndStatusInAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            Long employeeId,
            List<String> statuses,
            LocalDate endDate,
            LocalDate startDate
    );
}