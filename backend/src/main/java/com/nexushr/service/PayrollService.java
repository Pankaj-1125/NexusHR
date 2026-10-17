package com.nexushr.service;

import com.nexushr.entity.Employee;
import com.nexushr.entity.Payroll;
import com.nexushr.repository.EmployeeRepository;
import com.nexushr.repository.PayrollRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class PayrollService {

    @Value("${payroll.tax-rate:0.10}")
    private BigDecimal taxRate;

    @Value("${payroll.batch.allowance-rate:0.10}")
    private BigDecimal batchAllowanceRate;

    @Value("${payroll.batch.pf-rate:0.12}")
    private BigDecimal batchPfRate;

    @Value("${payroll.batch.professional-tax:200}")
    private BigDecimal batchProfessionalTax;

    private final PayrollRepository payrollRepository;
    private final PayslipPdfService payslipPdfService;

    @Autowired(required = false)
    private EmployeeRepository employeeRepository;

    @Autowired(required = false)
    private NotificationService notificationService;

    @Autowired(required = false)
    private EmailNotificationService emailNotificationService;

    @Autowired(required = false)
    private AuditLogService auditLogService;

    public PayrollService(
            PayrollRepository payrollRepository,
            PayslipPdfService payslipPdfService
    ) {
        this.payrollRepository = payrollRepository;
        this.payslipPdfService = payslipPdfService;
    }

    public List<Payroll> getAllPayrolls() {
        return payrollRepository.findAll();
    }

    public Optional<Payroll> getPayrollById(Long id) {
        return payrollRepository.findById(id);
    }

    public List<Payroll> getPayrollsByEmployee(Long employeeId) {
        return payrollRepository.findByEmployeeId(employeeId);
    }

    public List<Payroll> getPayrollsByMonth(LocalDate month) {
        return payrollRepository.findByPayrollMonth(normalizeMonth(month));
    }

    @Transactional
    public Payroll createPayroll(Payroll payroll) {
        validatePayroll(payroll);
        validateEmployeeAndMonth(payroll, null);
        calculateNetSalary(payroll);
        if (payroll.getStatus() == Payroll.PayrollStatus.PROCESSED && payroll.getProcessedAt() == null) {
            payroll.setProcessedAt(LocalDateTime.now());
        }
        if (payroll.getStatus() == Payroll.PayrollStatus.PAID) {
            if (payroll.getProcessedAt() == null) payroll.setProcessedAt(LocalDateTime.now());
            if (payroll.getPaidAt() == null) payroll.setPaidAt(LocalDateTime.now());
            if (payroll.getPaymentDate() == null) payroll.setPaymentDate(LocalDate.now());
        }
        Payroll saved = payrollRepository.save(payroll);
        audit("PAYROLL_CREATED", saved, "Payroll record created");
        return saved;
    }

    @Transactional
    public Payroll updatePayroll(Long id, Payroll updatedPayroll) {
        validatePayroll(updatedPayroll);
        Payroll existing = payrollRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Payroll record not found with id: " + id));

        if (existing.getStatus() == Payroll.PayrollStatus.PAID) {
            throw new IllegalStateException("Paid payroll is locked. Create an adjustment instead of editing it.");
        }

        validateEmployeeAndMonth(updatedPayroll, id);
        existing.setEmployee(updatedPayroll.getEmployee());
        existing.setPayrollMonth(normalizeMonth(updatedPayroll.getPayrollMonth()));
        copyAmounts(updatedPayroll, existing);
        existing.setNotes(updatedPayroll.getNotes());
        calculateNetSalary(existing);

        Payroll.PayrollStatus newStatus = updatedPayroll.getStatus();
        existing.setStatus(newStatus);
        if (newStatus == Payroll.PayrollStatus.PROCESSED && existing.getProcessedAt() == null) {
            existing.setProcessedAt(LocalDateTime.now());
        }
        Payroll saved = payrollRepository.save(existing);
        audit("PAYROLL_UPDATED", saved, "Payroll record updated");
        return saved;
    }

    @Transactional
    public Payroll processPayroll(Long id, boolean notifyEmployee) {
        Payroll payroll = requirePayroll(id);
        if (payroll.getStatus() == Payroll.PayrollStatus.PAID) {
            throw new IllegalStateException("Paid payroll cannot be processed again.");
        }
        calculateNetSalary(payroll);
        payroll.setStatus(Payroll.PayrollStatus.PROCESSED);
        payroll.setProcessedAt(LocalDateTime.now());
        Payroll saved = payrollRepository.save(payroll);
        audit("PAYROLL_PROCESSED", saved, "Payroll processed and ready for payment");
        if (notifyEmployee) notifyPayroll(saved, "Payroll Processed", false);
        return saved;
    }

    @Transactional
    public Payroll markPaid(Long id, String paymentReference, LocalDate paymentDate, boolean notifyEmployee) {
        Payroll payroll = requirePayroll(id);
        if (payroll.getStatus() == Payroll.PayrollStatus.DRAFT) {
            throw new IllegalStateException("Process payroll before marking it as paid.");
        }
        payroll.setStatus(Payroll.PayrollStatus.PAID);
        payroll.setPaymentDate(paymentDate == null ? LocalDate.now() : paymentDate);
        payroll.setPaymentReference(blankToNull(paymentReference));
        payroll.setPaidAt(LocalDateTime.now());
        if (payroll.getProcessedAt() == null) payroll.setProcessedAt(LocalDateTime.now());
        Payroll saved = payrollRepository.save(payroll);
        audit("PAYROLL_PAID", saved, "Payroll marked as paid");
        if (notifyEmployee) notifyPayroll(saved, "Salary Payment Confirmed", true);
        return saved;
    }

    @Transactional
    public Map<String, Object> runMonthlyBatch(LocalDate month, boolean autoProcess, boolean notifyEmployee) {
        if (employeeRepository == null) {
            throw new IllegalStateException("Employee repository is unavailable.");
        }
        LocalDate payrollMonth = normalizeMonth(month == null ? LocalDate.now() : month);
        List<Employee> employees = employeeRepository.findAll();
        List<Long> createdIds = new ArrayList<>();
        int skippedExisting = 0;
        int skippedInactive = 0;
        int skippedNoSalary = 0;

        for (Employee employee : employees) {
            if (employee.getEmploymentStatus() != null &&
                    !"ACTIVE".equalsIgnoreCase(employee.getEmploymentStatus())) {
                skippedInactive++;
                continue;
            }
            if (employee.getSalary() == null || employee.getSalary() < 0) {
                skippedNoSalary++;
                continue;
            }
            if (payrollRepository.existsByEmployeeIdAndPayrollMonth(employee.getId(), payrollMonth)) {
                skippedExisting++;
                continue;
            }

            Payroll payroll = buildPolicyPayroll(employee, payrollMonth);
            payroll.setStatus(autoProcess ? Payroll.PayrollStatus.PROCESSED : Payroll.PayrollStatus.DRAFT);
            if (autoProcess) payroll.setProcessedAt(LocalDateTime.now());
            Payroll saved = payrollRepository.save(payroll);
            createdIds.add(saved.getId());
            audit("PAYROLL_BATCH_CREATED", saved, "Monthly batch payroll created");
            if (notifyEmployee && autoProcess) notifyPayroll(saved, "Payroll Processed", false);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("month", payrollMonth);
        result.put("created", createdIds.size());
        result.put("createdIds", createdIds);
        result.put("skippedExisting", skippedExisting);
        result.put("skippedInactive", skippedInactive);
        result.put("skippedNoSalary", skippedNoSalary);
        result.put("autoProcessed", autoProcess);
        return result;
    }

    public Map<String, Object> getSummary(LocalDate month) {
        List<Payroll> rows = month == null
                ? payrollRepository.findAll()
                : payrollRepository.findByPayrollMonth(normalizeMonth(month));

        BigDecimal gross = sum(rows, Payroll::getGrossSalary);
        BigDecimal tax = sum(rows, Payroll::getTax);
        BigDecimal totalDeductions = rows.stream()
                .map(this::totalDeductions)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal net = sum(rows, Payroll::getNetSalary);
        long draft = rows.stream().filter(p -> p.getStatus() == Payroll.PayrollStatus.DRAFT).count();
        long processed = rows.stream().filter(p -> p.getStatus() == Payroll.PayrollStatus.PROCESSED).count();
        long paid = rows.stream().filter(p -> p.getStatus() == Payroll.PayrollStatus.PAID).count();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("month", month == null ? "ALL" : normalizeMonth(month));
        result.put("records", rows.size());
        result.put("draft", draft);
        result.put("processed", processed);
        result.put("paid", paid);
        result.put("grossPayroll", gross.setScale(2, RoundingMode.HALF_UP));
        result.put("tax", tax.setScale(2, RoundingMode.HALF_UP));
        result.put("totalDeductions", totalDeductions.setScale(2, RoundingMode.HALF_UP));
        result.put("netPayroll", net.setScale(2, RoundingMode.HALF_UP));
        return result;
    }

    public byte[] generatePayslip(Long payrollId) {
        return payslipPdfService.generatePayslip(requirePayroll(payrollId));
    }

    public Map<String, Object> emailPayslip(Long payrollId, String subject) {
        Payroll payroll = requirePayroll(payrollId);
        if (emailNotificationService == null) {
            throw new IllegalStateException("Email service is unavailable.");
        }
        byte[] pdf = payslipPdfService.generatePayslip(payroll);
        Employee employee = payroll.getEmployee();
        String month = payroll.getPayrollMonth() == null ? "Payroll" : payroll.getPayrollMonth().toString();
        String effectiveSubject = subject == null || subject.isBlank()
                ? "NexusHR Payslip - " + month
                : subject.trim();
        String body = "Hello " + safeName(employee) + ",\n\n"
                + "Your NexusHR payslip for " + month + " is attached.\n"
                + "Net salary: " + money(payroll.getNetSalary()) + "\n"
                + "Status: " + payroll.getStatus() + "\n\n"
                + "Regards,\nNexusHR Payroll Team";
        EmailNotificationService.DeliveryResult delivery = emailNotificationService.sendWithPdfAttachment(
                employee, effectiveSubject, body,
                "NexusHR_Payslip_" + payroll.getId() + ".pdf", pdf
        );
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", delivery.status());
        result.put("message", delivery.message());
        return result;
    }

    @Transactional
    public void deletePayroll(Long id) {
        if (!payrollRepository.existsById(id)) {
            throw new RuntimeException("Payroll record not found with id: " + id);
        }
        payrollRepository.deleteById(id);
    }

    private Payroll buildPolicyPayroll(Employee employee, LocalDate month) {
        BigDecimal basic = BigDecimal.valueOf(employee.getSalary()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal allowance = basic.multiply(zero(batchAllowanceRate)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal pf = basic.multiply(zero(batchPfRate)).setScale(2, RoundingMode.HALF_UP);

        Payroll p = new Payroll();
        p.setEmployee(employee);
        p.setPayrollMonth(month);
        p.setBasicSalary(basic);
        p.setAllowances(allowance);
        p.setBonus(BigDecimal.ZERO);
        p.setOvertimePay(BigDecimal.ZERO);
        p.setDeductions(BigDecimal.ZERO);
        p.setProvidentFund(pf);
        p.setProfessionalTax(zero(batchProfessionalTax).setScale(2, RoundingMode.HALF_UP));
        p.setOtherDeductions(BigDecimal.ZERO);
        p.setNotes("Generated by NexusHR configurable payroll policy engine. Verify statutory values before production filing.");
        calculateNetSalary(p);
        return p;
    }

    private void validateEmployeeAndMonth(Payroll payroll, Long currentId) {
        if (payroll.getEmployee() == null || payroll.getEmployee().getId() == null) {
            throw new IllegalArgumentException("Employee is required.");
        }
        if (payroll.getPayrollMonth() == null) {
            throw new IllegalArgumentException("Payroll month is required.");
        }
        payroll.setPayrollMonth(normalizeMonth(payroll.getPayrollMonth()));
        boolean duplicate = currentId == null
                ? payrollRepository.existsByEmployeeIdAndPayrollMonth(payroll.getEmployee().getId(), payroll.getPayrollMonth())
                : payrollRepository.existsByEmployeeIdAndPayrollMonthAndIdNot(payroll.getEmployee().getId(), payroll.getPayrollMonth(), currentId);
        if (duplicate) throw new IllegalArgumentException("Payroll already exists for this employee and month.");
    }

    private void validatePayroll(Payroll payroll) {
        if (payroll == null) throw new IllegalArgumentException("Payroll data is required.");
        requireNonNegative("Basic salary", payroll.getBasicSalary(), true);
        requireNonNegative("Allowances", payroll.getAllowances(), false);
        requireNonNegative("Bonus", payroll.getBonus(), false);
        requireNonNegative("Overtime pay", payroll.getOvertimePay(), false);
        requireNonNegative("Deductions", payroll.getDeductions(), false);
        requireNonNegative("Provident fund", payroll.getProvidentFund(), false);
        requireNonNegative("Professional tax", payroll.getProfessionalTax(), false);
        requireNonNegative("Other deductions", payroll.getOtherDeductions(), false);
        requireNonNegative("Tax", payroll.getTax(), false);
        if (payroll.getStatus() == null) throw new IllegalArgumentException("Payroll status is required.");
    }

    private void requireNonNegative(String label, BigDecimal value, boolean required) {
        if (required && value == null) throw new IllegalArgumentException(label + " cannot be negative.");
        if (value != null && value.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(label + " cannot be negative.");
        }
    }

    private void calculateNetSalary(Payroll payroll) {
        BigDecimal basic = zero(payroll.getBasicSalary());
        BigDecimal allowances = zero(payroll.getAllowances());
        BigDecimal bonus = zero(payroll.getBonus());
        BigDecimal overtime = zero(payroll.getOvertimePay());
        BigDecimal deductions = zero(payroll.getDeductions());
        BigDecimal pf = zero(payroll.getProvidentFund());
        BigDecimal professionalTax = zero(payroll.getProfessionalTax());
        BigDecimal other = zero(payroll.getOtherDeductions());

        BigDecimal gross = basic.add(allowances).add(bonus).add(overtime).setScale(2, RoundingMode.HALF_UP);
        BigDecimal calculatedTax = gross.multiply(zero(taxRate)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalDeductions = deductions.add(pf).add(professionalTax).add(other).add(calculatedTax);
        BigDecimal net = gross.subtract(totalDeductions).setScale(2, RoundingMode.HALF_UP);
        if (net.compareTo(BigDecimal.ZERO) < 0) throw new RuntimeException("Net salary cannot be negative");

        payroll.setGrossSalary(gross);
        payroll.setTax(calculatedTax);
        payroll.setNetSalary(net);
    }

    private void copyAmounts(Payroll from, Payroll to) {
        to.setBasicSalary(from.getBasicSalary());
        to.setAllowances(from.getAllowances());
        to.setBonus(from.getBonus());
        to.setOvertimePay(from.getOvertimePay());
        to.setDeductions(from.getDeductions());
        to.setProvidentFund(from.getProvidentFund());
        to.setProfessionalTax(from.getProfessionalTax());
        to.setOtherDeductions(from.getOtherDeductions());
    }

    private BigDecimal totalDeductions(Payroll p) {
        return zero(p.getDeductions()).add(zero(p.getProvidentFund()))
                .add(zero(p.getProfessionalTax())).add(zero(p.getOtherDeductions())).add(zero(p.getTax()));
    }

    private BigDecimal sum(List<Payroll> rows, java.util.function.Function<Payroll, BigDecimal> getter) {
        return rows.stream().map(getter).map(this::zero).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Payroll requirePayroll(Long id) {
        return payrollRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Payroll record not found with id: " + id));
    }

    private LocalDate normalizeMonth(LocalDate month) {
        if (month == null) throw new IllegalArgumentException("Payroll month is required.");
        return month.withDayOfMonth(1);
    }

    private BigDecimal zero(BigDecimal value) { return value == null ? BigDecimal.ZERO : value; }
    private String blankToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    private String money(BigDecimal v) { return "INR " + zero(v).setScale(2, RoundingMode.HALF_UP); }
    private String safeName(Employee e) {
        if (e == null) return "Employee";
        return ((e.getFirstName() == null ? "" : e.getFirstName()) + " " + (e.getLastName() == null ? "" : e.getLastName())).trim();
    }

    private void audit(String action, Payroll payroll, String description) {
        if (auditLogService != null) {
            auditLogService.log(action, "PAYROLL", payroll == null ? null : payroll.getId(), description);
        }
    }

    private void notifyPayroll(Payroll payroll, String subject, boolean paid) {
        Employee employee = payroll.getEmployee();
        if (employee == null || employee.getId() == null) return;
        String month = payroll.getPayrollMonth() == null ? "the selected month" : payroll.getPayrollMonth().toString();
        String message = paid
                ? "Salary payment for " + month + " has been marked paid. Net salary: " + money(payroll.getNetSalary())
                : "Payroll for " + month + " has been processed. Net salary: " + money(payroll.getNetSalary());
        if (notificationService != null) {
            notificationService.createNotification(employee.getId(), subject, message, "PAYROLL");
        }
        if (emailNotificationService != null) {
            if (paid) {
                byte[] pdf = payslipPdfService.generatePayslip(payroll);
                emailNotificationService.sendWithPdfAttachment(
                        employee,
                        subject + " - Payslip",
                        message + "\n\nYour final payslip is attached. Open NexusHR > My Payroll for history.",
                        "NexusHR_Payslip_" + payroll.getId() + ".pdf",
                        pdf
                );
            } else {
                emailNotificationService.send(
                        employee,
                        subject,
                        message + "\n\nOpen NexusHR > My Payroll for complete details."
                );
            }
        }
    }
}
