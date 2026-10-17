package com.nexushr.controller;

import com.nexushr.entity.PerformanceReview;
import com.nexushr.service.PerformanceReviewService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/performance")
public class PerformanceReviewController {
    private final PerformanceReviewService performanceReviewService;
    public PerformanceReviewController(PerformanceReviewService performanceReviewService){this.performanceReviewService=performanceReviewService;}

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER')")
    public ResponseEntity<List<PerformanceReview>> getAllReviews(){return ResponseEntity.ok(performanceReviewService.getAllReviews());}

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER')")
    public ResponseEntity<PerformanceReview> getReviewById(@PathVariable Long id){return performanceReviewService.getReviewById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());}

    @GetMapping("/employee/{employeeId}")
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER')")
    public ResponseEntity<List<PerformanceReview>> getReviewsByEmployee(@PathVariable Long employeeId){return ResponseEntity.ok(performanceReviewService.getReviewsByEmployee(employeeId));}

    @GetMapping("/analytics")
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER')")
    public ResponseEntity<Map<String,Object>> analytics(@RequestParam(required=false) Long employeeId){return ResponseEntity.ok(performanceReviewService.getAnalytics(employeeId));}

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER')")
    public ResponseEntity<PerformanceReview> createReview(@RequestBody PerformanceReview review){return ResponseEntity.ok(performanceReviewService.createReview(review));}

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER')")
    public ResponseEntity<PerformanceReview> updateReview(@PathVariable Long id,@RequestBody PerformanceReview review){return ResponseEntity.ok(performanceReviewService.updateReview(id,review));}

    @PostMapping("/{id}/workflow")
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER')")
    public ResponseEntity<PerformanceReview> workflow(@PathVariable Long id,@RequestBody WorkflowRequest request){return ResponseEntity.ok(performanceReviewService.advanceWorkflow(id,request.status(),request.notifyEmployee()));}

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public ResponseEntity<Void> deleteReview(@PathVariable Long id){performanceReviewService.deleteReview(id);return ResponseEntity.noContent().build();}

    public record WorkflowRequest(PerformanceReview.ReviewStatus status, boolean notifyEmployee){}
}
