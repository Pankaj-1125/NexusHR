package com.nexushr.service;

import com.nexushr.entity.Employee;
import com.nexushr.entity.Payroll;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;

@Service
public class PayslipPdfService {

    private final PDType1Font regularFont =
            new PDType1Font(Standard14Fonts.FontName.HELVETICA);

    private final PDType1Font boldFont =
            new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

    public byte[] generatePayslip(Payroll payroll) {

        if (payroll == null) {
            throw new IllegalArgumentException(
                    "Payroll data is required."
            );
        }

        if (payroll.getEmployee() == null) {
            throw new IllegalArgumentException(
                    "Employee information is missing from payroll."
            );
        }

        try (
                PDDocument document = new PDDocument();
                ByteArrayOutputStream outputStream =
                        new ByteArrayOutputStream()
        ) {

            PDPage page =
                    new PDPage(PDRectangle.A4);

            document.addPage(page);

            try (
                    PDPageContentStream content =
                            new PDPageContentStream(
                                    document,
                                    page
                            )
            ) {

                Employee employee =
                        payroll.getEmployee();

                float left = 55;
                float right = 540;

                // ========================================
                // HEADER
                // ========================================

                writeText(
                        content,
                        boldFont,
                        24,
                        left,
                        790,
                        "NexusHR"
                );

                writeText(
                        content,
                        regularFont,
                        10,
                        left,
                        773,
                        "AI-Enabled Enterprise HR & Workforce Intelligence Platform"
                );

                writeText(
                        content,
                        boldFont,
                        18,
                        410,
                        790,
                        "PAYSLIP"
                );

                drawLine(
                        content,
                        left,
                        758,
                        right,
                        758
                );

                // ========================================
                // PAYROLL INFORMATION
                // ========================================

                String payrollMonth =
                        payroll.getPayrollMonth() != null
                                ? payroll.getPayrollMonth()
                                .format(
                                        DateTimeFormatter.ofPattern(
                                                "MMMM yyyy"
                                        )
                                )
                                : "-";

                writeText(
                        content,
                        boldFont,
                        11,
                        left,
                        735,
                        "Payroll Period:"
                );

                writeText(
                        content,
                        regularFont,
                        11,
                        155,
                        735,
                        payrollMonth
                );

                writeText(
                        content,
                        boldFont,
                        11,
                        350,
                        735,
                        "Status:"
                );

                writeText(
                        content,
                        regularFont,
                        11,
                        405,
                        735,
                        payroll.getStatus() != null
                                ? payroll.getStatus().name()
                                : "-"
                );

                // ========================================
                // EMPLOYEE INFORMATION
                // ========================================

                writeText(
                        content,
                        boldFont,
                        14,
                        left,
                        700,
                        "Employee Information"
                );

                drawLine(
                        content,
                        left,
                        690,
                        right,
                        690
                );

                String employeeName =
                        safe(employee.getFirstName())
                                + " "
                                + safe(employee.getLastName());

                writeLabelValue(
                        content,
                        "Employee ID",
                        employee.getId() != null
                                ? String.valueOf(employee.getId())
                                : "-",
                        left,
                        665
                );

                writeLabelValue(
                        content,
                        "Employee Name",
                        employeeName.trim(),
                        left,
                        645
                );

                writeLabelValue(
                        content,
                        "Email",
                        safe(employee.getEmail()),
                        left,
                        625
                );

                writeLabelValue(
                        content,
                        "Department",
                        safe(employee.getDepartment()),
                        left,
                        605
                );

                writeLabelValue(
                        content,
                        "Job Title",
                        safe(employee.getJobTitle()),
                        left,
                        585
                );

                // ========================================
                // SALARY DETAILS
                // ========================================

                writeText(
                        content,
                        boldFont,
                        14,
                        left,
                        545,
                        "Salary Details"
                );

                drawLine(
                        content,
                        left,
                        535,
                        right,
                        535
                );

                BigDecimal basic =
                        zero(payroll.getBasicSalary());

                BigDecimal allowances =
                        zero(payroll.getAllowances());

                BigDecimal bonus = zero(payroll.getBonus());
                BigDecimal overtimePay = zero(payroll.getOvertimePay());
                BigDecimal deductions = zero(payroll.getDeductions());
                BigDecimal providentFund = zero(payroll.getProvidentFund());
                BigDecimal professionalTax = zero(payroll.getProfessionalTax());
                BigDecimal otherDeductions = zero(payroll.getOtherDeductions());
                BigDecimal tax = zero(payroll.getTax());
                BigDecimal netSalary = zero(payroll.getNetSalary());
                BigDecimal grossSalary = payroll.getGrossSalary() != null
                        ? payroll.getGrossSalary()
                        : basic.add(allowances).add(bonus).add(overtimePay);

                float y = 505;

                writeSalaryRow(
                        content,
                        "Basic Salary",
                        basic,
                        y
                );

                y -= 28;

                writeSalaryRow(
                        content,
                        "Allowances",
                        allowances,
                        y
                );

                y -= 24;
                writeSalaryRow(content, "Bonus", bonus, y);
                y -= 24;
                writeSalaryRow(content, "Overtime Pay", overtimePay, y);
                y -= 24;

                drawLine(content, left, y + 12, right, y + 12);
                writeSalaryRow(content, "Gross Salary", grossSalary, y);
                y -= 28;

                writeSalaryRow(content, "Other Deductions", deductions, y);
                y -= 22;
                writeSalaryRow(content, "Provident Fund", providentFund, y);
                y -= 22;
                writeSalaryRow(content, "Professional Tax", professionalTax, y);
                y -= 22;
                writeSalaryRow(content, "Additional Deductions", otherDeductions, y);
                y -= 22;
                writeSalaryRow(content, "Income Tax / TDS", tax, y);
                y -= 30;

                drawLine(
                        content,
                        left,
                        y + 15,
                        right,
                        y + 15
                );

                writeText(
                        content,
                        boldFont,
                        14,
                        left,
                        y - 5,
                        "Net Salary"
                );

                writeText(
                        content,
                        boldFont,
                        14,
                        430,
                        y - 5,
                        formatMoney(netSalary)
                );

                drawLine(
                        content,
                        left,
                        y - 18,
                        right,
                        y - 18
                );

                if (payroll.getPaymentDate() != null) {
                    writeText(content, boldFont, 10, left, y - 45, "Payment Date:");
                    writeText(content, regularFont, 10, 145, y - 45, payroll.getPaymentDate().toString());
                }
                if (payroll.getPaymentReference() != null && !payroll.getPaymentReference().isBlank()) {
                    writeText(content, boldFont, 10, 300, y - 45, "Reference:");
                    writeText(content, regularFont, 10, 370, y - 45, payroll.getPaymentReference());
                }

                // ========================================
                // FOOTER
                // ========================================

                drawLine(
                        content,
                        left,
                        120,
                        right,
                        120
                );

                writeText(
                        content,
                        regularFont,
                        9,
                        left,
                        105,
                        "This is a system-generated payslip and does not require a signature."
                );

                writeText(
                        content,
                        regularFont,
                        9,
                        left,
                        88,
                        "Generated by NexusHR - Workforce Intelligence Platform"
                );
            }

            document.save(outputStream);

            return outputStream.toByteArray();

        } catch (IOException exception) {

            throw new RuntimeException(
                    "Failed to generate payslip PDF.",
                    exception
            );
        }
    }

