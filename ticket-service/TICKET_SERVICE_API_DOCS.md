# Ticket Service API Documentation

## Overview
The Ticket Service provides comprehensive APIs for managing tickets, projects, ticket comments, task relationships, and file attachments in a project management system with **strict role-based access control**.

## 🔐 **Access Control Model**

### **Role-Based Permissions**

| **Action** | **Project Owner** | **Project Member** | **Non-Member** |
|------------|-------------------|-------------------|----------------|
| **Create Ticket** | ✅ Yes | ❌ No | ❌ No |
| **Update Ticket** | ✅ Yes | ❌ No | ❌ No |
| **View Project Tickets** | ✅ Yes | ✅ Yes | ❌ No |
| **Delete Ticket** | ✅ Yes | ❌ No | ❌ No |
| **Assign Ticket** | ✅ Yes | ❌ No | ❌ No |
| **Comment on Ticket** | ✅ Yes | ✅ Yes | ❌ No |
| **Update Own Comment** | ✅ Yes | ✅ Yes | ❌ No |
| **Delete Any Comment** | ✅ Yes | ❌ No | ❌ No |
| **Add/Remove Members** | ✅ Yes | ❌ No | ❌ No |
| **Update Project** | ✅ Yes | ❌ No | ❌ No |

### **Key Access Rules**
1. **Ticket Creation/Modification**: Only project owners can create, update, or delete tickets
2. **Ticket Viewing**: Project members can view all tickets in their projects
3. **Ticket Comments**: All project members can comment on tickets (linked to tickets, not projects)
4. **Project Management**: Only owners can manage project settings and membership
5. **Cross-Project Isolation**: Users cannot access projects they don't belong to

## Base URLs
- **Direct Access**: `http://localhost:8084`
- **Via API Gateway**: `http://localhost:8080/api`
- **Swagger UI**: `http://localhost:8084/swagger-ui.html`
- **API Docs**: `http://localhost:8084/api-docs`

## Authentication
Most endpoints require JWT authentication via `Authorization: Bearer <token>` header.

---

## 📋 Ticket Management APIs

### 1. Get All Tickets (Admin Only)
**GET** `/api/tickets`

**Description**: Retrieve all tickets with pagination support

**Authentication**: Required (Admin/Elevated Privileges Only)

**Access Control**: 
- ❌ **Regular Users**: 403 Forbidden
- ✅ **Admin Users**: Full access to all tickets

**Query Parameters**:
- `page` (int): Page number (default: 0)
- `size` (int): Page size (default: 20)
- `sort` (string): Sort criteria (e.g., "id,desc")

**Response**:
```json
{
  "content": [
    {
      "id": 1,
      "title": "Fix login bug",
      "description": "Users cannot login with special characters",
      "status": "OPEN",
      "priority": "HIGH",
      "finishPercentage": 0,
      "project": {
        "id": 1,
        "name": "Web Application",
        "description": "Main web app project",
        "owner": {
          "id": 1,
          "username": "admin",
          "email": "admin@example.com"
        }
      },
      "assignee": {
        "id": 2,
        "username": "john.doe",
        "email": "john@example.com"
      },
      "createdAt": "2024-01-15T10:30:00",
      "updatedAt": "2024-01-15T10:30:00",
      "dueDate": "2024-01-20T17:00:00"
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 20
  },
  "totalElements": 1,
  "totalPages": 1
}
```

### 2. Get Ticket by ID (Project Members Only)
**GET** `/api/tickets/{id}`

**Authentication**: Required

**Access Control**: Only project members can view tickets

**Path Parameters**:
- `id` (long): Ticket ID

**Response**: Same as ticket object above

### 3. Create New Ticket (Project Owners Only)
**POST** `/api/tickets`

**Authentication**: Required

**Access Control**: ✅ **Project Owners Only** - Members cannot create tickets

**Request Body**:
```json
{
  "title": "Fix login bug",
  "description": "Users cannot login with special characters",
  "priority": "HIGH",
  "projectId": 1,
  "assigneeId": 2,
  "dueDate": "2024-01-20T17:00:00"
}
```

