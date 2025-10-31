package com.stickerapp.utils;

import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Properties;
import com.stickerapp.utils.ResourceManager;

/**
 * Singleton error reporting utility for collecting, generating, and sending error reports.
 * Collects error details including timestamp, type, message, stack trace, and system info.
 * Can generate reports and optionally send them to a file or email.
 */
public class ErrorReporter {
    private static ErrorReporter instance;
    private final Logger logger = Logger.getInstance();
    private final ConfigManager config = ConfigManager.getInstance();

    private ErrorReporter() {}

    /**
     * Gets the singleton instance of ErrorReporter.
     * @return the ErrorReporter instance
     */
    public static synchronized ErrorReporter getInstance() {
        if (instance == null) {
            instance = new ErrorReporter();
        }
        return instance;
    }

    /**
     * Reports an error by collecting details and optionally generating/sending a report.
     * @param exception the exception that occurred
     * @param additionalInfo additional context information
     */
    public void reportError(Exception exception, String additionalInfo) {
        ErrorReport report = collectErrorDetails(exception, additionalInfo);
        logger.error(ErrorReporter.class.getSimpleName(), "Error reported: " + report.getMessage());

        // Generate and send report if enabled
        if (Boolean.parseBoolean(config.getProperty("error.reporting.enabled", "true"))) {
            String reportContent = generateReport(report);
            sendReport(reportContent);
        }
    }

    /**
     * Collects error details into an ErrorReport object.
     * @param exception the exception
     * @param additionalInfo additional context
     * @return the error report
     */
    private ErrorReport collectErrorDetails(Exception exception, String additionalInfo) {
        ErrorReport report = new ErrorReport();
        report.setTimestamp(LocalDateTime.now());
        report.setErrorType(exception.getClass().getSimpleName());
        report.setMessage(exception.getMessage() != null ? exception.getMessage() : "No message");
        report.setStackTrace(getStackTraceAsString(exception));
        report.setSystemInfo(collectSystemInfo());
        report.setAdditionalInfo(additionalInfo);
        return report;
    }

