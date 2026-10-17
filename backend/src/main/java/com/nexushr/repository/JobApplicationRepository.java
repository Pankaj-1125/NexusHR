package com.nexushr.repository;

import com.nexushr.entity.JobApplication;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JobApplicationRepository
        extends JpaRepository<JobApplication, Long> {

    List<JobApplication> findByStatus(
            JobApplication.ApplicationStatus status
    );

    List<JobApplication> findByJobId(Long jobId);

    List<JobApplication> findByEmailContainingIgnoreCase(
            String email
    );

    List<JobApplication> findByApplicantNameContainingIgnoreCase(
            String applicantName
    );

    Optional<JobApplication> findByApplicationReferenceIgnoreCaseAndEmailIgnoreCase(
            String applicationReference,
            String email
    );
}
