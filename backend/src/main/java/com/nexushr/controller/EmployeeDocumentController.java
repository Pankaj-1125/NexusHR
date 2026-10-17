package com.nexushr.controller;

import com.nexushr.dto.EmployeeDocumentDTO;
import com.nexushr.entity.EmployeeDocument;
import com.nexushr.service.EmployeeDocumentService;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/employees/{employeeId}/documents")
public class EmployeeDocumentController {

    private final EmployeeDocumentService documentService;

    public EmployeeDocumentController(
            EmployeeDocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'MANAGER')")
    public ResponseEntity<EmployeeDocumentDTO> uploadDocument(
            @PathVariable Long employeeId,
            @RequestParam("documentName") String documentName,
            @RequestParam("documentType") String documentType,
            @RequestParam("file") MultipartFile file) {

        try {
            EmployeeDocumentDTO document =
                    documentService.uploadDocument(
                            employeeId,
                            documentName,
                            documentType,
                            file);

            return ResponseEntity.ok(document);

        } catch (IOException | RuntimeException exception) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'MANAGER')")
    public ResponseEntity<List<EmployeeDocumentDTO>> getDocuments(
            @PathVariable Long employeeId) {

        try {
            return ResponseEntity.ok(
                    documentService.getEmployeeDocuments(employeeId));

        } catch (RuntimeException exception) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/{documentId}/download")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'MANAGER')")
    public ResponseEntity<ByteArrayResource> downloadDocument(
            @PathVariable Long employeeId,
            @PathVariable Long documentId) {

        try {
            EmployeeDocument document =
                    documentService.getDocument(
                            employeeId,
                            documentId);

            ByteArrayResource resource =
                    new ByteArrayResource(
                            document.getFileData());

            MediaType mediaType;

            try {
                mediaType = MediaType.parseMediaType(
                        document.getContentType());
            } catch (Exception exception) {
                mediaType = MediaType.APPLICATION_OCTET_STREAM;
            }

            return ResponseEntity.ok()
                    .contentType(mediaType)
                    .contentLength(document.getFileSize())
                    .header(
                            HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" +
                                    document.getFileName() +
                                    "\"")
                    .body(resource);

        } catch (RuntimeException exception) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{documentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'MANAGER')")
    public ResponseEntity<Void> deleteDocument(
            @PathVariable Long employeeId,
            @PathVariable Long documentId) {

        try {
            documentService.deleteDocument(
                    employeeId,
                    documentId);

            return ResponseEntity.noContent().build();

        } catch (RuntimeException exception) {
            return ResponseEntity.notFound().build();
        }
    }
}