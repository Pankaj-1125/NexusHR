package com.nexushr.service;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.BearerToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleRefreshTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Message;

import com.nexushr.entity.Employee;

import jakarta.activation.DataHandler;
import jakarta.activation.DataSource;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.util.ByteArrayDataSource;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;

import java.util.Base64;
import java.util.Properties;

@Service
public class EmailNotificationService {

    private static final String TOKEN_SERVER_URL =
            "https://oauth2.googleapis.com/token";

    private final AuditLogService auditLogService;
    private final Environment environment;


    public EmailNotificationService(
            AuditLogService auditLogService,
            Environment environment
    ) {

        this.auditLogService =
                auditLogService;

        this.environment =
                environment;
    }


    // =========================================================
    // SEND EMAIL TO EMPLOYEE
    // =========================================================

    public DeliveryResult send(
            Employee employee,
            String subject,
            String message
    ) {

        if (employee == null) {

            return DeliveryResult.failed(
                    "Employee is required"
            );
        }


        return sendInternal(
                employee.getEmail(),
                subject,
                message,
                "EMPLOYEE",
                employee.getId()
        );
    }


    // =========================================================
    // SEND EMAIL DIRECTLY TO ADDRESS
    // =========================================================

    public DeliveryResult sendToAddress(
            String recipient,
            String subject,
            String message
    ) {

        return sendInternal(
                recipient,
                subject,
                message,
                "AUTHENTICATION",
                null
        );
    }


    // =========================================================
    // SEND EMAIL WITH PDF ATTACHMENT
    // =========================================================

