package com.nexushr.service;

import com.nexushr.entity.Employee;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Service
public class SmsNotificationService {

    private static final Logger log = LoggerFactory.getLogger(SmsNotificationService.class);

    private final AuditLogService auditLogService;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${nexushr.notifications.sms.provider:SIMULATION}")
    private String provider;

    @Value("${nexushr.notifications.sms.simulation-enabled:true}")
    private boolean simulationEnabled;

    @Value("${twilio.account-sid:}")
    private String accountSid;

    @Value("${twilio.auth-token:}")
    private String authToken;

    @Value("${twilio.from-number:}")
    private String fromNumber;

    public SmsNotificationService(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    /**
     * Primary SMS method. If SMS_PROVIDER=TWILIO and credentials are configured,
     * a real SMS is sent. Otherwise NexusHR safely falls back to simulation.
     */
    public DeliveryResult sendSms(
            Employee employee,
            String subject,
            String message
    ) {
        if (employee == null) {
            return DeliveryResult.failed("Employee is required", "NONE");
        }

        String phone = normalizePhone(employee.getPhone());
        if (phone == null || phone.isBlank()) {
            return DeliveryResult.skipped("Employee phone number is not available", "NONE");
        }

        String smsText = buildSmsText(subject, message);
        if ("TWILIO".equalsIgnoreCase(provider)) {
            if (isBlank(accountSid) || isBlank(authToken) || isBlank(fromNumber)) {
                if (simulationEnabled) {
                    return simulate(employee, phone, smsText,
                            "Twilio is selected but credentials are incomplete; simulation used");
                }
                return DeliveryResult.failed("Twilio credentials are not configured", "TWILIO");
            }
            return sendTwilio(employee, phone, smsText);
        }

        if (simulationEnabled) {
            return simulate(employee, phone, smsText, "SMS simulation completed");
        }

        return DeliveryResult.skipped("SMS delivery is disabled", "NONE");
    }

    /** Backward-compatible method used by existing code/tests. */
    public DeliveryResult sendSimulatedSms(
            Employee employee,
            String title,
            String message
    ) {
        if (employee == null) {
            return DeliveryResult.failed("Employee is required", "SIMULATION");
        }
        String phone = normalizePhone(employee.getPhone());
        if (phone == null || phone.isBlank()) {
            return DeliveryResult.skipped("Employee phone number is not available", "SIMULATION");
        }
        return simulate(employee, phone, buildSmsText(title, message), "SMS simulation completed");
    }

    private DeliveryResult sendTwilio(
            Employee employee,
            String phone,
            String smsText
    ) {
        try {
            String url = "https://api.twilio.com/2010-04-01/Accounts/"
                    + accountSid.trim() + "/Messages.json";

            HttpHeaders headers = new HttpHeaders();
            headers.setBasicAuth(accountSid.trim(), authToken.trim());
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("To", phone);
            body.add("From", fromNumber.trim());
            body.add("Body", smsText);

            ResponseEntity<String> response = restTemplate.postForEntity(
                    url,
                    new HttpEntity<>(body, headers),
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                auditLogService.log(
                        "NOTIFICATION_SMS_SENT",
                        "EMPLOYEE",
                        employee.getId(),
                        "Real SMS sent through Twilio to " + maskPhone(phone)
                );
                return DeliveryResult.sent(
                        "SMS sent to " + maskPhone(phone),
                        "TWILIO"
                );
            }

            return DeliveryResult.failed(
                    "Twilio returned HTTP " + response.getStatusCode().value(),
                    "TWILIO"
            );

        } catch (Exception ex) {
            String detail = rootMessage(ex);
            auditLogService.log(
                    "NOTIFICATION_SMS_FAILED",
                    "EMPLOYEE",
                    employee.getId(),
                    "SMS failed for " + maskPhone(phone) + ": " + detail
            );
            return DeliveryResult.failed("SMS failed: " + detail, "TWILIO");
        }
    }

    private DeliveryResult simulate(
            Employee employee,
            String phone,
            String smsText,
            String detail
    ) {
        log.info(
                "[NexusHR SMS SIMULATION] employeeId={}, phone={}, message={}",
                employee.getId(),
                maskPhone(phone),
                smsText
        );
        auditLogService.log(
                "NOTIFICATION_SMS_SIMULATED",
                "EMPLOYEE",
                employee.getId(),
                "SMS simulation created for " + maskPhone(phone)
        );
        return DeliveryResult.sent(
                detail + " for " + maskPhone(phone),
                "SIMULATION"
        );
    }

    private String buildSmsText(String subject, String message) {
        String safeSubject = isBlank(subject) ? "NexusHR" : subject.trim();
        String safeMessage = message == null ? "" : message.trim();
        String combined = safeSubject + ": " + safeMessage;
        // Keep SMS intentionally short. Full details are sent through email/in-app.
        if (combined.length() <= 300) return combined;
        return combined.substring(0, 297) + "...";
    }

    private String normalizePhone(String value) {
        if (value == null) return null;
        String phone = value.trim().replace(" ", "").replace("-", "");
        if (phone.matches("^[6-9]\\d{9}$")) {
            return "+91" + phone;
        }
        return phone;
    }

    private String maskPhone(String phone) {
        String trimmed = phone == null ? "" : phone.trim();
        if (trimmed.length() <= 4) return "****";
        return "****" + trimmed.substring(trimmed.length() - 4);
    }

    private String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        Throwable deepest = throwable;
        while (current != null) {
            deepest = current;
            current = current.getCause();
        }
        String message = deepest.getMessage();
        return isBlank(message) ? deepest.getClass().getSimpleName() : message;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public record DeliveryResult(String status, String message, String mode) {
        public static DeliveryResult sent(String message, String mode) {
            return new DeliveryResult("SENT", message, mode);
        }
        public static DeliveryResult skipped(String message, String mode) {
            return new DeliveryResult("SKIPPED", message, mode);
        }
        public static DeliveryResult failed(String message, String mode) {
            return new DeliveryResult("FAILED", message, mode);
        }
    }
}
