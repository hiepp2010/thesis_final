# Authentication Service API Documentation

## Overview
The Authentication Service provides secure user authentication, registration, and JWT token management with Redis-based refresh token storage for enhanced security and session management.

## Base URLs
- **Direct Access**: `http://localhost:8081`
- **Via API Gateway**: `http://localhost:8080/api`
- **Swagger UI**: `http://localhost:8081/swagger-ui.html`
- **API Docs**: `http://localhost:8081/api-docs`

## 🔒 Security Architecture
- **JWT Access Tokens**: Stateless, short-lived (24h) for API access
- **Redis Refresh Tokens**: Stored securely with revocation capabilities (7 days)
- **Session Management**: Track and control user sessions across devices
- **Instant Revocation**: Immediate token invalidation for security

## Authentication Flow
1. **Register/Login** → Get `accessToken` + `refreshToken` (stored in Redis)
2. **Use accessToken** → Access protected resources (expires in 24h)
3. **Use refreshToken** → Get new `accessToken` when expired (validated via Redis)
4. **Logout** → Revoke refresh token from Redis
5. **Validate tokens** → Verify token validity for other services

---

## 🔐 Authentication APIs

### 1. User Registration
**POST** `/api/auth/register`

**Description**: Register a new user account with automatic JWT token generation

**Authentication**: Not required

**Request Body**:
```json
{
  "username": "john.doe",
  "email": "john@example.com", 
  "password": "password123",
  "firstName": "John",
  "lastName": "Doe"
}
```

**Required Fields**:
- `username` (string, 3-50 chars): Unique username
- `email` (string): Valid email address
- `password` (string, min 6 chars): User password

**Optional Fields**:
- `firstName` (string): User's first name
- `lastName` (string): User's last name

**Success Response (200)**:
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJqb2huLmRvZSIsInVzZXJJZCI6MSwidG9rZW5UeXBlIjoiYWNjZXNzIiwiaWF0IjoxNzA2MTc5MjAwLCJleHAiOjE3MDYyNjU2MDB9...",
  "refreshToken": "550e8400-e29b-41d4-a716-446655440000",
  "tokenType": "Bearer",
  "userId": 1,
  "username": "john.doe",
  "email": "john@example.com",
  "roles": ["USER"]
}
```

**🔒 Security Note**: Refresh token is now a secure UUID stored in Redis, not a JWT. This enables instant revocation and better security.

**Error Responses**:
```json
// 400 - Username taken
{
  "error": "Username is already taken"
}

// 400 - Email taken
{
  "error": "Email is already in use"
}

// 400 - Validation error
{
  "error": "Username must be between 3 and 50 characters"
}
```

### 2. User Login
**POST** `/api/auth/login`

**Description**: Authenticate user with credentials and get JWT tokens

**Authentication**: Not required

**Request Body**:
```json
{
  "username": "john.doe",
  "password": "password123"
}
```

**Required Fields**:
- `username` (string): Username or email
- `password` (string): User password

**Success Response (200)**:
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "userId": 1,
  "username": "john.doe",
  "email": "john@example.com",
  "roles": ["USER", "ADMIN"]
}
```

**Error Responses**:
```json
// 400 - Invalid credentials
{
  "error": "Invalid credentials"
}

// 400 - Account disabled
{
  "error": "User account is disabled"
}

// 400 - User not found
{
  "error": "User not found"
}
```

### 3. Refresh Access Token
**POST** `/api/auth/refresh`

**Description**: Generate new access token using refresh token

**Authentication**: Not required (but needs valid refresh token)

**Request Body**:
```json
{
  "refreshToken": "550e8400-e29b-41d4-a716-446655440000"
}
```

**Required Fields**:
- `refreshToken` (string): Valid refresh token UUID (stored in Redis)

**Success Response (200)**:
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "550e8400-e29b-41d4-a716-446655440001",
  "tokenType": "Bearer",
  "userId": 1,
  "username": "john.doe",
  "email": "john@example.com",
  "roles": ["USER"]
}
```

**🔄 Process**: Old refresh token is revoked from Redis, new one is stored with 7-day TTL.

**Error Responses**:
```json
// 400 - Missing refresh token
{
  "error": "Refresh token is required"
}

