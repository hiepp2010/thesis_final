package com.microservices.mail.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.Map;

public class MailRequest {
    
    @NotEmpty(message = "To field cannot be empty")
    private List<@Email String> to;
    
    private List<@Email String> cc;
    
    private List<@Email String> bcc;
    
    @NotBlank(message = "Subject cannot be blank")
    private String subject;
    
    @NotBlank(message = "Content cannot be blank")
    private String content;
    
    private String template;
    
    private Map<String, Object> templateVariables;
    
    private boolean isHtml = false;
    
    private String priority = "NORMAL"; // LOW, NORMAL, HIGH
    
    private String service; // Which service sent this request
    
    // Constructors
    public MailRequest() {}
    
    public MailRequest(List<String> to, String subject, String content) {
        this.to = to;
        this.subject = subject;
        this.content = content;
    }
    
    // Getters and Setters
    public List<String> getTo() {
        return to;
    }
    
    public void setTo(List<String> to) {
        this.to = to;
    }
    
    public List<String> getCc() {
        return cc;
    }
    
    public void setCc(List<String> cc) {
        this.cc = cc;
    }
    
    public List<String> getBcc() {
        return bcc;
    }
    
    public void setBcc(List<String> bcc) {
        this.bcc = bcc;
    }
    
    public String getSubject() {
        return subject;
    }
    
    public void setSubject(String subject) {
        this.subject = subject;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public String getTemplate() {
        return template;
    }
    
    public void setTemplate(String template) {
        this.template = template;
    }
    
    public Map<String, Object> getTemplateVariables() {
        return templateVariables;
    }
    
    public void setTemplateVariables(Map<String, Object> templateVariables) {
        this.templateVariables = templateVariables;
    }
    
    public boolean isHtml() {
        return isHtml;
    }
    
    public void setHtml(boolean html) {
        isHtml = html;
    }
    
    public String getPriority() {
        return priority;
    }
    
    public void setPriority(String priority) {
        this.priority = priority;
    }
    
    public String getService() {
        return service;
    }
    
    public void setService(String service) {
        this.service = service;
    }
} 