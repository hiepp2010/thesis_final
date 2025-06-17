package com.microservices.mail.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "mail_logs")
public class MailLog {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "to_addresses", columnDefinition = "TEXT")
    private String toAddresses;
    
    @Column(name = "cc_addresses", columnDefinition = "TEXT")
    private String ccAddresses;
    
    @Column(name = "bcc_addresses", columnDefinition = "TEXT")
    private String bccAddresses;
    
    @Column(name = "subject", nullable = false)
    private String subject;
    
    @Column(name = "content", columnDefinition = "TEXT")
    private String content;
    
    @Column(name = "template_name")
    private String templateName;
    
    @Column(name = "is_html")
    private boolean isHtml;
    
    @Column(name = "priority")
    private String priority;
    
    @Column(name = "service_name")
    private String serviceName;
    
    @Column(name = "status")
    private String status; // SENT, FAILED, PENDING
    
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
    
    @Column(name = "message_id")
    private String messageId;
    
    @Column(name = "sent_at")
    private LocalDateTime sentAt;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
    
    // Constructors
    public MailLog() {}
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getToAddresses() {
        return toAddresses;
    }
    
    public void setToAddresses(String toAddresses) {
        this.toAddresses = toAddresses;
    }
    
    public String getCcAddresses() {
        return ccAddresses;
    }
    
    public void setCcAddresses(String ccAddresses) {
        this.ccAddresses = ccAddresses;
    }
    
    public String getBccAddresses() {
        return bccAddresses;
    }
    
    public void setBccAddresses(String bccAddresses) {
        this.bccAddresses = bccAddresses;
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
    
    public String getTemplateName() {
        return templateName;
    }
    
    public void setTemplateName(String templateName) {
        this.templateName = templateName;
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
    
    public String getServiceName() {
        return serviceName;
    }
    
    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    public String getMessageId() {
        return messageId;
    }
    
    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }
    
    public LocalDateTime getSentAt() {
        return sentAt;
    }
    
    public void setSentAt(LocalDateTime sentAt) {
        this.sentAt = sentAt;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
} 