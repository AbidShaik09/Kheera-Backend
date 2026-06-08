# Kheera Backend API Documentation

## Overview

Kheera is a collaborative project management platform designed for small engineering teams.

This document explains every publicly exposed API endpoint currently available in the Kheera Backend.

---

## Base URL

```text
https://theknightdevelopers.online/backend/kheera
```

---

## Swagger Documentation

Swagger UI:

```text
https://theknightdevelopers.online/backend/kheera/api/swagger-ui/index.html
```

OpenAPI Specification:

```text
https://theknightdevelopers.online/backend/kheera/v3/api-docs
```

---

# Authentication

Kheera uses JWT Authentication.

Protected endpoints require:

```http
Authorization: Bearer <jwt-token>
```

Example:

```http
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

---

# Health Controller

## Health Check

### Endpoint

```http
GET /api/health
```

### Purpose

Used to verify that the backend service is running.

### Authentication

Not Required

### Example Request

```http
GET /api/health
```

### Expected Response

```text
Application is running
```

### Learning Objectives

Observe:

* Simplest controller
* HTTP GET request
* Basic response handling

---

# Authentication Controller

Authentication APIs are responsible for:

* User registration
* User login
* OTP verification
* Password recovery

---

## Sign Up Email

### Endpoint

```http
POST /api/auth/signup-email
```

### Purpose

Sends an OTP to the specified email address.

### Authentication

Not Required

### Request Body

```json
{
  "email": "john@example.com"
}
```

### Flow

```text
User enters email
      ↓
OTP generated
      ↓
Email sent
      ↓
User receives OTP
```

---

## OTP Validation

### Endpoint

```http
POST /api/auth/otp-validation
```

### Purpose

Validates the OTP sent to the user.

### Authentication

Not Required

### Request Body

```json
{
  "email": "john@example.com",
  "otp": 123456
}
```

### Flow

```text
Email + OTP
      ↓
Validate OTP
      ↓
Return Success/Failure
```

---

## User Registration

### Endpoint

```http
POST /api/auth/signup
```

### Purpose

Creates a new user account.

### Authentication

Not Required

### Request Body

```json
{
  "email": "john@example.com",
  "password": "StrongPassword123",
  "name": "John Doe",
  "otp": 123456
}
```

### Field Description

| Field    | Description   |
| -------- | ------------- |
| email    | User email    |
| password | User password |
| name     | Display name  |
| otp      | Verified OTP  |

### Flow

```text
Email Verification
       ↓
OTP Validation
       ↓
Create User
       ↓
Store User in Database
```

### Notes

Passwords should never be stored in plain text.

Observe how password hashing is implemented in the service layer.

---

## Login

### Endpoint

```http
POST /api/auth/login
```

### Purpose

Authenticates a user and returns a JWT token.

### Authentication

Not Required

### Request Body

```json
{
  "email": "john@example.com",
  "password": "StrongPassword123"
}
```

### Flow

```text
Validate Credentials
        ↓
Generate JWT
        ↓
Return Token
```

### Learning Objectives

Observe:

* Authentication flow
* Password validation
* JWT generation
* Security filters

---

## Forgot Password

### Endpoint

```http
POST /api/auth/forgot-password
```

### Purpose

Initiates password recovery.

### Authentication

Not Required

### Request Body

```json
{
  "email": "john@example.com"
}
```

### Flow

```text
Email Submitted
       ↓
OTP Generated
       ↓
OTP Sent
       ↓
Password Reset Allowed
```

---

## Reset Password

### Endpoint

```http
POST /api/auth/reset-password
```

### Purpose

Resets a user's password after OTP verification.

### Authentication

Not Required

### Request Body

```json
{
  "email": "john@example.com",
  "password": "NewPassword123",
  "otp": 123456
}
```

### Flow

```text
Email
  ↓
OTP
  ↓
New Password
  ↓
Password Updated
```

### Learning Objectives

Observe:

* Validation
* Password hashing
* Database update operation

---

# User Controller

User APIs provide access to registered users.

---

## Get All Users

### Endpoint

```http
GET /api/users
```

### Authentication

Required

### Authorization Header

```http
Authorization: Bearer <jwt-token>
```

### Purpose

Returns all registered users.

### Response

```json
[
  {
    "id": "uuid",
    "name": "John Doe",
    "email": "john@example.com"
  }
]
```

### Response Fields

| Field | Description |
| ----- | ----------- |
| id    | User UUID   |
| name  | User name   |
| email | User email  |

### Learning Objectives

Observe:

* JWT authentication
* Security filters
* Protected routes
* DTO responses

### Debugging Exercise

Place breakpoints in:

```text
JwtAuthenticationFilter
UserController
UserService
UserRepository
```

Observe the entire authentication flow.

---

# Weather Controller

⚠️ Educational Controller

The Weather Controller exists primarily for learning purposes.

It demonstrates:

* REST APIs
* Request mapping
* HTTP methods
* Request bodies
* Controller development

It is not part of the core Kheera business functionality.

---

## Get Weather

### Endpoint

```http
GET /api/weather
```

### Purpose

Returns weather-related information.

### Authentication

Not Required

### Example Request

```http
GET /api/weather
```

### Learning Objectives

Observe:

* GET requests
* Controller responses
* Endpoint mapping

---

## Register Weather

### Endpoint

```http
POST /api/weather
```

### Purpose

Demonstrates handling request bodies.

### Authentication

Not Required

### Request Body

```json
"Some Weather Data"
```

### Learning Objectives

Observe:

* POST requests
* Request body binding
* Response generation

---

# DTO Reference

## LoginRequest

```json
{
  "email": "john@example.com",
  "password": "password"
}
```

---

## EmailRequest

```json
{
  "email": "john@example.com"
}
```

---

## OtpValidationRequest

```json
{
  "email": "john@example.com",
  "otp": 123456
}
```

---

## ResetPasswordRequest

```json
{
  "email": "john@example.com",
  "password": "NewPassword123",
  "otp": 123456
}
```

---

## SignUpRequest

```json
{
  "email": "john@example.com",
  "password": "StrongPassword123",
  "name": "John Doe",
  "otp": 123456
}
```

---

# Suggested Learning Path

Follow the APIs in this order:

1. GET /api/health
2. POST /api/auth/signup-email
3. POST /api/auth/otp-validation
4. POST /api/auth/signup
5. POST /api/auth/login
6. GET /api/users
7. POST /api/auth/forgot-password
8. POST /api/auth/reset-password

---

# Final Assignment

After understanding all existing APIs, implement:

```text
User Profile CRUD
```

Required Endpoints:

```http
POST   /api/profile
GET    /api/profile/{id}
PUT    /api/profile/{id}
DELETE /api/profile/{id}
```

Follow the same architecture used throughout Kheera:

```text
Controller
    ↓
Service
    ↓
Repository
    ↓
Database
```

The goal is to become comfortable reading an existing codebase and extending it with new functionality.
