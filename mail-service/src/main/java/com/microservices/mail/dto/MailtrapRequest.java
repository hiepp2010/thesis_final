package com.microservices.mail.dto;

import java.util.List;

public class MailtrapRequest {
    
    private MailtrapSender from;
    private List<MailtrapRecipient> to;
    private List<MailtrapRecipient> cc;
    private List<MailtrapRecipient> bcc;
    private String subject;
    private String text;
    private String html;
    private String category;
    
    // Constructors
    public MailtrapRequest() {}
    
    // Getters and Setters
    public MailtrapSender getFrom() {
        return from;
    }
    
    public void setFrom(MailtrapSender from) {
        this.from = from;
    }
    
    public List<MailtrapRecipient> getTo() {
        return to;
    }
    
    public void setTo(List<MailtrapRecipient> to) {
        this.to = to;
    }
    
    public List<MailtrapRecipient> getCc() {
        return cc;
    }
    
    public void setCc(List<MailtrapRecipient> cc) {
        this.cc = cc;
    }
    
    public List<MailtrapRecipient> getBcc() {
        return bcc;
    }
    
    public void setBcc(List<MailtrapRecipient> bcc) {
        this.bcc = bcc;
    }
    
    public String getSubject() {
        return subject;
    }
    
    public void setSubject(String subject) {
        this.subject = subject;
    }
    
    public String getText() {
        return text;
    }
    
    public void setText(String text) {
        this.text = text;
    }
    
    public String getHtml() {
        return html;
    }
    
    public void setHtml(String html) {
        this.html = html;
    }
    
    public String getCategory() {
        return category;
    }
    
    public void setCategory(String category) {
        this.category = category;
    }
    
    // Inner classes for nested objects
    public static class MailtrapSender {
        private String email;
        private String name;
        
        public MailtrapSender() {}
        
        public MailtrapSender(String email, String name) {
            this.email = email;
            this.name = name;
        }
        
        public String getEmail() {
            return email;
        }
        
        public void setEmail(String email) {
            this.email = email;
        }
        
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
        }
    }
    
    public static class MailtrapRecipient {
        private String email;
        private String name;
        
        public MailtrapRecipient() {}
        
        public MailtrapRecipient(String email) {
            this.email = email;
        }
        
        public MailtrapRecipient(String email, String name) {
            this.email = email;
            this.name = name;
        }
        
        public String getEmail() {
            return email;
        }
        
        public void setEmail(String email) {
            this.email = email;
        }
        
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
        }
    }
} 