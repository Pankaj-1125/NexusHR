package com.nexushr.service;

import com.nexushr.entity.Employee;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailNotificationService {

    private final JavaMailSender mailSender;
    private final AuditLogService auditLogService;
    private final Environment environment;

    public EmailNotificationService(
            JavaMailSender mailSender,
            AuditLogService auditLogService,
            Environment environment
    ) {
        this.mailSender = mailSender;
        this.auditLogService = auditLogService;
        this.environment = environment;
    }

    public DeliveryResult send(
            Employee employee,
            String subject,
            String message
    ) {
        if (employee == null) {
            return DeliveryResult.failed("Employee is required");
        }
        return sendInternal(
                employee.getEmail(), subject, message,
                "EMPLOYEE", employee.getId()
        );
    }

    public DeliveryResult sendToAddress(
            String recipient,
            String subject,
            String message
    ) {
        return sendInternal(
                recipient, subject, message,
                "AUTHENTICATION", null
        );
    }

    public DeliveryResult sendWithPdfAttachment(
            Employee employee,
            String subject,
            String message,
            String fileName,
            byte[] pdfBytes
    ) {
        if (employee == null) {
            return DeliveryResult.failed("Employee is required");
        }
        return sendWithAttachmentInternal(
                employee.getEmail(),
                subject,
                message,
                fileName,
                pdfBytes,
                "application/pdf",
                "EMPLOYEE",
                employee.getId()
        );
    }

    private DeliveryResult sendInternal(
            String recipientValue,
            String subject,
            String message,
            String entityType,
            Long entityId
    ) {
        String validationError = validateConfiguration(recipientValue);
        if (validationError != null) {
            return validationError.startsWith("SKIP:")
                    ? DeliveryResult.skipped(validationError.substring(5))
                    : DeliveryResult.failed(validationError);
        }

        String recipient = cleanEmail(recipientValue);
        String sender = cleanEmail(resolveSender());

        try {
            SimpleMailMessage mailMessage = new SimpleMailMessage();
            mailMessage.setFrom(sender);
            mailMessage.setTo(recipient);
            mailMessage.setSubject(normalizeSubject(subject));
            mailMessage.setText(message == null ? "" : message.trim());
            mailSender.send(mailMessage);

            auditLogService.log(
                    "NOTIFICATION_EMAIL_SENT",
                    entityType,
                    entityId,
                    "Email notification sent to " + recipient
            );
            return DeliveryResult.sent("Email sent to " + recipient);

        } catch (MailException ex) {
            return handleFailure(ex, recipient, entityType, entityId);
        } catch (Exception ex) {
            return handleFailure(ex, recipient, entityType, entityId);
        }
    }

    private DeliveryResult sendWithAttachmentInternal(
            String recipientValue,
            String subject,
            String message,
            String fileName,
            byte[] attachment,
            String contentType,
            String entityType,
            Long entityId
    ) {
        String validationError = validateConfiguration(recipientValue);
        if (validationError != null) {
            return validationError.startsWith("SKIP:")
                    ? DeliveryResult.skipped(validationError.substring(5))
                    : DeliveryResult.failed(validationError);
        }
        if (attachment == null || attachment.length == 0) {
            return DeliveryResult.failed("PDF attachment is empty");
        }

        String recipient = cleanEmail(recipientValue);
        String sender = cleanEmail(resolveSender());

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setFrom(sender);
            helper.setTo(recipient);
            helper.setSubject(normalizeSubject(subject));
            helper.setText(message == null ? "" : message.trim(), false);
            helper.addAttachment(
                    fileName == null || fileName.isBlank() ? "NexusHR.pdf" : fileName,
                    new ByteArrayResource(attachment),
                    contentType == null ? "application/octet-stream" : contentType
            );
            mailSender.send(mimeMessage);

            auditLogService.log(
                    "NOTIFICATION_EMAIL_ATTACHMENT_SENT",
                    entityType,
                    entityId,
                    "Email with PDF attachment sent to " + recipient
            );
            return DeliveryResult.sent("Email with PDF sent to " + recipient);

        } catch (Exception ex) {
            return handleFailure(ex, recipient, entityType, entityId);
        }
    }

    private String validateConfiguration(String recipientValue) {
        if (!resolveEnabled()) {
            return "SKIP:Email delivery is disabled";
        }
        String recipient = cleanEmail(recipientValue);
        if (recipient == null || recipient.isBlank()) {
            return "SKIP:Recipient email is not available";
        }
        if (!isValidEmail(recipient)) {
            return "Invalid recipient email address";
        }
        String sender = cleanEmail(resolveSender());
        if (sender == null || sender.isBlank()) {
            return "Sender email is not configured";
        }
        if (!isValidEmail(sender)) {
            return "Invalid sender email configuration";
        }
        return null;
    }

    private DeliveryResult handleFailure(
            Throwable ex,
            String recipient,
            String entityType,
            Long entityId
    ) {
        String detail = rootMessage(ex);
        auditLogService.log(
                "NOTIFICATION_EMAIL_FAILED",
                entityType,
                entityId,
                "Email notification failed for " + recipient + ": " + detail
        );
        return DeliveryResult.failed("Email failed: " + detail);
    }

    private String normalizeSubject(String subject) {
        return subject == null || subject.isBlank()
                ? "NexusHR Notification"
                : subject.trim();
    }

    private boolean resolveEnabled() {
        String value = firstNonBlank(
                environment.getProperty("nexushr.notifications.email.enabled"),
                System.getenv("MAIL_ENABLED"),
                System.getenv("EMAIL_ENABLED")
        );
        return value != null && Boolean.parseBoolean(value.trim());
    }

    private String resolveSender() {
        return firstNonBlank(
                environment.getProperty("nexushr.notifications.email.from"),
                environment.getProperty("spring.mail.username"),
                System.getenv("MAIL_FROM"),
                System.getenv("MAIL_USERNAME")
        );
    }

    private String cleanEmail(String value) {
        if (value == null) return null;
        String cleaned = value.trim();
        if (cleaned.length() >= 2 &&
                ((cleaned.startsWith("\"") && cleaned.endsWith("\"")) ||
                        (cleaned.startsWith("'") && cleaned.endsWith("'")))) {
            cleaned = cleaned.substring(1, cleaned.length() - 1).trim();
        }
        if (cleaned.startsWith("<") && cleaned.endsWith(">")) {
            cleaned = cleaned.substring(1, cleaned.length() - 1).trim();
        }
        return cleaned;
    }

    private boolean isValidEmail(String email) {
        try {
            InternetAddress address = new InternetAddress(email, true);
            address.validate();
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    private String firstNonBlank(String... values) {
        if (values == null) return null;
        for (String value : values) {
            if (value != null && !value.isBlank()) return value;
        }
        return null;
    }

    private String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        Throwable deepest = throwable;
        while (current != null) {
            deepest = current;
            current = current.getCause();
        }
        String message = deepest.getMessage();
        if (message == null || message.isBlank()) {
            message = deepest.getClass().getSimpleName();
        }
        return message;
    }

    public record DeliveryResult(String status, String message) {
        public static DeliveryResult sent(String message) {
            return new DeliveryResult("SENT", message);
        }
        public static DeliveryResult skipped(String message) {
            return new DeliveryResult("SKIPPED", message);
        }
        public static DeliveryResult failed(String message) {
            return new DeliveryResult("FAILED", message);
        }
    }
}