**Required Fields**:
- `title` (string): Ticket title
- `priority` (enum): LOW, MEDIUM, HIGH, CRITICAL
- `projectId` (long): Associated project ID

**Optional Fields**:
- `description` (string): Detailed description
- `assigneeId` (long): User ID to assign ticket to (must be project member)
- `dueDate` (datetime): Due date in ISO format

**Response**: Created ticket object (201 Created)

**Error Responses**:
- `403 Forbidden`: If user is not the project owner
- `400 Bad Request`: If trying to assign to non-member

### 4. Update Ticket (Project Owners Only)
**PUT** `/api/tickets/{id}`

**Authentication**: Required

**Access Control**: ✅ **Project Owners Only** - Members cannot update tickets

**Request Body** (all fields optional):
```json
{
  "title": "Updated title",
  "description": "Updated description",
  "status": "IN_PROGRESS",
  "priority": "MEDIUM",
  "assigneeId": 3,
  "dueDate": "2024-01-25T17:00:00",
  "finishPercentage": 50
}
```

**Error Responses**:
- `403 Forbidden`: If user is not the project owner

### 5. Delete Ticket (Project Owners Only)
**DELETE** `/api/tickets/{id}`

**Authentication**: Required

**Access Control**: ✅ **Project Owners Only** - Members cannot delete tickets

**Response**: 204 No Content

**Error Responses**:
- `403 Forbidden`: If user is not the project owner

### 6. Get Tickets by Project (Project Members Only)
**GET** `/api/tickets/project/{projectId}`

**Authentication**: Required

**Access Control**: ✅ **Project Members** - Can view all tickets in their projects

**Response**: Array of ticket objects

**Error Responses**:
- `403 Forbidden`: If user is not a project member

### 7. Get Tickets by Assignee
**GET** `/api/tickets/assignee/{assigneeId}`

**Authentication**: Required

**Response**: Array of ticket objects

### 8. Get Tickets by Status
**GET** `/api/tickets/status/{status}`

**Authentication**: Required

**Path Parameters**:
- `status` (enum): OPEN, IN_PROGRESS, RESOLVED, CLOSED, REOPENED

### 9. Search Tickets
**GET** `/api/tickets/search?keyword={keyword}`

**Authentication**: Required

**Query Parameters**:
- `keyword` (string): Search term for title/description
- Pagination parameters (`page`, `size`, `sort`)

### 10. Assign Ticket (Project Owners Only)
**PUT** `/api/tickets/{ticketId}/assign/{assigneeId}`

**Authentication**: Required

**Access Control**: ✅ **Project Owners Only** - Members cannot assign tickets

**Error Responses**:
- `403 Forbidden`: If user is not the project owner
- `400 Bad Request`: If trying to assign to non-member

### 11. Update Ticket Status (Project Owners Only)
**PUT** `/api/tickets/{ticketId}/status`

**Authentication**: Required

**Access Control**: ✅ **Project Owners Only** - Members cannot update status

**Request Body**:
```json
{
  "status": "IN_PROGRESS"
}
```

**Error Responses**:
- `403 Forbidden`: If user is not the project owner

### 12. Update Ticket Progress (Project Owners Only)
**PUT** `/api/tickets/{ticketId}/progress`

**Authentication**: Required

**Access Control**: ✅ **Project Owners Only** - Members cannot update progress

**Request Body**:
```json
{
  "finishPercentage": 75
}
```

**Error Responses**:
- `403 Forbidden`: If user is not the project owner

---

## 🏗️ Project Management APIs

### 1. Get All Projects (Admin Only)
**GET** `/api/projects`

**Authentication**: Required (Admin/Elevated Privileges)

**Description**: Returns **ALL projects in the entire system**. This is an administrative endpoint that provides system-wide visibility of all projects regardless of ownership or membership.

**Access Control**: 
- ❌ **Regular Users**: 403 Forbidden
- ✅ **Admin Users**: Full access to all projects

**⚠️ Important Notes**:
- This endpoint is designed for administrative purposes only
- Regular users should use user-specific endpoints (see endpoints 8-9 below)
- Returns 403 Forbidden for users without elevated privileges

