package com.microservices.mail.service;

import com.microservices.mail.config.MailtrapConfig;
import com.microservices.mail.dto.*;
import com.microservices.mail.dto.MailtrapRequest.MailtrapRecipient;
import com.microservices.mail.dto.MailtrapRequest.MailtrapSender;
import com.microservices.mail.entity.MailLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class MailService {

    private static final Logger logger = LoggerFactory.getLogger(MailService.class);

    @Autowired
    private MailtrapConfig mailtrapConfig;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private TemplateEngine templateEngine;

    @Autowired
    private MailLogService mailLogService;

    private static final String DEFAULT_FROM_EMAIL = "noreply@microservices.com";
    private static final String DEFAULT_FROM_NAME = "Microservices Mail Service";

    public MailResponse sendMail(MailRequest mailRequest) {
        logger.info("Processing mail request for subject: {}", mailRequest.getSubject());
        
        MailLog mailLog = createMailLog(mailRequest);
        mailLog.setStatus("PENDING");
        mailLogService.saveMailLog(mailLog);

        try {
            // Convert MailRequest to MailtrapRequest
            MailtrapRequest mailtrapRequest = convertToMailtrapRequest(mailRequest);
            
            // Send via Mailtrap HTTP API
            MailtrapResponse mailtrapResponse = sendViaMailtrapAPI(mailtrapRequest);
            
            String messageId = mailtrapResponse.getMessageIds() != null && !mailtrapResponse.getMessageIds().isEmpty()
                    ? mailtrapResponse.getMessageIds().get(0)
                    : UUID.randomUUID().toString();

            mailLog.setStatus("SENT");
            mailLog.setMessageId(messageId);
            mailLog.setSentAt(LocalDateTime.now());
            mailLogService.saveMailLog(mailLog);

            logger.info("Email sent successfully with message ID: {}", messageId);
            return new MailResponse(true, "Email sent successfully via Mailtrap", messageId);

        } catch (Exception e) {
            logger.error("Failed to send email: {}", e.getMessage(), e);
            
            mailLog.setStatus("FAILED");
            mailLog.setErrorMessage(e.getMessage());
            mailLogService.saveMailLog(mailLog);

            return new MailResponse(false, "Failed to send email: " + e.getMessage());
        }
    }

    private MailtrapRequest convertToMailtrapRequest(MailRequest mailRequest) {
        MailtrapRequest mailtrapRequest = new MailtrapRequest();
        
        // Set sender - use demomailtrap.co domain for production API
        mailtrapRequest.setFrom(new MailtrapSender("hello@demomailtrap.co", "Microservices Mail Service"));
        
        // Force all recipients to go to hieppclone@gmail.com for testing
        List<MailtrapRecipient> recipients = new ArrayList<>();
        recipients.add(new MailtrapRecipient(mailtrapConfig.getDefaultRecipient(), "Hiep"));
        mailtrapRequest.setTo(recipients);
        
        // Don't set CC/BCC for production testing - keep it simple
        // if (mailRequest.getCc() != null && !mailRequest.getCc().isEmpty()) {
        //     mailtrapRequest.setCc(convertToMailtrapRecipients(mailRequest.getCc()));
        // }
        // 
        // if (mailRequest.getBcc() != null && !mailRequest.getBcc().isEmpty()) {
        //     mailtrapRequest.setBcc(convertToMailtrapRecipients(mailRequest.getBcc()));
        // }
        
        // Set subject
        mailtrapRequest.setSubject(mailRequest.getSubject());
        
        // Set content
        String content;
        if (mailRequest.getTemplate() != null) {
            content = processTemplate(mailRequest.getTemplate(), mailRequest.getTemplateVariables());
            mailtrapRequest.setHtml(content);
        } else {
            content = mailRequest.getContent();
            if (mailRequest.isHtml()) {
                mailtrapRequest.setHtml(content);
            } else {
                mailtrapRequest.setText(content);
            }
        }
        
        // Set category for organization
        String category = mailRequest.getService() != null ? mailRequest.getService() : "microservices-mail";
        mailtrapRequest.setCategory(category);
        
        return mailtrapRequest;
    }

    private List<MailtrapRecipient> convertToMailtrapRecipients(List<String> emails) {
        return emails.stream()
                .map(MailtrapRecipient::new)
                .collect(Collectors.toList());
    }

    private MailtrapResponse sendViaMailtrapAPI(MailtrapRequest mailtrapRequest) {
        String url = mailtrapConfig.getApiUrl();
        
        // Create headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(mailtrapConfig.getApiToken());
        
        // Create request entity
        HttpEntity<MailtrapRequest> requestEntity = new HttpEntity<>(mailtrapRequest, headers);
        
        logger.debug("Sending email via Mailtrap API to: {}", url);
        logger.debug("Request payload: {}", mailtrapRequest);
        
        // Send request
        ResponseEntity<MailtrapResponse> responseEntity = restTemplate.exchange(
                url,
                HttpMethod.POST,
                requestEntity,
                MailtrapResponse.class
        );
        
        if (responseEntity.getStatusCode().is2xxSuccessful() && responseEntity.getBody() != null) {
            MailtrapResponse response = responseEntity.getBody();
            response.setSuccess(true); // Mailtrap doesn't return success field, we set it based on status code
            return response;
        } else {
            throw new RuntimeException("Failed to send email via Mailtrap API. Status: " + responseEntity.getStatusCode());
        }
    }

    private String processTemplate(String templateName, Object templateVariables) {
        Context context = new Context();
        if (templateVariables != null) {
            if (templateVariables instanceof java.util.Map) {
                @SuppressWarnings("unchecked")
                java.util.Map<String, Object> variables = (java.util.Map<String, Object>) templateVariables;
                variables.forEach(context::setVariable);
            }
        }
        return templateEngine.process(templateName, context);
    }

    private MailLog createMailLog(MailRequest mailRequest) {
        MailLog mailLog = new MailLog();
        mailLog.setToAddresses(String.join(",", mailRequest.getTo()));
        
        if (mailRequest.getCc() != null) {
            mailLog.setCcAddresses(String.join(",", mailRequest.getCc()));
        }
        
        if (mailRequest.getBcc() != null) {
            mailLog.setBccAddresses(String.join(",", mailRequest.getBcc()));
        }
        
        mailLog.setSubject(mailRequest.getSubject());
        mailLog.setContent(mailRequest.getContent());
        mailLog.setTemplateName(mailRequest.getTemplate());
        // Set HTML flag: true if template is used OR if explicitly set as HTML
        mailLog.setHtml(mailRequest.getTemplate() != null || mailRequest.isHtml());
        mailLog.setPriority(mailRequest.getPriority());
        mailLog.setServiceName(mailRequest.getService());
        
        return mailLog;
    }
} 