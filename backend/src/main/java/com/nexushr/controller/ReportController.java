package com.nexushr.controller;

import com.nexushr.dto.ReportSummaryDTO;
import com.nexushr.service.ReportService;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(
            ReportService reportService
    ) {
        this.reportService = reportService;
    }

    @GetMapping("/summary")
    public ResponseEntity<ReportSummaryDTO>
    getSummary() {

        return ResponseEntity.ok(
                reportService.getSummary()
        );
    }

    @GetMapping(
            value = "/export/pdf",
            produces = MediaType.APPLICATION_PDF_VALUE
    )
    public ResponseEntity<byte[]>
    exportPdf() throws IOException {

        byte[] pdf =
                reportService.generatePdfReport();

        String filename =
                "nexushr-workforce-report-"
                        + LocalDate.now()
                        + ".pdf";

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition
                                .attachment()
                                .filename(filename)
                                .build()
                                .toString()
                )
                .contentType(
                        MediaType.APPLICATION_PDF
                )
                .body(pdf);
    }

    @GetMapping(
            value = "/export/excel",
            produces =
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    )
    public ResponseEntity<byte[]>
    exportExcel() throws IOException {

        byte[] excel =
                reportService.generateExcelReport();

        String filename =
                "nexushr-workforce-report-"
                        + LocalDate.now()
                        + ".xlsx";

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition
                                .attachment()
                                .filename(filename)
                                .build()
                                .toString()
                )
                .contentType(
                        MediaType.parseMediaType(
                                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                        )
                )
                .body(excel);
    }
}
