# Kheera Backend

Spring Boot backend for the Kheera project.

Built with:

* Java 21
* Spring Boot
* PostgreSQL
* JWT Authentication
* Swagger/OpenAPI

---

# Tech Stack

| Technology      | Purpose                        |
| --------------- | ------------------------------ |
| Java 21         | Backend language               |
| Spring Boot     | Backend framework              |
| PostgreSQL      | Database                       |
| Spring Security | Authentication & authorization |
| JWT             | Stateless authentication       |
| Swagger/OpenAPI | API documentation              |
| Maven           | Dependency management          |

---

# Prerequisites

Install the following before starting:

## Required Software

### 1. Java 21

Install JDK 21.

Verify installation:

```bash
java -version
```

---

### 2. IntelliJ IDEA

Recommended IDE:

* IntelliJ IDEA Community Edition

---

### 3. PostgreSQL

Install PostgreSQL.

Verify PostgreSQL is running.

---

# Clone Repository

```bash
git clone [<Kheera Backend>](https://github.com/AbidShaik09/Kheera-Backend)
```

Open the project in IntelliJ IDEA.

---

# Environment Variables

Create a file named:

```text
.env
```

in the project root.

Copy values from:

```text
.env.example
```

---

## Example .env

```env
JWT_SECRET=this_is_secret_key_make_it_long

DB_USERNAME=postgres
DB_PASSWORD=123

DB_SERVER=jdbc:postgresql://localhost:5433/kheera
```

---

# Example .env.example

```env
JWT_SECRET=

DB_USERNAME=

DB_PASSWORD=

DB_SERVER=
```

---

# Create Database

Open PostgreSQL and create database:

```sql
CREATE DATABASE kheera;
```

---

# Configure IntelliJ

## Set Project SDK

In IntelliJ:

```text
File → Project Structure → Project SDK
```

Select:

```text
Java 21
```

---

# Install Maven Dependencies

Open:

```text
pom.xml
```

Then click:

```text
Load Maven Changes
```

Wait for dependencies to download.

---

# Run Application

Run:

```text
KheeraBackendApplication.java
```

Application will start at:

```text
http://localhost:8081
```

---

# Swagger API Documentation

Swagger UI:

```text
http://localhost:8081/swagger-ui/index.html
```

---

# Authentication Flow

## Public Endpoints

```text
POST /api/auth/signup
POST /api/auth/login
```

---

## Protected Endpoints

All other endpoints require JWT token.

Use Swagger "Authorize" button to paste JWT token.

---

# Project Structure

```text
src/main/java/com/knightdevelopers/kheerabackend

├── config
├── controller
├── dto
├── entity
├── repository
├── security
├── service
```

---

# Git Workflow

## Create Feature Branch

```bash
git checkout -b feature/your-feature-name
```

---

## Commit Changes

```bash
git add .
git commit -m "Added feature"
```

---

## Push Branch

```bash
git push origin feature/your-feature-name
```

---

## Create Pull Request

Create PR into:

```text
develop
```

Never push directly to:

```text
main
```

---

# Team Rules

* Everyone writes code
* Everyone participates in backend/frontend discussions
* Small commits preferred
* Pull request review mandatory
* Ask questions early
* Focus on consistency over perfection

---

# Goal

This project is part of a 90-day challenge to:

* become employable full-stack developers,
* build real-world engineering skills,
* learn team collaboration,
* prepare for software engineering interviews.

We are not just building a project.

We are building engineers.