**Response** (Admin users only):
```json
[
  {
    "id": 1,
    "name": "Web Application", 
    "description": "Main web application project",
    "owner": {
      "id": 1,
      "username": "admin",
      "email": "admin@example.com"
    },
    "createdAt": "2024-01-01T10:00:00",
    "updatedAt": "2024-01-15T10:30:00"
  },
  {
    "id": 2,
    "name": "Mobile App",
    "description": "Mobile application project", 
    "owner": {
      "id": 3,
      "username": "mobile.lead",
      "email": "mobile@example.com"
    },
    "createdAt": "2024-01-02T10:00:00",
    "updatedAt": "2024-01-16T10:30:00"
  }
]
```

**Alternative for Regular Users**:
- Use `GET /api/projects/owner/{ownerId}` to get projects you own
- Use `GET /api/projects/member/{memberId}` to get projects where you're a member

### 2. Get Project by ID
**GET** `/api/projects/{id}`

**Authentication**: Required

### 3. Create Project
**POST** `/api/projects`

**Authentication**: Required

**Request Body**:
```json
{
  "name": "New Project",
  "description": "Project description",
  "memberIds": [2, 3, 4]
}
```

**Required Fields**:
- `name` (string): Project name

**Optional Fields**:
- `description` (string): Project description
- `memberIds` (array): Array of user IDs to add as initial project members

### 4. Update Project
**PUT** `/api/projects/{id}`

**Authentication**: Required (Owner Only)

**Request Body**:
```json
{
  "name": "Updated Project Name",
  "description": "Updated description",
  "memberIds": [2, 3, 4, 5]
}
```

### 5. Delete Project
**DELETE** `/api/projects/{id}`

**Authentication**: Required (Owner Only)

### 6. Add Member to Project
**PUT** `/api/projects/{projectId}/members/{memberId}`

**Authentication**: Required (Owner Only)

**Description**: Add a user as a member to a project. Only project owners can add members.

**Path Parameters**:
- `projectId` (long): Project ID
- `memberId` (long): User ID to add as member

**Response**: Updated project object with new member

### 7. Remove Member from Project
**DELETE** `/api/projects/{projectId}/members/{memberId}`

**Authentication**: Required (Owner Only)

**Description**: Remove a user from project membership. Only project owners can remove members.

**Path Parameters**:
- `projectId` (long): Project ID
- `memberId` (long): User ID to remove from project

**Response**: Updated project object without the removed member

### 8. Get Projects by Owner (Recommended for Users)
**GET** `/api/projects/owner/{ownerId}`

**Authentication**: Required

**Description**: Get all projects owned by a specific user. **This is the recommended endpoint for users to get their owned projects.**

**Path Parameters**:
- `ownerId` (long): Owner user ID

**Usage Examples**:
```bash
# Get your own projects (replace 57 with your user ID)
curl -H "Authorization: Bearer YOUR_JWT_TOKEN" \
     http://localhost:8080/api/projects/owner/57
```

**Response**: Array of project objects
```json
[
  {
    "id": 9,
    "name": "User's Project",
    "description": "A project owned by the user",
    "owner": {
      "id": 57,
      "username": "john.doe",
      "email": "john@example.com"
    },
    "createdAt": "2024-01-15T10:30:00",
    "updatedAt": "2024-01-15T10:30:00"
  }
]
```

### 9. Get Projects by Member (Recommended for Users)
**GET** `/api/projects/member/{memberId}`

**Authentication**: Required

**Description**: Get all projects where a user is a member (but not necessarily the owner). **This is the recommended endpoint for users to get projects they participate in.**

**Path Parameters**:
- `memberId` (long): Member user ID

**Usage Examples**:
```bash
# Get projects where you're a member (replace 57 with your user ID)
curl -H "Authorization: Bearer YOUR_JWT_TOKEN" \
     http://localhost:8080/api/projects/member/57
```

**Response**: Array of project objects where the user is a member

**💡 Pro Tip**: To get all projects you have access to (both owned and member), call both endpoints:
- `/api/projects/owner/{yourUserId}` - Projects you own
- `/api/projects/member/{yourUserId}` - Projects you're a member of

