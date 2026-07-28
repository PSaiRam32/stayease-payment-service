# 💳 StayEase Payment Service

![Java](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.5-brightgreen)
![Spring Security](https://img.shields.io/badge/Spring%20Security-Enabled-success)
![MySQL](https://img.shields.io/badge/Database-MySQL-blue)
![Spring Data JPA](https://img.shields.io/badge/Spring%20Data%20JPA-Hibernate-success)
![OpenFeign](https://img.shields.io/badge/OpenFeign-Service%20Communication-orange)
![Resilience4j](https://img.shields.io/badge/Resilience4j-Circuit%20Breaker-success)
![Razorpay](https://img.shields.io/badge/Payment-Razorpay-blue)
![Gradle](https://img.shields.io/badge/Build-Gradle-blueviolet)
![License](https://img.shields.io/badge/License-MIT-green)

---

# 📖 Overview

The **StayEase Payment Service** is responsible for managing the complete payment lifecycle within the StayEase microservices ecosystem.

As the financial transaction domain of the platform, the Payment Service handles payment order creation, payment confirmation, payment verification, refund processing, receipt generation, audit logging, scheduled payment expiration, and secure communication with external payment gateways.

The service integrates with the Booking Service to coordinate reservation payments and communicates with the Razorpay Payment Gateway for payment order creation, payment verification, and webhook processing.

Designed using Spring Boot and Spring Data JPA, the Payment Service follows enterprise microservices principles by maintaining dedicated ownership of financial transactions while collaborating with other services through well-defined APIs.

---

# 🎯 Business Problem

Processing online payments requires far more than simply recording whether a payment succeeded or failed.

A modern booking platform must ensure that:

- Payment orders are securely generated.
- Every payment is uniquely identifiable.
- Payments are verified before confirming bookings.
- Failed payments can be retried safely.
- Refunds are processed reliably.
- Payment receipts are generated.
- Financial events are auditable.
- Pending payments expire automatically.
- External payment gateway events are processed securely.
- Payment information remains isolated from other business domains.

Without a dedicated Payment Service, financial logic would become tightly coupled with booking management, leading to duplicated business rules, poor maintainability, and increased security risks.

---

# 💡 Business Solution

The StayEase Payment Service centralizes all payment-related functionality into a dedicated microservice while integrating with Razorpay for secure payment processing.

The service is responsible for:

- Payment Order Creation
- Payment Confirmation
- Payment Verification
- Retry Failed Payments
- Refund Processing
- Receipt Generation
- Payment Status Tracking
- Payment History
- Audit Logging
- Scheduled Payment Expiration
- Razorpay Webhook Processing
- Booking Payment Coordination

By isolating all payment-related operations into a dedicated service, StayEase maintains clear financial ownership while supporting scalable and secure transaction processing.

---

# ⚠ Development Note

The Payment Service integrates with the official Razorpay Java SDK for payment order creation and verification.

During backend development, a dedicated test confirmation endpoint is available to simulate successful payment completion because the StayEase frontend payment interface has not yet been implemented.

Once the frontend is integrated, payment confirmation will occur through the standard Razorpay Checkout flow, and the development endpoint can be removed without affecting the overall payment architecture.

---

# 🏢 Enterprise Concepts Demonstrated

This project demonstrates several enterprise backend engineering concepts commonly adopted in production systems.

- Database per Service
- Payment Lifecycle Management
- External Payment Gateway Integration
- Layered Architecture
- OpenFeign Client Communication
- Secure Payment Verification
- Refund Management
- Receipt Generation
- Audit Logging
- Webhook Processing
- Scheduled Background Jobs
- Bean Validation
- Global Exception Handling
- Structured Logging
- Transaction Management
- Resilience4j Retry
- Resilience4j Circuit Breaker
- Domain-Driven Service Separation

---

# 🎯 Project Objectives

The Payment Service has been designed with the following objectives:

- Centralize payment processing.
- Maintain ownership of financial transactions.
- Integrate securely with Razorpay.
- Coordinate booking payments.
- Support payment verification.
- Process refunds reliably.
- Generate payment receipts.
- Maintain complete payment audit history.
- Demonstrate enterprise-grade payment architecture.

---

# ✨ Features

## 💳 Payment Management

- Payment Order Creation
- Payment Confirmation
- Payment Verification
- Payment Status Tracking
- Payment History
- Retry Failed Payments

---

## 💰 Refund Management

- Refund Processing
- Refund Status Tracking
- Booking Refund Coordination

---

## 🧾 Receipt Management

- Receipt Generation
- Receipt Retrieval

---

## 🔐 Payment Security

- Razorpay Signature Verification
- Secure Payment Validation
- Payment Status Verification

---

## 📩 Gateway Integration

- Razorpay Order Creation
- Razorpay Webhook Processing
- Payment Synchronization

---

## 📋 Audit & Background Processing

- Audit Logging
- Scheduled Expiration of Pending Payments

---

## 🔄 Service Communication

- Booking Service Integration
- OpenFeign Communication

---

## 🚀 Reliability

- Resilience4j Retry
- Resilience4j Circuit Breaker
- Global Exception Handling
- Bean Validation
- Structured Logging
- Transaction Management

---

# 🛠 Technology Stack

| Category | Technology |
|-----------|------------|
| Language | Java 21 |
| Framework | Spring Boot 3 |
| Security | Spring Security |
| Database | MySQL |
| ORM | Spring Data JPA |
| Payment Gateway | Razorpay |
| Service Communication | OpenFeign |
| Fault Tolerance | Resilience4j |
| Validation | Bean Validation |
| Build Tool | Gradle |

---

# 🏛 High-Level Architecture

```text
                 Client / Booking Service
                          │
                          ▼
                 Payment Controller
                          │
                          ▼
                  Payment Service
                          │
      ┌───────────────────┴───────────────────┐
      ▼                                       ▼
 Payment Repository                    Razorpay Gateway
      │
      ▼
 MySQL Database
```

---

# 💳 Payment Service Responsibilities

The Payment Service acts as the financial transaction domain for the StayEase platform.

Its primary responsibilities include:

- Managing payment orders.
- Processing payment confirmations.
- Verifying payments.
- Coordinating booking payments.
- Processing refunds.
- Generating payment receipts.
- Maintaining audit logs.
- Handling payment webhooks.
- Expiring pending payments.
- Tracking payment history.

By isolating payment processing into a dedicated microservice, the StayEase platform maintains secure financial boundaries while enabling independent scalability and maintainability.

---

# 🌟 Why a Dedicated Payment Service?

Separating payment processing into its own microservice provides several enterprise advantages.

- Independent Financial Domain
- Secure Payment Processing
- External Payment Gateway Integration
- Centralized Refund Management
- Complete Audit Trail
- Reduced Service Coupling
- Independent Scalability
- Production-Ready Payment Architecture

---
# 📂 Project Structure

```text
stayease-payment-service
│
├── gradle/
│
├── src
│   ├── main
│   │
│   ├── java
│   │   └── com
│   │       └── stayease
│   │           └── payment_service
│   │
│   │               ├── config
│   │               │   ├── BookingServiceClient.java
│   │               │   ├── FeignClientConfig.java
│   │               │   ├── FeignConfig.java
│   │               │   ├── FeignErrorDecoder.java
│   │               │   ├── FeignLoggingConfig.java
│   │               │   ├── RazorpayClient.java
│   │               │   └── RazorpayFeignConfig.java
│   │               │
│   │               ├── controller
│   │               │   └── PaymentController.java
│   │               │
│   │               ├── dto
│   │               │   ├── request
│   │               │   ├── response
│   │               │   └── UserResponseDTO.java
│   │               │
│   │               ├── entity
│   │               │   ├── AuditLog.java
│   │               │   ├── PaymentOrder.java
│   │               │   ├── PaymentOrderStatus.java
│   │               │   ├── RefundStatus.java
│   │               │   └── RefundTransaction.java
│   │               │
│   │               ├── exception
│   │               │   ├── BusinessException.java
│   │               │   ├── GlobalExceptionHandler.java
│   │               │   └── ResourceNotFoundException.java
│   │               │
│   │               ├── repository
│   │               │   ├── AuditLogRepository.java
│   │               │   ├── PaymentOrderRepository.java
│   │               │   └── RefundTransactionRepository.java
│   │               │
│   │               ├── security
│   │               │   ├── HeaderAuthenticationFilter.java
│   │               │   └── SecurityConfig.java
│   │               │
│   │               ├── service
│   │               │
│   │               └── PaymentServiceApplication.java
│   │
│   ├── resources
│   │   └── application.yaml
│   │
│   └── test
│       └── java
│           └── com
│               └── stayease
│                   └── payment_service
│                       └── PaymentServiceApplicationTests.java
│
├── .gitattributes
├── .gitignore
├── LICENSE
├── build.gradle
├── build.log
├── gradlew
├── gradlew.bat
├── settings.gradle
└── README.md
```

---

# 📦 Package Responsibilities

The Payment Service follows a modular package structure where each package has a clearly defined responsibility. This separation improves maintainability, testability, scalability, and aligns with enterprise software engineering principles.

| Package | Responsibility |
|----------|----------------|
| **config** | Contains infrastructure configuration including OpenFeign clients, Razorpay integration, Feign logging, error decoding, and communication with external services. |
| **controller** | Exposes REST endpoints for payment creation, payment confirmation, refund processing, payment status retrieval, payment history, and receipt-related operations. |
| **dto** | Defines request and response models exchanged between clients, Booking Service, and external payment gateway integrations while keeping domain entities isolated from API contracts. |
| **entity** | Represents the payment domain model including payment orders, payment lifecycle states, refund transactions, audit logs. |
| **exception** | Implements centralized exception handling for business rule violations, resource lookup failures, payment processing errors, and standardized API error responses. |
| **repository** | Performs persistence operations using Spring Data JPA for payment orders, refund transactions, and audit logs while abstracting database access from business logic. |
| **security** | Secures internal APIs using Spring Security and Header Authentication Filter to ensure only trusted users and microservices access payment operations. |
| **service** | Contains the complete payment business logic including payment order creation, payment confirmation, payment verification, refund management, audit logging, receipt generation, and Booking Service coordination. |
| **resources** | Stores externalized configuration such as application properties, Razorpay credentials, logging configuration, and environment-specific settings. |
| **test** | Contains unit and integration tests validating payment workflows, business rules, and service interactions. |

---

# 🏗 Layered Architecture

The Payment Service follows a layered architecture that separates API exposure, business logic, persistence, and external payment gateway integration into independent layers.

```text
                    Booking Service / Client
                              │
                              ▼
                     Payment Controller
                              │
                              ▼
                      Payment Service
                 (Business Orchestration)
                              │
        ┌─────────────────────┼──────────────────────┐
        ▼                     ▼                      ▼
 Payment Repository     Booking Service        Razorpay SDK
                              │
                              ▼
                        Booking Service
                              │
                              ▼
                        MySQL Database
```

Each layer has a dedicated responsibility:

- **Controller Layer** receives and validates incoming REST requests.
- **Service Layer** executes payment business rules and orchestrates payment workflows.
- **Repository Layer** manages persistent financial records.
- **Booking Service Client** coordinates payment state with booking lifecycle.
- **Razorpay Integration** communicates securely with the external payment gateway.

This architecture promotes loose coupling, high cohesion, and independent evolution of each layer.

---

# 📚 Package Overview

## 📁 config

The `config` package contains all infrastructure-level configuration required for external communication and application behavior.

Responsibilities include:

- OpenFeign client configuration
- Razorpay client initialization
- Feign logging configuration
- Custom Feign error decoding
- Booking Service communication
- Infrastructure bean configuration

Keeping infrastructure concerns separate from business logic simplifies maintenance and improves extensibility.

---

## 📁 controller

The controller layer serves as the entry point for all payment-related REST APIs.

Major responsibilities include:

- Creating payment orders
- Confirming payments
- Retrieving payment details
- Processing refunds
- Fetching payment history
- Generating receipts
- Exposing development testing endpoints

Controllers remain lightweight by delegating business processing to the service layer.

---

## 📁 dto

The DTO package defines API contracts exchanged between services and external clients.

It contains:

- Request DTOs
- Response DTOs
- UserResponseDTO
- Payment response models
- Refund response models

Using DTOs prevents direct exposure of internal entities and enables API evolution without affecting persistence models.

---

## 📁 entity

The entity package models the financial domain of the application.

Core entities include:

- PaymentOrder
- PaymentOrderStatus
- RefundTransaction
- RefundStatus
- AuditLog

These entities represent the complete payment lifecycle while maintaining strong domain boundaries.

---

## 📁 repository

Repositories provide data access using Spring Data JPA.

Responsibilities include:

- Persisting payment orders
- Managing refund transactions
- Storing audit logs
- Querying payment history
- Tracking payment status

The repository layer abstracts database interaction from business logic.

---

## 📁 security

The security package protects payment operations against unauthorized access.

Responsibilities include:

- Spring Security configuration
- Header-based authentication
- Internal microservice authentication
- Request filtering
- Authorization support

Security is centralized to provide consistent protection across all payment APIs.

---

## 📁 service

The service layer is the core of the Payment Service.

It implements:

- Payment order creation
- Payment confirmation
- Payment verification
- Refund processing
- Receipt generation
- Audit logging
- Booking Service synchronization
- Payment retry handling
- Financial business validation

The service layer orchestrates all payment-related operations while coordinating with Razorpay and the Booking Service.

---
# 🔄 Payment Request Lifecycle

Every payment request follows a structured processing pipeline to ensure financial consistency, secure gateway communication, and reliable transaction management.

```text
                 Booking Service
                        │
                        ▼
              Payment Request Received
                        │
                        ▼
               Request Validation
                        │
                        ▼
         Verify Booking Information
                        │
                        ▼
        Generate Razorpay Payment Order
                        │
                        ▼
         Persist Payment Information
                        │
                        ▼
         Return Payment Order Details
                        │
                        ▼
               Await Payment Completion
```

This lifecycle ensures every payment request is validated, persisted, and synchronized with the external payment gateway before user payment begins.

---

# 💳 Payment Lifecycle

A payment progresses through multiple business states during its lifecycle.

```text
                Payment Created
                       │
                       ▼
                  PENDING
                       │
        ┌──────────────┴──────────────┐
        ▼                             ▼
 Payment Successful            Payment Failed
        │                             │
        ▼                             ▼
   COMPLETED                     FAILED
        │                             │
        ▼                             ▼
 Receipt Generated          Retry Payment
        │
        ▼
  Refund Requested
        │
        ▼
Refund Processing
        │
        ▼
Refund Completed
```

The Payment Service controls every transition to ensure payment consistency while coordinating with the Booking Service.

---

# 🏦 Payment Order Creation Workflow

When a customer books a property, the Booking Service requests the Payment Service to create a payment order.

```text
             Booking Service
                    │
                    ▼
        Create Payment Request
                    │
                    ▼
      Validate Booking Details
                    │
                    ▼
     Generate Razorpay Order
                    │
                    ▼
     Save Payment Order
                    │
                    ▼
 Return Payment Order Response
                    │
                    ▼
       Booking Service
```

The Payment Service owns payment creation while Razorpay generates the external payment order identifier used during checkout.

---

# ✅ Payment Confirmation Workflow

After the customer completes payment, the payment confirmation process validates and finalizes the transaction.

```text
             Payment Confirmation
                     │
                     ▼
         Retrieve Payment Order
                     │
                     ▼
        Verify Payment Status
                     │
                     ▼
     Validate Razorpay Response
                     │
                     ▼
      Update Payment Status
                     │
                     ▼
 Notify Booking Service
                     │
                     ▼
 Generate Receipt
                     │
                     ▼
 Return Success Response
```

During backend development, payment confirmation can also be triggered using the dedicated testing endpoint. Once the frontend is integrated, this confirmation will be initiated through the Razorpay Checkout flow.

---

# 🔐 Razorpay Verification Workflow

Payment authenticity is verified before any booking is confirmed.

```text
          Razorpay Payment
                  │
                  ▼
      Receive Payment Details
                  │
                  ▼
      Verify Payment Signature
                  │
                  ▼
   Retrieve Payment Information
                  │
                  ▼
 Compare Transaction Details
                  │
                  ▼
 Verification Successful
                  │
                  ▼
 Update Payment Status
```

This verification process protects the application against forged or tampered payment confirmations.

---

# 💰 Refund Processing Workflow

Refunds are processed independently of payment creation while maintaining a complete financial history.

```text
          Refund Request
                 │
                 ▼
     Validate Payment Status
                 │
                 ▼
   Validate Refund Eligibility
                 │
                 ▼
 Create Refund Transaction
                 │
                 ▼
 Update Refund Status
                 │
                 ▼
 Persist Refund Details
                 │
                 ▼
 Return Refund Response
```

Each refund operation is recorded separately to preserve complete transaction traceability.

---

# 🧾 Receipt Generation Workflow

Receipts are generated only after successful payment confirmation.

```text
      Payment Completed
              │
              ▼
 Retrieve Payment Details
              │
              ▼
 Generate Receipt
              │
              ▼
 Persist Receipt Information
              │
              ▼
 Return Receipt Response
```

Receipt generation provides users with an official record of completed financial transactions.

---

# 🔄 Retry Payment Workflow

If a payment fails due to temporary issues, users can retry payment without recreating the booking.

```text
        Payment Failed
               │
               ▼
      Retry Payment Request
               │
               ▼
 Validate Existing Booking
               │
               ▼
 Generate New Payment Order
               │
               ▼
 Save Updated Payment
               │
               ▼
 Return New Payment Order
```

This improves user experience while avoiding duplicate bookings.

---

# 📩 Booking Service Coordination Workflow

The Payment Service coordinates payment status with the Booking Service throughout the booking lifecycle.

```text
          Booking Service
                 │
                 ▼
      Create Payment Order
                 │
                 ▼
         Payment Service
                 │
        ┌────────┴────────┐
        ▼                 ▼
 Razorpay Gateway     Payment Database
        │
        ▼
 Payment Confirmation
        │
        ▼
 Notify Booking Service
        │
        ▼
 Booking Status Updated
```

The Booking Service remains the owner of reservation data, while the Payment Service exclusively owns financial transactions.

---

# 🌐 External Payment Gateway Communication

The Payment Service integrates with Razorpay to securely process payment operations.

```text
         Payment Service
                │
                ▼
        Razorpay Java SDK
                │
                ▼
       Razorpay REST APIs
                │
      ┌─────────┴─────────┐
      ▼                   ▼
Create Payment Order   Verify Payment
      │                   │
      └─────────┬─────────┘
                ▼
      Payment Response Returned
```

Using the official Razorpay SDK simplifies secure communication while reducing custom integration complexity.

---

# 🏢 Why Separate Payment Management?

Financial transactions require stronger consistency, traceability, and security than most business operations.

A dedicated Payment Service provides several enterprise advantages:

- Centralized ownership of all payment operations.
- Independent financial transaction lifecycle.
- Secure integration with external payment gateways.
- Reliable refund management.
- Complete payment audit history.
- Loose coupling between booking and payment domains.
- Independent scaling of payment workloads.
- Easier compliance with financial and auditing requirements.
- Improved maintainability through clear service boundaries.

By isolating payment processing into its own microservice, StayEase follows Domain-Driven Design principles while maintaining a scalable, secure, and production-ready financial architecture.

# 🏛 Enterprise Architecture Decisions

The StayEase Payment Service has been designed using enterprise software engineering principles to provide secure, scalable, maintainable, and reliable payment processing within a distributed microservices architecture.

Rather than acting as a simple CRUD application, the service owns the complete financial transaction domain while collaborating with other services through well-defined APIs and external payment gateway integration.

---

# 💳 Payment Processing Strategy

The Payment Service follows a centralized payment processing strategy where every financial transaction is managed within a dedicated microservice.

Instead of allowing multiple services to process payments independently, all payment operations are routed through the Payment Service.

This strategy provides several advantages:

- Centralized financial ownership
- Consistent payment lifecycle management
- Easier payment auditing
- Reduced business logic duplication
- Simplified gateway integration
- Improved maintainability

Centralizing payment processing ensures every transaction follows the same validation, verification, and persistence workflow.

---

# 🏦 Razorpay Integration Strategy

Rather than implementing payment processing from scratch, StayEase integrates with Razorpay as the external payment gateway.

The Payment Service uses the official Razorpay Java SDK to:

- Create payment orders
- Verify payment signatures
- Retrieve payment details
- Process refunds
- Synchronize payment information

Delegating payment gateway responsibilities to Razorpay allows the application to leverage a secure and industry-proven payment infrastructure while keeping business logic focused on booking and transaction coordination.

---

# 🔐 Payment Verification Strategy

Receiving a payment confirmation request alone is not sufficient to mark a transaction as successful.

Every payment undergoes a verification process before the payment status is updated.

The verification process includes:

- Payment existence validation
- Payment status validation
- Razorpay signature verification
- Transaction detail verification
- Business rule validation

Only verified payments are considered successful.

This prevents fraudulent or forged payment confirmations from affecting booking data.

---

# 🔄 Payment Lifecycle Strategy

Payments transition through a controlled lifecycle rather than allowing unrestricted status changes.

Typical payment states include:

- Pending
- Completed
- Failed
- Refunded

Each transition is validated against business rules before persistence.

Managing payments as a state machine improves consistency and prevents invalid financial operations.

---

# 💰 Refund Management Strategy

Refund processing is isolated from payment creation.

Each refund is treated as an independent financial transaction while maintaining a relationship with the original payment.

This strategy provides:

- Complete refund history
- Independent refund tracking
- Financial traceability
- Easier reconciliation
- Better reporting capabilities

Separating refunds from payments simplifies future enhancements such as partial refunds or multiple refund attempts.

---

# 🧾 Receipt Generation Strategy

Receipts are generated only after successful payment confirmation.

Generating receipts after verification ensures that only valid financial transactions produce official payment records.

Receipt generation provides:

- Proof of payment
- Financial documentation
- Customer transparency
- Future reporting support

This separation also allows receipt formats to evolve independently of payment processing.

---

# 📋 Audit Logging Strategy

Financial operations require complete traceability.

The Payment Service records important business events in dedicated audit logs.

Typical events include:

- Payment creation
- Payment confirmation
- Refund processing
- Payment failures
- Retry attempts

Maintaining audit logs provides:

- Operational visibility
- Easier debugging
- Financial auditing
- Historical tracking
- Compliance readiness

Audit logging ensures that every important financial action can be traced throughout the payment lifecycle.

---

# 🌐 Service Communication Strategy

The Payment Service communicates with the Booking Service using OpenFeign.

Responsibilities remain clearly separated:

**Booking Service**

- Booking ownership
- Reservation lifecycle
- Booking validation

**Payment Service**

- Payment ownership
- Financial transactions
- Refund processing
- Receipt generation

This loose coupling enables each service to evolve independently while collaborating through well-defined APIs.

---

# 🔄 Fault Tolerance Strategy

Distributed systems must handle temporary failures gracefully.

The Payment Service utilizes Resilience4j to improve communication reliability.

Fault tolerance mechanisms include:

- Retry
- Circuit Breaker

Benefits include:

- Temporary network recovery
- Reduced cascading failures
- Improved service availability
- Better user experience

These mechanisms increase overall resilience without complicating business logic.

---

# 🛡 Security Strategy

Financial APIs require stronger security controls than typical business APIs.

The Payment Service secures endpoints using Spring Security together with Header Authentication.

Security responsibilities include:

- Authentication
- Authorization
- Internal service validation
- Protected payment operations

Only authenticated users and trusted internal services can perform payment-related operations.

---

# 🗄 Database Design Strategy

The Payment Service follows the **Database per Service** pattern.

Only payment-related information is stored within the Payment Service database.

Examples include:

- Payment orders
- Refund transactions
- Audit logs

Booking information remains exclusively owned by the Booking Service.

This approach reduces coupling while maintaining clear domain ownership.

---

# 📦 DTO Strategy

DTOs separate API contracts from persistence models.

Benefits include:

- Entity encapsulation
- Stable APIs
- Independent model evolution
- Better validation
- Reduced coupling

Clients never interact directly with JPA entities.

---

# 🔁 Transaction Management Strategy

Payment operations frequently update multiple records.

Transactional boundaries ensure these operations execute atomically.

Typical transactional operations include:

- Payment confirmation
- Refund creation
- Audit log persistence

If any operation fails, the transaction is rolled back, preserving financial consistency.

---

# ✅ Validation Strategy

Validation occurs across multiple layers.

The Payment Service validates:

- Incoming request payloads
- Business rules
- Payment states
- Refund eligibility
- Booking consistency

Early validation reduces unnecessary processing and improves API reliability.

---

# ⚠ Exception Handling Strategy

The application centralizes error handling using a Global Exception Handler.

Business exceptions include:

- ResourceNotFoundException
- BusinessException

Centralized exception handling provides:

- Consistent API responses
- Cleaner controllers
- Easier maintenance
- Better client experience

---

# ⚙ Externalized Configuration Strategy

Application configuration is externalized through Spring Boot configuration files.

Examples include:

- Database configuration
- Razorpay credentials
- OpenFeign settings
- Logging configuration
- Environment profiles

Externalizing configuration improves portability and simplifies deployment across multiple environments.

---

# 📝 Logging Strategy

The Payment Service uses structured logging throughout important business operations.

Logged events include:

- Payment creation
- Payment confirmation
- Refund processing
- External gateway communication
- Business validation failures

Structured logging significantly improves production monitoring and troubleshooting.

---

# 🚀 Production Readiness

The Payment Service has been designed with production-oriented architecture principles.

Key production capabilities include:

- Secure payment processing
- External payment gateway integration
- Transaction management
- Global exception handling
- Audit logging
- Layered architecture
- OpenFeign communication
- Bean Validation
- Structured logging
- Resilience4j
- Database per Service
- Externalized configuration

These practices improve scalability, maintainability, and operational reliability.

---

# 🔮 Future Enhancements

The architecture has been designed to support future enhancements with minimal changes.

Potential improvements include:

- Razorpay Checkout UI integration
- Webhook-driven payment confirmation
- Kafka event publishing
- Distributed transactions using Saga Pattern
- Redis caching
- Payment analytics
- Multiple payment gateway support
- Scheduled reconciliation jobs
- Observability with Prometheus and Grafana
- Docker deployment
- Kubernetes orchestration
- CI/CD pipelines

The current architecture allows these capabilities to be introduced without significant refactoring.

---

# 🏗 Enterprise Design Principles

The Payment Service follows several enterprise software engineering principles.

- Single Responsibility Principle
- Separation of Concerns
- High Cohesion
- Loose Coupling
- Layered Architecture
- Domain-Driven Design
- Database per Service
- API-First Design
- Transactional Consistency
- Externalized Configuration

These principles improve long-term maintainability and support enterprise-scale growth.

---

# 📖 Enterprise Design Summary

The StayEase Payment Service demonstrates a production-oriented payment architecture by combining secure payment processing, external gateway integration, centralized financial ownership, audit logging, refund management, transaction consistency, and resilient inter-service communication.

Rather than embedding payment logic inside booking workflows, the application delegates all financial responsibilities to a dedicated Payment Service. This separation enables independent scalability, stronger security, easier maintenance, and cleaner domain boundaries while following modern microservices architecture principles.

# 🚀 Getting Started

Follow the steps below to set up and run the StayEase Payment Service locally.

---

# 📋 Prerequisites

Ensure the following software is installed before running the project.

| Software | Version |
|----------|---------|
| Java | 21 or later |
| Gradle | 8.x or later |
| MySQL | 8.x |
| Git | Latest |
| IDE | IntelliJ IDEA / Eclipse / VS Code |

You will also need:

- Running MySQL instance
- Razorpay Test Account
- Booking Service running for inter-service communication

---

# 📥 Clone Repository

```bash
git clone https://github.com/PSaiRam32/stayease-payment-service.git

cd stayease-payment-service
```

---

# ⚙ Configure Application

Update the `application.yaml` file with your local configuration.

Typical configurations include:

- Database URL
- Database Username
- Database Password
- Razorpay Key ID
- Razorpay Secret
- Booking Service URL
- Server Port

Never commit production secrets to the repository.

---

# 🗄 Database Setup

Create a MySQL database for the Payment Service.

Example:

```sql
CREATE DATABASE stayease_payment_service;
```

Spring Boot will automatically create the required tables using Hibernate.

---

# ▶ Running the Application

Using Gradle:

```bash
./gradlew bootRun
```

Or build the project:

```bash
./gradlew clean build
```

The service will start on the configured server port.

---

# 🌐 REST API Overview

The Payment Service exposes REST APIs for payment management.

Major API groups include:

## Payment APIs

- Create Payment Order
- Confirm Payment
- Verify Payment
- Retry Failed Payment
- Get Payment Details
- Get Payment History

## Refund APIs

- Process Refund
- Get Refund Details

## Receipt APIs

- Generate Receipt
- Get Receipt

## Development APIs

- Test Payment Confirmation

---

# 🧪 Testing

Testing can be performed using:

- Postman
- IntelliJ HTTP Client
- Swagger (if enabled)

Recommended test scenarios:

- Payment Order Creation
- Payment Confirmation
- Failed Payment
- Retry Payment
- Refund Processing
- Payment History Retrieval
- Receipt Generation
- Invalid Payment Requests

---

# 📊 Monitoring

Production deployments should monitor:

- Payment Success Rate
- Failed Payments
- Refund Volume
- API Response Time
- External Gateway Latency
- Error Rate
- Database Performance

These metrics help identify operational issues early.

---

# ⚡ Performance

The Payment Service is designed with performance and reliability in mind.

Performance considerations include:

- Spring Data JPA
- Efficient database access
- OpenFeign communication
- Structured logging
- Transaction management
- Resilience4j fault tolerance

---

# 🔒 Security Best Practices

The Payment Service protects sensitive financial operations using multiple security mechanisms.

Best practices include:

- Secure Razorpay credentials
- Header-based authentication
- Spring Security
- Input validation
- Payment verification
- Secure API communication

Secrets should always be managed using environment variables or external configuration services.

---

# 🤝 Contributing

Contributions are welcome.

Recommended workflow:

1. Fork the repository.
2. Create a feature branch.
3. Commit changes with meaningful messages.
4. Push the branch.
5. Open a Pull Request.

Please ensure:

- Coding standards are followed.
- Unit tests pass.
- Documentation is updated when required.

---

# 📄 License

This project is licensed under the MIT License.

Refer to the LICENSE file for details.

---

# 👨‍💻 Author

**Sai Ram Paidipati**

Java Backend Developer

GitHub:
https://github.com/PSaiRam32

LinkedIn:
https://www.linkedin.com/in/sairam-paidipati/

---

# 💬 Support

If you encounter issues or have suggestions:

- Open a GitHub Issue.
- Submit a Pull Request.
- Contact the author through GitHub or LinkedIn.

Constructive feedback and contributions are always appreciated.

---

# 📚 Learning Outcomes

This project demonstrates practical implementation of enterprise backend concepts including:

- Payment Processing
- External Payment Gateway Integration
- Razorpay SDK
- Payment Verification
- Refund Management
- Audit Logging
- Receipt Generation
- Spring Security
- Spring Data JPA
- OpenFeign
- Bean Validation
- Global Exception Handling
- Resilience4j
- Layered Architecture
- Domain-Driven Design
- Database per Service
- Transaction Management
- Structured Logging

---

# 📖 References

Official documentation and resources:

- Spring Boot Documentation
- Spring Security Documentation
- Spring Data JPA Documentation
- OpenFeign Documentation
- Resilience4j Documentation
- Razorpay Developer Documentation
- MySQL Documentation
- Gradle Documentation

---

# 📝 Project Summary

The StayEase Payment Service serves as the financial transaction domain of the StayEase platform.

It centralizes payment order creation, payment verification, refund management, receipt generation, audit logging, and secure communication with Razorpay while coordinating payment status with the Booking Service.

By following modern microservices principles such as Database per Service, Layered Architecture, OpenFeign communication, transaction management, and secure gateway integration, the Payment Service provides a scalable, maintainable, and production-oriented foundation for handling financial operations.

---

# 🙏 Acknowledgements

This project was developed to demonstrate enterprise-grade payment processing within a distributed microservices architecture.

It incorporates modern backend engineering practices including:

- Secure Payment Processing
- Razorpay Integration
- Payment Verification
- Refund Processing
- Receipt Generation
- Audit Logging
- Transaction Management
- OpenFeign Communication
- Spring Security
- Bean Validation
- Layered Architecture
- Domain-Driven Design
- Database per Service
- Resilience4j Fault Tolerance

Thank you for exploring the StayEase Payment Service repository. Your feedback and contributions are greatly appreciated.
