package com.nexushr.repository;

import com.nexushr.entity.Payroll;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;

public interface PayrollRepository extends JpaRepository<Payroll, Long> {
    List<Payroll> findByEmployeeId(Long employeeId);
    List<Payroll> findByPayrollMonth(LocalDate payrollMonth);
    List<Payroll> findByStatus(Payroll.PayrollStatus status);
    boolean existsByEmployeeIdAndPayrollMonth(Long employeeId, LocalDate payrollMonth);
    boolean existsByEmployeeIdAndPayrollMonthAndIdNot(Long employeeId, LocalDate payrollMonth, Long id);
}
