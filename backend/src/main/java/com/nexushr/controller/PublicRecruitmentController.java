package com.nexushr.controller;

import com.nexushr.entity.Job;
import com.nexushr.entity.JobApplication;
import com.nexushr.repository.JobRepository;
import com.nexushr.service.JobApplicationService;
import com.nexushr.service.InterviewScheduleService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/public")
public class PublicRecruitmentController {

    private final JobRepository jobRepository;
    private final JobApplicationService jobApplicationService;
    private final InterviewScheduleService interviewScheduleService;

    public PublicRecruitmentController(
            JobRepository jobRepository,
            JobApplicationService jobApplicationService,
            InterviewScheduleService interviewScheduleService
    ) {
        this.jobRepository = jobRepository;
        this.jobApplicationService = jobApplicationService;
        this.interviewScheduleService = interviewScheduleService;
    }

    @GetMapping("/jobs")
    public ResponseEntity<List<Job>> getOpenJobs() {
        List<Job> jobs = jobRepository.findByStatus(Job.JobStatus.OPEN)
                .stream()
                .filter(job -> job.getClosingDate() == null || !job.getClosingDate().isBefore(LocalDate.now()))
                .toList();
        return ResponseEntity.ok(jobs);
    }

    @GetMapping("/jobs/{id}")
    public ResponseEntity<Job> getOpenJob(@PathVariable Long id) {
        return jobRepository.findById(id)
                .filter(job -> job.getStatus() == Job.JobStatus.OPEN)
                .filter(job -> job.getClosingDate() == null || !job.getClosingDate().isBefore(LocalDate.now()))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/applications")
    public ResponseEntity<Map<String, Object>> apply(@RequestBody JobApplication application) {
        JobApplication saved = jobApplicationService.createPublicApplication(application);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("message", "Application submitted successfully.");
        response.put("applicationReference", saved.getApplicationReference());
        response.put("status", saved.getStatus());
        response.put("jobTitle", saved.getJob().getTitle());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/applications/status")
    public ResponseEntity<?> track(
            @RequestParam String reference,
            @RequestParam String email
    ) {
        return jobApplicationService.findPublicStatus(reference, email)
                .<ResponseEntity<?>>map(application -> {
                    Map<String, Object> result = new LinkedHashMap<>();
                    result.put("applicationReference", application.getApplicationReference());
                    result.put("applicantName", application.getApplicantName());
                    result.put("jobTitle", application.getJob().getTitle());
                    result.put("status", application.getStatus());
                    result.put("appliedDate", application.getAppliedDate());
                    if (application.getFinalSelectionMessage() != null || application.getRequiredDocuments() != null) {
                        Map<String, Object> finalSelection = new LinkedHashMap<>();
                        finalSelection.put("message", application.getFinalSelectionMessage());
                        finalSelection.put("requiredDocuments", application.getRequiredDocuments());
                        finalSelection.put("reportingInstructions", application.getReportingInstructions());
                        finalSelection.put("approvedAt", application.getFinalApprovedAt());
                        result.put("finalSelection", finalSelection);
                    }
                    var interview = interviewScheduleService.latest(application.getId());
                    if (interview != null) {
                        Map<String, Object> interviewInfo = new LinkedHashMap<>();
                        interviewInfo.put("id", interview.getId());
                        interviewInfo.put("date", interview.getInterviewDate());
                        interviewInfo.put("time", interview.getInterviewTime());
                        interviewInfo.put("location", interview.getLocation());
                        interviewInfo.put("message", interview.getMessage());
                        interviewInfo.put("status", interview.getStatus());
                        result.put("interview", interviewInfo);
                    }
                    return ResponseEntity.ok(result);
                })
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "Application could not be found for this reference and email.")));
    }
}