---

## 💬 Ticket Comments APIs

### **🔄 Important Change**: Comments are now linked to **tickets**, not projects!

### 1. Get Comments for Ticket (Project Members Only)
**GET** `/api/tickets/{ticketId}/comments`

**Authentication**: Required

**Access Control**: ✅ **Project Members** - Can view comments on tickets in their projects

**Path Parameters**:
- `ticketId` (long): Ticket ID

**Response**:
```json
[
  {
    "id": 1,
    "content": "I think this might be related to the recent security update",
    "createdAt": "2024-01-15T14:30:00",
    "updatedAt": "2024-01-15T14:30:00",
    "author": {
      "id": 1,
      "username": "john.doe",
      "email": "john@example.com"
    },
    "ticket": {
      "id": 1,
      "title": "Fix login bug"
    }
  }
]
```

**Error Responses**:
- `403 Forbidden`: If user is not a project member
- `404 Not Found`: If ticket doesn't exist

### 2. Get Comments for Ticket (Paginated)
**GET** `/api/tickets/{ticketId}/comments/paginated`

**Authentication**: Required

**Access Control**: ✅ **Project Members** - Can view comments on tickets in their projects

**Query Parameters**:
- `page` (int): Page number (default: 0)
- `size` (int): Page size (default: 20)
- `sort` (string): Sort criteria

**Response**: Page object with comment list

### 3. Create Comment on Ticket (Project Members Only)
**POST** `/api/tickets/{ticketId}/comments`

**Authentication**: Required

**Access Control**: ✅ **Project Members** - All project members can comment on tickets

**Path Parameters**:
- `ticketId` (long): Ticket ID

**Request Body**:
```json
{
  "content": "This is my comment on the ticket. I think we should investigate the authentication module."
}
```

**Required Fields**:
- `content` (string): Comment content

**Response**: Created comment object (201 Created)

**Error Responses**:
- `403 Forbidden`: If user is not a project member
- `400 Bad Request`: If content is empty or invalid
- `404 Not Found`: If ticket doesn't exist

### 4. Update Comment (Comment Author Only)
**PUT** `/api/tickets/{ticketId}/comments/{commentId}`

**Authentication**: Required

**Access Control**: ✅ **Comment Author Only** - Users can only edit their own comments

**Path Parameters**:
- `ticketId` (long): Ticket ID
- `commentId` (long): Comment ID

**Request Body**:
```json
{
  "content": "Updated comment content with more details"
}
```

**Error Responses**:
- `403 Forbidden`: If user is not the comment author
- `400 Bad Request`: If content is empty
- `404 Not Found`: If comment doesn't exist

### 5. Delete Comment (Author or Project Owner)
**DELETE** `/api/tickets/{ticketId}/comments/{commentId}`

**Authentication**: Required

**Access Control**: 
- ✅ **Comment Author**: Can delete their own comments
- ✅ **Project Owner**: Can delete any comment in their project

**Path Parameters**:
- `ticketId` (long): Ticket ID
- `commentId` (long): Comment ID

**Response**: 204 No Content

**Error Responses**:
- `403 Forbidden`: If user is neither comment author nor project owner
- `404 Not Found`: If comment doesn't exist

### 6. Get Comment Count for Ticket
**GET** `/api/tickets/{ticketId}/comments/count`

**Authentication**: Required

**Response**:
```json
{
  "count": 15
}
```

### 7. Search Comments in Ticket
**GET** `/api/tickets/{ticketId}/comments/search`

**Authentication**: Required

**Access Control**: ✅ **Project Members** - Can search comments in tickets they have access to

**Query Parameters**:
- `keyword` (string): Search term for comment content
- Pagination parameters (`page`, `size`, `sort`)

**Description**: Search for comments within a specific ticket based on keyword

**Error Responses**:
- `403 Forbidden`: If user is not a project member

---

## 🔐 Project Access Control & Endpoint Guide

### **Quick Reference: Which Endpoint Should I Use?**

