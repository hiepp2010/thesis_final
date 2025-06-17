package com.microservices.mail.controller;

import com.microservices.mail.dto.MailRequest;
import com.microservices.mail.dto.MailResponse;
import com.microservices.mail.entity.MailLog;
import com.microservices.mail.service.MailLogService;
import com.microservices.mail.service.MailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/mail")
public class MailController {

    @Autowired
    private MailService mailService;

    @Autowired
    private MailLogService mailLogService;

    @PostMapping("/send")
    public ResponseEntity<MailResponse> sendMail(@Valid @RequestBody MailRequest mailRequest) {
        MailResponse response = mailService.sendMail(mailRequest);
        
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/logs")
    public ResponseEntity<List<MailLog>> getAllMailLogs() {
        List<MailLog> mailLogs = mailLogService.getAllMailLogs();
        return ResponseEntity.ok(mailLogs);
    }

    @GetMapping("/logs/{id}")
    public ResponseEntity<MailLog> getMailLogById(@PathVariable Long id) {
        Optional<MailLog> mailLog = mailLogService.getMailLogById(id);
        
        if (mailLog.isPresent()) {
            return ResponseEntity.ok(mailLog.get());
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/logs/service/{serviceName}")
    public ResponseEntity<List<MailLog>> getMailLogsByService(@PathVariable String serviceName) {
        List<MailLog> mailLogs = mailLogService.getMailLogsByService(serviceName);
        return ResponseEntity.ok(mailLogs);
    }

    @GetMapping("/logs/status/{status}")
    public ResponseEntity<List<MailLog>> getMailLogsByStatus(@PathVariable String status) {
        List<MailLog> mailLogs = mailLogService.getMailLogsByStatus(status);
        return ResponseEntity.ok(mailLogs);
    }

    @DeleteMapping("/logs/{id}")
    public ResponseEntity<Void> deleteMailLog(@PathVariable Long id) {
        try {
            mailLogService.deleteMailLog(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Mail service is running");
    }
} 