// 400 - Invalid refresh token (not found in Redis)
{
  "error": "Invalid refresh token"
}

// 400 - Refresh token expired (TTL expired in Redis)
{
  "error": "Refresh token expired"
}
```

### 4. Validate JWT Token
**POST** `/api/auth/validate`

**Description**: Validate JWT token and extract user information (used by API Gateway)

**Authentication**: Not required

**Request Body**:
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**Required Fields**:
- `token` (string): JWT token to validate

**Success Response (200)**:
```json
{
  "valid": true,
  "username": "john.doe",
  "roles": ["USER", "ADMIN"]
}
```

**Invalid Token Response (200)**:
```json
{
  "valid": false
}
```

**Error Responses**:
```json
// 400 - Missing token
{
  "valid": false,
  "error": "Token is required"
}

// 400 - Token processing error
{
  "valid": false,
  "error": "JWT signature does not match"
}
```

### 5. Health Check
**GET** `/api/auth/health`

**Description**: Check if the authentication service is running

**Authentication**: Not required

**Success Response (200)**:
```json
{
  "status": "UP",
  "service": "auth-service"
}
```

---

## 🚪 Session Management APIs

### 6. User Logout
**POST** `/api/auth/logout`

**Description**: Logout user and revoke the current refresh token

**Authentication**: Not required (but needs valid refresh token)

**Request Body**:
```json
{
  "refreshToken": "550e8400-e29b-41d4-a716-446655440000"
}
```

**Required Fields**:
- `refreshToken` (string): Valid refresh token UUID to revoke

**Success Response (200)**:
```json
{
  "message": "Successfully logged out"
}
```

**Error Responses**:
```json
// 400 - Missing refresh token
{
  "error": "Refresh token is required"
}

// 400 - Invalid refresh token
{
  "error": "Invalid refresh token"
}
```

### 7. Logout from All Devices
**POST** `/api/auth/logout-all`

**Description**: Logout user from all devices by revoking all refresh tokens

**Authentication**: Not required (but needs valid user ID)

**Request Body**:
```json
{
  "userId": 1
}
```

**Required Fields**:
- `userId` (number): User ID to logout all sessions for

**Success Response (200)**:
```json
{
  "message": "Successfully logged out from all devices"
}
```

**Error Responses**:
```json
// 400 - Missing user ID
{
  "error": "User ID is required"
}

// 400 - Invalid user ID
{
  "error": "Invalid user ID"
}
```

### 8. Get Active Sessions
**GET** `/api/auth/sessions/{userId}`

**Description**: Get all active sessions for a specific user

**Authentication**: Not required

**Path Parameters**:
- `userId` (number): User ID to get sessions for

**Success Response (200)**:
```json
{
  "sessions": [
    {
      "token": "550e8400-e29b-41d4-a716-446655440000",
      "deviceInfo": "Chrome on Windows (192.168.1.100)",
      "createdAt": "2024-01-15T10:30:00",
      "lastUsed": "2024-01-15T14:30:00"
    },
    {
      "token": "550e8400-e29b-41d4-a716-446655440001",
      "deviceInfo": "Mobile Safari on iOS (192.168.1.101)",
      "createdAt": "2024-01-14T08:15:00",
      "lastUsed": "2024-01-15T12:00:00"
    }
  ]
}
```

**Error Responses**:
```json
// 400 - Invalid user ID
{
  "error": "Invalid user ID"
}
```

---

## 🎫 Token Management Details

### Access Token (JWT)
- **Purpose**: Access protected resources
- **Storage**: Stateless (not stored in server)
- **Expiration**: 24 hours (86400000ms)
- **Type**: `access`
- **Format**: JWT
- **Contains**: 
  - `sub` (username)
  - `userId` (user ID)
  - `roles` (user roles array)
  - `email` (user email)
  - `tokenType` ("access")
  - `iat` (issued at)
  - `exp` (expiration)

### Refresh Token (Redis UUID)
- **Purpose**: Generate new access tokens
- **Storage**: Redis with TTL
- **Expiration**: 7 days (604800 seconds)
- **Format**: UUID (e.g., `550e8400-e29b-41d4-a716-446655440000`)
- **Security Features**:
  - ✅ **Instant revocation** - Delete from Redis
  - ✅ **Session tracking** - Monitor active sessions
  - ✅ **Device management** - Track device info
  - ✅ **Automatic expiry** - Redis TTL handles expiration
- **Contains** (in Redis):
  - `userId` (user ID)
  - `username` (username)
  - `deviceInfo` (browser/device info)
  - `createdAt` (creation timestamp)
  - `lastUsedAt` (last usage timestamp)
  - `ttl` (time to live in seconds)

### Token Usage
```bash
# Using access token in requests
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