| Use Case | Endpoint | Access Level | Status |
|----------|----------|--------------|---------|
| **Get all projects I own** | `GET /api/projects/owner/{myUserId}` | ✅ Any User | **RECOMMENDED** |
| **Get projects I'm member of** | `GET /api/projects/member/{myUserId}` | ✅ Any User | **RECOMMENDED** |
| **Get ALL projects in system** | `GET /api/projects` | ❌ Admin Only | Returns 403 for regular users |
| **Get specific project details** | `GET /api/projects/{projectId}` | ❌ Admin Only | Returns 403 for regular users |
| **Create new project** | `POST /api/projects` | ✅ Any User | Creates project with you as owner |
| **Create ticket** | `POST /api/tickets` | ✅ Project Owner Only | Members cannot create |
| **View project tickets** | `GET /api/tickets/project/{projectId}` | ✅ Project Members | Both owners and members |
| **Comment on ticket** | `POST /api/tickets/{ticketId}/comments` | ✅ Project Members | Both owners and members |

### **Access Control Summary**

**🟢 Project Owner Access**:
- Full project management (create, update, delete)
- Complete ticket control (create, update, delete, assign)
- Member management (add/remove members)
- Comment management (create, update own, delete any)
- Project statistics and reporting

**🟡 Project Member Access**:
- View project details and tickets
- Comment on tickets (create, update own)
- Be assigned to tickets
- View project statistics

**🔴 Non-Member Access**:
- No access to project resources
- Cannot view tickets or comments
- Cannot participate in project activities

### **Recommended Usage Pattern for Regular Users**

```bash
# 1. Get your user ID from login response
LOGIN_RESPONSE=$(curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"your_username","password":"your_password"}')

USER_ID=$(echo $LOGIN_RESPONSE | grep -o '"userId":[0-9]*' | cut -d':' -f2)
TOKEN=$(echo $LOGIN_RESPONSE | grep -o '"accessToken":"[^"]*' | cut -d'"' -f4)

# 2. Get all projects you have access to
curl -H "Authorization: Bearer $TOKEN" \
     "http://localhost:8080/api/projects/owner/$USER_ID"

curl -H "Authorization: Bearer $TOKEN" \
     "http://localhost:8080/api/projects/member/$USER_ID"

# 3. Create a new project (you become owner)
curl -X POST -H "Authorization: Bearer $TOKEN" \
     -H "Content-Type: application/json" \
     -d '{"name":"My Project","description":"Project description"}' \
     "http://localhost:8080/api/projects"

# 4. Create tickets in your project (owner only)
curl -X POST -H "Authorization: Bearer $TOKEN" \
     -H "Content-Type: application/json" \
     -d '{"title":"My Ticket","priority":"HIGH","projectId":1}' \
     "http://localhost:8080/api/tickets"

# 5. Comment on tickets (members and owners)
curl -X POST -H "Authorization: Bearer $TOKEN" \
     -H "Content-Type: application/json" \
     -d '{"content":"This is my comment"}' \
     "http://localhost:8080/api/tickets/1/comments"
```

---

## 🔗 Task Relationship APIs

### 1. Create Task Relationship
**POST** `/api/tickets/relationships`

**Authentication**: Required

**Request Body**:
```json
{
  "sourceTaskId": 1,
  "targetTaskId": 2,
  "relationshipType": "BLOCKS"
}
```

**Relationship Types**:
- `BLOCKS`: Source task blocks target task
- `DEPENDS_ON`: Source task depends on target task
- `SUBTASK`: Source task is a subtask of target task
- `RELATED`: General relationship

### 2. Get Relationships by Task
**GET** `/api/tickets/relationships/task/{taskId}`

**Authentication**: Required

**Description**: Get all relationships (both incoming and outgoing) for a specific task

### 3. Get Outgoing Relationships
**GET** `/api/tickets/relationships/task/{taskId}/outgoing`

**Authentication**: Required

**Description**: Get relationships where the specified task is the source

### 4. Get Incoming Relationships
**GET** `/api/tickets/relationships/task/{taskId}/incoming`

**Authentication**: Required

**Description**: Get relationships where the specified task is the target

### 5. Get Relationships by Project
**GET** `/api/tickets/relationships/project/{projectId}`

**Authentication**: Required