    private void writeLabelValue(
            PDPageContentStream content,
            String label,
            String value,
            float x,
            float y
    ) throws IOException {

        writeText(
                content,
                boldFont,
                10,
                x,
                y,
                label + ":"
        );

        writeText(
                content,
                regularFont,
                10,
                x + 115,
                y,
                value
        );
    }

    private void writeSalaryRow(
            PDPageContentStream content,
            String label,
            BigDecimal value,
            float y
    ) throws IOException {

        writeText(
                content,
                regularFont,
                11,
                70,
                y,
                label
        );

        writeText(
                content,
                regularFont,
                11,
                430,
                y,
                formatMoney(value)
        );
    }

    private void writeText(
            PDPageContentStream content,
            PDType1Font font,
            float fontSize,
            float x,
            float y,
            String text
    ) throws IOException {

        content.beginText();

        content.setFont(
                font,
                fontSize
        );

        content.newLineAtOffset(
                x,
                y
        );

        content.showText(
                sanitize(text)
        );

        content.endText();
    }

    private void drawLine(
            PDPageContentStream content,
            float x1,
            float y1,
            float x2,
            float y2
    ) throws IOException {

        content.moveTo(
                x1,
                y1
        );

        content.lineTo(
                x2,
                y2
        );

        content.stroke();
    }

    private BigDecimal zero(
            BigDecimal value
    ) {

        return value != null
                ? value
                : BigDecimal.ZERO;
    }

    private String formatMoney(
            BigDecimal value
    ) {

        return "INR "
                + zero(value)
                .setScale(
                        2,
                        RoundingMode.HALF_UP
                )
                .toPlainString();
    }

    private String safe(
            String value
    ) {

        if (value == null || value.isBlank()) {
            return "-";
        }

        return value;
    }

    private String sanitize(
            String value
    ) {

        if (value == null) {
            return "-";
        }

        return value
                .replace("\n", " ")
                .replace("\r", " ")
                .replace("₹", "INR ");
    }
}