package com.nexushr.service;

import com.nexushr.dto.EmployeeDocumentDTO;
import com.nexushr.entity.Employee;
import com.nexushr.entity.EmployeeDocument;
import com.nexushr.repository.EmployeeDocumentRepository;
import com.nexushr.repository.EmployeeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class EmployeeDocumentService {

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;

    private final EmployeeDocumentRepository documentRepository;
    private final EmployeeRepository employeeRepository;

    public EmployeeDocumentService(
            EmployeeDocumentRepository documentRepository,
            EmployeeRepository employeeRepository) {
        this.documentRepository = documentRepository;
        this.employeeRepository = employeeRepository;
    }

    @Transactional
    public EmployeeDocumentDTO uploadDocument(
            Long employeeId,
            String documentName,
            String documentType,
            MultipartFile file) throws IOException {

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Employee not found with ID: " + employeeId));

        if (file == null || file.isEmpty()) {
            throw new RuntimeException("File is required");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new RuntimeException(
                    "File size must not exceed 10 MB");
        }

        if (documentName == null || documentName.isBlank()) {
            throw new RuntimeException("Document name is required");
        }

        if (documentType == null || documentType.isBlank()) {
            throw new RuntimeException("Document type is required");
        }

        String originalFileName = file.getOriginalFilename();

        if (originalFileName == null || originalFileName.isBlank()) {
            throw new RuntimeException("File name is required");
        }

        EmployeeDocument document = new EmployeeDocument();

        document.setEmployee(employee);
        document.setDocumentName(documentName.trim());
        document.setDocumentType(documentType.trim());
        document.setFileName(originalFileName);
        document.setContentType(
                file.getContentType() != null
                        ? file.getContentType()
                        : "application/octet-stream");
        document.setFileSize(file.getSize());
        document.setFileData(file.getBytes());
        document.setUploadedAt(LocalDateTime.now());

        EmployeeDocument saved = documentRepository.save(document);

        return convertToDTO(saved);
    }

    @Transactional(readOnly = true)
    public List<EmployeeDocumentDTO> getEmployeeDocuments(Long employeeId) {

        employeeRepository.findById(employeeId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Employee not found with ID: " + employeeId));

        return documentRepository.findByEmployeeId(employeeId)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public EmployeeDocument getDocument(
            Long employeeId,
            Long documentId) {

        EmployeeDocument document = documentRepository.findById(documentId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Document not found with ID: " + documentId));

        if (!document.getEmployee().getId().equals(employeeId)) {
            throw new RuntimeException(
                    "Document does not belong to this employee");
        }

        return document;
    }

    @Transactional
    public void deleteDocument(
            Long employeeId,
            Long documentId) {

        EmployeeDocument document =
                getDocument(employeeId, documentId);

        documentRepository.delete(document);
    }

    private EmployeeDocumentDTO convertToDTO(
            EmployeeDocument document) {

        EmployeeDocumentDTO dto = new EmployeeDocumentDTO();

        dto.setId(document.getId());
        dto.setEmployeeId(document.getEmployee().getId());
        dto.setDocumentName(document.getDocumentName());
        dto.setDocumentType(document.getDocumentType());
        dto.setFileName(document.getFileName());
        dto.setContentType(document.getContentType());
        dto.setFileSize(document.getFileSize());
        dto.setUploadedAt(document.getUploadedAt());

        return dto;
    }
}