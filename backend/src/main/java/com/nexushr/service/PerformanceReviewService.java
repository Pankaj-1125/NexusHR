package com.nexushr.service;

import com.nexushr.entity.PerformanceFeedback;
import com.nexushr.entity.PerformanceGoal;
import com.nexushr.entity.PerformanceReview;
import com.nexushr.repository.PerformanceFeedbackRepository;
import com.nexushr.repository.PerformanceGoalRepository;
import com.nexushr.repository.PerformanceReviewRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class PerformanceReviewService {

    private final PerformanceReviewRepository performanceReviewRepository;

    @Autowired(required = false)
    private PerformanceGoalRepository goalRepository;

    @Autowired(required = false)
    private PerformanceFeedbackRepository feedbackRepository;

    @Autowired(required = false)
    private NotificationService notificationService;

    @Autowired(required = false)
    private EmailNotificationService emailNotificationService;

    @Autowired(required = false)
    private AuditLogService auditLogService;

    public PerformanceReviewService(PerformanceReviewRepository performanceReviewRepository) {
        this.performanceReviewRepository = performanceReviewRepository;
    }

    public List<PerformanceReview> getAllReviews() { return performanceReviewRepository.findAll(); }
    public Optional<PerformanceReview> getReviewById(Long id) { return performanceReviewRepository.findById(id); }
    public List<PerformanceReview> getReviewsByEmployee(Long employeeId) { return performanceReviewRepository.findByEmployeeId(employeeId); }

    @Transactional
    public PerformanceReview createReview(PerformanceReview review) {
        validateRating(review.getRating());
        validateDates(review);
        if (review.getStatus() == null) review.setStatus(PerformanceReview.ReviewStatus.DRAFT);
        if (review.getReviewType() == null || review.getReviewType().isBlank()) review.setReviewType("ANNUAL");
        calculateScores(review);
        PerformanceReview saved = performanceReviewRepository.save(review);
        audit("PERFORMANCE_REVIEW_CREATED", saved, "Performance review created");
        return saved;
    }

    @Transactional
    public PerformanceReview updateReview(Long id, PerformanceReview updatedReview) {
        validateRating(updatedReview.getRating());
        validateDates(updatedReview);
        PerformanceReview existingReview = performanceReviewRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Performance review not found with id: " + id));

        existingReview.setEmployee(updatedReview.getEmployee());
        existingReview.setReviewPeriodStart(updatedReview.getReviewPeriodStart());
        existingReview.setReviewPeriodEnd(updatedReview.getReviewPeriodEnd());
        existingReview.setRating(updatedReview.getRating());
        if (updatedReview.getStatus() != null) existingReview.setStatus(updatedReview.getStatus());
        existingReview.setReviewCycle(updatedReview.getReviewCycle());
        existingReview.setReviewType(updatedReview.getReviewType());
        existingReview.setCompetencyScore(updatedReview.getCompetencyScore());
        existingReview.setGoalScore(updatedReview.getGoalScore());
        existingReview.setGoals(updatedReview.getGoals());
        existingReview.setStrengths(updatedReview.getStrengths());
        existingReview.setAreasForImprovement(updatedReview.getAreasForImprovement());
        existingReview.setManagerComments(updatedReview.getManagerComments());
        existingReview.setEmployeeComments(updatedReview.getEmployeeComments());
        calculateScores(existingReview);

        PerformanceReview saved = performanceReviewRepository.save(existingReview);
        audit("PERFORMANCE_REVIEW_UPDATED", saved, "Performance review updated");
        return saved;
    }

    @Transactional
    public PerformanceReview advanceWorkflow(Long id, PerformanceReview.ReviewStatus targetStatus, boolean notifyEmployee) {
        PerformanceReview review = performanceReviewRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Performance review not found with id: " + id));
        if (targetStatus == null) throw new IllegalArgumentException("Target review status is required.");

        PerformanceReview.ReviewStatus current = review.getStatus() == null
                ? PerformanceReview.ReviewStatus.DRAFT : review.getStatus();
        if (!isValidTransition(current, targetStatus)) {
            throw new IllegalStateException("Invalid performance workflow transition: " + current + " -> " + targetStatus);
        }

        review.setStatus(targetStatus);
        if (targetStatus == PerformanceReview.ReviewStatus.SUBMITTED && review.getSubmittedAt() == null) {
            review.setSubmittedAt(LocalDateTime.now());
        }
        if (targetStatus == PerformanceReview.ReviewStatus.COMPLETED) {
            calculateScores(review);
            review.setCompletedAt(LocalDateTime.now());
        }
        PerformanceReview saved = performanceReviewRepository.save(review);
        audit("PERFORMANCE_REVIEW_STATUS_CHANGED", saved, "Review status changed to " + targetStatus);
        if (notifyEmployee) notifyEmployee(saved, targetStatus);
        return saved;
    }

    public Map<String,Object> getAnalytics(Long employeeId) {
        List<PerformanceReview> reviews = employeeId == null
                ? performanceReviewRepository.findAll()
                : performanceReviewRepository.findByEmployeeId(employeeId);
        List<PerformanceGoal> goals = goalRepository == null ? List.of()
                : (employeeId == null ? goalRepository.findAll() : goalRepository.findByEmployeeIdOrderByTargetDateAsc(employeeId));
        List<PerformanceFeedback> feedback = feedbackRepository == null ? List.of()
                : (employeeId == null ? feedbackRepository.findAll() : feedbackRepository.findByEmployeeId(employeeId));

        double avgRating = reviews.stream().map(PerformanceReview::getRating).filter(java.util.Objects::nonNull)
                .mapToInt(Integer::intValue).average().orElse(0.0);
        double avgFinal = reviews.stream().map(PerformanceReview::getFinalScore).filter(java.util.Objects::nonNull)
                .mapToDouble(BigDecimal::doubleValue).average().orElse(0.0);
        double avgFeedback = feedback.stream().map(PerformanceFeedback::getRating).filter(java.util.Objects::nonNull)
                .mapToInt(Integer::intValue).average().orElse(0.0);
        double avgProgress = goals.stream().map(PerformanceGoal::getProgress).filter(java.util.Objects::nonNull)
                .mapToInt(Integer::intValue).average().orElse(0.0);
        long completedReviews = reviews.stream().filter(r -> r.getStatus() == PerformanceReview.ReviewStatus.COMPLETED).count();
        long activeGoals = goals.stream().filter(g -> g.getStatus() == PerformanceGoal.GoalStatus.ACTIVE).count();
        long completedGoals = goals.stream().filter(g -> g.getStatus() == PerformanceGoal.GoalStatus.COMPLETED).count();
        long overdueGoals = goals.stream().filter(g -> g.getTargetDate() != null && g.getTargetDate().isBefore(LocalDate.now())
                && g.getStatus() != PerformanceGoal.GoalStatus.COMPLETED && g.getStatus() != PerformanceGoal.GoalStatus.CANCELLED).count();

        Map<String,Long> feedbackMix = new LinkedHashMap<>();
        for (PerformanceFeedback.FeedbackType type : PerformanceFeedback.FeedbackType.values()) {
            feedbackMix.put(type.name(), feedback.stream().filter(f -> f.getFeedbackType() == type).count());
        }

        Map<String,Object> result = new LinkedHashMap<>();
        result.put("reviews", reviews.size());
        result.put("completedReviews", completedReviews);
        result.put("averageRating", round(avgRating));
        result.put("averageFinalScore", round(avgFinal));
        result.put("goals", goals.size());
        result.put("activeGoals", activeGoals);
        result.put("completedGoals", completedGoals);
        result.put("overdueGoals", overdueGoals);
        result.put("averageGoalProgress", round(avgProgress));
        result.put("feedbackCount", feedback.size());
        result.put("average360Rating", round(avgFeedback));
        result.put("feedbackMix", feedbackMix);
        return result;
    }

    @Transactional
    public void deleteReview(Long id) {
        performanceReviewRepository.deleteById(id);
    }

    private void validateRating(Integer rating) {
        if (rating == null) throw new IllegalArgumentException("Rating is required.");
        if (rating < 1 || rating > 5) throw new IllegalArgumentException("Rating must be between 1 and 5.");
    }

    private void validateDates(PerformanceReview review) {
        if (review == null) throw new IllegalArgumentException("Performance review is required.");
        if (review.getReviewPeriodStart() != null && review.getReviewPeriodEnd() != null
                && review.getReviewPeriodEnd().isBefore(review.getReviewPeriodStart())) {
            throw new IllegalArgumentException("Review period end cannot be before start date.");
        }
    }

    private void calculateScores(PerformanceReview review) {
        BigDecimal ratingScore = BigDecimal.valueOf((review.getRating() == null ? 0 : review.getRating()) * 20.0);
        BigDecimal competency = review.getCompetencyScore() == null ? ratingScore : clamp(review.getCompetencyScore());
        BigDecimal goal = review.getGoalScore() == null ? calculateGoalScore(review) : clamp(review.getGoalScore());
        BigDecimal finalScore = competency.multiply(new BigDecimal("0.50"))
                .add(goal.multiply(new BigDecimal("0.30")))
                .add(ratingScore.multiply(new BigDecimal("0.20")))
                .setScale(2, RoundingMode.HALF_UP);
        review.setCompetencyScore(competency.setScale(2, RoundingMode.HALF_UP));
        review.setGoalScore(goal.setScale(2, RoundingMode.HALF_UP));
        review.setFinalScore(finalScore);
    }

    private BigDecimal calculateGoalScore(PerformanceReview review) {
        if (goalRepository == null || review.getEmployee() == null || review.getEmployee().getId() == null) {
            return BigDecimal.valueOf((review.getRating() == null ? 0 : review.getRating()) * 20.0);
        }
        List<PerformanceGoal> goals = goalRepository.findByEmployeeIdOrderByTargetDateAsc(review.getEmployee().getId());
        if (goals.isEmpty()) return BigDecimal.valueOf(review.getRating() * 20.0);
        double weighted = 0; int weights = 0;
        for (PerformanceGoal goal : goals) {
            int w = goal.getWeight() == null ? 100 : goal.getWeight();
            int p = goal.getProgress() == null ? 0 : goal.getProgress();
            weighted += p * w; weights += w;
        }
        return BigDecimal.valueOf(weights == 0 ? 0 : weighted / weights).setScale(2, RoundingMode.HALF_UP);
    }

    private boolean isValidTransition(PerformanceReview.ReviewStatus current, PerformanceReview.ReviewStatus target) {
        if (current == target) return true;
        return switch (current) {
            case DRAFT -> target == PerformanceReview.ReviewStatus.SELF_REVIEW || target == PerformanceReview.ReviewStatus.SUBMITTED;
            case SELF_REVIEW -> target == PerformanceReview.ReviewStatus.SUBMITTED;
            case SUBMITTED -> target == PerformanceReview.ReviewStatus.MANAGER_REVIEW;
            case MANAGER_REVIEW -> target == PerformanceReview.ReviewStatus.COMPLETED;
            case COMPLETED -> false;
        };
    }

    private BigDecimal clamp(BigDecimal value) {
        if (value.compareTo(BigDecimal.ZERO) < 0) return BigDecimal.ZERO;
        if (value.compareTo(new BigDecimal("100")) > 0) return new BigDecimal("100");
        return value;
    }

    private double round(double value) { return Math.round(value * 100.0) / 100.0; }

    private void audit(String action, PerformanceReview review, String description) {
        if (auditLogService != null) auditLogService.log(action, "PERFORMANCE_REVIEW", review == null ? null : review.getId(), description);
    }

    private void notifyEmployee(PerformanceReview review, PerformanceReview.ReviewStatus status) {
        if (review.getEmployee() == null || review.getEmployee().getId() == null) return;
        String subject = status == PerformanceReview.ReviewStatus.COMPLETED ? "Performance Review Completed" : "Performance Review Update";
        String message = "Your performance review for " + review.getReviewPeriodStart() + " to " + review.getReviewPeriodEnd()
                + " is now " + status + ". Rating: " + review.getRating() + "/5"
                + (review.getFinalScore() == null ? "" : ", final score: " + review.getFinalScore() + "/100") + ".";
        if (notificationService != null) notificationService.createNotification(review.getEmployee().getId(), subject, message, "PERFORMANCE");
        if (emailNotificationService != null) emailNotificationService.send(review.getEmployee(), subject, message + "\n\nOpen NexusHR > My Performance for details.");
    }
}
