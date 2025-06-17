package com.microservices.mail.service;

import com.microservices.mail.entity.MailLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Repository
interface MailLogRepository extends JpaRepository<MailLog, Long> {
    List<MailLog> findByServiceNameOrderByCreatedAtDesc(String serviceName);
    List<MailLog> findByStatusOrderByCreatedAtDesc(String status);
}

@Service
public class MailLogService {

    @Autowired
    private MailLogRepository mailLogRepository;

    public MailLog saveMailLog(MailLog mailLog) {
        return mailLogRepository.save(mailLog);
    }

    public List<MailLog> getAllMailLogs() {
        return mailLogRepository.findAll();
    }

    public Optional<MailLog> getMailLogById(Long id) {
        return mailLogRepository.findById(id);
    }

    public List<MailLog> getMailLogsByService(String serviceName) {
        return mailLogRepository.findByServiceNameOrderByCreatedAtDesc(serviceName);
    }

    public List<MailLog> getMailLogsByStatus(String status) {
        return mailLogRepository.findByStatusOrderByCreatedAtDesc(status);
    }

    public void deleteMailLog(Long id) {
        mailLogRepository.deleteById(id);
    }
} 