    /**
     * Generates a formatted error report string.
     * @param report the error report data
     * @return the formatted report
     */
    private String generateReport(ErrorReport report) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== ERROR REPORT ===\n");
        sb.append("Timestamp: ").append(report.getTimestamp().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)).append("\n");
        sb.append("Error Type: ").append(report.getErrorType()).append("\n");
        sb.append("Message: ").append(report.getMessage()).append("\n");
        sb.append("Additional Info: ").append(report.getAdditionalInfo() != null ? report.getAdditionalInfo() : "None").append("\n");
        sb.append("System Info:\n").append(report.getSystemInfo()).append("\n");
        sb.append("Stack Trace:\n").append(report.getStackTrace()).append("\n");
        sb.append("=== END REPORT ===\n");
        return sb.toString();
    }

    /**
     * Sends the error report to a configured destination (file, email, or SMS).
     * @param reportContent the report content
     */
    private void sendReport(String reportContent) {
        String destination = config.getProperty("error.reporting.destination", "file");
        if ("file".equalsIgnoreCase(destination)) {
            writeToFile(reportContent);
        } else if ("email".equalsIgnoreCase(destination)) {
            sendEmailReport(reportContent);
        } else if ("sms".equalsIgnoreCase(destination)) {
            sendSMSReport(reportContent);
        } else {
            logger.warn(ErrorReporter.class.getSimpleName(), "Unknown reporting destination: " + destination + ". Falling back to file.");
            writeToFile(reportContent);
        }
    }

    /**
     * Writes the report to a file.
     * @param content the report content
     */
    private void writeToFile(String content) {
        String filePath = config.getProperty("error.reporting.file.path", "logs/error_report.txt");
        try (FileWriter writer = new FileWriter(filePath, true)) {
            writer.write(content);
            writer.write("\n");
            logger.info(ErrorReporter.class.getSimpleName(), "Error report written to file: " + filePath);
        } catch (IOException e) {
            logger.error(ErrorReporter.class.getSimpleName(), "Failed to write error report to file", e);
        }

        // Register log file for cleanup if it's a temporary file
        if (filePath.contains("temp") || filePath.contains("tmp")) {
            ResourceManager.getInstance().registerTempFile(filePath);
        }
    }

    /**
     * Sends error report via email using SMTP.
     * @param reportContent the report content
     */
    private void sendEmailReport(String reportContent) {
        String smtpHost = config.getProperty("error.reporting.email.smtp.host", "smtp.gmail.com");
        String smtpPort = config.getProperty("error.reporting.email.smtp.port", "587");
        String username = config.getProperty("error.reporting.email.username");
        String password = config.getProperty("error.reporting.email.password");
        String fromEmail = config.getProperty("error.reporting.email.from", username);
        String toEmail = config.getProperty("error.reporting.email.to");

        if (username == null || password == null || toEmail == null) {
            logger.warn(ErrorReporter.class.getSimpleName(), "Email configuration incomplete. Falling back to file reporting.");
            writeToFile(reportContent);
            return;
        }

        try {
            // Use JavaMail API for sending emails
            Properties props = new Properties();
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.host", smtpHost);
            props.put("mail.smtp.port", smtpPort);

            // Note: JavaMail classes would be imported if available
            // For now, simulate email sending by logging
            logger.info(ErrorReporter.class.getSimpleName(),
                "Email report would be sent to: " + toEmail + " via " + smtpHost + ":" + smtpPort);
            logger.info(ErrorReporter.class.getSimpleName(), "Report content: " + reportContent.substring(0, Math.min(200, reportContent.length())) + "...");

            // In a real implementation, uncomment and use proper JavaMail imports:
            /*
            Session session = Session.getInstance(props, new Authenticator() {
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(username, password);
                }
            });

            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(fromEmail));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject("StickerPrinterPro Error Report");
            message.setText(reportContent);

            Transport.send(message);
            */

            // For now, just log success
            logger.info(ErrorReporter.class.getSimpleName(), "Error report 'sent' via email to: " + toEmail);
        } catch (Exception e) {
            logger.error(ErrorReporter.class.getSimpleName(), "Failed to send email report", e);
            // Fallback to file reporting
            writeToFile(reportContent);
        }
    }

    /**
     * Sends error report via SMS using HTTP API.
     * @param reportContent the report content
     */
    private void sendSMSReport(String reportContent) {
        String smsApiUrl = config.getProperty("error.reporting.sms.api.url");
        String smsApiKey = config.getProperty("error.reporting.sms.api.key");
        String smsFrom = config.getProperty("error.reporting.sms.from");
        String smsTo = config.getProperty("error.reporting.sms.to");

        if (smsApiUrl == null || smsApiKey == null || smsTo == null) {
            logger.warn(ErrorReporter.class.getSimpleName(), "SMS configuration incomplete. Falling back to file reporting.");
            writeToFile(reportContent);
            return;
        }

        try {
            // Truncate message if too long for SMS
            String smsMessage = reportContent.length() > 160 ?
                reportContent.substring(0, 157) + "..." : reportContent;

            // Construct HTTP request for SMS API
            java.net.URL url = new java.net.URL(smsApiUrl);
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + smsApiKey);
            conn.setDoOutput(true);

            String jsonPayload = String.format(
                "{\"from\":\"%s\",\"to\":\"%s\",\"message\":\"%s\"}",
                smsFrom != null ? smsFrom : "StickerPrinterPro",
                smsTo,
                smsMessage.replace("\"", "\\\"").replace("\n", " ")
            );

            try (java.io.OutputStream os = conn.getOutputStream()) {
                os.write(jsonPayload.getBytes("UTF-8"));
            }

            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                logger.info(ErrorReporter.class.getSimpleName(), "Error report sent via SMS to: " + smsTo);
            } else {
                throw new RuntimeException("SMS API returned status: " + responseCode);
            }
        } catch (Exception e) {
            logger.error(ErrorReporter.class.getSimpleName(), "Failed to send SMS report", e);
            // Fallback to file reporting
            writeToFile(reportContent);
        }
    }

    /**
     * Converts stack trace to string.
     * @param exception the exception
     * @return stack trace as string
     */
    private String getStackTraceAsString(Exception exception) {
        StringBuilder sb = new StringBuilder();
        for (StackTraceElement element : exception.getStackTrace()) {
            sb.append(element.toString()).append("\n");
        }
        return sb.toString();
    }

    /**
     * Collects system information.
     * @return system info as string
     */
    private String collectSystemInfo() {
        Properties props = System.getProperties();
        StringBuilder sb = new StringBuilder();
        sb.append("OS: ").append(props.getProperty("os.name")).append(" ").append(props.getProperty("os.version")).append("\n");
        sb.append("Java Version: ").append(props.getProperty("java.version")).append("\n");
        sb.append("User: ").append(props.getProperty("user.name")).append("\n");
        // Add more system info as needed
        return sb.toString();
    }

    /**
     * Inner class to hold error report data.
     */
    private static class ErrorReport {
        private LocalDateTime timestamp;
        private String errorType;
        private String message;
        private String stackTrace;
        private String systemInfo;
        private String additionalInfo;

        // Getters and setters
        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
        public String getErrorType() { return errorType; }
        public void setErrorType(String errorType) { this.errorType = errorType; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public String getStackTrace() { return stackTrace; }
        public void setStackTrace(String stackTrace) { this.stackTrace = stackTrace; }
        public String getSystemInfo() { return systemInfo; }
        public void setSystemInfo(String systemInfo) { this.systemInfo = systemInfo; }
        public String getAdditionalInfo() { return additionalInfo; }
        public void setAdditionalInfo(String additionalInfo) { this.additionalInfo = additionalInfo; }
    }
}