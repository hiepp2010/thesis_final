# Mail Service

A microservice for handling email functionality in the microservices architecture. This service consumes mail requests from Kafka topics and sends emails using SMTP.

## Features

- **Kafka Integration**: Consumes mail requests from Kafka topics
- **Template Support**: Supports HTML email templates using Thymeleaf
- **Mail Logging**: Tracks all sent emails in the database
- **Multiple Recipients**: Supports TO, CC, and BCC recipients
- **REST API**: Direct email sending via REST endpoints
- **Health Monitoring**: Health check endpoints for monitoring

## Kafka Topics

The mail service listens to the following Kafka topics:

- `mail-requests`: General email requests
- `welcome-emails`: Welcome emails for new users
- `notification-emails`: System notifications

## Configuration

### Environment Variables

Set the following environment variables for email configuration:

```bash
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=your-app-password
```

### Gmail Setup

1. Enable 2-factor authentication on your Gmail account
2. Generate an App Password:
   - Go to Google Account settings
   - Security → 2-Step Verification → App passwords
   - Generate a password for "Mail"
3. Use the generated password as `MAIL_PASSWORD`

## Usage

### From Other Services (Recommended)

Use Kafka to send mail requests:

```java
// Add to your service's Kafka producer configuration
@Autowired
private KafkaTemplate<String, Object> kafkaTemplate;

// Send a simple email
MailRequest mailRequest = new MailRequest();
mailRequest.setTo(Arrays.asList("user@example.com"));
mailRequest.setSubject("Test Email");
mailRequest.setContent("Hello from microservice!");
mailRequest.setService("auth-service");

kafkaTemplate.send("mail-requests", mailRequest);
```

### Welcome Email Example

```java
// Send welcome email
MailRequest welcomeEmail = new MailRequest();
welcomeEmail.setTo(Arrays.asList("newuser@example.com"));
welcomeEmail.setService("auth-service");

Map<String, Object> variables = new HashMap<>();
variables.put("name", "John Doe");
variables.put("email", "newuser@example.com");
variables.put("loginUrl", "https://yourapp.com/login");

welcomeEmail.setTemplateVariables(variables);

kafkaTemplate.send("welcome-emails", welcomeEmail);
```

### Direct REST API

```bash
# Send email directly
curl -X POST http://localhost:8083/api/mail/send \
  -H "Content-Type: application/json" \
  -d '{
    "to": ["recipient@example.com"],
    "subject": "Test Email",
    "content": "Hello World!",
    "service": "test-service"
  }'

# Get mail logs
curl http://localhost:8083/api/mail/logs

# Health check
curl http://localhost:8083/api/mail/health
```

## Email Templates

### Available Templates

1. **welcome-template**: For new user registrations
2. **notification-template**: For system notifications

### Template Variables

#### Welcome Template
- `name`: User's name
- `email`: User's email
- `loginUrl`: Login page URL

#### Notification Template
- `name`: User's name
- `email`: User's email
- `title`: Notification title
- `message`: Notification message
- `alertType`: Alert type (info, warning, danger)
- `details`: Additional details (optional)
- `actionUrl`: Action button URL (optional)
- `actionText`: Action button text (optional)

## Database Schema

The service creates a `mail_logs` table to track email history:

```sql
CREATE TABLE mail_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    to_addresses TEXT,
    cc_addresses TEXT,
    bcc_addresses TEXT,
    subject VARCHAR(255) NOT NULL,
    content TEXT,
    template_name VARCHAR(100),
    is_html BOOLEAN DEFAULT FALSE,
    priority VARCHAR(20),
    service_name VARCHAR(100),
    status VARCHAR(20),
    error_message TEXT,
    message_id VARCHAR(100),
    sent_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

## Integration with Other Services

### Auth Service Integration

Add to your auth service to send welcome emails:

```java
@Autowired
private KafkaTemplate<String, Object> kafkaTemplate;

// After user registration
public void sendWelcomeEmail(User user) {
    MailRequest mailRequest = new MailRequest();
    mailRequest.setTo(Arrays.asList(user.getEmail()));
    mailRequest.setService("auth-service");
    
    Map<String, Object> variables = new HashMap<>();
    variables.put("name", user.getName());
    variables.put("email", user.getEmail());
    variables.put("loginUrl", "http://localhost:8080/login");
    
    mailRequest.setTemplateVariables(variables);
    
    kafkaTemplate.send("welcome-emails", mailRequest);
}
```

### HRMS Service Integration

```java
// Send notification emails
public void sendNotificationEmail(Employee employee, String message) {
    MailRequest mailRequest = new MailRequest();
    mailRequest.setTo(Arrays.asList(employee.getEmail()));
    mailRequest.setService("hrms-service");
    
    Map<String, Object> variables = new HashMap<>();
    variables.put("name", employee.getName());
    variables.put("email", employee.getEmail());
    variables.put("title", "HRMS Notification");
    variables.put("message", message);
    variables.put("alertType", "info");
    
    mailRequest.setTemplateVariables(variables);
    
    kafkaTemplate.send("notification-emails", mailRequest);
}
```

## Monitoring

- **Health Check**: `GET /api/mail/health`
- **Mail Logs**: `GET /api/mail/logs`
- **Service Logs**: Check application logs for email sending status

## Troubleshooting

### Common Issues

1. **Authentication Failed**: Check Gmail app password
2. **Connection Timeout**: Verify SMTP settings and firewall
3. **Template Not Found**: Ensure template files exist in `src/main/resources/templates/`
4. **Kafka Connection**: Verify Kafka is running and accessible

### Logs

Check the application logs for detailed error messages:

```bash
docker logs mail-service
```

## Development

### Building

```bash
cd microservices/mail-service
mvn clean package
```

### Running Locally

```bash
# Set environment variables
export MAIL_USERNAME=your-email@gmail.com
export MAIL_PASSWORD=your-app-password

# Run the service
mvn spring-boot:run
```

### Docker

```bash
# Build and run with Docker Compose
docker-compose up mail-service
``` 