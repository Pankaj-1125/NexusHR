package com.nexushr.repository;

import com.nexushr.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface EmployeeRepository
        extends JpaRepository<Employee, Long> {

    Optional<Employee> findByEmail(String email);

    Optional<Employee> findByEmailIgnoreCase(String email);

    Optional<Employee> findByUserId(Long userId);

    Optional<Employee> findByUserUsername(String username);

    boolean existsByUserId(Long userId);

    long countByEmploymentStatus(String employmentStatus);

    @Query("""
            SELECT e.department, COUNT(e)
            FROM Employee e
            WHERE e.department IS NOT NULL
            GROUP BY e.department
            ORDER BY e.department
            """)
    List<Object[]> countEmployeesByDepartment();
}