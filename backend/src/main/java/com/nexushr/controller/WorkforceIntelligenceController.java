package com.nexushr.controller;

import com.nexushr.dto.WorkforceIntelligenceDTO;
import com.nexushr.service.WorkforceIntelligenceService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/workforce-intelligence")
@PreAuthorize("hasAnyRole('ADMIN', 'HR', 'MANAGER')")
public class WorkforceIntelligenceController {

    private final WorkforceIntelligenceService workforceIntelligenceService;

    public WorkforceIntelligenceController(WorkforceIntelligenceService workforceIntelligenceService) {
        this.workforceIntelligenceService = workforceIntelligenceService;
    }

    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<WorkforceIntelligenceDTO> analyzeEmployee(@PathVariable Long employeeId) {
        return ResponseEntity.ok(workforceIntelligenceService.analyzeEmployee(employeeId));
    }

    @GetMapping("/employees")
    public ResponseEntity<List<WorkforceIntelligenceDTO>> analyzeAllEmployees() {
        return ResponseEntity.ok(workforceIntelligenceService.analyzeAllEmployees());
    }

    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> summary() {
        return ResponseEntity.ok(workforceIntelligenceService.getWorkforceSummary());
    }

    @GetMapping("/departments")
    public ResponseEntity<List<Map<String, Object>>> departments() {
        return ResponseEntity.ok(workforceIntelligenceService.getDepartmentInsights());
    }
}
