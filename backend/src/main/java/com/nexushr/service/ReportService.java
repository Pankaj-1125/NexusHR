package com.nexushr.service;

import com.nexushr.dto.DashboardStatsDTO;
import com.nexushr.dto.ReportSummaryDTO;
import com.nexushr.entity.Payroll;
import com.nexushr.entity.PerformanceReview;
import com.nexushr.repository.PayrollRepository;
import com.nexushr.repository.PerformanceReviewRepository;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class ReportService {

    private static final Color NAVY = new Color(15, 23, 42);
    private static final Color BLUE = new Color(37, 99, 235);
    private static final Color BLUE_DARK = new Color(29, 78, 216);
    private static final Color SLATE = new Color(71, 85, 105);
    private static final Color MUTED = new Color(100, 116, 139);
    private static final Color BORDER = new Color(226, 232, 240);
    private static final Color SURFACE = new Color(248, 250, 252);
    private static final Color SUCCESS_BG = new Color(236, 253, 245);
    private static final Color SUCCESS_TEXT = new Color(5, 150, 105);
    private static final Color WARNING_BG = new Color(255, 251, 235);
    private static final Color WARNING_TEXT = new Color(217, 119, 6);
    private static final Color PURPLE_BG = new Color(245, 243, 255);
    private static final Color PURPLE_TEXT = new Color(124, 58, 237);

    private static final float PAGE_MARGIN = 42f;
    private static final float PAGE_WIDTH = PDRectangle.A4.getWidth();
    private static final float CONTENT_WIDTH = PAGE_WIDTH - (PAGE_MARGIN * 2);

    private final DashboardService dashboardService;
    private final PayrollRepository payrollRepository;
    private final PerformanceReviewRepository performanceReviewRepository;
    private final AuditLogService auditLogService;

    public ReportService(
            DashboardService dashboardService,
            PayrollRepository payrollRepository,
            PerformanceReviewRepository performanceReviewRepository,
            AuditLogService auditLogService) {

        this.dashboardService = dashboardService;
        this.payrollRepository = payrollRepository;
        this.performanceReviewRepository = performanceReviewRepository;
        this.auditLogService = auditLogService;
    }

    public ReportSummaryDTO getSummary() {

        DashboardStatsDTO dashboard =
                dashboardService.getDashboardStats();

        List<Payroll> payrolls =
                payrollRepository.findAll();

        List<PerformanceReview> reviews =
                performanceReviewRepository.findAll();

        ReportSummaryDTO result =
                new ReportSummaryDTO();

        result.setTotalEmployees(
                dashboard.getTotalEmployees());

        result.setActiveEmployees(
                dashboard.getActiveEmployees());

        result.setInactiveEmployees(
                dashboard.getInactiveEmployees());

        result.setTodayPresent(
                dashboard.getTodayPresent());

        result.setTodayAbsent(
                dashboard.getTodayAbsent());

        result.setPendingLeaves(
                dashboard.getPendingLeaves());

        result.setApprovedLeaves(
                dashboard.getApprovedLeaves());

        result.setDepartmentStats(
                dashboard.getDepartmentStats());

        result.setTotalPayrollRecords(
                payrolls.size());

        result.setDraftPayrolls(
                countPayrollStatus(payrolls, "DRAFT"));

        result.setProcessedPayrolls(
                countPayrollStatus(payrolls, "PROCESSED"));

        result.setPaidPayrolls(
                countPayrollStatus(payrolls, "PAID"));

        result.setTotalPerformanceReviews(
                reviews.size());

        result.setDraftPerformanceReviews(
                countReviewStatus(reviews, "DRAFT"));

        result.setSubmittedPerformanceReviews(
                countReviewStatus(reviews, "SUBMITTED"));

        result.setCompletedPerformanceReviews(
                countReviewStatus(reviews, "COMPLETED"));

        double averageRating =
                reviews.stream()
                        .filter(review -> review.getRating() != null)
                        .mapToInt(PerformanceReview::getRating)
                        .average()
                        .orElse(0.0);

        result.setAveragePerformanceRating(
                Math.round(averageRating * 100.0) / 100.0);

        return result;
    }

    // =========================================================
    // PROFESSIONAL PDF REPORT
    // =========================================================

    public byte[] generatePdfReport() throws IOException {

        ReportSummaryDTO summary = getSummary();

        try (PDDocument document = new PDDocument()) {

            PDType1Font regularFont =
                    new PDType1Font(Standard14Fonts.FontName.HELVETICA);

            PDType1Font boldFont =
                    new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

            // PAGE 1 --------------------------------------------------
            PDPage page1 = new PDPage(PDRectangle.A4);
            document.addPage(page1);

            try (PDPageContentStream content =
                         new PDPageContentStream(document, page1)) {

                drawBrandHeader(content, regularFont, boldFont);

                float y = 706f;

                drawText(content, boldFont, 19f, PAGE_MARGIN, y,
                        "Workforce Management Report", NAVY);

                y -= 22f;

                drawText(content, regularFont, 9f, PAGE_MARGIN, y,
                        "Generated on " + LocalDate.now()
                                .format(DateTimeFormatter.ofPattern("dd MMM yyyy"))
                                + "  |  Enterprise workforce summary",
                        MUTED);

                y -= 31f;

                drawSectionHeading(content, boldFont, "Executive Snapshot", y);
                y -= 34f;

                float cardGap = 10f;
                float cardWidth = (CONTENT_WIDTH - (cardGap * 2)) / 3f;

                drawMetricCard(
                        content,
                        regularFont,
                        boldFont,
                        PAGE_MARGIN,
                        y,
                        cardWidth,
                        "TOTAL EMPLOYEES",
                        String.valueOf(summary.getTotalEmployees()),
                        "Current workforce",
                        BLUE,
                        new Color(239, 246, 255));

                drawMetricCard(
                        content,
                        regularFont,
                        boldFont,
                        PAGE_MARGIN + cardWidth + cardGap,
                        y,
                        cardWidth,
                        "ACTIVE EMPLOYEES",
                        String.valueOf(summary.getActiveEmployees()),
                        "Currently active",
                        SUCCESS_TEXT,
                        SUCCESS_BG);

                drawMetricCard(
                        content,
                        regularFont,
                        boldFont,
                        PAGE_MARGIN + ((cardWidth + cardGap) * 2),
                        y,
                        cardWidth,
                        "PRESENT TODAY",
                        String.valueOf(summary.getTodayPresent()),
                        "Attendance today",
                        PURPLE_TEXT,
                        PURPLE_BG);

                y -= 103f;

                drawSectionHeading(content, boldFont, "Attendance & Leave", y);
                y -= 28f;

                List<String[]> attendanceRows = new ArrayList<>();
                attendanceRows.add(new String[]{"Metric", "Value", "Status"});
                attendanceRows.add(new String[]{"Present Today",
                        String.valueOf(summary.getTodayPresent()), "Recorded"});
                attendanceRows.add(new String[]{"Absent Today",
                        String.valueOf(summary.getTodayAbsent()), "Recorded"});
                attendanceRows.add(new String[]{"Pending Leave Requests",
                        String.valueOf(summary.getPendingLeaves()), "Pending"});
                attendanceRows.add(new String[]{"Approved Leave Requests",
                        String.valueOf(summary.getApprovedLeaves()), "Approved"});

                y = drawThreeColumnTable(
                        content,
                        regularFont,
                        boldFont,
                        PAGE_MARGIN,
                        y,
                        CONTENT_WIDTH,
                        attendanceRows);

                y -= 22f;

                drawSectionHeading(content, boldFont, "Payroll Overview", y);
                y -= 28f;

                List<String[]> payrollRows = new ArrayList<>();
                payrollRows.add(new String[]{"Payroll Status", "Records", "Interpretation"});
                payrollRows.add(new String[]{"Total Payroll Records",
                        String.valueOf(summary.getTotalPayrollRecords()), "All records"});
                payrollRows.add(new String[]{"Draft",
                        String.valueOf(summary.getDraftPayrolls()), "In preparation"});
                payrollRows.add(new String[]{"Processed",
                        String.valueOf(summary.getProcessedPayrolls()), "Processed"});
                payrollRows.add(new String[]{"Paid",
                        String.valueOf(summary.getPaidPayrolls()), "Completed"});

                drawThreeColumnTable(
                        content,
                        regularFont,
                        boldFont,
                        PAGE_MARGIN,
                        y,
                        CONTENT_WIDTH,
                        payrollRows);

                drawFooter(content, regularFont, 1);
            }

            // PAGE 2 --------------------------------------------------
            PDPage page2 = new PDPage(PDRectangle.A4);
            document.addPage(page2);

            try (PDPageContentStream content =
                         new PDPageContentStream(document, page2)) {

                drawSmallHeader(content, regularFont, boldFont, "Performance & Workforce Distribution");

                float y = 744f;

                drawSectionHeading(content, boldFont, "Performance Summary", y);
                y -= 28f;

                List<String[]> performanceRows = new ArrayList<>();
                performanceRows.add(new String[]{"Performance Metric", "Value", "Status"});
                performanceRows.add(new String[]{"Total Reviews",
                        String.valueOf(summary.getTotalPerformanceReviews()), "All reviews"});
                performanceRows.add(new String[]{"Draft Reviews",
                        String.valueOf(summary.getDraftPerformanceReviews()), "In progress"});
                performanceRows.add(new String[]{"Submitted Reviews",
                        String.valueOf(summary.getSubmittedPerformanceReviews()), "Awaiting completion"});
                performanceRows.add(new String[]{"Completed Reviews",
                        String.valueOf(summary.getCompletedPerformanceReviews()), "Completed"});
                performanceRows.add(new String[]{"Average Rating",
                        String.format("%.2f / 5", summary.getAveragePerformanceRating()),
                        ratingLabel(summary.getAveragePerformanceRating())});

                y = drawThreeColumnTable(
                        content,
                        regularFont,
                        boldFont,
                        PAGE_MARGIN,
                        y,
                        CONTENT_WIDTH,
                        performanceRows);

                y -= 28f;

                drawSectionHeading(content, boldFont, "Employees by Department", y);
                y -= 28f;

                List<String[]> departmentRows = new ArrayList<>();
                departmentRows.add(new String[]{"Department", "Employees", "Share"});

                long totalEmployees = Math.max(1, summary.getTotalEmployees());

                if (summary.getDepartmentStats() != null
                        && !summary.getDepartmentStats().isEmpty()) {

                    summary.getDepartmentStats()
                            .entrySet()
                            .stream()
                            .sorted(Map.Entry.comparingByKey())
                            .forEach(entry -> {
                                double share =
                                        (entry.getValue() * 100.0) / totalEmployees;

                                departmentRows.add(new String[]{
                                        entry.getKey(),
                                        String.valueOf(entry.getValue()),
                                        String.format("%.1f%%", share)
                                });
                            });
                } else {
                    departmentRows.add(new String[]{"No department data", "0", "0.0%"});
                }

                y = drawThreeColumnTable(
                        content,
                        regularFont,
                        boldFont,
                        PAGE_MARGIN,
                        y,
                        CONTENT_WIDTH,
                        departmentRows);

                y -= 30f;

                drawSectionHeading(content, boldFont, "Report Notes", y);
                y -= 30f;

                drawInfoBox(
                        content,
                        regularFont,
                        boldFont,
                        PAGE_MARGIN,
                        y,
                        CONTENT_WIDTH,
                        "Data scope",
                        "This report summarizes the current NexusHR employee, attendance, leave, payroll, performance and department data available at generation time.");

                y -= 78f;

                drawInfoBox(
                        content,
                        regularFont,
                        boldFont,
                        PAGE_MARGIN,
                        y,
                        CONTENT_WIDTH,
                        "Confidentiality",
                        "Internal HR information. Share only with authorized personnel according to your organization's data-handling policies.");

                drawFooter(content, regularFont, 2);
            }

            ByteArrayOutputStream output =
                    new ByteArrayOutputStream();

            document.save(output);

            auditLogService.log(
                    "REPORT_EXPORTED_PDF",
                    "REPORT",
                    0L,
                    "Professional workforce report exported as PDF");

            return output.toByteArray();
        }
    }

    private void drawBrandHeader(
            PDPageContentStream content,
            PDType1Font regularFont,
            PDType1Font boldFont) throws IOException {

        content.setNonStrokingColor(NAVY);
        content.addRect(0, 756, PAGE_WIDTH, 86);
        content.fill();

        content.setNonStrokingColor(BLUE);
        content.addRect(0, 756, 7, 86);
        content.fill();

        drawText(content, boldFont, 24f, PAGE_MARGIN, 806,
                "NexusHR", Color.WHITE);

        drawText(content, regularFont, 9.5f, PAGE_MARGIN, 786,
                "AI-Enabled Enterprise HR & Workforce Intelligence Platform",
                new Color(203, 213, 225));

        drawText(content, boldFont, 8.5f, PAGE_WIDTH - 132, 806,
                "HR ANALYTICS", new Color(147, 197, 253));

        drawText(content, regularFont, 8.5f, PAGE_WIDTH - 132, 790,
                "CONFIDENTIAL", new Color(203, 213, 225));
    }

    private void drawSmallHeader(
            PDPageContentStream content,
            PDType1Font regularFont,
            PDType1Font boldFont,
            String subtitle) throws IOException {

        content.setNonStrokingColor(NAVY);
        content.addRect(0, 786, PAGE_WIDTH, 56);
        content.fill();

        content.setNonStrokingColor(BLUE);
        content.addRect(0, 786, 6, 56);
        content.fill();

        drawText(content, boldFont, 17f, PAGE_MARGIN, 816,
                "NexusHR", Color.WHITE);

        drawText(content, regularFont, 8.5f, PAGE_MARGIN, 800,
                subtitle, new Color(203, 213, 225));
    }

    private void drawSectionHeading(
            PDPageContentStream content,
            PDType1Font boldFont,
            String title,
            float y) throws IOException {

        content.setNonStrokingColor(BLUE);
        content.addRect(PAGE_MARGIN, y - 5, 4, 19);
        content.fill();

        drawText(content, boldFont, 12.5f,
                PAGE_MARGIN + 12, y,
                title, NAVY);
    }

    private void drawMetricCard(
            PDPageContentStream content,
            PDType1Font regularFont,
            PDType1Font boldFont,
            float x,
            float y,
            float width,
            String label,
            String value,
            String subtitle,
            Color accent,
            Color background) throws IOException {

        float height = 78f;
        float bottom = y - height;

        content.setNonStrokingColor(background);
        content.addRect(x, bottom, width, height);
        content.fill();

        content.setStrokingColor(BORDER);
        content.setLineWidth(0.8f);
        content.addRect(x, bottom, width, height);
        content.stroke();

        content.setNonStrokingColor(accent);
        content.addRect(x, bottom, 4f, height);
        content.fill();

        drawText(content, boldFont, 7.5f,
                x + 12, y - 18,
                label, SLATE);

        drawText(content, boldFont, 22f,
                x + 12, y - 46,
                value, NAVY);

        drawText(content, regularFont, 7.5f,
                x + 12, y - 64,
                subtitle, MUTED);
    }

    private float drawThreeColumnTable(
            PDPageContentStream content,
            PDType1Font regularFont,
            PDType1Font boldFont,
            float x,
            float y,
            float width,
            List<String[]> rows) throws IOException {

        float rowHeight = 24f;
        float col1 = width * 0.55f;
        float col2 = width * 0.18f;
        float col3 = width - col1 - col2;

        for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {

            float rowBottom = y - rowHeight;
            boolean header = rowIndex == 0;

            if (header) {
                content.setNonStrokingColor(NAVY);
                content.addRect(x, rowBottom, width, rowHeight);
                content.fill();
            } else if (rowIndex % 2 == 0) {
                content.setNonStrokingColor(SURFACE);
                content.addRect(x, rowBottom, width, rowHeight);
                content.fill();
            }

            content.setStrokingColor(BORDER);
            content.setLineWidth(0.6f);

            content.addRect(x, rowBottom, col1, rowHeight);
            content.stroke();

            content.addRect(x + col1, rowBottom, col2, rowHeight);
            content.stroke();

            content.addRect(x + col1 + col2, rowBottom, col3, rowHeight);
            content.stroke();

            Color textColor = header ? Color.WHITE : NAVY;
            PDType1Font rowFont = header ? boldFont : regularFont;

            drawText(content, rowFont, 8.5f,
                    x + 8, rowBottom + 8,
                    rows.get(rowIndex)[0], textColor);

            drawText(content, rowFont, 8.5f,
                    x + col1 + 8, rowBottom + 8,
                    rows.get(rowIndex)[1], textColor);

            drawText(content, rowFont, 8.5f,
                    x + col1 + col2 + 8, rowBottom + 8,
                    rows.get(rowIndex)[2], textColor);

            y -= rowHeight;
        }

        return y;
    }

    private void drawInfoBox(
            PDPageContentStream content,
            PDType1Font regularFont,
            PDType1Font boldFont,
            float x,
            float y,
            float width,
            String heading,
            String body) throws IOException {

        float height = 62f;
        float bottom = y - height;

        content.setNonStrokingColor(SURFACE);
        content.addRect(x, bottom, width, height);
        content.fill();

        content.setStrokingColor(BORDER);
        content.setLineWidth(0.8f);
        content.addRect(x, bottom, width, height);
        content.stroke();

        content.setNonStrokingColor(BLUE);
        content.addRect(x, bottom, 4f, height);
        content.fill();

        drawText(content, boldFont, 9.5f,
                x + 14, y - 18,
                heading, NAVY);

        List<String> lines = wrapText(body, 92);
        float lineY = y - 36;

        for (String line : lines) {
            drawText(content, regularFont, 8f,
                    x + 14, lineY,
                    line, SLATE);
            lineY -= 11f;
        }
    }

    private void drawFooter(
            PDPageContentStream content,
            PDType1Font regularFont,
            int pageNumber) throws IOException {

        content.setStrokingColor(BORDER);
        content.setLineWidth(0.7f);
        content.moveTo(PAGE_MARGIN, 42);
        content.lineTo(PAGE_WIDTH - PAGE_MARGIN, 42);
        content.stroke();

        drawText(content, regularFont, 7.5f,
                PAGE_MARGIN, 27,
                "NexusHR  |  Internal HR Report", MUTED);

        drawText(content, regularFont, 7.5f,
                PAGE_WIDTH - 91, 27,
                "Page " + pageNumber, MUTED);
    }

    private void drawText(
            PDPageContentStream content,
            PDType1Font font,
            float fontSize,
            float x,
            float y,
            String text,
            Color color) throws IOException {

        content.setNonStrokingColor(color);
        content.beginText();
        content.setFont(font, fontSize);
        content.newLineAtOffset(x, y);
        content.showText(sanitizePdfText(text));
        content.endText();
    }

    private List<String> wrapText(String text, int maxCharacters) {

        List<String> lines = new ArrayList<>();

        if (text == null || text.isBlank()) {
            lines.add("");
            return lines;
        }

        String[] words = text.trim().split("\\s+");
        StringBuilder current = new StringBuilder();

        for (String word : words) {
            if (current.length() == 0) {
                current.append(word);
            } else if (current.length() + 1 + word.length() <= maxCharacters) {
                current.append(' ').append(word);
            } else {
                lines.add(current.toString());
                current = new StringBuilder(word);
            }
        }

        if (current.length() > 0) {
            lines.add(current.toString());
        }

        return lines;
    }

    private String ratingLabel(double rating) {
        if (rating >= 4.5) {
            return "Excellent";
        }
        if (rating >= 3.5) {
            return "Strong";
        }
        if (rating >= 2.5) {
            return "Developing";
        }
        if (rating > 0) {
            return "Needs attention";
        }
        return "No rating yet";
    }

    private String sanitizePdfText(String value) {

        if (value == null) {
            return "";
        }

        return value
                .replace("₹", "INR ")
                .replace("–", "-")
                .replace("—", "-")
                .replace("’", "'")
                .replace("“", "\"")
                .replace("”", "\"")
                .replace("•", "-");
    }

    // =========================================================
    // EXCEL EXPORT
    // =========================================================

    public byte[] generateExcelReport() throws IOException {

        ReportSummaryDTO summary = getSummary();

        try (Workbook workbook = new XSSFWorkbook()) {

            Sheet summarySheet = workbook.createSheet("Summary");

            CellStyle titleStyle = workbook.createCellStyle();
            Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 16);
            titleStyle.setFont(titleFont);

            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            int rowIndex = 0;

            Row titleRow = summarySheet.createRow(rowIndex++);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("NexusHR Workforce Report");
            titleCell.setCellStyle(titleStyle);

            Row generatedRow = summarySheet.createRow(rowIndex++);
            generatedRow.createCell(0).setCellValue("Generated");
            generatedRow.createCell(1).setCellValue(LocalDate.now().toString());

            rowIndex++;

            rowIndex = addSection(
                    summarySheet,
                    headerStyle,
                    rowIndex,
                    "Workforce Summary",
                    new Object[][]{
                            {"Total Employees", summary.getTotalEmployees()},
                            {"Active Employees", summary.getActiveEmployees()},
                            {"Inactive Employees", summary.getInactiveEmployees()}
                    });

            rowIndex = addSection(
                    summarySheet,
                    headerStyle,
                    rowIndex,
                    "Attendance Summary",
                    new Object[][]{
                            {"Present Today", summary.getTodayPresent()},
                            {"Absent Today", summary.getTodayAbsent()}
                    });

            rowIndex = addSection(
                    summarySheet,
                    headerStyle,
                    rowIndex,
                    "Leave Summary",
                    new Object[][]{
                            {"Pending Leaves", summary.getPendingLeaves()},
                            {"Approved Leaves", summary.getApprovedLeaves()}
                    });

            rowIndex = addSection(
                    summarySheet,
                    headerStyle,
                    rowIndex,
                    "Payroll Summary",
                    new Object[][]{
                            {"Total Payroll Records", summary.getTotalPayrollRecords()},
                            {"Draft", summary.getDraftPayrolls()},
                            {"Processed", summary.getProcessedPayrolls()},
                            {"Paid", summary.getPaidPayrolls()}
                    });

            addSection(
                    summarySheet,
                    headerStyle,
                    rowIndex,
                    "Performance Summary",
                    new Object[][]{
                            {"Total Reviews", summary.getTotalPerformanceReviews()},
                            {"Draft", summary.getDraftPerformanceReviews()},
                            {"Submitted", summary.getSubmittedPerformanceReviews()},
                            {"Completed", summary.getCompletedPerformanceReviews()},
                            {"Average Rating", summary.getAveragePerformanceRating()}
                    });

            summarySheet.autoSizeColumn(0);
            summarySheet.autoSizeColumn(1);

            Sheet departmentSheet = workbook.createSheet("Departments");

            Row departmentHeader = departmentSheet.createRow(0);

            Cell departmentCell = departmentHeader.createCell(0);
            departmentCell.setCellValue("Department");
            departmentCell.setCellStyle(headerStyle);

            Cell countCell = departmentHeader.createCell(1);
            countCell.setCellValue("Employees");
            countCell.setCellStyle(headerStyle);

            Cell shareCell = departmentHeader.createCell(2);
            shareCell.setCellValue("Share");
            shareCell.setCellStyle(headerStyle);

            int departmentRow = 1;
            long total = Math.max(1, summary.getTotalEmployees());

            if (summary.getDepartmentStats() != null) {
                for (Map.Entry<String, Long> entry :
                        summary.getDepartmentStats().entrySet()) {

                    Row row = departmentSheet.createRow(departmentRow++);
                    row.createCell(0).setCellValue(entry.getKey());
                    row.createCell(1).setCellValue(entry.getValue());
                    row.createCell(2).setCellValue(
                            String.format("%.1f%%", (entry.getValue() * 100.0) / total));
                }
            }

            departmentSheet.autoSizeColumn(0);
            departmentSheet.autoSizeColumn(1);
            departmentSheet.autoSizeColumn(2);

            ByteArrayOutputStream output = new ByteArrayOutputStream();
            workbook.write(output);

            auditLogService.log(
                    "REPORT_EXPORTED_EXCEL",
                    "REPORT",
                    0L,
                    "Workforce summary report exported as Excel");

            return output.toByteArray();
        }
    }

    private long countPayrollStatus(
            List<Payroll> payrolls,
            String expected) {

        return payrolls.stream()
                .filter(payroll ->
                        payroll.getStatus() != null
                                && expected.equalsIgnoreCase(
                                String.valueOf(payroll.getStatus())))
                .count();
    }

    private long countReviewStatus(
            List<PerformanceReview> reviews,
            String expected) {

        return reviews.stream()
                .filter(review ->
                        review.getStatus() != null
                                && expected.equalsIgnoreCase(
                                String.valueOf(review.getStatus())))
                .count();
    }

    private int addSection(
            Sheet sheet,
            CellStyle headerStyle,
            int rowIndex,
            String title,
            Object[][] values) {

        Row headingRow = sheet.createRow(rowIndex++);
        Cell headingCell = headingRow.createCell(0);
        headingCell.setCellValue(title);
        headingCell.setCellStyle(headerStyle);

        for (Object[] value : values) {

            Row row = sheet.createRow(rowIndex++);
            row.createCell(0).setCellValue(String.valueOf(value[0]));

            Object cellValue = value[1];

            if (cellValue instanceof Number number) {
                row.createCell(1).setCellValue(number.doubleValue());
            } else {
                row.createCell(1).setCellValue(String.valueOf(cellValue));
            }
        }

        return rowIndex + 1;
    }
}