**Description**: Get all task relationships within a specific project

### 6. Get Relationships by Type
**GET** `/api/tickets/relationships/type/{relationshipType}`

**Authentication**: Required

**Description**: Get all relationships of a specific type

**Path Parameters**:
- `relationshipType` (enum): BLOCKS, DEPENDS_ON, SUBTASK, RELATED

### 7. Delete Relationship
**DELETE** `/api/tickets/relationships/{relationshipId}`

**Authentication**: Required

### 8. Get Blocked Tasks
**GET** `/api/tickets/relationships/blocked-tasks`

**Authentication**: Required

**Description**: Get all tasks that are currently blocked by other tasks

### 9. Get Blocking Tasks
**GET** `/api/tickets/relationships/task/{taskId}/blocking`

**Authentication**: Required

**Description**: Get all tasks that are blocking the specified task

### 10. Get Subtasks
**GET** `/api/tickets/relationships/task/{taskId}/subtasks`

**Authentication**: Required

**Description**: Get all subtasks of the specified task

### 11. Get Parent Tasks
**GET** `/api/tickets/relationships/task/{taskId}/parents`

**Authentication**: Required

**Description**: Get all parent tasks of the specified task

### 12. Get Relationship Types
**GET** `/api/tickets/relationships/types`

**Authentication**: Not required

**Response**:
```json
["BLOCKS", "DEPENDS_ON", "SUBTASK", "RELATED"]
```

---

## 📊 Statistics APIs

### 1. Get Ticket Count by Status
**GET** `/api/tickets/stats/status/{status}`

**Authentication**: Required

**Response**:
```json
{
  "count": 15,
  "status": "OPEN"
}
```

### 2. Get Open Tickets by Assignee
**GET** `/api/tickets/stats/assignee/{assigneeId}/open`

**Authentication**: Required

**Response**:
```json
{
  "openTickets": 8,
  "assigneeId": 2
}
```

### 3. Get Project Statistics
**GET** `/api/tickets/stats/project/{projectId}`

**Authentication**: Required

**Response**:
```json
{
  "ticketCount": 25,
  "averageProgress": 68.5,
  "projectId": 1
}
```

---

## 🔧 Utility APIs

### 1. Get Ticket Priorities
**GET** `/api/tickets/priorities`

**Authentication**: Not required

**Response**:
```json
["LOW", "MEDIUM", "HIGH", "CRITICAL"]
```

### 2. Get Ticket Statuses
**GET** `/api/tickets/statuses`

**Authentication**: Not required

**Response**:
```json
["OPEN", "IN_PROGRESS", "RESOLVED", "CLOSED", "REOPENED"]
```

---

## 🚨 Error Responses

### Common Error Codes:
- **400 Bad Request**: Invalid input data
- **401 Unauthorized**: Missing or invalid JWT token
- **403 Forbidden**: Insufficient permissions (e.g., member trying to create ticket, non-member accessing project)
- **404 Not Found**: Resource not found
- **409 Conflict**: Constraint violation
- **500 Internal Server Error**: Server error

### Error Response Format:
```json
{
  "timestamp": "2024-01-15T14:30:00",
  "status": 403,
  "error": "Forbidden",
  "message": "Access denied. Only project owners can create tickets.",
  "path": "/api/tickets"
}
```

### **Access Control Error Examples**:

**Member trying to create ticket**:
```json
{
  "status": 403,
  "error": "Forbidden", 
  "message": "Access denied. Only project owners can create tickets."
}
```

**Non-member trying to view project tickets**:
```json
{
  "status": 403,
  "error": "Forbidden",
  "message": "Access denied. User is not a member of this project."
}
```

**Member trying to update ticket**:
```json
{
  "status": 403,
  "error": "Forbidden",
  "message": "Access denied. Only project owners can update tickets."
}
```

---

## 🔑 Authentication Headers

All protected endpoints require:
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

The JWT token should contain:
- `userId`: User ID
- `username`: Username
- `email`: User email
- `roles`: User roles array

---

## 📝 Data Models

