package com.microservices.mail.event;

import com.microservices.mail.dto.MailRequest;
import com.microservices.mail.dto.MailResponse;
import com.microservices.mail.service.MailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class MailEventListener {

    private static final Logger logger = LoggerFactory.getLogger(MailEventListener.class);

    @Autowired
    private MailService mailService;

    @KafkaListener(topics = "mail-requests", groupId = "mail-service-group")
    public void handleMailRequest(MailRequest mailRequest) {
        logger.info("Received mail request from Kafka for subject: {}", mailRequest.getSubject());
        
        try {
            MailResponse response = mailService.sendMail(mailRequest);
            
            if (response.isSuccess()) {
                logger.info("Mail sent successfully: {}", response.getMessage());
            } else {
                logger.error("Failed to send mail: {}", response.getMessage());
            }
            
        } catch (Exception e) {
            logger.error("Error processing mail request: {}", e.getMessage(), e);
        }
    }

    // Removed separate topic listeners - now everything goes through mail-requests topic
    // Templates can be specified in the MailRequest itself
} 