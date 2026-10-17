package com.nexushr.service;

import com.nexushr.entity.Employee;
import com.nexushr.entity.PerformanceReview;
import com.nexushr.repository.PerformanceReviewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PerformanceReviewServiceTest {

    @Mock
    private PerformanceReviewRepository performanceReviewRepository;

    @InjectMocks
    private PerformanceReviewService performanceReviewService;

    private Employee employee;
    private PerformanceReview review;

    @BeforeEach
    void setUp() {
        employee = new Employee();
        employee.setId(1L);
        employee.setFirstName("John");
        employee.setLastName("Doe");
        employee.setEmail("john.doe@example.com");

        review = new PerformanceReview();
        review.setId(1L);
        review.setEmployee(employee);
        review.setReviewPeriodStart(LocalDate.of(2026, 1, 1));
        review.setReviewPeriodEnd(LocalDate.of(2026, 6, 30));
        review.setRating(4);
        review.setGoals("Complete project milestones");
        review.setStrengths("Communication and teamwork");
        review.setAreasForImprovement("Time management");
        review.setManagerComments("Good performance");
        review.setEmployeeComments("I will continue improving");
    }

    @Test
    void getAllReviewsShouldReturnAllReviews() {
        when(performanceReviewRepository.findAll())
                .thenReturn(List.of(review));

        List<PerformanceReview> result =
                performanceReviewService.getAllReviews();

        assertEquals(1, result.size());
        assertEquals(review, result.get(0));

        verify(performanceReviewRepository).findAll();
    }

    @Test
    void getReviewByIdShouldReturnReview() {
        when(performanceReviewRepository.findById(1L))
                .thenReturn(Optional.of(review));

        Optional<PerformanceReview> result =
                performanceReviewService.getReviewById(1L);

        assertTrue(result.isPresent());
        assertEquals(review, result.get());

        verify(performanceReviewRepository).findById(1L);
    }

    @Test
    void getReviewsByEmployeeShouldReturnEmployeeReviews() {
        when(performanceReviewRepository.findByEmployeeId(1L))
                .thenReturn(List.of(review));

        List<PerformanceReview> result =
                performanceReviewService.getReviewsByEmployee(1L);

        assertEquals(1, result.size());
        assertEquals(review, result.get(0));

        verify(performanceReviewRepository)
                .findByEmployeeId(1L);
    }

    @Test
    void createReviewShouldValidateRatingAndSaveReview() {
        when(performanceReviewRepository.save(review))
                .thenReturn(review);

        PerformanceReview result =
                performanceReviewService.createReview(review);

        assertEquals(review, result);
        assertEquals(4, result.getRating());

        verify(performanceReviewRepository).save(review);
    }

    @Test
    void createReviewShouldAcceptMinimumRating() {
        review.setRating(1);

        when(performanceReviewRepository.save(review))
                .thenReturn(review);

        PerformanceReview result =
                performanceReviewService.createReview(review);

        assertEquals(1, result.getRating());

        verify(performanceReviewRepository).save(review);
    }

    @Test
    void createReviewShouldAcceptMaximumRating() {
        review.setRating(5);

        when(performanceReviewRepository.save(review))
                .thenReturn(review);

        PerformanceReview result =
                performanceReviewService.createReview(review);

        assertEquals(5, result.getRating());

        verify(performanceReviewRepository).save(review);
    }

    @Test
    void createReviewShouldRejectNullRating() {
        review.setRating(null);

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> performanceReviewService.createReview(review)
                );

        assertEquals(
                "Rating is required.",
                exception.getMessage()
        );

        verify(performanceReviewRepository, never())
                .save(any(PerformanceReview.class));
    }

    @Test
    void createReviewShouldRejectRatingBelowOne() {
        review.setRating(0);

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> performanceReviewService.createReview(review)
                );

        assertEquals(
                "Rating must be between 1 and 5.",
                exception.getMessage()
        );

        verify(performanceReviewRepository, never())
                .save(any(PerformanceReview.class));
    }

    @Test
    void createReviewShouldRejectRatingAboveFive() {
        review.setRating(6);

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> performanceReviewService.createReview(review)
                );

        assertEquals(
                "Rating must be between 1 and 5.",
                exception.getMessage()
        );

        verify(performanceReviewRepository, never())
                .save(any(PerformanceReview.class));
    }

    @Test
    void updateReviewShouldUpdateExistingReview() {
        PerformanceReview updatedReview = new PerformanceReview();
        updatedReview.setEmployee(employee);
        updatedReview.setReviewPeriodStart(
                LocalDate.of(2026, 7, 1)
        );
        updatedReview.setReviewPeriodEnd(
                LocalDate.of(2026, 12, 31)
        );
        updatedReview.setRating(5);
        updatedReview.setGoals("Complete all assigned goals");
        updatedReview.setStrengths("Leadership");
        updatedReview.setAreasForImprovement("Delegation");
        updatedReview.setManagerComments("Excellent performance");
        updatedReview.setEmployeeComments("Thank you");

        when(performanceReviewRepository.findById(1L))
                .thenReturn(Optional.of(review));

        when(performanceReviewRepository.save(any(PerformanceReview.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PerformanceReview result =
                performanceReviewService.updateReview(
                        1L,
                        updatedReview
                );

        assertEquals(employee, result.getEmployee());
        assertEquals(
                LocalDate.of(2026, 7, 1),
                result.getReviewPeriodStart()
        );
        assertEquals(
                LocalDate.of(2026, 12, 31),
                result.getReviewPeriodEnd()
        );
        assertEquals(5, result.getRating());
        assertEquals(
                "Complete all assigned goals",
                result.getGoals()
        );
        assertEquals(
                "Leadership",
                result.getStrengths()
        );
        assertEquals(
                "Delegation",
                result.getAreasForImprovement()
        );
        assertEquals(
                "Excellent performance",
                result.getManagerComments()
        );
        assertEquals(
                "Thank you",
                result.getEmployeeComments()
        );

        verify(performanceReviewRepository).findById(1L);
        verify(performanceReviewRepository).save(review);
    }

    @Test
    void updateReviewShouldRejectInvalidRating() {
        review.setRating(6);

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> performanceReviewService.updateReview(
                                1L,
                                review
                        )
                );

        assertEquals(
                "Rating must be between 1 and 5.",
                exception.getMessage()
        );

        verify(performanceReviewRepository, never())
                .findById(anyLong());

        verify(performanceReviewRepository, never())
                .save(any(PerformanceReview.class));
    }

    @Test
    void updateReviewShouldRejectNonexistentReview() {
        when(performanceReviewRepository.findById(999L))
                .thenReturn(Optional.empty());

        RuntimeException exception =
                assertThrows(
                        RuntimeException.class,
                        () -> performanceReviewService.updateReview(
                                999L,
                                review
                        )
                );

        assertEquals(
                "Performance review not found with id: 999",
                exception.getMessage()
        );

        verify(performanceReviewRepository).findById(999L);
        verify(performanceReviewRepository, never())
                .save(any(PerformanceReview.class));
    }

    @Test
    void deleteReviewShouldDeleteReview() {
        performanceReviewService.deleteReview(1L);

        verify(performanceReviewRepository).deleteById(1L);
    }
}
