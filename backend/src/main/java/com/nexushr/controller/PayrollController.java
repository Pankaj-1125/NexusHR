package com.nexushr.controller;

import com.nexushr.entity.Payroll;
import com.nexushr.service.PayrollService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/payroll")
public class PayrollController {
    private final PayrollService payrollService;
    public PayrollController(PayrollService payrollService) { this.payrollService = payrollService; }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER')")
    public ResponseEntity<List<Payroll>> getAllPayrolls() { return ResponseEntity.ok(payrollService.getAllPayrolls()); }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER')")
    public ResponseEntity<Payroll> getPayrollById(@PathVariable Long id) {
        return payrollService.getPayrollById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/employee/{employeeId}")
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER')")
    public ResponseEntity<List<Payroll>> getPayrollsByEmployee(@PathVariable Long employeeId) {
        return ResponseEntity.ok(payrollService.getPayrollsByEmployee(employeeId));
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER')")
    public ResponseEntity<Map<String,Object>> getSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate month) {
        return ResponseEntity.ok(payrollService.getSummary(month));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER')")
    public ResponseEntity<Payroll> createPayroll(@RequestBody Payroll payroll) {
        return ResponseEntity.ok(payrollService.createPayroll(payroll));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER')")
    public ResponseEntity<Payroll> updatePayroll(@PathVariable Long id, @RequestBody Payroll payroll) {
        return ResponseEntity.ok(payrollService.updatePayroll(id, payroll));
    }

    @PostMapping("/batch")
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER')")
    public ResponseEntity<Map<String,Object>> runBatch(@RequestBody BatchRequest request) {
        LocalDate month = request.month() == null ? LocalDate.now() : request.month();
        return ResponseEntity.ok(payrollService.runMonthlyBatch(month, request.autoProcess(), request.notifyEmployee()));
    }

    @PostMapping("/{id}/process")
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER')")
    public ResponseEntity<Payroll> process(@PathVariable Long id, @RequestBody(required = false) ActionRequest request) {
        return ResponseEntity.ok(payrollService.processPayroll(id, request != null && request.notifyEmployee()));
    }

    @PostMapping("/{id}/paid")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Payroll> markPaid(@PathVariable Long id, @RequestBody PaidRequest request) {
        return ResponseEntity.ok(payrollService.markPaid(id, request.paymentReference(), request.paymentDate(), request.notifyEmployee()));
    }

    @PostMapping("/{id}/email-payslip")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public ResponseEntity<Map<String,Object>> emailPayslip(@PathVariable Long id, @RequestBody(required = false) EmailPayslipRequest request) {
        return ResponseEntity.ok(payrollService.emailPayslip(id, request == null ? null : request.subject()));
    }

    @GetMapping(value = "/{id}/payslip", produces = MediaType.APPLICATION_PDF_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER')")
    public ResponseEntity<byte[]> downloadPayslip(@PathVariable Long id) {
        Payroll payroll = payrollService.getPayrollById(id)
                .orElseThrow(() -> new RuntimeException("Payroll record not found with id: " + id));
        byte[] pdf = payrollService.generatePayslip(id);
        String employeeName = "employee";
        if (payroll.getEmployee() != null) {
            String first = payroll.getEmployee().getFirstName() == null ? "" : payroll.getEmployee().getFirstName();
            String last = payroll.getEmployee().getLastName() == null ? "" : payroll.getEmployee().getLastName();
            employeeName = (first + "-" + last).trim().replaceAll("\\s+", "-").replaceAll("[^a-zA-Z0-9-]", "");
        }
        if (employeeName.isBlank()) employeeName = "employee";
        String month = payroll.getPayrollMonth() == null ? "payroll" : payroll.getPayrollMonth().toString();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=NexusHR-Payslip-" + employeeName + "-" + month + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deletePayroll(@PathVariable Long id) {
        payrollService.deletePayroll(id);
        return ResponseEntity.noContent().build();
    }

    public record BatchRequest(LocalDate month, boolean autoProcess, boolean notifyEmployee) {}
    public record ActionRequest(boolean notifyEmployee) {}
    public record PaidRequest(String paymentReference, LocalDate paymentDate, boolean notifyEmployee) {}
    public record EmailPayslipRequest(String subject) {}
}
