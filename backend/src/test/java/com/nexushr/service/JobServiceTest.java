package com.nexushr.service;

import com.nexushr.entity.Job;
import com.nexushr.repository.JobRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobServiceTest {

    @Mock
    private JobRepository jobRepository;

    @InjectMocks
    private JobService jobService;

    private Job job;

    @BeforeEach
    void setUp() {
        job = new Job();
        job.setId(1L);
        job.setTitle("Java Developer");
        job.setDepartment("Engineering");
        job.setLocation("Pune");
        job.setEmploymentType(Job.EmploymentType.FULL_TIME);
        job.setDescription("Develop Java applications");
        job.setRequirements("Java, Spring Boot, PostgreSQL");
        job.setSalaryMin(new BigDecimal("50000"));
        job.setSalaryMax(new BigDecimal("80000"));
        job.setStatus(Job.JobStatus.OPEN);
        job.setPostedDate(LocalDate.of(2026, 9, 1));
        job.setClosingDate(LocalDate.of(2026, 10, 1));
    }

    @Test
    void getAllJobsShouldReturnAllJobs() {
        when(jobRepository.findAll()).thenReturn(List.of(job));

        List<Job> result = jobService.getAllJobs();

        assertEquals(1, result.size());
        assertEquals(job, result.get(0));

        verify(jobRepository).findAll();
    }

    @Test
    void getJobByIdShouldReturnJob() {
        when(jobRepository.findById(1L)).thenReturn(Optional.of(job));

        Optional<Job> result = jobService.getJobById(1L);

        assertTrue(result.isPresent());
        assertEquals(job, result.get());

        verify(jobRepository).findById(1L);
    }

    @Test
    void getJobsByStatusShouldReturnMatchingJobs() {
        when(jobRepository.findByStatus(Job.JobStatus.OPEN))
                .thenReturn(List.of(job));

        List<Job> result =
                jobService.getJobsByStatus(Job.JobStatus.OPEN);

        assertEquals(1, result.size());
        assertEquals(job, result.get(0));

        verify(jobRepository).findByStatus(Job.JobStatus.OPEN);
    }

    @Test
    void getJobsByDepartmentShouldReturnMatchingJobs() {
        when(jobRepository.findByDepartmentIgnoreCase("engineering"))
                .thenReturn(List.of(job));

        List<Job> result =
                jobService.getJobsByDepartment("engineering");

        assertEquals(1, result.size());
        assertEquals(job, result.get(0));

        verify(jobRepository)
                .findByDepartmentIgnoreCase("engineering");
    }

    @Test
    void searchJobsByTitleShouldReturnMatchingJobs() {
        when(jobRepository.findByTitleContainingIgnoreCase("java"))
                .thenReturn(List.of(job));

        List<Job> result =
                jobService.searchJobsByTitle("java");

        assertEquals(1, result.size());
        assertEquals(job, result.get(0));

        verify(jobRepository)
                .findByTitleContainingIgnoreCase("java");
    }

    @Test
    void createJobShouldValidateAndSaveJob() {
        when(jobRepository.save(job)).thenReturn(job);

        Job result = jobService.createJob(job);

        assertEquals(job, result);

        verify(jobRepository).save(job);
    }

    @Test
    void createJobShouldAcceptNullSalaryValues() {
        job.setSalaryMin(null);
        job.setSalaryMax(null);

        when(jobRepository.save(job)).thenReturn(job);

        Job result = jobService.createJob(job);

        assertEquals(job, result);

        verify(jobRepository).save(job);
    }

    @Test
    void createJobShouldRejectBlankTitle() {
        job.setTitle("   ");

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> jobService.createJob(job)
                );

        assertEquals(
                "Job title is required.",
                exception.getMessage()
        );

        verify(jobRepository, never()).save(any(Job.class));
    }

    @Test
    void createJobShouldRejectMissingTitle() {
        job.setTitle(null);

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> jobService.createJob(job)
                );

        assertEquals(
                "Job title is required.",
                exception.getMessage()
        );

        verify(jobRepository, never()).save(any(Job.class));
    }

    @Test
    void createJobShouldRejectBlankDepartment() {
        job.setDepartment("   ");

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> jobService.createJob(job)
                );

        assertEquals(
                "Department is required.",
                exception.getMessage()
        );

        verify(jobRepository, never()).save(any(Job.class));
    }

    @Test
    void createJobShouldRejectMissingDepartment() {
        job.setDepartment(null);

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> jobService.createJob(job)
                );

        assertEquals(
                "Department is required.",
                exception.getMessage()
        );

        verify(jobRepository, never()).save(any(Job.class));
    }

    @Test
    void createJobShouldRejectBlankLocation() {
        job.setLocation("   ");

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> jobService.createJob(job)
                );

        assertEquals(
                "Location is required.",
                exception.getMessage()
        );

        verify(jobRepository, never()).save(any(Job.class));
    }

    @Test
    void createJobShouldRejectMissingLocation() {
        job.setLocation(null);

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> jobService.createJob(job)
                );

        assertEquals(
                "Location is required.",
                exception.getMessage()
        );

        verify(jobRepository, never()).save(any(Job.class));
    }

    @Test
    void createJobShouldRejectMissingEmploymentType() {
        job.setEmploymentType(null);

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> jobService.createJob(job)
                );

        assertEquals(
                "Employment type is required.",
                exception.getMessage()
        );

        verify(jobRepository, never()).save(any(Job.class));
    }

    @Test
    void createJobShouldRejectMissingStatus() {
        job.setStatus(null);

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> jobService.createJob(job)
                );

        assertEquals(
                "Job status is required.",
                exception.getMessage()
        );

        verify(jobRepository, never()).save(any(Job.class));
    }

    @Test
    void createJobShouldRejectMissingPostedDate() {
        job.setPostedDate(null);

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> jobService.createJob(job)
                );

        assertEquals(
                "Posted date is required.",
                exception.getMessage()
        );

        verify(jobRepository, never()).save(any(Job.class));
    }

    @Test
    void createJobShouldRejectMinimumSalaryGreaterThanMaximumSalary() {
        job.setSalaryMin(new BigDecimal("90000"));
        job.setSalaryMax(new BigDecimal("80000"));

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> jobService.createJob(job)
                );

        assertEquals(
                "Minimum salary cannot be greater than maximum salary.",
                exception.getMessage()
        );

        verify(jobRepository, never()).save(any(Job.class));
    }

    @Test
    void createJobShouldAcceptEqualMinimumAndMaximumSalary() {
        job.setSalaryMin(new BigDecimal("80000"));
        job.setSalaryMax(new BigDecimal("80000"));

        when(jobRepository.save(job)).thenReturn(job);

        Job result = jobService.createJob(job);

        assertEquals(job, result);

        verify(jobRepository).save(job);
    }

    @Test
    void createJobShouldRejectClosingDateBeforePostedDate() {
        job.setClosingDate(LocalDate.of(2026, 8, 31));

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> jobService.createJob(job)
                );

        assertEquals(
                "Closing date cannot be before posted date.",
                exception.getMessage()
        );

        verify(jobRepository, never()).save(any(Job.class));
    }

    @Test
    void createJobShouldAcceptClosingDateEqualToPostedDate() {
        job.setClosingDate(job.getPostedDate());

        when(jobRepository.save(job)).thenReturn(job);

        Job result = jobService.createJob(job);

        assertEquals(job, result);

        verify(jobRepository).save(job);
    }

    @Test
    void updateJobShouldUpdateExistingJob() {
        Job updatedJob = new Job();
        updatedJob.setTitle("Senior Java Developer");
        updatedJob.setDepartment("Technology");
        updatedJob.setLocation("Mumbai");
        updatedJob.setEmploymentType(Job.EmploymentType.FULL_TIME);
        updatedJob.setDescription("Build enterprise applications");
        updatedJob.setRequirements("Java, Spring Boot, AWS");
        updatedJob.setSalaryMin(new BigDecimal("90000"));
        updatedJob.setSalaryMax(new BigDecimal("120000"));
        updatedJob.setStatus(Job.JobStatus.OPEN);
        updatedJob.setPostedDate(LocalDate.of(2026, 9, 15));
        updatedJob.setClosingDate(LocalDate.of(2026, 11, 15));

        when(jobRepository.findById(1L))
                .thenReturn(Optional.of(job));

        when(jobRepository.save(any(Job.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Job result =
                jobService.updateJob(1L, updatedJob);

        assertEquals(
                "Senior Java Developer",
                result.getTitle()
        );
        assertEquals("Technology", result.getDepartment());
        assertEquals("Mumbai", result.getLocation());
        assertEquals(
                Job.EmploymentType.FULL_TIME,
                result.getEmploymentType()
        );
        assertEquals(
                "Build enterprise applications",
                result.getDescription()
        );
        assertEquals(
                "Java, Spring Boot, AWS",
                result.getRequirements()
        );
        assertEquals(
                new BigDecimal("90000"),
                result.getSalaryMin()
        );
        assertEquals(
                new BigDecimal("120000"),
                result.getSalaryMax()
        );
        assertEquals(
                Job.JobStatus.OPEN,
                result.getStatus()
        );
        assertEquals(
                LocalDate.of(2026, 9, 15),
                result.getPostedDate()
        );
        assertEquals(
                LocalDate.of(2026, 11, 15),
                result.getClosingDate()
        );

        verify(jobRepository).findById(1L);
        verify(jobRepository).save(job);
    }

    @Test
    void updateJobShouldRejectInvalidUpdatedJobBeforeRepositoryLookup() {
        Job updatedJob = new Job();
        updatedJob.setTitle("");
        updatedJob.setDepartment("Technology");
        updatedJob.setLocation("Mumbai");
        updatedJob.setEmploymentType(Job.EmploymentType.FULL_TIME);
        updatedJob.setStatus(Job.JobStatus.OPEN);
        updatedJob.setPostedDate(LocalDate.of(2026, 9, 15));

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> jobService.updateJob(1L, updatedJob)
                );

        assertEquals(
                "Job title is required.",
                exception.getMessage()
        );

        verify(jobRepository, never()).findById(anyLong());
        verify(jobRepository, never()).save(any(Job.class));
    }

    @Test
    void updateJobShouldRejectNonexistentJob() {
        when(jobRepository.findById(999L))
                .thenReturn(Optional.empty());

        RuntimeException exception =
                assertThrows(
                        RuntimeException.class,
                        () -> jobService.updateJob(999L, job)
                );

        assertEquals(
                "Job not found with id: 999",
                exception.getMessage()
        );

        verify(jobRepository).findById(999L);
        verify(jobRepository, never()).save(any(Job.class));
    }

    @Test
    void deleteJobShouldDeleteJob() {
        jobService.deleteJob(1L);

        verify(jobRepository).deleteById(1L);
    }
}
