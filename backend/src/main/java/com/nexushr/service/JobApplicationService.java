package com.nexushr.service;

import com.nexushr.entity.Job;
import com.nexushr.entity.JobApplication;
import com.nexushr.repository.JobApplicationRepository;
import com.nexushr.repository.JobRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class JobApplicationService {

    private final JobApplicationRepository jobApplicationRepository;
    private final JobRepository jobRepository;

    @Autowired(required = false)
    private EmailNotificationService emailNotificationService;

    public JobApplicationService(
            JobApplicationRepository jobApplicationRepository,
            JobRepository jobRepository
    ) {
        this.jobApplicationRepository = jobApplicationRepository;
        this.jobRepository = jobRepository;
    }

    @Transactional(readOnly = true)
    public List<JobApplication> getAllApplications() {
        return jobApplicationRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<JobApplication> getApplicationById(Long id) {
        return jobApplicationRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<JobApplication> getApplicationsByStatus(
            JobApplication.ApplicationStatus status
    ) {
        return jobApplicationRepository.findByStatus(status);
    }

    @Transactional(readOnly = true)
    public List<JobApplication> getApplicationsByJobId(Long jobId) {
        return jobApplicationRepository.findByJobId(jobId);
    }

    @Transactional(readOnly = true)
    public List<JobApplication> searchApplicationsByName(String applicantName) {
        return jobApplicationRepository.findByApplicantNameContainingIgnoreCase(applicantName);
    }

    @Transactional(readOnly = true)
    public List<JobApplication> searchApplicationsByEmail(String email) {
        return jobApplicationRepository.findByEmailContainingIgnoreCase(email);
    }

    @Transactional(readOnly = true)
    public Optional<JobApplication> findPublicStatus(String reference, String email) {
        if (reference == null || reference.isBlank() || email == null || email.isBlank()) {
            return Optional.empty();
        }
        return jobApplicationRepository
                .findByApplicationReferenceIgnoreCaseAndEmailIgnoreCase(reference.trim(), email.trim());
    }

    public JobApplication createApplication(JobApplication application) {
        validateApplication(application, false);

        if (application.getAppliedDate() == null) {
            application.setAppliedDate(LocalDate.now());
        }
        if (application.getSource() == null || application.getSource().isBlank()) {
            application.setSource("INTERNAL");
        }

        ensureReference(application);
        return jobApplicationRepository.save(application);
    }

    /**
     * Public/Google-Forms-like application submission. The client cannot choose a
     * privileged recruitment status; every external submission starts as APPLIED.
     */
    public JobApplication createPublicApplication(JobApplication application) {
        application.setStatus(JobApplication.ApplicationStatus.APPLIED);
        application.setAppliedDate(LocalDate.now());
        application.setSource(
                application.getSource() == null || application.getSource().isBlank()
                        ? "PUBLIC_PORTAL"
                        : application.getSource().trim().toUpperCase(Locale.ROOT)
        );

        validateApplication(application, true);

        ensureReference(application);
        JobApplication saved = jobApplicationRepository.save(application);

        sendCandidateEmail(
                saved,
                "Application received - " + saved.getJob().getTitle(),
                "Hello " + saved.getApplicantName() + ",\n\n"
                        + "Your application for " + saved.getJob().getTitle() + " has been received by NexusHR.\n"
                        + "Application reference: " + saved.getApplicationReference() + "\n"
                        + "Current status: APPLIED\n\n"
                        + "Keep this reference to track your application status on the Careers page.\n\n"
                        + "Regards,\nRecruitment Team"
        );

        return saved;
    }

    public JobApplication updateApplication(Long id, JobApplication updatedApplication) {
        validateApplication(updatedApplication, false);

        return jobApplicationRepository.findById(id)
                .map(existing -> {
                    existing.setJob(updatedApplication.getJob());
                    existing.setApplicantName(updatedApplication.getApplicantName());
                    existing.setEmail(updatedApplication.getEmail());
                    existing.setPhone(updatedApplication.getPhone());
                    existing.setResumeUrl(updatedApplication.getResumeUrl());
                    existing.setCoverLetter(updatedApplication.getCoverLetter());
                    existing.setAppliedDate(updatedApplication.getAppliedDate());
                    existing.setNotes(updatedApplication.getNotes());
                    existing.setQualification(updatedApplication.getQualification());
                    existing.setExperienceYears(updatedApplication.getExperienceYears());
                    existing.setSkills(updatedApplication.getSkills());
                    existing.setCurrentCity(updatedApplication.getCurrentCity());
                    existing.setLinkedinUrl(updatedApplication.getLinkedinUrl());
                    existing.setPortfolioUrl(updatedApplication.getPortfolioUrl());
                    existing.setExpectedSalary(updatedApplication.getExpectedSalary());
                    existing.setNoticePeriod(updatedApplication.getNoticePeriod());
                    existing.setRecruiterNotes(updatedApplication.getRecruiterNotes());

                    // Preserve workflow audit fields while allowing explicit status edits
                    // from authenticated recruitment management screens.
                    if (updatedApplication.getStatus() != null) {
                        existing.setStatus(updatedApplication.getStatus());
                    }

                    return jobApplicationRepository.save(existing);
                })
                .orElseThrow(() -> new RuntimeException("Job application not found with id: " + id));
    }

    public JobApplication moveToScreening(Long id, Authentication authentication) {
        JobApplication application = requireApplication(id);
        application.setStatus(JobApplication.ApplicationStatus.SCREENING);
        application.setRecruiterNotes(appendNote(
                application.getRecruiterNotes(),
                "Moved to screening by " + actor(authentication)
        ));
        return jobApplicationRepository.save(application);
    }

    /** Manager recommendation stage. Admin final approval is still required. */
    public JobApplication managerShortlist(Long id, Authentication authentication, String note) {
        JobApplication application = requireApplication(id);
        application.setStatus(JobApplication.ApplicationStatus.MANAGER_SHORTLISTED);
        application.setManagerShortlistedBy(actor(authentication));
        application.setManagerShortlistedAt(LocalDateTime.now());
        if (note != null && !note.isBlank()) {
            application.setRecruiterNotes(appendNote(application.getRecruiterNotes(), note.trim()));
        }
        JobApplication saved = jobApplicationRepository.save(application);

        sendCandidateEmail(
                saved,
                "Recruitment update - " + saved.getJob().getTitle(),
                "Hello " + saved.getApplicantName() + ",\n\n"
                        + "Your application has progressed to the manager-shortlisted stage. "
                        + "Final recruitment approval is still pending.\n\n"
                        + "Application reference: " + saved.getApplicationReference() + "\n"
                        + "Regards,\nRecruitment Team"
        );
        return saved;
    }


    /** Admin can directly shortlist a candidate without waiting for manager recommendation. */
    public JobApplication adminDirectShortlist(Long id, Authentication authentication, String note) {
        JobApplication application = requireApplication(id);
        if (application.getStatus() == JobApplication.ApplicationStatus.REJECTED
                || application.getStatus() == JobApplication.ApplicationStatus.HIRED) {
            throw new IllegalStateException("This application cannot be directly shortlisted from its current status.");
        }
        application.setStatus(JobApplication.ApplicationStatus.ADMIN_APPROVED);
        application.setAdminApprovedBy(actor(authentication));
        application.setAdminApprovedAt(LocalDateTime.now());
        application.setRecruiterNotes(appendNote(
                application.getRecruiterNotes(),
                "Directly shortlisted by Admin " + actor(authentication)
                        + (note == null || note.isBlank() ? "" : ": " + note.trim())
        ));
        JobApplication saved = jobApplicationRepository.save(application);
        sendCandidateEmail(
                saved,
                "You are shortlisted - " + saved.getJob().getTitle(),
                "Hello " + saved.getApplicantName() + ",\n\n"
                        + "Your application has been directly shortlisted by the recruitment team for "
                        + saved.getJob().getTitle() + ".\n"
                        + "Interview details will be shared after scheduling.\n\n"
                        + "Application reference: " + saved.getApplicationReference() + "\n"
                        + "Regards,\nRecruitment Team"
        );
        return saved;
    }

    /** Final shortlist approval owned by ADMIN. */
    public JobApplication adminApprove(Long id, Authentication authentication, String note) {
        JobApplication application = requireApplication(id);
        if (application.getStatus() != JobApplication.ApplicationStatus.MANAGER_SHORTLISTED) {
            throw new IllegalStateException("Manager shortlist is required before final admin approval.");
        }
        application.setStatus(JobApplication.ApplicationStatus.ADMIN_APPROVED);
        application.setAdminApprovedBy(actor(authentication));
        application.setAdminApprovedAt(LocalDateTime.now());
        if (note != null && !note.isBlank()) {
            application.setRecruiterNotes(appendNote(application.getRecruiterNotes(), note.trim()));
        }
        JobApplication saved = jobApplicationRepository.save(application);

        sendCandidateEmail(
                saved,
                "You are shortlisted - " + saved.getJob().getTitle(),
                "Hello " + saved.getApplicantName() + ",\n\n"
                        + "Your application has received final shortlist approval for "
                        + saved.getJob().getTitle() + ".\n"
                        + "Our recruitment team will contact you with the next interview steps.\n\n"
                        + "Application reference: " + saved.getApplicationReference() + "\n"
                        + "Regards,\nRecruitment Team"
        );
        return saved;
    }

    public JobApplication setInterview(Long id, Authentication authentication, String note) {
        JobApplication application = requireApplication(id);
        application.setStatus(JobApplication.ApplicationStatus.INTERVIEW);
        if (note != null && !note.isBlank()) {
            application.setRecruiterNotes(appendNote(application.getRecruiterNotes(), note.trim()));
        }
        JobApplication saved = jobApplicationRepository.save(application);
        sendCandidateEmail(
                saved,
                "Interview stage - " + saved.getJob().getTitle(),
                "Hello " + saved.getApplicantName() + ",\n\n"
                        + "Your application is now in the interview stage. Please wait for interview scheduling details.\n\n"
                        + "Application reference: " + saved.getApplicationReference() + "\n"
                        + "Regards,\nRecruitment Team"
        );
        return saved;
    }

    /**
     * Final selection after the interview. This is intentionally ADMIN-owned.
     * The OFFER stage is used as the final approved/selected stage before hiring.
     */
    public JobApplication finalApproveAfterInterview(
            Long id,
            Authentication authentication,
            String finalMessage,
            String requiredDocuments,
            String reportingInstructions
    ) {
        JobApplication application = requireApplication(id);

        if (application.getStatus() != JobApplication.ApplicationStatus.INTERVIEW_COMPLETED) {
            throw new IllegalStateException("Final approval is available only after the interview has been marked completed.");
        }

        String message = finalMessage == null || finalMessage.isBlank()
                ? "Congratulations! You have successfully cleared the interview and have been selected for the next joining formalities."
                : finalMessage.trim();

        String documents = requiredDocuments == null || requiredDocuments.isBlank()
                ? "Government photo ID, updated resume, educational certificates/marksheets, address proof, PAN card, Aadhaar card, passport-size photographs, and previous employment documents if applicable."
                : requiredDocuments.trim();

        String instructions = reportingInstructions == null || reportingInstructions.isBlank()
                ? "Please keep the required documents ready. The HR team will contact you for document verification and joining formalities."
                : reportingInstructions.trim();

        application.setStatus(JobApplication.ApplicationStatus.OFFER);
        application.setFinalSelectionMessage(message);
        application.setRequiredDocuments(documents);
        application.setReportingInstructions(instructions);
        application.setFinalApprovedBy(actor(authentication));
        application.setFinalApprovedAt(LocalDateTime.now());
        application.setRecruiterNotes(appendNote(
                application.getRecruiterNotes(),
                "Final post-interview approval by Admin " + actor(authentication)
        ));

        JobApplication saved = jobApplicationRepository.save(application);

        sendCandidateEmail(
                saved,
                "Congratulations - Final selection for " + saved.getJob().getTitle(),
                "Hello " + saved.getApplicantName() + ",\n\n"
                        + message + "\n\n"
                        + "Position: " + saved.getJob().getTitle() + "\n"
                        + "Application reference: " + saved.getApplicationReference() + "\n\n"
                        + "Required documents:\n" + documents + "\n\n"
                        + "Additional / reporting instructions:\n" + instructions + "\n\n"
                        + "Please carry original documents for verification where requested.\n\n"
                        + "Regards,\nRecruitment Team"
        );

        return saved;
    }

    public JobApplication reject(Long id, Authentication authentication, String reason) {
        JobApplication application = requireApplication(id);
        application.setStatus(JobApplication.ApplicationStatus.REJECTED);
        application.setRejectionReason(reason == null ? null : reason.trim());
        application.setRecruiterNotes(appendNote(
                application.getRecruiterNotes(),
                "Rejected by " + actor(authentication)
                        + (reason == null || reason.isBlank() ? "" : ": " + reason.trim())
        ));
        JobApplication saved = jobApplicationRepository.save(application);
        sendCandidateEmail(
                saved,
                "Recruitment update - " + saved.getJob().getTitle(),
                "Hello " + saved.getApplicantName() + ",\n\n"
                        + "Thank you for applying for " + saved.getJob().getTitle() + ". "
                        + "After review, we will not be progressing this application further at this time.\n\n"
                        + "Application reference: " + saved.getApplicationReference() + "\n"
                        + "Regards,\nRecruitment Team"
        );
        return saved;
    }

    public void deleteApplication(Long id) {
        jobApplicationRepository.deleteById(id);
    }

    private JobApplication requireApplication(Long id) {
        return jobApplicationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Job application not found with id: " + id));
    }

    private void ensureReference(JobApplication application) {
        if (application.getApplicationReference() == null || application.getApplicationReference().isBlank()) {
            String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase(Locale.ROOT);
            application.setApplicationReference(
                    "NXR-APP-" + LocalDate.now().getYear() + "-" + suffix
            );
        }
    }

    private void validateApplication(JobApplication application, boolean publicSubmission) {
        if (application == null) {
            throw new IllegalArgumentException("Application is required.");
        }
        if (application.getJob() == null || application.getJob().getId() == null) {
            throw new IllegalArgumentException("Job is required.");
        }

        Long jobId = application.getJob().getId();
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job not found with id: " + jobId));

        if (publicSubmission) {
            if (job.getStatus() != Job.JobStatus.OPEN) {
                throw new IllegalArgumentException("This job is not accepting applications.");
            }
            if (job.getClosingDate() != null && job.getClosingDate().isBefore(LocalDate.now())) {
                throw new IllegalArgumentException("Applications for this job are closed.");
            }
        }

        application.setJob(job);

        if (application.getApplicantName() == null || application.getApplicantName().isBlank()) {
            throw new IllegalArgumentException("Applicant name is required.");
        }
        if (application.getEmail() == null || application.getEmail().isBlank()) {
            throw new IllegalArgumentException("Email is required.");
        }
        if (!application.getEmail().contains("@")) {
            throw new IllegalArgumentException("Please enter a valid email address.");
        }
        if (application.getStatus() == null) {
            application.setStatus(JobApplication.ApplicationStatus.APPLIED);
        }
    }

    private String actor(Authentication authentication) {
        return authentication == null || authentication.getName() == null
                ? "SYSTEM"
                : authentication.getName();
    }

    private String appendNote(String original, String note) {
        if (note == null || note.isBlank()) return original;
        if (original == null || original.isBlank()) return note;
        return original + "\n" + note;
    }

    private void sendCandidateEmail(JobApplication application, String subject, String message) {
        if (emailNotificationService == null || application == null) return;
        try {
            emailNotificationService.sendToAddress(application.getEmail(), subject, message);
        } catch (Exception ignored) {
            // Recruitment workflow must remain functional even when SMTP is unavailable.
        }
    }
}
