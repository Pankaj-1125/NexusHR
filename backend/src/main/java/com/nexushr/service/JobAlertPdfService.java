package com.nexushr.service;

import com.nexushr.entity.Job;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class JobAlertPdfService {

    private final PDType1Font regular =
            new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    private final PDType1Font bold =
            new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

    public byte[] generate(Job job, String subject, String senderUsername, String senderRole) {
        if (job == null) {
            throw new IllegalArgumentException("Job is required");
        }

        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {

            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                float left = 52;
                float y = 790;
                float width = 490;

                text(content, bold, 24, left, y, "NexusHR");
                y -= 20;
                text(content, regular, 10, left, y,
                        "AI-Enabled Enterprise HR & Workforce Intelligence Platform");
                y -= 26;
                line(content, left, y, left + width, y);
                y -= 28;

                text(content, bold, 18, left, y,
                        safe(subject, "Internal Job Opportunity"));
                y -= 30;

                y = labelValue(content, "Position", job.getTitle(), left, y);
                y = labelValue(content, "Department", job.getDepartment(), left, y);
                y = labelValue(content, "Location", job.getLocation(), left, y);
                y = labelValue(content, "Employment Type",
                        job.getEmploymentType() == null ? "-" : job.getEmploymentType().name(), left, y);
                y = labelValue(content, "Status",
                        job.getStatus() == null ? "-" : job.getStatus().name(), left, y);
                y = labelValue(content, "Posted Date",
                        job.getPostedDate() == null ? "-" : job.getPostedDate().toString(), left, y);
                y = labelValue(content, "Closing Date",
                        job.getClosingDate() == null ? "Open until filled" : job.getClosingDate().toString(), left, y);
                y = labelValue(content, "Salary Range", salary(job), left, y);

                y -= 8;
                text(content, bold, 13, left, y, "Job Description");
                y -= 18;
                y = wrapped(content, regular, 10, left, y, width,
                        safe(job.getDescription(), "No description provided."));

                y -= 12;
                text(content, bold, 13, left, y, "Requirements");
                y -= 18;
                y = wrapped(content, regular, 10, left, y, width,
                        safe(job.getRequirements(), "No requirements provided."));

                y -= 20;
                line(content, left, y, left + width, y);
                y -= 18;
                text(content, regular, 9, left, y,
                        "Published by: " + safe(senderUsername, "SYSTEM")
                                + " (" + safe(senderRole, "SYSTEM") + ")");
                y -= 14;
                text(content, regular, 9, left, y,
                        "This document was generated automatically by NexusHR Recruitment.");
            }

            document.save(output);
            return output.toByteArray();

        } catch (IOException ex) {
            throw new RuntimeException("Unable to generate job alert PDF", ex);
        }
    }

    private float labelValue(PDPageContentStream content, String label, String value,
                             float x, float y) throws IOException {
        text(content, bold, 10, x, y, label + ":");
        text(content, regular, 10, x + 115, y, safe(value, "-"));
        return y - 18;
    }

    private float wrapped(PDPageContentStream content, PDType1Font font, float size,
                          float x, float y, float maxWidth, String value) throws IOException {
        for (String line : wrap(font, size, cleanPdfText(value), maxWidth)) {
            if (y < 80) break;
            text(content, font, size, x, y, line);
            y -= 14;
        }
        return y;
    }

    private List<String> wrap(PDType1Font font, float size, String text, float maxWidth)
            throws IOException {
        List<String> lines = new ArrayList<>();
        String normalized = text == null ? "" : text.replace("\r", "");
        for (String paragraph : normalized.split("\n")) {
            StringBuilder current = new StringBuilder();
            for (String word : paragraph.split("\\s+")) {
                if (word.isBlank()) continue;
                String candidate = current.isEmpty() ? word : current + " " + word;
                float width = font.getStringWidth(candidate) / 1000f * size;
                if (width > maxWidth && !current.isEmpty()) {
                    lines.add(current.toString());
                    current = new StringBuilder(word);
                } else {
                    current = new StringBuilder(candidate);
                }
            }
            if (!current.isEmpty()) lines.add(current.toString());
            if (paragraph.isBlank()) lines.add("");
        }
        return lines;
    }

    private void text(PDPageContentStream content, PDType1Font font, float size,
                      float x, float y, String value) throws IOException {
        content.beginText();
        content.setFont(font, size);
        content.newLineAtOffset(x, y);
        content.showText(cleanPdfText(value));
        content.endText();
    }

    private void line(PDPageContentStream content, float x1, float y1,
                      float x2, float y2) throws IOException {
        content.moveTo(x1, y1);
        content.lineTo(x2, y2);
        content.stroke();
    }

    private String salary(Job job) {
        if (job.getSalaryMin() == null && job.getSalaryMax() == null) return "Not specified";
        if (job.getSalaryMin() != null && job.getSalaryMax() != null) {
            return "INR " + job.getSalaryMin().toPlainString() + " - INR " + job.getSalaryMax().toPlainString();
        }
        if (job.getSalaryMin() != null) return "From INR " + job.getSalaryMin().toPlainString();
        return "Up to INR " + job.getSalaryMax().toPlainString();
    }

    private String safe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private String cleanPdfText(String value) {
        return safe(value, "-")
                .replace("₹", "INR ")
                .replace("•", "-")
                .replace("–", "-")
                .replace("—", "-")
                .replaceAll("[^\\x20-\\x7E]", "?");
    }
}