---

## 🔧 User Roles & Permissions

### Available Roles in the System

| Role | Description | Capabilities | Assignment |
|------|-------------|--------------|------------|
| **USER** | Default Employee Role | • Basic system access<br/>• Create tickets<br/>• View own profile<br/>• Submit requests | ✅ Auto-assigned on registration |
| **HR** | Human Resources | • Manage employees<br/>• Department management<br/>• Attendance tracking<br/>• Leave approvals<br/>• Full HRMS access | ⚠️ Manual assignment required |
| **ADMIN** | System Administrator | • Full system access<br/>• User management<br/>• System configuration<br/>• All HR capabilities<br/>• Override permissions | ⚠️ Manual assignment required |
| **MANAGER** | Department/Team Manager | • Team management<br/>• Approve team requests<br/>• View team reports<br/>• Limited HR functions | ⚠️ Manual assignment required |

### Role Hierarchy & Permissions

```
ADMIN (Highest Authority)
  ├── Full system access
  ├── All HR capabilities
  ├── User role management
  └── System configuration

HR (Human Resources)
  ├── Employee management
  ├── Department operations
  ├── Attendance & leave
  └── HRMS full access

MANAGER (Team Leadership)
  ├── Team/department head
  ├── Approve team requests
  ├── View team analytics
  └── Limited HR functions

USER (Default Employee)
  ├── Basic system access
  ├── Personal profile
  ├── Submit requests
  └── View assigned tickets
```

### Multi-Role Support

**✅ Users can have multiple roles simultaneously:**

```json
{
  "userId": 123,
  "username": "john.manager",
  "email": "john@company.com",
  "roles": ["USER", "MANAGER", "HR"]
}
```

**Role Combination Examples:**
- `["USER"]` - Basic employee
- `["USER", "MANAGER"]` - Employee + Team lead
- `["USER", "HR"]` - Employee + HR access
- `["USER", "MANAGER", "HR"]` - Employee + Manager + HR
- `["USER", "ADMIN"]` - Employee + System admin
- `["ADMIN"]` - Pure administrator

### Role Assignment Logic

#### ✅ Automatic Assignment (Registration)
```sql
-- Default role assigned to all new users
INSERT INTO user_roles (user_id, role_id) 
SELECT user.id, role.id FROM users user, roles role 
WHERE user.username = 'new_user' AND role.name = 'USER';
```

#### ⚠️ Manual Assignment (Admin/HR Required)
```json
// Future endpoint: POST /api/auth/users/{userId}/roles
{
  "action": "add",
  "roles": ["MANAGER", "HR"]
}
```

### Role-Based Access Control (RBAC)

#### Service-Level Permissions

**Auth Service:**
- All endpoints: No role restrictions (authentication-based)

**HRMS Service:**
```java
@PreAuthorize("hasRole('HR') or hasRole('ADMIN')")  // HR operations
@PreAuthorize("hasRole('MANAGER')")                 // Manager operations  
```

**Ticket Service:**
- Create tickets: `USER` and above
- Assign tickets: `MANAGER` and above
- Admin functions: `ADMIN` only

**API Gateway:**
- Injects user roles in headers: `X-User-Roles: USER,MANAGER`

### Default System Users

| Username | Email | Roles | Password | Purpose |
|----------|-------|-------|----------|---------|
| `admin` | admin@company.com | `ADMIN` | `admin123` | System administrator |

### Role Validation Examples

