package com.nexushr.service;

import com.nexushr.entity.Employee;
import com.nexushr.entity.Payroll;
import com.nexushr.repository.PayrollRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PayrollServiceTest {

    @Mock
    private PayrollRepository payrollRepository;

    @InjectMocks
    private PayrollService payrollService;

    private Employee employee;
    private Payroll payroll;

    @BeforeEach
    void setUp() throws Exception {

        /*
         * PayrollService uses:
         *
         * @Value("${payroll.tax-rate:0.10}")
         * private BigDecimal taxRate;
         *
         * Because this is a Mockito unit test and Spring context
         * is not started, @Value is not injected automatically.
         *
         * Therefore we inject the configured default manually.
         */
        Field taxRateField =
                PayrollService.class.getDeclaredField("taxRate");

        taxRateField.setAccessible(true);

        taxRateField.set(
                payrollService,
                new BigDecimal("0.10")
        );

        employee = new Employee();

        employee.setId(1L);
        employee.setFirstName("Test");
        employee.setLastName("Employee");
        employee.setEmail("test@example.com");
        employee.setSalary(50000.00);

        payroll = new Payroll();

        payroll.setId(1L);

        payroll.setEmployee(employee);

        payroll.setPayrollMonth(
                LocalDate.of(2026, 9, 1)
        );

        payroll.setBasicSalary(
                new BigDecimal("50000.00")
        );

        payroll.setAllowances(
                new BigDecimal("5000.00")
        );

        payroll.setDeductions(
                new BigDecimal("2000.00")
        );

        payroll.setTax(
                new BigDecimal("5500.00")
        );

        payroll.setNetSalary(
                new BigDecimal("47500.00")
        );

        payroll.setStatus(
                Payroll.PayrollStatus.PROCESSED
        );
    }

    // ============================================================
    // GET ALL PAYROLLS
    // ============================================================

    @Test
    void getAllPayrollsShouldReturnPayrollList() {

        when(payrollRepository.findAll())
                .thenReturn(List.of(payroll));

        List<Payroll> result =
                payrollService.getAllPayrolls();

        assertNotNull(result);

        assertEquals(
                1,
                result.size()
        );

        assertEquals(
                payroll,
                result.get(0)
        );

        verify(payrollRepository)
                .findAll();
    }

    // ============================================================
    // GET PAYROLL BY ID
    // ============================================================

    @Test
    void getPayrollByIdShouldReturnPayroll() {

        when(payrollRepository.findById(1L))
                .thenReturn(Optional.of(payroll));

        Optional<Payroll> result =
                payrollService.getPayrollById(1L);

        assertTrue(result.isPresent());

        assertEquals(
                payroll,
                result.get()
        );

        verify(payrollRepository)
                .findById(1L);
    }

    @Test
    void getPayrollByIdShouldReturnEmptyWhenPayrollDoesNotExist() {

        when(payrollRepository.findById(999L))
                .thenReturn(Optional.empty());

        Optional<Payroll> result =
                payrollService.getPayrollById(999L);

        assertTrue(result.isEmpty());

        verify(payrollRepository)
                .findById(999L);
    }

    // ============================================================
    // GET PAYROLLS BY EMPLOYEE
    // ============================================================

    @Test
    void getPayrollsByEmployeeShouldReturnEmployeePayrolls() {

        when(payrollRepository.findByEmployeeId(1L))
                .thenReturn(List.of(payroll));

        List<Payroll> result =
                payrollService.getPayrollsByEmployee(1L);

        assertNotNull(result);

        assertEquals(
                1,
                result.size()
        );

        assertEquals(
                payroll,
                result.get(0)
        );

        verify(payrollRepository)
                .findByEmployeeId(1L);
    }

    // ============================================================
    // CREATE PAYROLL
    // ============================================================

    @Test
    void createPayrollShouldCalculateTaxAndNetSalaryAutomatically() {

        Payroll newPayroll = new Payroll();

        newPayroll.setEmployee(employee);

        newPayroll.setPayrollMonth(
                LocalDate.of(2026, 10, 1)
        );

        newPayroll.setBasicSalary(
                new BigDecimal("50000.00")
        );

        newPayroll.setAllowances(
                new BigDecimal("5000.00")
        );

        newPayroll.setDeductions(
                new BigDecimal("2000.00")
        );

        newPayroll.setStatus(
                Payroll.PayrollStatus.PROCESSED
        );

        when(
                payrollRepository.existsByEmployeeIdAndPayrollMonth(
                        1L,
                        LocalDate.of(2026, 10, 1)
                )
        ).thenReturn(false);

        when(
                payrollRepository.save(any(Payroll.class))
        ).thenAnswer(invocation -> {

            Payroll saved =
                    invocation.getArgument(0);

            saved.setId(2L);

            return saved;
        });

        Payroll result =
                payrollService.createPayroll(newPayroll);

        assertNotNull(result);

        /*
         * Gross:
         * 50,000 + 5,000 = 55,000
         *
         * Tax:
         * 10% of 55,000 = 5,500
         *
         * Net:
         * 55,000 - 2,000 - 5,500 = 47,500
         */

        assertEquals(
                new BigDecimal("5500.00"),
                result.getTax()
        );

        assertEquals(
                new BigDecimal("47500.00"),
                result.getNetSalary()
        );

        verify(payrollRepository)
                .save(any(Payroll.class));
    }

    // ============================================================
    // TAX CALCULATION
    // ============================================================

    @Test
    void createPayrollShouldCalculateTenPercentTax() {

        Payroll newPayroll = new Payroll();

        newPayroll.setEmployee(employee);

        newPayroll.setPayrollMonth(
                LocalDate.of(2026, 11, 1)
        );

        newPayroll.setBasicSalary(
                new BigDecimal("60000.00")
        );

        newPayroll.setAllowances(
                new BigDecimal("10000.00")
        );

        newPayroll.setDeductions(
                new BigDecimal("5000.00")
        );

        newPayroll.setStatus(
                Payroll.PayrollStatus.PROCESSED
        );

        when(
                payrollRepository.existsByEmployeeIdAndPayrollMonth(
                        1L,
                        LocalDate.of(2026, 11, 1)
                )
        ).thenReturn(false);

        when(
                payrollRepository.save(any(Payroll.class))
        ).thenAnswer(invocation ->
                invocation.getArgument(0)
        );

        Payroll result =
                payrollService.createPayroll(newPayroll);

        /*
         * Gross = 60,000 + 10,000 = 70,000
         *
         * Tax = 10% = 7,000
         *
         * Net = 70,000 - 5,000 - 7,000
         *     = 58,000
         */

        assertEquals(
                new BigDecimal("7000.00"),
                result.getTax()
        );

        assertEquals(
                new BigDecimal("58000.00"),
                result.getNetSalary()
        );
    }

    // ============================================================
    // DUPLICATE PAYROLL
    // ============================================================

    @Test
    void createPayrollShouldRejectDuplicateEmployeeAndMonth() {

        Payroll duplicatePayroll = new Payroll();

        duplicatePayroll.setEmployee(employee);

        duplicatePayroll.setPayrollMonth(
                LocalDate.of(2026, 9, 1)
        );

        duplicatePayroll.setBasicSalary(
                new BigDecimal("50000.00")
        );

        duplicatePayroll.setAllowances(
                new BigDecimal("5000.00")
        );

        duplicatePayroll.setDeductions(
                new BigDecimal("2000.00")
        );

        duplicatePayroll.setStatus(
                Payroll.PayrollStatus.PROCESSED
        );

        when(
                payrollRepository.existsByEmployeeIdAndPayrollMonth(
                        1L,
                        LocalDate.of(2026, 9, 1)
                )
        ).thenReturn(true);

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                payrollService.createPayroll(
                                        duplicatePayroll
                                )
                );

        assertTrue(
                exception.getMessage()
                        .toLowerCase()
                        .contains("payroll")
        );

        verify(
                payrollRepository,
                never()
        ).save(any(Payroll.class));
    }

    // ============================================================
    // INVALID PAYROLL
    // ============================================================

    @Test
    void createPayrollShouldRejectNullPayroll() {

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                payrollService.createPayroll(null)
                );

        assertEquals(
                "Payroll data is required.",
                exception.getMessage()
        );

        verify(
                payrollRepository,
                never()
        ).save(any(Payroll.class));
    }

    // ============================================================
    // NEGATIVE BASIC SALARY
    // ============================================================

    @Test
    void createPayrollShouldRejectNegativeBasicSalary() {

        Payroll invalidPayroll = new Payroll();

        invalidPayroll.setEmployee(employee);

        invalidPayroll.setPayrollMonth(
                LocalDate.of(2026, 12, 1)
        );

        invalidPayroll.setBasicSalary(
                new BigDecimal("-50000.00")
        );

        invalidPayroll.setAllowances(
                new BigDecimal("5000.00")
        );

        invalidPayroll.setDeductions(
                new BigDecimal("1000.00")
        );

        invalidPayroll.setStatus(
                Payroll.PayrollStatus.PROCESSED
        );

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                payrollService.createPayroll(
                                        invalidPayroll
                                )
                );

        assertEquals(
                "Basic salary cannot be negative.",
                exception.getMessage()
        );

        verify(
                payrollRepository,
                never()
        ).save(any(Payroll.class));
    }

    // ============================================================
    // NEGATIVE ALLOWANCES
    // ============================================================

    @Test
    void createPayrollShouldRejectNegativeAllowances() {

        Payroll invalidPayroll = new Payroll();

        invalidPayroll.setEmployee(employee);

        invalidPayroll.setPayrollMonth(
                LocalDate.of(2026, 12, 1)
        );

        invalidPayroll.setBasicSalary(
                new BigDecimal("50000.00")
        );

        invalidPayroll.setAllowances(
                new BigDecimal("-5000.00")
        );

        invalidPayroll.setDeductions(
                new BigDecimal("1000.00")
        );

        invalidPayroll.setStatus(
                Payroll.PayrollStatus.PROCESSED
        );

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                payrollService.createPayroll(
                                        invalidPayroll
                                )
                );

        assertEquals(
                "Allowances cannot be negative.",
                exception.getMessage()
        );

        verify(
                payrollRepository,
                never()
        ).save(any(Payroll.class));
    }

    // ============================================================
    // NEGATIVE DEDUCTIONS
    // ============================================================

    @Test
    void createPayrollShouldRejectNegativeDeductions() {

        Payroll invalidPayroll = new Payroll();

        invalidPayroll.setEmployee(employee);

        invalidPayroll.setPayrollMonth(
                LocalDate.of(2026, 12, 1)
        );

        invalidPayroll.setBasicSalary(
                new BigDecimal("50000.00")
        );

        invalidPayroll.setAllowances(
                new BigDecimal("5000.00")
        );

        invalidPayroll.setDeductions(
                new BigDecimal("-1000.00")
        );

        invalidPayroll.setStatus(
                Payroll.PayrollStatus.PROCESSED
        );

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                payrollService.createPayroll(
                                        invalidPayroll
                                )
                );

        assertEquals(
                "Deductions cannot be negative.",
                exception.getMessage()
        );

        verify(
                payrollRepository,
                never()
        ).save(any(Payroll.class));
    }

    // ============================================================
    // NEGATIVE TAX
    // ============================================================

    @Test
    void createPayrollShouldRejectNegativeTax() {

        Payroll invalidPayroll = new Payroll();

        invalidPayroll.setEmployee(employee);

        invalidPayroll.setPayrollMonth(
                LocalDate.of(2026, 12, 1)
        );

        invalidPayroll.setBasicSalary(
                new BigDecimal("50000.00")
        );

        invalidPayroll.setAllowances(
                new BigDecimal("5000.00")
        );

        invalidPayroll.setDeductions(
                new BigDecimal("1000.00")
        );

        invalidPayroll.setTax(
                new BigDecimal("-500.00")
        );

        invalidPayroll.setStatus(
                Payroll.PayrollStatus.PROCESSED
        );

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                payrollService.createPayroll(
                                        invalidPayroll
                                )
                );

        assertEquals(
                "Tax cannot be negative.",
                exception.getMessage()
        );

        verify(
                payrollRepository,
                never()
        ).save(any(Payroll.class));
    }

    // ============================================================
    // UPDATE PAYROLL
    // ============================================================

    @Test
    void updatePayrollShouldRecalculateTaxAndNetSalary() {

        Payroll updatedPayroll = new Payroll();

        updatedPayroll.setEmployee(employee);

        updatedPayroll.setPayrollMonth(
                LocalDate.of(2026, 9, 1)
        );

        updatedPayroll.setBasicSalary(
                new BigDecimal("60000.00")
        );

        updatedPayroll.setAllowances(
                new BigDecimal("10000.00")
        );

        updatedPayroll.setDeductions(
                new BigDecimal("5000.00")
        );

        updatedPayroll.setStatus(
                Payroll.PayrollStatus.PROCESSED
        );

        when(
                payrollRepository.findById(1L)
        ).thenReturn(
                Optional.of(payroll)
        );

        when(
                payrollRepository
                        .existsByEmployeeIdAndPayrollMonthAndIdNot(
                                1L,
                                LocalDate.of(2026, 9, 1),
                                1L
                        )
        ).thenReturn(false);

        when(
                payrollRepository.save(any(Payroll.class))
        ).thenAnswer(invocation ->
                invocation.getArgument(0)
        );

        Payroll result =
                payrollService.updatePayroll(
                        1L,
                        updatedPayroll
                );

        assertNotNull(result);

        /*
         * Gross = 60,000 + 10,000 = 70,000
         *
         * Tax = 7,000
         *
         * Net = 70,000 - 5,000 - 7,000
         *     = 58,000
         */

        assertEquals(
                new BigDecimal("7000.00"),
                result.getTax()
        );

        assertEquals(
                new BigDecimal("58000.00"),
                result.getNetSalary()
        );

        verify(
                payrollRepository
        ).findById(1L);

        verify(
                payrollRepository
        ).save(any(Payroll.class));
    }

    // ============================================================
    // UPDATE NON-EXISTING PAYROLL
    // ============================================================

    @Test
    void updatePayrollShouldThrowWhenPayrollDoesNotExist() {

        when(
                payrollRepository.findById(999L)
        ).thenReturn(Optional.empty());

        RuntimeException exception =
                assertThrows(
                        RuntimeException.class,
                        () ->
                                payrollService.updatePayroll(
                                        999L,
                                        payroll
                                )
                );

        assertTrue(
                exception.getMessage()
                        .contains("Payroll record not found")
        );

        verify(
                payrollRepository,
                never()
        ).save(any(Payroll.class));
    }

    // ============================================================
    // DELETE PAYROLL
    // ============================================================

    @Test
    void deletePayrollShouldDeleteExistingPayroll() {

        when(
                payrollRepository.existsById(1L)
        ).thenReturn(true);

        payrollService.deletePayroll(1L);

        verify(
                payrollRepository
        ).existsById(1L);

        verify(
                payrollRepository
        ).deleteById(1L);
    }

    // ============================================================
    // DELETE NON-EXISTING PAYROLL
    // ============================================================

    @Test
    void deletePayrollShouldThrowWhenPayrollDoesNotExist() {

        when(
                payrollRepository.existsById(999L)
        ).thenReturn(false);

        RuntimeException exception =
                assertThrows(
                        RuntimeException.class,
                        () ->
                                payrollService.deletePayroll(999L)
                );

        assertTrue(
                exception.getMessage()
                        .contains("Payroll record not found")
        );

        verify(
                payrollRepository,
                never()
        ).deleteById(999L);
    }

    // ============================================================
    // NET SALARY CANNOT BE NEGATIVE
    // ============================================================

    @Test
    void createPayrollShouldRejectNegativeNetSalary() {

        Employee lowSalaryEmployee =
                new Employee();

        lowSalaryEmployee.setId(2L);
        lowSalaryEmployee.setFirstName("Low");
        lowSalaryEmployee.setLastName("Salary");
        lowSalaryEmployee.setEmail("low@example.com");

        Payroll invalidPayroll =
                new Payroll();

        invalidPayroll.setEmployee(
                lowSalaryEmployee
        );

        invalidPayroll.setPayrollMonth(
                LocalDate.of(2026, 12, 1)
        );

        invalidPayroll.setBasicSalary(
                new BigDecimal("1000.00")
        );

        invalidPayroll.setAllowances(
                BigDecimal.ZERO
        );

        invalidPayroll.setDeductions(
                new BigDecimal("2000.00")
        );

        invalidPayroll.setStatus(
                Payroll.PayrollStatus.PROCESSED
        );

        when(
                payrollRepository.existsByEmployeeIdAndPayrollMonth(
                        2L,
                        LocalDate.of(2026, 12, 1)
                )
        ).thenReturn(false);

        RuntimeException exception =
                assertThrows(
                        RuntimeException.class,
                        () ->
                                payrollService.createPayroll(
                                        invalidPayroll
                                )
                );

        assertEquals(
                "Net salary cannot be negative",
                exception.getMessage()
        );

        verify(
                payrollRepository,
                never()
        ).save(any(Payroll.class));
    }
}