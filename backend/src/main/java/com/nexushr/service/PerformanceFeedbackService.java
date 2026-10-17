package com.nexushr.service;

import com.nexushr.entity.Employee;
import com.nexushr.entity.PerformanceFeedback;
import com.nexushr.entity.PerformanceReview;

import com.nexushr.repository.EmployeeRepository;
import com.nexushr.repository.PerformanceFeedbackRepository;
import com.nexushr.repository.PerformanceReviewRepository;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class PerformanceFeedbackService {

    private final PerformanceFeedbackRepository
            feedbackRepository;

    private final PerformanceReviewRepository
            reviewRepository;

    private final EmployeeRepository
            employeeRepository;

    public PerformanceFeedbackService(
            PerformanceFeedbackRepository feedbackRepository,
            PerformanceReviewRepository reviewRepository,
            EmployeeRepository employeeRepository
    ) {
        this.feedbackRepository =
                feedbackRepository;

        this.reviewRepository =
                reviewRepository;

        this.employeeRepository =
                employeeRepository;
    }

    public List<PerformanceFeedback>
    getAllFeedback() {

        return feedbackRepository.findAll();
    }

    public Optional<PerformanceFeedback>
    getFeedbackById(Long id) {

        return feedbackRepository.findById(id);
    }

    public List<PerformanceFeedback>
    getFeedbackByReview(Long reviewId) {

        return feedbackRepository
                .findByReviewId(reviewId);
    }

    public List<PerformanceFeedback>
    getFeedbackByEmployee(Long employeeId) {

        return feedbackRepository
                .findByEmployeeId(employeeId);
    }

    public PerformanceFeedback createFeedback(
            PerformanceFeedback feedback
    ) {

        validateFeedback(feedback);

        PerformanceReview review =
                reviewRepository
                        .findById(
                                feedback
                                        .getReview()
                                        .getId()
                        )
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Performance review not found."
                                )
                        );

        Employee employee =
                employeeRepository
                        .findById(
                                feedback
                                        .getEmployee()
                                        .getId()
                        )
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Employee not found."
                                )
                        );

        feedback.setReview(review);
        feedback.setEmployee(employee);

        if (feedback.getReviewer() != null &&
                feedback.getReviewer().getId() != null) {

            Employee reviewer =
                    employeeRepository
                            .findById(
                                    feedback
                                            .getReviewer()
                                            .getId()
                            )
                            .orElseThrow(() ->
                                    new RuntimeException(
                                            "Reviewer not found."
                                    )
                            );

            feedback.setReviewer(reviewer);
        }

        return feedbackRepository.save(feedback);
    }

    public PerformanceFeedback updateFeedback(
            Long id,
            PerformanceFeedback updatedFeedback
    ) {

        validateFeedback(updatedFeedback);

        PerformanceFeedback existing =
                feedbackRepository
                        .findById(id)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Performance feedback not found with id: "
                                                + id
                                )
                        );

        PerformanceReview review =
                reviewRepository
                        .findById(
                                updatedFeedback
                                        .getReview()
                                        .getId()
                        )
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Performance review not found."
                                )
                        );

        Employee employee =
                employeeRepository
                        .findById(
                                updatedFeedback
                                        .getEmployee()
                                        .getId()
                        )
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Employee not found."
                                )
                        );

        existing.setReview(review);
        existing.setEmployee(employee);

        if (updatedFeedback.getReviewer() != null &&
                updatedFeedback
                        .getReviewer()
                        .getId() != null) {

            Employee reviewer =
                    employeeRepository
                            .findById(
                                    updatedFeedback
                                            .getReviewer()
                                            .getId()
                            )
                            .orElseThrow(() ->
                                    new RuntimeException(
                                            "Reviewer not found."
                                    )
                            );

            existing.setReviewer(reviewer);

        } else {

            existing.setReviewer(null);
        }

        existing.setFeedbackType(
                updatedFeedback.getFeedbackType()
        );

        existing.setRating(
                updatedFeedback.getRating()
        );

        existing.setComments(
                updatedFeedback.getComments()
        );

        existing.setStrengths(
                updatedFeedback.getStrengths()
        );

        existing.setAreasForImprovement(
                updatedFeedback
                        .getAreasForImprovement()
        );

        return feedbackRepository.save(existing);
    }

    public void deleteFeedback(Long id) {

        if (!feedbackRepository.existsById(id)) {
            throw new RuntimeException(
                    "Performance feedback not found with id: "
                            + id
            );
        }

        feedbackRepository.deleteById(id);
    }

    public Double calculateAverageRating(
            Long reviewId
    ) {

        List<PerformanceFeedback> feedbackList =
                feedbackRepository
                        .findByReviewId(reviewId);

        if (feedbackList.isEmpty()) {
            return 0.0;
        }

        return feedbackList
                .stream()
                .mapToInt(
                        PerformanceFeedback::getRating
                )
                .average()
                .orElse(0.0);
    }

    private void validateFeedback(
            PerformanceFeedback feedback
    ) {

        if (feedback == null) {
            throw new IllegalArgumentException(
                    "Feedback is required."
            );
        }

        if (feedback.getReview() == null ||
                feedback.getReview().getId() == null) {

            throw new IllegalArgumentException(
                    "Performance review is required."
            );
        }

        if (feedback.getEmployee() == null ||
                feedback.getEmployee().getId() == null) {

            throw new IllegalArgumentException(
                    "Employee is required."
            );
        }

        if (feedback.getFeedbackType() == null) {

            throw new IllegalArgumentException(
                    "Feedback type is required."
            );
        }

        Integer rating = feedback.getRating();

        if (rating == null) {
            throw new IllegalArgumentException(
                    "Rating is required."
            );
        }

        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException(
                    "Rating must be between 1 and 5."
            );
        }
    }
}