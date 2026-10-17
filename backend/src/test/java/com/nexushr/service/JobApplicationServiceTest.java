package com.nexushr.service;

import com.nexushr.entity.Job;
import com.nexushr.entity.JobApplication;
import com.nexushr.repository.JobApplicationRepository;
import com.nexushr.repository.JobRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobApplicationServiceTest {

    @Mock
    private JobApplicationRepository jobApplicationRepository;

    @Mock
    private JobRepository jobRepository;

    @InjectMocks
    private JobApplicationService jobApplicationService;

    @Test
    void getAllApplications_shouldReturnAllApplications() {
        JobApplication application = new JobApplication();

        when(jobApplicationRepository.findAll())
                .thenReturn(List.of(application));

        List<JobApplication> result =
                jobApplicationService.getAllApplications();

        assertEquals(1, result.size());
        assertSame(application, result.get(0));

        verify(jobApplicationRepository).findAll();
    }

    @Test
    void getApplicationById_shouldReturnApplication() {
        JobApplication application = new JobApplication();

        when(jobApplicationRepository.findById(1L))
                .thenReturn(Optional.of(application));

        Optional<JobApplication> result =
                jobApplicationService.getApplicationById(1L);

        assertTrue(result.isPresent());
        assertSame(application, result.get());

        verify(jobApplicationRepository).findById(1L);
    }

    @Test
    void getApplicationsByStatus_shouldReturnMatchingApplications() {
        JobApplication application = new JobApplication();

        when(jobApplicationRepository.findByStatus(
                JobApplication.ApplicationStatus.APPLIED
        )).thenReturn(List.of(application));

        List<JobApplication> result =
                jobApplicationService.getApplicationsByStatus(
                        JobApplication.ApplicationStatus.APPLIED
                );

        assertEquals(1, result.size());
        assertSame(application, result.get(0));

        verify(jobApplicationRepository).findByStatus(
                JobApplication.ApplicationStatus.APPLIED
        );
    }

    @Test
    void getApplicationsByJobId_shouldReturnMatchingApplications() {
        JobApplication application = new JobApplication();

        when(jobApplicationRepository.findByJobId(10L))
                .thenReturn(List.of(application));

        List<JobApplication> result =
                jobApplicationService.getApplicationsByJobId(10L);

        assertEquals(1, result.size());
        assertSame(application, result.get(0));

        verify(jobApplicationRepository).findByJobId(10L);
    }

    @Test
    void searchApplicationsByName_shouldDelegateToRepository() {
        JobApplication application = new JobApplication();

        when(jobApplicationRepository
                .findByApplicantNameContainingIgnoreCase("john"))
                .thenReturn(List.of(application));

        List<JobApplication> result =
                jobApplicationService
                        .searchApplicationsByName("john");

        assertEquals(1, result.size());
        assertSame(application, result.get(0));

        verify(jobApplicationRepository)
                .findByApplicantNameContainingIgnoreCase("john");
    }

    @Test
    void searchApplicationsByEmail_shouldDelegateToRepository() {
        JobApplication application = new JobApplication();

        when(jobApplicationRepository
                .findByEmailContainingIgnoreCase("@example"))
                .thenReturn(List.of(application));

        List<JobApplication> result =
                jobApplicationService
                        .searchApplicationsByEmail("@example");

        assertEquals(1, result.size());
        assertSame(application, result.get(0));

        verify(jobApplicationRepository)
                .findByEmailContainingIgnoreCase("@example");
    }

    @Test
    void createApplication_shouldSetDefaultStatusAndAppliedDate() {
        Job job = new Job();
        job.setId(10L);

        JobApplication application = new JobApplication();
        application.setJob(job);
        application.setApplicantName("John Doe");
        application.setEmail("john@example.com");

        when(jobRepository.findById(10L))
                .thenReturn(Optional.of(job));

        when(jobApplicationRepository.save(application))
                .thenReturn(application);

        JobApplication result =
                jobApplicationService.createApplication(application);

        assertNotNull(result);
        assertSame(job, result.getJob());
        assertEquals("John Doe", result.getApplicantName());
        assertEquals("john@example.com", result.getEmail());

        assertEquals(
                JobApplication.ApplicationStatus.APPLIED,
                result.getStatus()
        );

        assertEquals(LocalDate.now(), result.getAppliedDate());

        verify(jobRepository).findById(10L);
        verify(jobApplicationRepository).save(application);
    }

    @Test
    void createApplication_shouldPreserveProvidedStatusAndAppliedDate() {
        Job job = new Job();
        job.setId(11L);

        LocalDate appliedDate =
                LocalDate.of(2026, 9, 15);

        JobApplication application = new JobApplication();
        application.setJob(job);
        application.setApplicantName("Jane Doe");
        application.setEmail("jane@example.com");
        application.setStatus(
                JobApplication.ApplicationStatus.INTERVIEW
        );
        application.setAppliedDate(appliedDate);

        when(jobRepository.findById(11L))
                .thenReturn(Optional.of(job));

        when(jobApplicationRepository.save(application))
                .thenReturn(application);

        JobApplication result =
                jobApplicationService.createApplication(application);

        assertEquals(
                JobApplication.ApplicationStatus.INTERVIEW,
                result.getStatus()
        );

        assertEquals(
                appliedDate,
                result.getAppliedDate()
        );

        verify(jobApplicationRepository).save(application);
    }

    @Test
    void createApplication_shouldRejectMissingJob() {
        JobApplication application = new JobApplication();
        application.setApplicantName("John Doe");
        application.setEmail("john@example.com");

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> jobApplicationService
                                .createApplication(application)
                );

        assertEquals(
                "Job is required.",
                exception.getMessage()
        );

        verifyNoInteractions(
                jobRepository,
                jobApplicationRepository
        );
    }

    @Test
    void createApplication_shouldRejectJobWithoutId() {
        Job job = new Job();

        JobApplication application = new JobApplication();
        application.setJob(job);
        application.setApplicantName("John Doe");
        application.setEmail("john@example.com");

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> jobApplicationService
                                .createApplication(application)
                );

        assertEquals(
                "Job is required.",
                exception.getMessage()
        );

        verifyNoInteractions(
                jobRepository,
                jobApplicationRepository
        );
    }

    @Test
    void createApplication_shouldRejectNonExistingJob() {
        Job job = new Job();
        job.setId(999L);

        JobApplication application = new JobApplication();
        application.setJob(job);
        application.setApplicantName("John Doe");
        application.setEmail("john@example.com");

        when(jobRepository.findById(999L))
                .thenReturn(Optional.empty());

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> jobApplicationService
                                .createApplication(application)
                );

        assertEquals(
                "Job not found with id: 999",
                exception.getMessage()
        );

        verify(jobRepository).findById(999L);
        verify(jobApplicationRepository, never())
                .save(any(JobApplication.class));
    }

    @Test
    void createApplication_shouldRejectBlankApplicantName() {
        Job job = new Job();
        job.setId(10L);

        JobApplication application = new JobApplication();
        application.setJob(job);
        application.setApplicantName("   ");
        application.setEmail("john@example.com");

        when(jobRepository.findById(10L))
                .thenReturn(Optional.of(job));

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> jobApplicationService
                                .createApplication(application)
                );

        assertEquals(
                "Applicant name is required.",
                exception.getMessage()
        );

        verify(jobApplicationRepository, never())
                .save(any(JobApplication.class));
    }

    @Test
    void createApplication_shouldRejectMissingEmail() {
        Job job = new Job();
        job.setId(10L);

        JobApplication application = new JobApplication();
        application.setJob(job);
        application.setApplicantName("John Doe");

        when(jobRepository.findById(10L))
                .thenReturn(Optional.of(job));

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> jobApplicationService
                                .createApplication(application)
                );

        assertEquals(
                "Email is required.",
                exception.getMessage()
        );

        verify(jobApplicationRepository, never())
                .save(any(JobApplication.class));
    }

    @Test
    void createApplication_shouldRejectInvalidEmail() {
        Job job = new Job();
        job.setId(10L);

        JobApplication application = new JobApplication();
        application.setJob(job);
        application.setApplicantName("John Doe");
        application.setEmail("invalid-email");

        when(jobRepository.findById(10L))
                .thenReturn(Optional.of(job));

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> jobApplicationService
                                .createApplication(application)
                );

        assertEquals(
                "Please enter a valid email address.",
                exception.getMessage()
        );

        verify(jobApplicationRepository, never())
                .save(any(JobApplication.class));
    }

    @Test
    void updateApplication_shouldUpdateExistingApplication() {
        Job existingJob = new Job();
        existingJob.setId(10L);

        Job updatedJob = new Job();
        updatedJob.setId(20L);

        JobApplication existing = new JobApplication();
        existing.setJob(existingJob);
        existing.setApplicantName("Old Name");
        existing.setEmail("old@example.com");

        JobApplication updated = new JobApplication();
        updated.setJob(updatedJob);
        updated.setApplicantName("New Name");
        updated.setEmail("new@example.com");
        updated.setPhone("9999999999");
        updated.setResumeUrl("resume.pdf");
        updated.setCoverLetter("Cover letter");
        updated.setStatus(
                JobApplication.ApplicationStatus.HIRED
        );
        updated.setAppliedDate(
                LocalDate.of(2026, 9, 20)
        );
        updated.setNotes("Selected candidate");

        when(jobRepository.findById(20L))
                .thenReturn(Optional.of(updatedJob));

        when(jobApplicationRepository.findById(1L))
                .thenReturn(Optional.of(existing));

        when(jobApplicationRepository.save(existing))
                .thenReturn(existing);

        JobApplication result =
                jobApplicationService.updateApplication(
                        1L,
                        updated
                );

        assertSame(existing, result);

        assertSame(updatedJob, existing.getJob());
        assertEquals("New Name", existing.getApplicantName());
        assertEquals("new@example.com", existing.getEmail());
        assertEquals("9999999999", existing.getPhone());
        assertEquals("resume.pdf", existing.getResumeUrl());
        assertEquals("Cover letter", existing.getCoverLetter());
        assertEquals(
                JobApplication.ApplicationStatus.HIRED,
                existing.getStatus()
        );
        assertEquals(
                LocalDate.of(2026, 9, 20),
                existing.getAppliedDate()
        );
        assertEquals(
                "Selected candidate",
                existing.getNotes()
        );

        verify(jobApplicationRepository).findById(1L);
        verify(jobApplicationRepository).save(existing);
    }

    @Test
    void updateApplication_shouldThrowWhenApplicationDoesNotExist() {
        Job job = new Job();
        job.setId(10L);

        JobApplication updated = new JobApplication();
        updated.setJob(job);
        updated.setApplicantName("John Doe");
        updated.setEmail("john@example.com");

        when(jobRepository.findById(10L))
                .thenReturn(Optional.of(job));

        when(jobApplicationRepository.findById(999L))
                .thenReturn(Optional.empty());

        RuntimeException exception =
                assertThrows(
                        RuntimeException.class,
                        () -> jobApplicationService
                                .updateApplication(999L, updated)
                );

        assertEquals(
                "Job application not found with id: 999",
                exception.getMessage()
        );

        verify(jobApplicationRepository, never())
                .save(any(JobApplication.class));
    }

    @Test
    void deleteApplication_shouldDeleteById() {
        jobApplicationService.deleteApplication(5L);

        verify(jobApplicationRepository)
                .deleteById(5L);
    }
}

