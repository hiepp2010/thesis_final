package com.microservices.mail.util;

import com.microservices.mail.dto.MailRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class MailHelper {

    @Autowired
    private KafkaTemplate<String, MailRequest> kafkaTemplate;

    /**
     * Send a simple text email
     */
    public void sendSimpleEmail(String to, String subject, String content, String serviceName) {
        MailRequest mailRequest = new MailRequest();
        mailRequest.setTo(Arrays.asList(to));
        mailRequest.setSubject(subject);
        mailRequest.setContent(content);
        mailRequest.setService(serviceName);
        mailRequest.setHtml(false);
        
        kafkaTemplate.send("mail-requests", mailRequest);
    }

    /**
     * Send an HTML email
     */
    public void sendHtmlEmail(String to, String subject, String htmlContent, String serviceName) {
        MailRequest mailRequest = new MailRequest();
        mailRequest.setTo(Arrays.asList(to));
        mailRequest.setSubject(subject);
        mailRequest.setContent(htmlContent);
        mailRequest.setService(serviceName);
        mailRequest.setHtml(true);
        
        kafkaTemplate.send("mail-requests", mailRequest);
    }

    /**
     * Send a welcome email using template
     */
    public void sendWelcomeEmail(String to, String name, String email, String loginUrl, String serviceName) {
        MailRequest mailRequest = new MailRequest();
        mailRequest.setTo(Arrays.asList(to));
        mailRequest.setSubject("Welcome to Our Platform!");
        mailRequest.setService(serviceName);
        
        Map<String, Object> variables = new HashMap<>();
        variables.put("name", name);
        variables.put("email", email);
        variables.put("loginUrl", loginUrl);
        
        mailRequest.setTemplateVariables(variables);
        
        kafkaTemplate.send("welcome-emails", mailRequest);
    }

    /**
     * Send a notification email using template
     */
    public void sendNotificationEmail(String to, String name, String email, String title, 
                                    String message, String alertType, String serviceName) {
        MailRequest mailRequest = new MailRequest();
        mailRequest.setTo(Arrays.asList(to));
        mailRequest.setSubject(title);
        mailRequest.setService(serviceName);
        
        Map<String, Object> variables = new HashMap<>();
        variables.put("name", name);
        variables.put("email", email);
        variables.put("title", title);
        variables.put("message", message);
        variables.put("alertType", alertType); // info, warning, danger
        variables.put("timestamp", java.time.LocalDateTime.now().toString());
        
        mailRequest.setTemplateVariables(variables);
        
        kafkaTemplate.send("notification-emails", mailRequest);
    }

    /**
     * Send email to multiple recipients
     */
    public void sendBulkEmail(List<String> recipients, String subject, String content, 
                            String serviceName, boolean isHtml) {
        MailRequest mailRequest = new MailRequest();
        mailRequest.setTo(recipients);
        mailRequest.setSubject(subject);
        mailRequest.setContent(content);
        mailRequest.setService(serviceName);
        mailRequest.setHtml(isHtml);
        
        kafkaTemplate.send("mail-requests", mailRequest);
    }

    /**
     * Send email with CC and BCC
     */
    public void sendEmailWithCopies(List<String> to, List<String> cc, List<String> bcc,
                                  String subject, String content, String serviceName, boolean isHtml) {
        MailRequest mailRequest = new MailRequest();
        mailRequest.setTo(to);
        mailRequest.setCc(cc);
        mailRequest.setBcc(bcc);
        mailRequest.setSubject(subject);
        mailRequest.setContent(content);
        mailRequest.setService(serviceName);
        mailRequest.setHtml(isHtml);
        
        kafkaTemplate.send("mail-requests", mailRequest);
    }
} 