    public DeliveryResult sendWithPdfAttachment(
            Employee employee,
            String subject,
            String message,
            String fileName,
            byte[] pdfBytes
    ) {

        if (employee == null) {

            return DeliveryResult.failed(
                    "Employee is required"
            );
        }


        if (pdfBytes == null
                || pdfBytes.length == 0) {

            return DeliveryResult.failed(
                    "PDF attachment is empty"
            );
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


    // =========================================================
    // NORMAL EMAIL
    // =========================================================

    private DeliveryResult sendInternal(
            String recipientValue,
            String subject,
            String message,
            String entityType,
            Long entityId
    ) {

        String validationError =
                validateConfiguration(
                        recipientValue
                );


        if (validationError != null) {

            if (validationError.startsWith(
                    "SKIP:"
            )) {

                return DeliveryResult.skipped(
                        validationError.substring(5)
                );
            }


            return DeliveryResult.failed(
                    validationError
            );
        }


        String recipient =
                cleanEmail(
                        recipientValue
                );


        String sender =
                cleanEmail(
                        resolveSender()
                );


        try {

            MimeMessage mimeMessage =
                    createTextMessage(
                            sender,
                            recipient,
                            normalizeSubject(
                                    subject
                            ),
                            message
                    );


            sendViaGmailApi(
                    mimeMessage
            );


            auditLogService.log(
                    "NOTIFICATION_EMAIL_SENT",
                    entityType,
                    entityId,
                    "Email notification sent to "
                            + recipient
                            + " using Gmail API"
            );


            System.out.println(
                    "[GMAIL API] Email sent to "
                            + recipient
            );


            return DeliveryResult.sent(
                    "Email sent to "
                            + recipient
            );


        } catch (Exception ex) {

            return handleFailure(
                    ex,
                    recipient,
                    entityType,
                    entityId
            );
        }
    }


    // =========================================================
    // EMAIL WITH ATTACHMENT
    // =========================================================

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

        String validationError =
                validateConfiguration(
                        recipientValue
                );


        if (validationError != null) {

            if (validationError.startsWith(
                    "SKIP:"
            )) {

                return DeliveryResult.skipped(
                        validationError.substring(5)
                );
            }


            return DeliveryResult.failed(
                    validationError
            );
        }


        if (attachment == null
                || attachment.length == 0) {

            return DeliveryResult.failed(
                    "Attachment is empty"
            );
        }


        String recipient =
                cleanEmail(
                        recipientValue
                );


        String sender =
                cleanEmail(
                        resolveSender()
                );


        try {

            MimeMessage mimeMessage =
                    createAttachmentMessage(
                            sender,
                            recipient,
                            normalizeSubject(
                                    subject
                            ),
                            message,
                            fileName,
                            attachment,
                            contentType
                    );


            sendViaGmailApi(
                    mimeMessage
            );


            auditLogService.log(
                    "NOTIFICATION_EMAIL_ATTACHMENT_SENT",
                    entityType,
                    entityId,
                    "Email with attachment sent to "
                            + recipient
                            + " using Gmail API"
            );


            return DeliveryResult.sent(
                    "Email with PDF sent to "
                            + recipient
            );


        } catch (Exception ex) {

            return handleFailure(
                    ex,
                    recipient,
                    entityType,
                    entityId
            );
        }
    }


    // =========================================================
    // GMAIL API
    // =========================================================

    private void sendViaGmailApi(
            MimeMessage mimeMessage
    ) throws Exception {

        String clientId =
                resolveClientId();

        String clientSecret =
                resolveClientSecret();

        String refreshToken =
                resolveRefreshToken();


        /*
         * Exchange refresh token for a fresh OAuth access token.
         */
        GoogleTokenResponse tokenResponse =
                new GoogleRefreshTokenRequest(
                        GoogleNetHttpTransport
                                .newTrustedTransport(),

                        GsonFactory
                                .getDefaultInstance(),

                        refreshToken,

                        clientId,

                        clientSecret
                )
                        .execute();


        String accessToken =
                tokenResponse
                        .getAccessToken();


        if (accessToken == null
                || accessToken.isBlank()) {

            throw new RuntimeException(
                    "Google did not return an access token"
            );
        }


        Credential credential =
                new Credential(
                        BearerToken
                                .authorizationHeaderAccessMethod()
                );


        credential.setAccessToken(
                accessToken
        );


        Gmail gmail =
                new Gmail.Builder(
                        GoogleNetHttpTransport
                                .newTrustedTransport(),

                        GsonFactory
                                .getDefaultInstance(),

                        credential
                )
                        .setApplicationName(
                                resolveApplicationName()
                        )
                        .build();


        ByteArrayOutputStream buffer =
                new ByteArrayOutputStream();


        mimeMessage.writeTo(
                buffer
        );


        String encodedEmail =
                Base64
                        .getUrlEncoder()
                        .withoutPadding()
                        .encodeToString(
                                buffer.toByteArray()
                        );


        Message gmailMessage =
                new Message();


        gmailMessage.setRaw(
                encodedEmail
        );


        gmail
                .users()
                .messages()
                .send(
                        "me",
                        gmailMessage
                )
                .execute();
    }


    // =========================================================
    // CREATE TEXT EMAIL
    // =========================================================

    private MimeMessage createTextMessage(
            String sender,
            String recipient,
            String subject,
            String message
    ) throws Exception {

        Session session =
                Session.getInstance(
                        new Properties()
                );


        MimeMessage email =
                new MimeMessage(
                        session
                );


        email.setFrom(
                new InternetAddress(
                        sender,
                        "NexusHR"
                )
        );


        email.setRecipient(
                jakarta.mail.Message.RecipientType.TO,
                new InternetAddress(
                        recipient
                )
        );


        email.setSubject(
                subject,
                "UTF-8"
        );


        email.setText(
                message == null
                        ? ""
                        : message.trim(),
                "UTF-8"
        );


        return email;
    }


    // =========================================================
    // CREATE EMAIL WITH ATTACHMENT
    // =========================================================

    private MimeMessage createAttachmentMessage(
            String sender,
            String recipient,
            String subject,
            String message,
            String fileName,
            byte[] attachment,
            String contentType
    ) throws Exception {

        Session session =
                Session.getInstance(
                        new Properties()
                );


        MimeMessage email =
                new MimeMessage(
                        session
                );


        email.setFrom(
                new InternetAddress(
                        sender,
                        "NexusHR"
                )
        );


        email.setRecipient(
                jakarta.mail.Message.RecipientType.TO,
                new InternetAddress(
                        recipient
                )
        );


        email.setSubject(
                subject,
                "UTF-8"
        );


        MimeMultipart multipart =
                new MimeMultipart();


        MimeBodyPart textPart =
                new MimeBodyPart();


        textPart.setText(
                message == null
                        ? ""
                        : message.trim(),
                "UTF-8"
        );


        multipart.addBodyPart(
                textPart
        );


        DataSource dataSource =
                new ByteArrayDataSource(
                        attachment,
                        contentType == null
                                ? "application/octet-stream"
                                : contentType
                );


        MimeBodyPart attachmentPart =
                new MimeBodyPart();


        attachmentPart.setDataHandler(
                new DataHandler(
                        dataSource
                )
        );


        attachmentPart.setFileName(
                fileName == null
                        || fileName.isBlank()

                        ? "NexusHR.pdf"

                        : fileName.trim()
        );


        multipart.addBodyPart(
                attachmentPart
        );


        email.setContent(
                multipart
        );


        return email;
    }


    // =========================================================
    // CONFIGURATION VALIDATION
    // =========================================================

    private String validateConfiguration(
            String recipientValue
    ) {

        if (!resolveEnabled()) {

            return "SKIP:Email delivery is disabled";
        }


        String recipient =
                cleanEmail(
                        recipientValue
                );


        if (recipient == null
                || recipient.isBlank()) {

            return "SKIP:Recipient email is not available";
        }


        if (!isValidEmail(
                recipient
        )) {

            return "Invalid recipient email address";
        }


        String sender =
                cleanEmail(
                        resolveSender()
                );


        if (sender == null
                || sender.isBlank()) {

            return "Gmail sender email is not configured";
        }


        if (!isValidEmail(
                sender
        )) {

            return "Invalid Gmail sender email";
        }


        if (isBlank(
                resolveClientId()
        )) {

            return "Google client ID is not configured";
        }


        if (isBlank(
                resolveClientSecret()
        )) {

            return "Google client secret is not configured";
        }


        if (isBlank(
                resolveRefreshToken()
        )) {

            return "Google refresh token is not configured";
        }


        return null;
    }


    // =========================================================
    // FAILURE HANDLER
    // =========================================================

    private DeliveryResult handleFailure(
            Throwable ex,
            String recipient,
            String entityType,
            Long entityId
    ) {

        String detail =
                rootMessage(
                        ex
                );


        auditLogService.log(
                "NOTIFICATION_EMAIL_FAILED",
                entityType,
                entityId,
                "Email notification failed for "
                        + recipient
                        + ": "
                        + detail
        );


        System.err.println(
                "[GMAIL API FAILED] Recipient="
                        + recipient
                        + " Error="
                        + detail
        );


        return DeliveryResult.failed(
                "Email failed: "
                        + detail
        );
    }


    // =========================================================
    // CONFIG VALUES
    // =========================================================

    private boolean resolveEnabled() {

        String value =
                firstNonBlank(
                        environment.getProperty(
                                "nexushr.notifications.email.enabled"
                        ),

                        System.getenv(
                                "MAIL_ENABLED"
                        )
                );


        return value != null
                &&
                Boolean.parseBoolean(
                        value.trim()
                );
    }


    private String resolveSender() {

        return firstNonBlank(
                environment.getProperty(
                        "nexushr.gmail.sender-email"
                ),

                System.getenv(
                        "GMAIL_SENDER_EMAIL"
                ),

                environment.getProperty(
                        "nexushr.notifications.email.from"
                ),

                System.getenv(
                        "MAIL_FROM"
                )
        );
    }


    private String resolveClientId() {

        return firstNonBlank(
                environment.getProperty(
                        "nexushr.gmail.client-id"
                ),

                System.getenv(
                        "GMAIL_CLIENT_ID"
                )
        );
    }


    private String resolveClientSecret() {

        return firstNonBlank(
                environment.getProperty(
                        "nexushr.gmail.client-secret"
                ),

                System.getenv(
                        "GMAIL_CLIENT_SECRET"
                )
        );
    }


    private String resolveRefreshToken() {

        return firstNonBlank(
                environment.getProperty(
                        "nexushr.gmail.refresh-token"
                ),

                System.getenv(
                        "GMAIL_REFRESH_TOKEN"
                )
        );
    }


    private String resolveApplicationName() {

        return firstNonBlank(
                environment.getProperty(
                        "nexushr.gmail.application-name"
                ),

                System.getenv(
                        "GMAIL_APPLICATION_NAME"
                ),

                "NexusHR"
        );
    }


    // =========================================================
    // HELPERS
    // =========================================================

    private String normalizeSubject(
            String subject
    ) {

        return subject == null
                || subject.isBlank()

                ? "NexusHR Notification"

                : subject.trim();
    }


    private String cleanEmail(
            String value
    ) {

        if (value == null) {

            return null;
        }


        return value
                .trim();
    }


    private boolean isValidEmail(
            String email
    ) {

        try {

            InternetAddress address =
                    new InternetAddress(
                            email,
                            true
                    );


            address.validate();

            return true;


        } catch (Exception ex) {

            return false;
        }
    }


    private boolean isBlank(
            String value
    ) {

        return value == null
                ||
                value.isBlank();
    }


    private String firstNonBlank(
            String... values
    ) {

        if (values == null) {

            return null;
        }


        for (String value : values) {

            if (value != null
                    &&
                    !value.isBlank()) {

                return value;
            }
        }


        return null;
    }


    private String rootMessage(
            Throwable throwable
    ) {

        Throwable current =
                throwable;

        Throwable deepest =
                throwable;


        while (current != null) {

            deepest =
                    current;

            current =
                    current.getCause();
        }


        String message =
                deepest.getMessage();


        if (message == null
                || message.isBlank()) {

            message =
                    deepest
                            .getClass()
                            .getSimpleName();
        }


        return message;
    }


    // =========================================================
    // DELIVERY RESULT
    // =========================================================

    public record DeliveryResult(
            String status,
            String message
    ) {

        public static DeliveryResult sent(
                String message
        ) {

            return new DeliveryResult(
                    "SENT",
                    message
            );
        }


        public static DeliveryResult skipped(
                String message
        ) {

            return new DeliveryResult(
                    "SKIPPED",
                    message
            );
        }


        public static DeliveryResult failed(
                String message
        ) {

            return new DeliveryResult(
                    "FAILED",
                    message
            );
        }
    }
}