```bash
# Login response shows user roles
curl -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'

# Response:
{
  "accessToken": "eyJ...",
  "refreshToken": "uuid-token",
  "userId": 1,
  "username": "admin", 
  "email": "admin@company.com",
  "roles": ["ADMIN"]
}
```

### Integration with Other Services

**API Gateway Injection:**
```http
X-User-Id: 123
X-Username: john.doe
X-User-Email: john@company.com  
X-User-Roles: USER,MANAGER,HR
```

**HRMS Service Usage:**
```java
// SecurityUtils checks
boolean isHR = SecurityUtils.isCurrentUserHR();           // HR role
boolean isManager = SecurityUtils.isCurrentUserManager(); // MANAGER role
boolean isAdmin = SecurityUtils.isCurrentUserAdmin();     // ADMIN role
boolean isEmployee = SecurityUtils.isCurrentUserEmployee(); // USER role
```

**Spring Security Integration:**
```java
@PreAuthorize("hasRole('HR')")
@PreAuthorize("hasRole('ADMIN')") 
@PreAuthorize("hasRole('MANAGER')")
@PreAuthorize("hasRole('USER')")
```

---

## 📊 Data Models

### User Entity
```json
{
  "id": "long",
  "username": "string (unique, 3-50 chars)",
  "email": "string (unique, valid email)",
  "password": "string (hashed)",
  "firstName": "string",
  "lastName": "string",
  "isActive": "boolean (default: true)",
  "roles": ["string array"],
  "createdAt": "datetime",
  "updatedAt": "datetime"
}
```

### LoginRequest
```json
{
  "username": "string (required)",
  "password": "string (required)"
}
```

### RegisterRequest
```json
{
  "username": "string (required, 3-50 chars)",
  "email": "string (required, valid email)",
  "password": "string (required, min 6 chars)",
  "firstName": "string (optional)",
  "lastName": "string (optional)"
}
```

### AuthResponse
```json
{
  "accessToken": "string",
  "refreshToken": "string", 
  "tokenType": "string (Bearer)",
  "userId": "long",
  "username": "string",
  "email": "string",
  "roles": ["string array"]
}
```

---

## 🚨 Error Responses

### Common Error Codes
- **400 Bad Request**: Invalid input data, validation errors
- **401 Unauthorized**: Invalid credentials
- **409 Conflict**: Username/email already exists
- **500 Internal Server Error**: Server error

### Error Response Format
```json
{
  "error": "Descriptive error message"
}
```

---

## 🧪 Testing the APIs

### Using cURL

1. **Register a new user**:
```bash
curl -X POST http://localhost:8081/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "email": "test@example.com",
    "password": "password123",
    "firstName": "Test",
    "lastName": "User"
  }'
```

2. **Login**:
```bash
curl -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "password123"
  }'
```

3. **Refresh token**:
```bash
curl -X POST http://localhost:8081/api/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{
    "refreshToken": "550e8400-e29b-41d4-a716-446655440000"
  }'
```

4. **Validate token**:
```bash
curl -X POST http://localhost:8081/api/auth/validate \
  -H "Content-Type: application/json" \
  -d '{
    "token": "YOUR_ACCESS_TOKEN_HERE"
  }'
```

5. **Health check**:
```bash
curl http://localhost:8081/api/auth/health
```

### Using JavaScript/Frontend

```javascript
// Register user
const registerResponse = await fetch('http://localhost:8081/api/auth/register', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    username: 'testuser',
    email: 'test@example.com',
    password: 'password123'
  })
});

const { accessToken, refreshToken } = await registerResponse.json();

// Use access token for protected requests
const protectedResponse = await fetch('http://localhost:8080/api/tickets', {
  headers: { 'Authorization': `Bearer ${accessToken}` }
});

// Refresh token when needed
const refreshResponse = await fetch('http://localhost:8081/api/auth/refresh', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ refreshToken })
});
```

---

## 🔄 Integration with Other Services

### API Gateway Integration
The API Gateway uses the `/validate` endpoint to:
1. Validate incoming JWT tokens
2. Extract user information (userId, username, roles)
3. Add user headers to downstream requests:
   - `X-User-Id`: User ID
   - `X-Username`: Username
   - `X-User-Email`: User email
   - `X-User-Roles`: Comma-separated roles

