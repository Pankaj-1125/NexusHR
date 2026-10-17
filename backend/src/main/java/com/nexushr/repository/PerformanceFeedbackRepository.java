package com.nexushr.repository;

import com.nexushr.entity.PerformanceFeedback;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PerformanceFeedbackRepository
        extends JpaRepository<PerformanceFeedback, Long> {

    List<PerformanceFeedback>
    findByReviewId(Long reviewId);

    List<PerformanceFeedback>
    findByEmployeeId(Long employeeId);

    List<PerformanceFeedback>
    findByReviewIdAndFeedbackType(
            Long reviewId,
            PerformanceFeedback.FeedbackType feedbackType
    );
}