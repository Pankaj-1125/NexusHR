package com.nexushr.controller;

import com.nexushr.entity.Job;
import com.nexushr.service.JobCommunicationService;
import com.nexushr.service.JobService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/jobs")
public class JobController {

    private final JobService jobService;
    private final JobCommunicationService jobCommunicationService;

    public JobController(
            JobService jobService,
            JobCommunicationService jobCommunicationService
    ) {
        this.jobService = jobService;
        this.jobCommunicationService = jobCommunicationService;
    }

    @GetMapping
    public ResponseEntity<List<Job>> getAllJobs() {
        return ResponseEntity.ok(jobService.getAllJobs());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Job> getJobById(@PathVariable Long id) {
        return jobService.getJobById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<Job>> getJobsByStatus(@PathVariable Job.JobStatus status) {
        return ResponseEntity.ok(jobService.getJobsByStatus(status));
    }

    @GetMapping("/department/{department}")
    public ResponseEntity<List<Job>> getJobsByDepartment(@PathVariable String department) {
        return ResponseEntity.ok(jobService.getJobsByDepartment(department));
    }

    @GetMapping("/search")
    public ResponseEntity<List<Job>> searchJobs(@RequestParam String title) {
        return ResponseEntity.ok(jobService.searchJobsByTitle(title));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'MANAGER')")
    public ResponseEntity<Job> createJob(@RequestBody Job job) {
        return ResponseEntity.ok(jobService.createJob(job));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'MANAGER')")
    public ResponseEntity<Job> updateJob(
            @PathVariable Long id,
            @RequestBody Job job
    ) {
        return ResponseEntity.ok(jobService.updateJob(id, job));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'MANAGER')")
    public ResponseEntity<Void> deleteJob(@PathVariable Long id) {
        jobService.deleteJob(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Publishes a job alert to every active employee. The in-app message always
     * records who sent it. Email delivery can include a generated PDF and SMS is
     * intentionally short (Twilio when configured, otherwise simulation).
     */
    @PostMapping("/{id}/announce")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'MANAGER')")
    public ResponseEntity<Map<String, Object>> announceJob(
            @PathVariable Long id,
            @RequestBody(required = false) JobAlertRequest request,
            Authentication authentication
    ) {
        Job job = jobService.getJobById(id)
                .orElseThrow(() -> new RuntimeException("Job not found with id: " + id));

        JobAlertRequest safeRequest = request == null
                ? new JobAlertRequest(null, null, true, false)
                : request;

        String sender = authentication == null || authentication.getName() == null
                ? "SYSTEM"
                : authentication.getName();

        String role = authentication == null || authentication.getAuthorities() == null
                ? "SYSTEM"
                : authentication.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .filter(value -> value != null && value.startsWith("ROLE_"))
                .map(value -> value.substring(5))
                .findFirst()
                .orElse("SYSTEM");

        return ResponseEntity.ok(
                jobCommunicationService.announceJob(
                        job,
                        safeRequest.subject(),
                        safeRequest.message(),
                        safeRequest.sendEmail() == null || safeRequest.sendEmail(),
                        Boolean.TRUE.equals(safeRequest.sendSms()),
                        sender,
                        role
                )
        );
    }

    public record JobAlertRequest(
            String subject,
            String message,
            Boolean sendEmail,
            Boolean sendSms
    ) {}
}