### Microservices Integration
Other services receive user context via headers:
```java
// In other microservices controllers
@RequestHeader("X-User-Id") Long userId
@RequestHeader("X-Username") String username
@RequestHeader("X-User-Roles") String roles
```

---

## 🔧 Configuration

### Environment Variables
- `JWT_SECRET`: Secret key for JWT signing (default: provided)
- `KAFKA_BOOTSTRAP_SERVERS`: Kafka server URL for user events
- `REDIS_HOST`: Redis server host (default: localhost)
- `REDIS_PORT`: Redis server port (default: 6379)
- `REDIS_PASSWORD`: Redis password (optional)
- Database connection (MySQL)

### Infrastructure Requirements
- **MySQL Database**: User and role data storage
- **Redis Server**: Refresh token storage and session management
- **Kafka Broker**: User event publishing

### Token Configuration
- **Access Token**: 24 hours (configurable via `jwt.access-token-expiration`)
- **Refresh Token**: 7 days (configurable via Redis TTL)
- **Redis TTL**: Automatic expiration handling

---

## 📈 Events Published

### User Registration Event
When a user registers, the service publishes a Kafka event:
```json
{
  "eventType": "USER_CREATED",
  "userId": 1,
  "username": "john.doe",
  "email": "john@example.com",
  "fullName": "John Doe"
}
```

**Topic**: `user-registered`

---

---

## 🔴 Redis Integration & Security Benefits

### Why Redis for Refresh Tokens?

| Feature | Previous (JWT) | Current (Redis) |
|---------|----------------|-----------------|
| **Revocation** | ❌ Impossible | ✅ Instant |
| **Session Tracking** | ❌ No | ✅ Full control |
| **Security** | ❌ Stolen tokens valid until expiry | ✅ Can revoke immediately |
| **Logout** | ❌ Not possible | ✅ Proper logout |
| **Device Management** | ❌ No tracking | ✅ Track all devices |
| **Performance** | ✅ Stateless | ✅ Very fast (Redis) |
| **Admin Control** | ❌ No control | ✅ Can revoke user sessions |

### Security Improvements
1. **Immediate Revocation**: Compromised tokens can be instantly invalidated
2. **Session Monitoring**: Track suspicious login patterns
3. **Device Management**: Users can see and manage their active sessions
4. **Proper Logout**: Users can securely log out from all devices
5. **Admin Oversight**: Administrators can revoke user access instantly

### Redis Data Structure
```
refresh_tokens:{tokenId} = {
  "userId": 123,
  "username": "john.doe",
  "deviceInfo": "Chrome 120 on Windows 10",
  "createdAt": "2024-01-15T10:30:00",
  "lastUsedAt": "2024-01-15T14:30:00",
  "ttl": 604800
}
```

---

## 🔧 Setup Instructions

### 1. Infrastructure Setup
```bash
# Start all services including Redis
docker-compose up -d

# Verify Redis is running
docker logs microservices-redis
```

### 2. Service Access
- **Swagger UI**: `http://localhost:8081/swagger-ui.html`
- **Via API Gateway**: `http://localhost:8080/api/swagger-ui.html`
- **API Documentation**: `http://localhost:8081/api-docs`
- **Redis Admin**: `redis-cli -h localhost -p 6379`

### 3. Database Setup
- **MySQL**: User/role data storage with automatic schema creation
- **Redis**: Refresh token storage with automatic TTL management

### 4. Initial Data
- Default roles (USER, ADMIN) should be seeded
- First user gets USER role automatically
- Redis starts empty and populates with user sessions

### 5. Monitoring
```bash
# Check Redis keys
redis-cli -h localhost -p 6379 keys "refresh_tokens:*"

# Monitor Redis operations
redis-cli -h localhost -p 6379 monitor

# Check active sessions count
redis-cli -h localhost -p 6379 eval "return #redis.call('keys', 'refresh_tokens:*')" 0
```

The Swagger UI provides an interactive interface to test all authentication APIs with enhanced security! 