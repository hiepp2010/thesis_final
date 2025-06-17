# Mail Service Integration Example

This document shows how to integrate the mail service with other microservices in your architecture.

## 1. Add Kafka Producer to Your Service

### Add Dependencies to pom.xml

```xml
<!-- Kafka for Event Publishing -->
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka</artifactId>
</dependency>
```

### Create Kafka Producer Configuration

```java
@Configuration
public class KafkaProducerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }
}
```

## 2. Create Mail Request DTO (Copy from Mail Service)

```java
package com.microservices.auth.dto;

import java.util.List;
import java.util.Map;

public class MailRequest {
    private List<String> to;
    private List<String> cc;
    private List<String> bcc;
    private String subject;
    private String content;
    private String template;
    private Map<String, Object> templateVariables;
    private boolean isHtml = false;
    private String priority = "NORMAL";
    private String service;
    
    // Constructors, getters, and setters...
}
```

## 3. Create Mail Service Helper

```java
@Service
public class MailServiceHelper {

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    public void sendWelcomeEmail(String email, String name) {
        MailRequest mailRequest = new MailRequest();
        mailRequest.setTo(Arrays.asList(email));
        mailRequest.setService("auth-service");
        
        Map<String, Object> variables = new HashMap<>();
        variables.put("name", name);
        variables.put("email", email);
        variables.put("loginUrl", "http://localhost:8080/login");
        
        mailRequest.setTemplateVariables(variables);
        
        kafkaTemplate.send("welcome-emails", mailRequest);
    }

    public void sendPasswordResetEmail(String email, String name, String resetToken) {
        MailRequest mailRequest = new MailRequest();
        mailRequest.setTo(Arrays.asList(email));
        mailRequest.setSubject("Password Reset Request");
        mailRequest.setService("auth-service");
        
        Map<String, Object> variables = new HashMap<>();
        variables.put("name", name);
        variables.put("email", email);
        variables.put("title", "Password Reset Request");
        variables.put("message", "You have requested to reset your password. Click the button below to proceed.");
        variables.put("alertType", "warning");
        variables.put("actionUrl", "http://localhost:8080/reset-password?token=" + resetToken);
        variables.put("actionText", "Reset Password");
        
        mailRequest.setTemplateVariables(variables);
        
        kafkaTemplate.send("notification-emails", mailRequest);
    }

    public void sendSimpleNotification(String email, String subject, String message) {
        MailRequest mailRequest = new MailRequest();
        mailRequest.setTo(Arrays.asList(email));
        mailRequest.setSubject(subject);
        mailRequest.setContent(message);
        mailRequest.setService("auth-service");
        mailRequest.setHtml(false);
        
        kafkaTemplate.send("mail-requests", mailRequest);
    }
}
```

## 4. Integration in Auth Service

### User Registration

```java
@Service
public class AuthService {

    @Autowired
    private MailServiceHelper mailServiceHelper;

    public ResponseEntity<?> registerUser(RegisterRequest request) {
        try {
            // Create user logic...
            User user = new User();
            user.setEmail(request.getEmail());
            user.setName(request.getName());
            // ... save user
            
            // Send welcome email
            mailServiceHelper.sendWelcomeEmail(user.getEmail(), user.getName());
            
            return ResponseEntity.ok(new MessageResponse("User registered successfully!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: " + e.getMessage()));
        }
    }
}
```

### Password Reset

```java
@Service
public class PasswordResetService {

    @Autowired
    private MailServiceHelper mailServiceHelper;

    public ResponseEntity<?> requestPasswordReset(String email) {
        User user = userRepository.findByEmail(email);
        if (user != null) {
            String resetToken = generateResetToken();
            // Save reset token...
            
            mailServiceHelper.sendPasswordResetEmail(
                user.getEmail(), 
                user.getName(), 
                resetToken
            );
            
            return ResponseEntity.ok(new MessageResponse("Password reset email sent!"));
        }
        return ResponseEntity.badRequest().body(new MessageResponse("Email not found"));
    }
}
```

## 5. HRMS Service Integration Example

```java
@Service
public class EmployeeService {

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    public void notifyEmployeeStatusChange(Employee employee, String status) {
        MailRequest mailRequest = new MailRequest();
        mailRequest.setTo(Arrays.asList(employee.getEmail()));
        mailRequest.setService("hrms-service");
        
        Map<String, Object> variables = new HashMap<>();
        variables.put("name", employee.getName());
        variables.put("email", employee.getEmail());
        variables.put("title", "Employment Status Update");
        variables.put("message", "Your employment status has been updated to: " + status);
        variables.put("alertType", "info");
        
        mailRequest.setTemplateVariables(variables);
        
        kafkaTemplate.send("notification-emails", mailRequest);
    }

    public void sendPayrollNotification(List<Employee> employees, String period) {
        for (Employee employee : employees) {
            MailRequest mailRequest = new MailRequest();
            mailRequest.setTo(Arrays.asList(employee.getEmail()));
            mailRequest.setService("hrms-service");
            
            Map<String, Object> variables = new HashMap<>();
            variables.put("name", employee.getName());
            variables.put("email", employee.getEmail());
            variables.put("title", "Payroll Processed");
            variables.put("message", "Your payroll for " + period + " has been processed.");
            variables.put("alertType", "info");
            variables.put("actionUrl", "http://localhost:8082/payroll/view");
            variables.put("actionText", "View Payroll");
            
            mailRequest.setTemplateVariables(variables);
            
            kafkaTemplate.send("notification-emails", mailRequest);
        }
    }
}
```

## 6. Configuration Updates

### Add to application.yml

```yaml
spring:
  kafka:
    bootstrap-servers: kafka:29092
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
```

## 7. Testing the Integration

### Test Welcome Email

```java
@Test
public void testWelcomeEmail() {
    // Register a new user
    RegisterRequest request = new RegisterRequest();
    request.setEmail("test@example.com");
    request.setName("Test User");
    
    ResponseEntity<?> response = authService.registerUser(request);
    
    // Verify user is created and email is sent
    assertEquals(HttpStatus.OK, response.getStatusCode());
    
    // Check mail logs via REST API
    // GET http://localhost:8083/api/mail/logs/service/auth-service
}
```

### Manual Testing

```bash
# 1. Start all services
docker-compose up

# 2. Register a new user
curl -X POST http://localhost:8081/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "name": "Test User",
    "password": "password123"
  }'

# 3. Check mail logs
curl http://localhost:8083/api/mail/logs

# 4. Check specific service logs
curl http://localhost:8083/api/mail/logs/service/auth-service
```

## 8. Best Practices

1. **Error Handling**: Always handle Kafka sending errors gracefully
2. **Async Processing**: Mail sending is asynchronous, don't wait for confirmation
3. **Template Variables**: Validate template variables before sending
4. **Rate Limiting**: Consider implementing rate limiting for bulk emails
5. **Monitoring**: Monitor mail logs for failed deliveries

## 9. Troubleshooting

### Common Issues

1. **Kafka Connection**: Ensure Kafka is running and accessible
2. **Serialization**: Make sure MailRequest DTO matches between services
3. **Topic Names**: Verify topic names are consistent
4. **Email Configuration**: Check SMTP settings in mail service

### Debug Steps

```bash
# Check Kafka topics
docker exec -it microservices-kafka kafka-topics --list --bootstrap-server localhost:9092

# Check mail service logs
docker logs mail-service

# Test direct email sending
curl -X POST http://localhost:8083/api/mail/send \
  -H "Content-Type: application/json" \
  -d '{
    "to": ["test@example.com"],
    "subject": "Test",
    "content": "Test message",
    "service": "test"
  }'
``` 