### Ticket
```json
{
  "id": "long",
  "title": "string (required)",
  "description": "string",
  "status": "enum (OPEN, IN_PROGRESS, RESOLVED, CLOSED, REOPENED)",
  "priority": "enum (LOW, MEDIUM, HIGH, CRITICAL)",
  "finishPercentage": "integer (0-100)",
  "project": "Project object",
  "assignee": "User object",
  "createdAt": "datetime",
  "updatedAt": "datetime",
  "dueDate": "datetime"
}
```

### Project
```json
{
  "id": "long",
  "name": "string (required)",
  "description": "string", 
  "owner": "User object",
  "members": [
    {
      "id": "long",
      "username": "string",
      "email": "string"
    }
  ],
  "createdAt": "datetime",
  "updatedAt": "datetime"
}
```

### TicketComment
```json
{
  "id": "long",
  "content": "string (required)",
  "ticket": "Ticket object",
  "author": "User object", 
  "createdAt": "datetime",
  "updatedAt": "datetime"
}
```

### TaskRelationship
```json
{
  "id": "long",
  "sourceTask": "Ticket object",
  "targetTask": "Ticket object", 
  "relationshipType": "enum (BLOCKS, DEPENDS_ON, SUBTASK, RELATED)",
  "createdAt": "datetime",
  "createdBy": "User object"
}
```

### User
```json
{
  "id": "long",
  "username": "string",
  "email": "string"
}
```

---

## 🧪 Testing the APIs

### Testing Access Control

#### **1. Project Owner Workflow**:
```bash
# Owner creates project
curl -X POST -H "Authorization: Bearer OWNER_TOKEN" \
     -H "Content-Type: application/json" \
     -d '{"name":"Test Project","description":"Testing"}' \
     http://localhost:8080/api/projects

# Owner creates ticket ✅ (allowed)
curl -X POST -H "Authorization: Bearer OWNER_TOKEN" \
     -H "Content-Type: application/json" \
     -d '{"title":"Owner Ticket","priority":"HIGH","projectId":1}' \
     http://localhost:8080/api/tickets

# Owner adds member
curl -X PUT -H "Authorization: Bearer OWNER_TOKEN" \
     http://localhost:8080/api/projects/1/members/2

# Owner comments on ticket ✅ (allowed)
curl -X POST -H "Authorization: Bearer OWNER_TOKEN" \
     -H "Content-Type: application/json" \
     -d '{"content":"Owner comment"}' \
     http://localhost:8080/api/tickets/1/comments
```

#### **2. Project Member Workflow**:
```bash
# Member views project tickets ✅ (allowed)
curl -H "Authorization: Bearer MEMBER_TOKEN" \
     http://localhost:8080/api/tickets/project/1

# Member tries to create ticket ❌ (should return 403)
curl -X POST -H "Authorization: Bearer MEMBER_TOKEN" \
     -H "Content-Type: application/json" \
     -d '{"title":"Member Ticket","priority":"LOW","projectId":1}' \
     http://localhost:8080/api/tickets
# Expected: 403 Forbidden - Only project owners can create tickets

# Member comments on ticket ✅ (allowed)
curl -X POST -H "Authorization: Bearer MEMBER_TOKEN" \
     -H "Content-Type: application/json" \
     -d '{"content":"Member comment"}' \
     http://localhost:8080/api/tickets/1/comments

# Member tries to update ticket ❌ (should return 403)
curl -X PUT -H "Authorization: Bearer MEMBER_TOKEN" \
     -H "Content-Type: application/json" \
     -d '{"status":"IN_PROGRESS"}' \
     http://localhost:8080/api/tickets/1/status
# Expected: 403 Forbidden - Only project owners can update tickets
```

#### **3. Non-Member Workflow**:
```bash
# Non-member tries to view project tickets ❌ (should return 403)
curl -H "Authorization: Bearer NON_MEMBER_TOKEN" \
     http://localhost:8080/api/tickets/project/1
# Expected: 403 Forbidden - User is not a member of this project

# Non-member tries to comment ❌ (should return 403)
curl -X POST -H "Authorization: Bearer NON_MEMBER_TOKEN" \
     -H "Content-Type: application/json" \
     -d '{"content":"Non-member comment"}' \
     http://localhost:8080/api/tickets/1/comments
# Expected: 403 Forbidden - Only project members can comment
```

