# Third-Party Email Service Setup

## Option 1: SendGrid (Recommended)
**Free tier: 100 emails/day forever**

1. **Sign up**: Go to [sendgrid.com](https://sendgrid.com) 
2. **Verify email**: Verify your email address
3. **Create API Key**:
   - Go to Settings → API Keys
   - Click "Create API Key"
   - Choose "Restricted Access"
   - Give it a name: "microservices-mail"
   - Grant "Mail Send" permission
   - Copy the API key

4. **Update Configuration**:
```bash
# Update docker-compose.yml
MAIL_USERNAME=apikey
MAIL_PASSWORD=your-sendgrid-api-key
```

```yaml
# Update application.yml
mail:
  host: smtp.sendgrid.net
  port: 587
  username: apikey  # Always "apikey" for SendGrid
  password: ${MAIL_PASSWORD}  # Your actual API key
```

## Option 2: Mailtrap (Best for Development)
**Free tier: 100 emails/month**

1. **Sign up**: Go to [mailtrap.io](https://mailtrap.io)
2. **Create Inbox**: Create a new inbox
3. **Get SMTP credentials** from the inbox settings

```bash
# Update docker-compose.yml  
MAIL_USERNAME=your-mailtrap-username
MAIL_PASSWORD=your-mailtrap-password
```

```yaml
# Update application.yml
mail:
  host: live.smtp.mailtrap.io
  port: 587
```

## Option 3: Mailgun
**Free tier: 100 emails/day for 3 months**

1. **Sign up**: Go to [mailgun.com](https://mailgun.com)
2. **Verify domain** (or use sandbox for testing)
3. **Get SMTP credentials** from dashboard

## Quick Test Setup (No Signup Required)

For immediate testing, I can configure a test SMTP service:

```bash
# Temporary test credentials (replace with real service later)
MAIL_USERNAME=testuser
MAIL_PASSWORD=testpass
```

**Choose your preferred option and I'll configure it for you!** 