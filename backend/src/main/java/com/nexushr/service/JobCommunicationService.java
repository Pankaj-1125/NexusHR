package com.nexushr.service;

import com.nexushr.entity.Employee;
import com.nexushr.entity.Job;
import com.nexushr.repository.EmployeeRepository;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class JobCommunicationService {

    private final EmployeeRepository employeeRepository;
    private final NotificationService notificationService;
    private final EmailNotificationService emailNotificationService;
    private final SmsNotificationService smsNotificationService;
    private final JobAlertPdfService jobAlertPdfService;

    public JobCommunicationService(
            EmployeeRepository employeeRepository,
            NotificationService notificationService,
            EmailNotificationService emailNotificationService,
            SmsNotificationService smsNotificationService,
            JobAlertPdfService jobAlertPdfService
    ) {
        this.employeeRepository = employeeRepository;
        this.notificationService = notificationService;
        this.emailNotificationService = emailNotificationService;
        this.smsNotificationService = smsNotificationService;
        this.jobAlertPdfService = jobAlertPdfService;
    }

    public Map<String, Object> announceJob(
            Job job,
            String subject,
            String message,
            boolean sendEmail,
            boolean sendSms,
            String senderUsername,
            String senderRole
    ) {
        if (job == null) throw new RuntimeException("Job is required");

        String finalSubject = subject == null || subject.isBlank()
                ? "New Job Opportunity: " + job.getTitle()
                : subject.trim();
        String finalMessage = message == null || message.isBlank()
                ? defaultMessage(job)
                : message.trim();

        byte[] pdf = jobAlertPdfService.generate(job, finalSubject, senderUsername, senderRole);
        String fileName = "NexusHR_Job_Alert_" + job.getId() + ".pdf";

        List<Employee> employees = employeeRepository.findAll().stream()
                .filter(this::isActiveEmployee)
                .toList();

        int notified = 0;
        int emailSent = 0;
        int emailSkipped = 0;
        int emailFailed = 0;
        int smsSent = 0;
        int smsSkipped = 0;
        int smsFailed = 0;
        String smsMode = "NONE";

        for (Employee employee : employees) {
            notificationService.createNotification(
                    employee.getId(),
                    finalSubject,
                    finalMessage,
                    "JOB_ALERT",
                    senderUsername,
                    senderRole
            );
            notified++;

            if (sendEmail) {
                EmailNotificationService.DeliveryResult result =
                        emailNotificationService.sendWithPdfAttachment(
                                employee,
                                finalSubject,
                                buildEmailBody(job, finalMessage, senderUsername, senderRole),
                                fileName,
                                pdf
                        );
                switch (result.status()) {
                    case "SENT" -> emailSent++;
                    case "FAILED" -> emailFailed++;
                    default -> emailSkipped++;
                }
            }

            if (sendSms) {
                SmsNotificationService.DeliveryResult result =
                        smsNotificationService.sendSms(
                                employee,
                                finalSubject,
                                shortSms(job)
                        );
                smsMode = result.mode();
                switch (result.status()) {
                    case "SENT" -> smsSent++;
                    case "FAILED" -> smsFailed++;
                    default -> smsSkipped++;
                }
            }
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("jobId", job.getId());
        response.put("subject", finalSubject);
        response.put("notified", notified);
        response.put("senderUsername", senderUsername);
        response.put("senderRole", senderRole);
        response.put("pdfFileName", fileName);
        response.put("message", "Job alert sent to " + notified + " active employee(s)");
        response.put("email", Map.of(
                "enabled", sendEmail,
                "sent", emailSent,
                "skipped", emailSkipped,
                "failed", emailFailed
        ));
        response.put("sms", Map.of(
                "enabled", sendSms,
                "sent", smsSent,
                "skipped", smsSkipped,
                "failed", smsFailed,
                "mode", smsMode
        ));
        return response;
    }

    private String defaultMessage(Job job) {
        return "A new internal job opportunity is available: " + job.getTitle()
                + " in " + job.getDepartment() + " at " + job.getLocation()
                + ". Open NexusHR Recruitment for full details.";
    }

    private String shortSms(Job job) {
        return job.getTitle() + " | " + job.getDepartment() + " | " + job.getLocation()
                + ". Check NexusHR or your email for the full PDF job alert.";
    }

    private String buildEmailBody(Job job, String message, String sender, String role) {
        return "NexusHR Recruitment Alert\n\n"
                + message + "\n\n"
                + "Position: " + job.getTitle() + "\n"
                + "Department: " + job.getDepartment() + "\n"
                + "Location: " + job.getLocation() + "\n"
                + "Employment Type: " + job.getEmploymentType() + "\n"
                + "Closing Date: " + (job.getClosingDate() == null ? "Open until filled" : job.getClosingDate()) + "\n\n"
                + "The complete job description and requirements are attached as a PDF.\n\n"
                + "Sent by: " + sender + " (" + role + ")\n"
                + "NexusHR Workforce Intelligence";
    }

    private boolean isActiveEmployee(Employee employee) {
        return employee.getEmploymentStatus() == null
                || !"INACTIVE".equalsIgnoreCase(employee.getEmploymentStatus());
    }
}