### **Testing Recommended Endpoints**:

```bash
# Get your own projects (works for any user)
curl -H "Authorization: Bearer YOUR_TOKEN" \
     "http://localhost:8080/api/projects/owner/YOUR_USER_ID"

# Get projects you're member of (works for any user)
curl -H "Authorization: Bearer YOUR_TOKEN" \
     "http://localhost:8080/api/projects/member/YOUR_USER_ID"

# Try to get all projects (403 for regular users)
curl -H "Authorization: Bearer YOUR_TOKEN" \
     "http://localhost:8080/api/projects"
# Expected: 403 Forbidden (unless you have admin privileges)
```

### **Testing Ticket Comments**:

```bash
# View comments on ticket (project members only)
curl -H "Authorization: Bearer MEMBER_TOKEN" \
     "http://localhost:8080/api/tickets/1/comments"

# Create comment (project members only)
curl -X POST -H "Authorization: Bearer MEMBER_TOKEN" \
     -H "Content-Type: application/json" \
     -d '{"content":"This is my comment on the ticket"}' \
     "http://localhost:8080/api/tickets/1/comments"

# Update own comment (comment author only)
curl -X PUT -H "Authorization: Bearer COMMENT_AUTHOR_TOKEN" \
     -H "Content-Type: application/json" \
     -d '{"content":"Updated comment"}' \
     "http://localhost:8080/api/tickets/1/comments/1"

# Search comments in ticket
curl -H "Authorization: Bearer MEMBER_TOKEN" \
     "http://localhost:8080/api/tickets/1/comments/search?keyword=security"
```

---

## 🔧 Setup Instructions

### 1. Database Configuration
The service uses PostgreSQL with the following configuration:
- **Host**: postgres:5432
- **Database**: tickets_db
- **Username**: ticket_user
- **Password**: ticket_password

### 2. Kafka Configuration
The service integrates with Kafka for event processing:
- **Bootstrap Servers**: kafka:29092
- **Consumer Group**: ticket-service-group-v2

### 3. MinIO Configuration
File attachment support via MinIO:
- **Endpoint**: http://minio:9000
- **Access Key**: minioadmin
- **Secret Key**: minioadmin123

### 4. Swagger Documentation
Access interactive API documentation:
- **Swagger UI**: `http://localhost:8084/swagger-ui.html`
- **API Docs JSON**: `http://localhost:8084/api-docs`

### 5. File Upload Configuration
The service supports file uploads with:
- **Maximum file size**: 20MB
- **Maximum request size**: 20MB

---

## 🚀 Additional Features

### Event-Driven Architecture
The service publishes and consumes events via Kafka for:
- User registration events
- Cross-service communication
- Real-time updates

### Security Features
- JWT-based authentication
- **Strict role-based access control** (Owner vs Member)
- Project ownership validation
- Member access restrictions
- **Project isolation** (users can only access their projects)

### Performance Features
- Lazy loading for entity relationships
- Pagination support for large datasets
- Optimized database queries
- Caching for frequently accessed data

### Development Features
- Comprehensive Swagger documentation
- Debug logging enabled
- SQL query logging for development
- Cross-origin resource sharing (CORS) support

---

## 📈 API Usage Patterns

### 1. Project Owner Workflow
```
1. Create Project → 2. Add Members → 3. Create Tickets → 4. Assign Tasks → 5. Track Progress
```

### 2. Project Member Workflow  
```
1. Join Project → 2. View Tickets → 3. Add Comments → 4. Get Assigned → 5. Collaborate
```

### 3. Ticket Discussion Workflow
```
1. View Ticket → 2. Add Comment → 3. Reply to Comments → 4. Update Progress → 5. Resolve
```

### **🔒 Access Control Workflow**
```
1. User Creates Project (becomes Owner) → 
2. Owner Adds Members → 
3. Owner Creates/Manages Tickets → 
4. Members View & Comment on Tickets → 
5. Collaborative Discussion via Comments
```

The Swagger UI provides an interactive interface to test all APIs directly from your browser! 