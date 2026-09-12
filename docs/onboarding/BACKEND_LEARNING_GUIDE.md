# Kheera Backend - Backend Development Learning Assignment

## Welcome

Welcome to the Kheera Backend Learning Program.

The goal of this assignment is not just to run a backend application but to understand how modern backend systems are built, structured, debugged, and extended.

By the end of this exercise, you will be able to:

* Setup a professional backend development environment
* Understand REST APIs
* Use Postman and Swagger
* Debug backend code
* Understand Spring Boot project structure
* Trace requests from Controller → Service → Repository → Database
* Build your own CRUD APIs
* Work with PostgreSQL databases

---

# Prerequisites

Please complete the following installations before beginning.

## 1. Install Git

Git is used for version control and collaboration.

Download:

https://git-scm.com/downloads

Verify Installation:

```bash
git --version
```

Expected Output:

```bash
git version x.x.x
```

---

## 2. Install PostgreSQL

PostgreSQL is the database used by Kheera Backend.

Download:

https://www.postgresql.org/download/

After installation:

Open pgAdmin or PostgreSQL terminal and execute:

```sql
CREATE DATABASE kheera;
```

Verify:

```sql
SELECT datname FROM pg_database;
```

You should see:

```text
kheera
```

---

## 3. Install IntelliJ IDEA

IntelliJ IDEA is the IDE used for backend development.

Download:

https://www.jetbrains.com/idea/download/

Community Edition is sufficient.

---

## 4. Install GitHub Desktop

GitHub Desktop simplifies Git operations.

Download:

https://desktop.github.com/

You may alternatively use Git commands directly.

---

## 5. Install JDK 21

Download:

https://adoptium.net/

Verify Installation:

```bash
java --version
```

Expected:

```bash
openjdk 21.x.x
```

---

# Project Setup

## 6. Clone Kheera Backend Repository

Clone the repository:

```bash
git clone <repository-url>
```

Navigate into the project:

```bash
cd kheera-backend
```

---

## 7. Configure Environment Variables

Create a file named:

```text
.env
```

In the project root directory.

Example:

```env
DB_URL=jdbc:postgresql://localhost:5432/kheera
DB_USERNAME=postgres
DB_PASSWORD=password

JWT_SECRET=your-secret-key
```

### Why use .env files?

Sensitive information should never be committed to Git repositories.

Examples:

* Database passwords
* API keys
* JWT secrets
* SMTP credentials

---

## 8. Run the Application

Open the project in IntelliJ.

Locate:

```text
KheeraBackendApplication.java
```

Run the application.

Wait until you see logs indicating:

```text
Started KheeraBackendApplication
```

---

# Verify Application

## 9. Check Health Endpoint

Open your browser:

```text
http://localhost:8080/api/health
```

Expected Response:

```json
{
  "status": "UP"
}
```

### What is a Health Endpoint?

Health endpoints are used by:

* Developers
* Monitoring systems
* Kubernetes
* Load Balancers

to determine whether a service is running properly.

---

# API Fundamentals

## 10. Install Postman

Download:

https://www.postman.com/downloads/

Postman is used to test APIs without needing a frontend application.

---

## 11. Learn REST APIs

REST stands for:

```text
Representational State Transfer
```

REST APIs expose resources through HTTP methods.

### GET

Used for reading data.

Example:

```http
GET /users
```

---

### POST

Used for creating data.

Example:

```http
POST /users
```

---

### PUT

Used for updating data.

Example:

```http
PUT /users/1
```

---

### DELETE

Used for deleting data.

Example:

```http
DELETE /users/1
```

---

# Swagger Documentation

## 12. Open Swagger UI

Navigate to:

```text
http://localhost:8080/swagger-ui/index.html
```

Swagger automatically documents all APIs.

Benefits:

* API discovery
* API testing
* Documentation
* Collaboration

---

## 13. Explore Existing APIs

Try calling every endpoint available.

Observe:

* Request Body
* Query Parameters
* Path Variables
* Response Body
* Status Codes

Examples:

```text
Register User
Login User
Change Password
Health Check
```

Questions to ask yourself:

* What data is being sent?
* What data is returned?
* What happens if invalid data is provided?

---

# Debugging

## 14. Learn Debugging

Debugging is one of the most important skills for backend engineers.

