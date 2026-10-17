package com.nexushr.service;

import com.nexushr.dto.InterviewScheduleRequest;
import com.nexushr.entity.InterviewSchedule;
import com.nexushr.entity.JobApplication;
import com.nexushr.repository.InterviewScheduleRepository;
import com.nexushr.repository.JobApplicationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class InterviewScheduleService {
    private final InterviewScheduleRepository interviewRepository;
    private final JobApplicationRepository applicationRepository;

    @Autowired(required = false)
    private EmailNotificationService emailNotificationService;

    public InterviewScheduleService(InterviewScheduleRepository interviewRepository,
                                    JobApplicationRepository applicationRepository) {
        this.interviewRepository = interviewRepository;
        this.applicationRepository = applicationRepository;
    }

    public InterviewSchedule schedule(Long applicationId, InterviewScheduleRequest request, Authentication authentication) {
        validate(request);
        JobApplication application = requireApprovedApplication(applicationId);

        InterviewSchedule interview = interviewRepository.findFirstByApplicationIdOrderByCreatedAtDesc(applicationId)
                .orElseGet(InterviewSchedule::new);
        boolean existing = interview.getId() != null;
        interview.setApplication(application);
        interview.setInterviewDate(request.interviewDate());
        interview.setInterviewTime(request.interviewTime());
        interview.setLocation(request.location().trim());
        interview.setMessage(trimToNull(request.message()));
        interview.setStatus(existing ? InterviewSchedule.InterviewStatus.RESCHEDULED : InterviewSchedule.InterviewStatus.SCHEDULED);
        interview.setSentBy(actor(authentication));
        interview.setSentByRole(role(authentication));

        InterviewSchedule saved = interviewRepository.save(interview);
        application.setStatus(JobApplication.ApplicationStatus.INTERVIEW);
        application.setRecruiterNotes(appendNote(application.getRecruiterNotes(),
                (existing ? "Interview rescheduled" : "Interview scheduled") + " by " + actor(authentication)
                        + " for " + saved.getInterviewDate() + " " + saved.getInterviewTime()
                        + " at " + saved.getLocation()));
        applicationRepository.save(application);
        sendInterviewEmail(saved);
        return saved;
    }

    @Transactional(readOnly = true)
    public List<InterviewSchedule> byApplication(Long applicationId) {
        return interviewRepository.findByApplicationIdOrderByCreatedAtDesc(applicationId);
    }

    @Transactional(readOnly = true)
    public InterviewSchedule latest(Long applicationId) {
        return interviewRepository.findFirstByApplicationIdOrderByCreatedAtDesc(applicationId).orElse(null);
    }

    public InterviewSchedule completeLatest(Long applicationId, Authentication authentication) {
        JobApplication application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Job application not found."));

        if (application.getStatus() != JobApplication.ApplicationStatus.INTERVIEW) {
            throw new IllegalStateException("The candidate must be in the interview stage before marking the interview completed.");
        }

        InterviewSchedule interview = interviewRepository.findFirstByApplicationIdOrderByCreatedAtDesc(applicationId)
                .orElseThrow(() -> new IllegalStateException("No interview schedule exists for this candidate."));

        if (interview.getStatus() == InterviewSchedule.InterviewStatus.CANCELLED) {
            throw new IllegalStateException("A cancelled interview cannot be marked completed.");
        }

        interview.setStatus(InterviewSchedule.InterviewStatus.COMPLETED);
        interview.setSentBy(actor(authentication));
        interview.setSentByRole(role(authentication));
        InterviewSchedule saved = interviewRepository.save(interview);

        application.setStatus(JobApplication.ApplicationStatus.INTERVIEW_COMPLETED);
        application.setRecruiterNotes(appendNote(application.getRecruiterNotes(),
                "Interview marked completed by " + actor(authentication)));
        applicationRepository.save(application);

        return saved;
    }

    public InterviewSchedule cancel(Long interviewId, Authentication authentication) {
        InterviewSchedule interview = interviewRepository.findById(interviewId)
                .orElseThrow(() -> new IllegalArgumentException("Interview schedule not found."));
        interview.setStatus(InterviewSchedule.InterviewStatus.CANCELLED);
        interview.setSentBy(actor(authentication));
        interview.setSentByRole(role(authentication));
        InterviewSchedule saved = interviewRepository.save(interview);
        sendEmail(saved.getApplication(), "Interview cancelled - " + saved.getApplication().getJob().getTitle(),
                "Hello " + saved.getApplication().getApplicantName() + ",\n\nYour interview scheduled for "
                        + saved.getInterviewDate() + " at " + saved.getInterviewTime() + " has been cancelled.\n\n"
                        + "Application reference: " + saved.getApplication().getApplicationReference() + "\nRegards,\nRecruitment Team");
        return saved;
    }

    private JobApplication requireApprovedApplication(Long id) {
        JobApplication application = applicationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Job application not found."));
        if (application.getStatus() != JobApplication.ApplicationStatus.ADMIN_APPROVED
                && application.getStatus() != JobApplication.ApplicationStatus.INTERVIEW) {
            throw new IllegalStateException("Admin shortlist approval is required before scheduling an interview.");
        }
        return application;
    }

    private void validate(InterviewScheduleRequest request) {
        if (request == null || request.interviewDate() == null || request.interviewTime() == null) {
            throw new IllegalArgumentException("Interview date and time are required.");
        }
        if (request.location() == null || request.location().isBlank()) {
            throw new IllegalArgumentException("Interview location is required.");
        }
    }

    private String actor(Authentication authentication) {
        return authentication == null || authentication.getName() == null ? "SYSTEM" : authentication.getName();
    }

    private String role(Authentication authentication) {
        if (authentication == null) return "SYSTEM";
        return authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith("ROLE_")).findFirst().orElse("SYSTEM").replace("ROLE_", "");
    }

    private String trimToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    private String appendNote(String original, String note) { return original == null || original.isBlank() ? note : original + "\n" + note; }

    private void sendInterviewEmail(InterviewSchedule interview) {
        JobApplication a = interview.getApplication();
        String custom = interview.getMessage() == null ? "Please attend the interview at the scheduled time." : interview.getMessage();
        sendEmail(a, "Interview scheduled - " + a.getJob().getTitle(),
                "Hello " + a.getApplicantName() + ",\n\n"
                        + custom + "\n\nPosition: " + a.getJob().getTitle()
                        + "\nDate: " + interview.getInterviewDate()
                        + "\nTime: " + interview.getInterviewTime()
                        + "\nLocation: " + interview.getLocation()
                        + "\n\nApplication reference: " + a.getApplicationReference()
                        + "\nRegards,\nRecruitment Team");
    }

    private void sendEmail(JobApplication application, String subject, String body) {
        if (emailNotificationService == null) return;
        try { emailNotificationService.sendToAddress(application.getEmail(), subject, body); }
        catch (Exception ignored) { }
    }
}
