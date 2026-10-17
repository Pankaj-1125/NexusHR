package com.nexushr.controller;

import com.nexushr.entity.PerformanceFeedback;
import com.nexushr.service.PerformanceFeedbackService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/performance-feedback")
@PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER')")
public class PerformanceFeedbackController {

    private final PerformanceFeedbackService
            feedbackService;

    public PerformanceFeedbackController(
            PerformanceFeedbackService feedbackService
    ) {
        this.feedbackService = feedbackService;
    }

    @GetMapping
    public ResponseEntity<List<PerformanceFeedback>>
    getAllFeedback() {

        return ResponseEntity.ok(
                feedbackService.getAllFeedback()
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<PerformanceFeedback>
    getFeedbackById(
            @PathVariable Long id
    ) {

        return feedbackService
                .getFeedbackById(id)
                .map(ResponseEntity::ok)
                .orElse(
                        ResponseEntity
                                .notFound()
                                .build()
                );
    }

    @GetMapping("/review/{reviewId}")
    public ResponseEntity<List<PerformanceFeedback>>
    getFeedbackByReview(
            @PathVariable Long reviewId
    ) {

        return ResponseEntity.ok(
                feedbackService
                        .getFeedbackByReview(reviewId)
        );
    }

    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<List<PerformanceFeedback>>
    getFeedbackByEmployee(
            @PathVariable Long employeeId
    ) {

        return ResponseEntity.ok(
                feedbackService
                        .getFeedbackByEmployee(employeeId)
        );
    }

    @GetMapping("/review/{reviewId}/average")
    public ResponseEntity<Map<String, Object>>
    getAverageRating(
            @PathVariable Long reviewId
    ) {

        Double average =
                feedbackService
                        .calculateAverageRating(reviewId);

        return ResponseEntity.ok(
                Map.of(
                        "reviewId",
                        reviewId,
                        "averageRating",
                        average
                )
        );
    }

    @PostMapping
    public ResponseEntity<PerformanceFeedback>
    createFeedback(
            @RequestBody PerformanceFeedback feedback
    ) {

        return ResponseEntity.ok(
                feedbackService
                        .createFeedback(feedback)
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<PerformanceFeedback>
    updateFeedback(
            @PathVariable Long id,
            @RequestBody PerformanceFeedback feedback
    ) {

        return ResponseEntity.ok(
                feedbackService
                        .updateFeedback(
                                id,
                                feedback
                        )
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void>
    deleteFeedback(
            @PathVariable Long id
    ) {

        feedbackService.deleteFeedback(id);

        return ResponseEntity
                .noContent()
                .build();
    }
}