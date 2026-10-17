package com.nexushr.controller;

import com.nexushr.dto.InterviewScheduleRequest;
import com.nexushr.entity.InterviewSchedule;
import com.nexushr.service.InterviewScheduleService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/interviews")
@PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
public class InterviewScheduleController {
    private final InterviewScheduleService interviewService;

    public InterviewScheduleController(InterviewScheduleService interviewService) { this.interviewService = interviewService; }

    @PostMapping("/application/{applicationId}")
    public ResponseEntity<InterviewSchedule> schedule(@PathVariable Long applicationId,
                                                       @RequestBody InterviewScheduleRequest request,
                                                       Authentication authentication) {
        return ResponseEntity.ok(interviewService.schedule(applicationId, request, authentication));
    }

    @PutMapping("/application/{applicationId}")
    public ResponseEntity<InterviewSchedule> reschedule(@PathVariable Long applicationId,
                                                         @RequestBody InterviewScheduleRequest request,
                                                         Authentication authentication) {
        return ResponseEntity.ok(interviewService.schedule(applicationId, request, authentication));
    }

    @GetMapping("/application/{applicationId}")
    public ResponseEntity<List<InterviewSchedule>> byApplication(@PathVariable Long applicationId) {
        return ResponseEntity.ok(interviewService.byApplication(applicationId));
    }

    @PutMapping("/application/{applicationId}/complete")
    public ResponseEntity<InterviewSchedule> completeLatest(@PathVariable Long applicationId, Authentication authentication) {
        return ResponseEntity.ok(interviewService.completeLatest(applicationId, authentication));
    }

    @PutMapping("/{interviewId}/cancel")
    public ResponseEntity<InterviewSchedule> cancel(@PathVariable Long interviewId, Authentication authentication) {
        return ResponseEntity.ok(interviewService.cancel(interviewId, authentication));
    }
}
