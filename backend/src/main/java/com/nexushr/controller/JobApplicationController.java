package com.nexushr.controller;

import com.nexushr.entity.JobApplication;
import com.nexushr.service.JobApplicationService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/applications")
@PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER')")
public class JobApplicationController {

    private final JobApplicationService jobApplicationService;

    public JobApplicationController(JobApplicationService jobApplicationService) {
        this.jobApplicationService = jobApplicationService;
    }

    @GetMapping
    public ResponseEntity<List<JobApplication>> getAllApplications() {
        return ResponseEntity.ok(jobApplicationService.getAllApplications());
    }

    @GetMapping("/{id}")
    public ResponseEntity<JobApplication> getApplicationById(@PathVariable Long id) {
        return jobApplicationService.getApplicationById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<JobApplication>> getApplicationsByStatus(
            @PathVariable JobApplication.ApplicationStatus status
    ) {
        return ResponseEntity.ok(jobApplicationService.getApplicationsByStatus(status));
    }

    @GetMapping("/job/{jobId}")
    public ResponseEntity<List<JobApplication>> getApplicationsByJobId(@PathVariable Long jobId) {
        return ResponseEntity.ok(jobApplicationService.getApplicationsByJobId(jobId));
    }

    @GetMapping("/search/name")
    public ResponseEntity<List<JobApplication>> searchByName(@RequestParam String name) {
        return ResponseEntity.ok(jobApplicationService.searchApplicationsByName(name));
    }

    @GetMapping("/search/email")
    public ResponseEntity<List<JobApplication>> searchByEmail(@RequestParam String email) {
        return ResponseEntity.ok(jobApplicationService.searchApplicationsByEmail(email));
    }

    @PostMapping
    public ResponseEntity<JobApplication> createApplication(@RequestBody JobApplication application) {
        return ResponseEntity.ok(jobApplicationService.createApplication(application));
    }

    @PutMapping("/{id}")
    public ResponseEntity<JobApplication> updateApplication(
            @PathVariable Long id,
            @RequestBody JobApplication application
    ) {
        return ResponseEntity.ok(jobApplicationService.updateApplication(id, application));
    }

    @PutMapping("/{id}/screen")
    public ResponseEntity<JobApplication> moveToScreening(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(jobApplicationService.moveToScreening(id, authentication));
    }

    @PutMapping("/{id}/manager-shortlist")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<JobApplication> managerShortlist(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body,
            Authentication authentication
    ) {
        String note = body == null ? null : body.get("note");
        return ResponseEntity.ok(jobApplicationService.managerShortlist(id, authentication, note));
    }


    @PutMapping("/{id}/admin-direct-shortlist")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<JobApplication> adminDirectShortlist(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body,
            Authentication authentication
    ) {
        String note = body == null ? null : body.get("note");
        return ResponseEntity.ok(jobApplicationService.adminDirectShortlist(id, authentication, note));
    }

    @PutMapping("/{id}/admin-approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<JobApplication> adminApprove(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body,
            Authentication authentication
    ) {
        String note = body == null ? null : body.get("note");
        return ResponseEntity.ok(jobApplicationService.adminApprove(id, authentication, note));
    }

    @PutMapping("/{id}/interview")
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER')")
    public ResponseEntity<JobApplication> moveToInterview(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body,
            Authentication authentication
    ) {
        String note = body == null ? null : body.get("note");
        return ResponseEntity.ok(jobApplicationService.setInterview(id, authentication, note));
    }


    @PutMapping("/{id}/final-approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<JobApplication> finalApproveAfterInterview(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body,
            Authentication authentication
    ) {
        String finalMessage = body == null ? null : body.get("finalMessage");
        String requiredDocuments = body == null ? null : body.get("requiredDocuments");
        String reportingInstructions = body == null ? null : body.get("reportingInstructions");
        return ResponseEntity.ok(jobApplicationService.finalApproveAfterInterview(
                id, authentication, finalMessage, requiredDocuments, reportingInstructions
        ));
    }

    @PutMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER')")
    public ResponseEntity<JobApplication> reject(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body,
            Authentication authentication
    ) {
        String reason = body == null ? null : body.get("reason");
        return ResponseEntity.ok(jobApplicationService.reject(id, authentication, reason));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteApplication(@PathVariable Long id) {
        jobApplicationService.deleteApplication(id);
        return ResponseEntity.noContent().build();
    }
}