Place a breakpoint on any controller method.

Example:

```java
@PostMapping("/change-password"){
    
}
```

Start application in Debug Mode.

Execute the API.

Observe:

* Request values
* Method execution
* Variables
* Service calls

Useful shortcuts:

### Step Over

Execute current line.

### Step Into

Go inside called method.

### Resume

Continue execution.

---

# Understanding Backend Architecture

## 15. Study the Change Password API

The Change Password feature demonstrates a complete backend flow.

Follow the request from:

```text
Controller
    ↓
Service
    ↓
Repository
    ↓
Database
```

---

## Controller Layer

Responsibilities:

* Receive HTTP requests
* Validate inputs
* Return HTTP responses

Example:

```java
@PostMapping("/change-password"){
    
}
```

Think of controllers as the entry gate.

---

## Service Layer

Responsibilities:

* Business Logic
* Validation Rules
* Processing

Example:

```java
changePassword(PassWordChangeClass p){
    
}
```

Think of services as the brain of the application.

---

## Repository Layer

Responsibilities:

* Database access
* Queries
* Persistence

Example:

```java
userRepository.save(user);
```

Think of repositories as translators between Java and SQL.

---

## Entity Layer

Responsibilities:

* Database table mappings

Example:

```java
@Entity
public class User
```

Think of entities as representations of database tables.

---

## DTO Layer

Responsibilities:

* API contracts
* Request payloads
* Response payloads

Example:

```java
ChangePasswordRequest
```

DTOs prevent exposing internal entities directly.

---

# Assignment

# User Profile CRUD

You are now required to build a complete CRUD feature.

---

## Create Entity

Create:

```java
UserProfile
```

Fields:

```java
Long id
Long userId
String fullName
String bio
String phoneNumber
String profilePictureUrl
LocalDateTime createdAt
LocalDateTime updatedAt
```

---

## Create Repository

Create:

```java
UserProfileRepository
```

Responsibilities:

* Save profiles
* Retrieve profiles
* Delete profiles

---

## Create Service

Create:

```java
UserProfileService
```

Responsibilities:

* Business logic
* Validation
* CRUD operations

---

## Create Controller

Create:

```java
UserProfileController
```

Expose the following endpoints.

---

### Create Profile

```http
POST /api/profile
```

Request:

```json
{
  "fullName": "John Doe",
  "bio": "Backend Developer",
  "phoneNumber": "9999999999",
  "profilePictureUrl": "https://example.com/image.jpg"
}
```

---

### Get Profile

```http
GET /api/profile/{id}
```

---

### Update Profile

```http
PUT /api/profile/{id}
```

---

### Delete Profile

```http
DELETE /api/profile/{id}
```

---

# Expected Deliverables

The following files should be created:

```text
UserProfile.java

UserProfileRepository.java

UserProfileService.java

UserProfileController.java

CreateUserProfileRequest.java

UpdateUserProfileRequest.java

UserProfileResponse.java
```

---

# Bonus Challenges

## Challenge 1

Return proper HTTP status codes.

Examples:

```text
200 OK
201 CREATED
400 BAD REQUEST
404 NOT FOUND
500 INTERNAL SERVER ERROR
```

---

## Challenge 2

Add Pagination

Example:

```http
GET /api/profile?page=0&size=10
```

---

## Challenge 3

Add Search

Example:

```http
GET /api/profile/search?name=john
```

---

## Challenge 4

Add Validation

Examples:

```java
@NotBlank
@NotNull
@Size
```

---

## Challenge 5

Add Swagger Documentation

Document all endpoints.

---

## Challenge 6

Write Unit Tests

Cover:

* Create Profile
* Get Profile
* Update Profile
* Delete Profile

---

# Submission

Push your code to GitHub.

Create a Pull Request.

Include:

* Screenshots of Swagger
* Screenshots of Postman requests
* Brief explanation of your implementation

---

# Final Goal

This assignment is designed to simulate a real-world backend development task.

If you successfully complete this assignment, you will have worked with:

* Git
* GitHub
* PostgreSQL
* Java 21
* Spring Boot
* REST APIs
* Swagger
* Postman
* IntelliJ IDEA
* Debugging
* JPA/Hibernate
* Layered Architecture
* CRUD Operations

These concepts form the foundation of modern backend development.
