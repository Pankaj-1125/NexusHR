package com.nexushr.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "payroll")
public class Payroll {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(nullable = false)
    private LocalDate payrollMonth;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal basicSalary;

    @Column(precision = 12, scale = 2)
    private BigDecimal allowances;

    @Column(precision = 12, scale = 2)
    private BigDecimal bonus;

    @Column(precision = 12, scale = 2)
    private BigDecimal overtimePay;

    @Column(precision = 12, scale = 2)
    private BigDecimal grossSalary;

    @Column(precision = 12, scale = 2)
    private BigDecimal deductions;

    @Column(precision = 12, scale = 2)
    private BigDecimal providentFund;

    @Column(precision = 12, scale = 2)
    private BigDecimal professionalTax;

    @Column(precision = 12, scale = 2)
    private BigDecimal otherDeductions;

    @Column(precision = 12, scale = 2)
    private BigDecimal tax;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal netSalary;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PayrollStatus status;

    private LocalDate paymentDate;
    private String paymentReference;

    @Column(columnDefinition = "TEXT")
    private String notes;

    private LocalDateTime processedAt;
    private LocalDateTime paidAt;

    public enum PayrollStatus { DRAFT, PROCESSED, PAID }

    public Payroll() {}

    public Long getId(){return id;} public void setId(Long id){this.id=id;}
    public Employee getEmployee(){return employee;} public void setEmployee(Employee employee){this.employee=employee;}
    public LocalDate getPayrollMonth(){return payrollMonth;} public void setPayrollMonth(LocalDate payrollMonth){this.payrollMonth=payrollMonth;}
    public BigDecimal getBasicSalary(){return basicSalary;} public void setBasicSalary(BigDecimal v){this.basicSalary=v;}
    public BigDecimal getAllowances(){return allowances;} public void setAllowances(BigDecimal v){this.allowances=v;}
    public BigDecimal getBonus(){return bonus;} public void setBonus(BigDecimal v){this.bonus=v;}
    public BigDecimal getOvertimePay(){return overtimePay;} public void setOvertimePay(BigDecimal v){this.overtimePay=v;}
    public BigDecimal getGrossSalary(){return grossSalary;} public void setGrossSalary(BigDecimal v){this.grossSalary=v;}
    public BigDecimal getDeductions(){return deductions;} public void setDeductions(BigDecimal v){this.deductions=v;}
    public BigDecimal getProvidentFund(){return providentFund;} public void setProvidentFund(BigDecimal v){this.providentFund=v;}
    public BigDecimal getProfessionalTax(){return professionalTax;} public void setProfessionalTax(BigDecimal v){this.professionalTax=v;}
    public BigDecimal getOtherDeductions(){return otherDeductions;} public void setOtherDeductions(BigDecimal v){this.otherDeductions=v;}
    public BigDecimal getTax(){return tax;} public void setTax(BigDecimal v){this.tax=v;}
    public BigDecimal getNetSalary(){return netSalary;} public void setNetSalary(BigDecimal v){this.netSalary=v;}
    public PayrollStatus getStatus(){return status;} public void setStatus(PayrollStatus v){this.status=v;}
    public LocalDate getPaymentDate(){return paymentDate;} public void setPaymentDate(LocalDate v){this.paymentDate=v;}
    public String getPaymentReference(){return paymentReference;} public void setPaymentReference(String v){this.paymentReference=v;}
    public String getNotes(){return notes;} public void setNotes(String v){this.notes=v;}
    public LocalDateTime getProcessedAt(){return processedAt;} public void setProcessedAt(LocalDateTime v){this.processedAt=v;}
    public LocalDateTime getPaidAt(){return paidAt;} public void setPaidAt(LocalDateTime v){this.paidAt=v;}
}
