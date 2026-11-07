# Blinkweb Library Backend Service

Tenant-Specific Library Microservice for the Blinkweb SaaS Platform built with Spring Boot 3.

## Overview

This microservice provides multi-tenant library management capabilities, allowing individual tenants to manage books, categories, and settings with strict tenant isolation.

### Features
- **Multi-Tenancy Support**: Context-driven multi-tenant isolation with Hibernate filters and tenant resolution interceptors.
- **Book & Category Management**: CRUD operations for tenant-specific books and categories.
- **Authentication & Subscription**: OTP-based authentication, JWT security tokens, and tenant subscription validation.
- **Email Service**: Spring Boot Mail integration for verification and OTP notifications.

## Tech Stack
- **Java 17**
- **Spring Boot 3.2.5** (Web, Data JPA, AOP, Mail, Validation)
- **H2 In-Memory Database**
- **JJWT (Java JWT)**
- **Lombok**
- **Maven**

## Getting Started

### Prerequisites
- JDK 17 or higher
- Maven 3.8+

### Build & Run
```bash
# Build the project
mvn clean install

# Run the application
mvn spring-boot:run
```

By default, the server runs on port `8081`.

## Configuration
Application configurations are available in `src/main/resources/application.yml`. You can configure:
- Server port (`8081`)
- Tenant headers (`X-Tenant-ID`)
- Mail credentials (`MAIL_USERNAME`, `MAIL_APP_PASSWORD`)
- JWT secret (`JWT_SECRET`)
