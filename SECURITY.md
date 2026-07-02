# Security Policy - NovaTicket Project

We take the security of the NovaTicket platform seriously. This document outlines our policy for reporting vulnerabilities, supported versions, and a brief overview of how we mitigate common security risks to keep our users' data safe.

## Supported Versions

Only the latest version currently active in our CI/CD pipeline and deployed to staging/production environments receives security updates and vulnerability patches.

| Version | Supported | Notes |
| ------- | :---: | ----- |
| v1.0.x  | :white_check_mark: | Active Development & Testing (Sprint 4) |
| < v1.0  | :x: | Legacy / Experimental Builds |

## Our Security Architecture (OWASP Top 10 Mitigation)

NovaTicket is designed and built with security-first principles. We have implemented robust defense mechanisms to protect the application layer against common OWASP Top 10 vulnerabilities:

### 1. API Security & Input Sanitization (SQLi & XSS Defense)
*   **SQL Injection (SQLi) Prevention:** We use **Spring Data JPA** with parameterized queries and object-relational mapping (ORM) as our primary database access layer. All dynamic queries are parameterized to prevent malicious SQL command injection.
*   **Cross-Site Scripting (XSS) Sanitization:** We integrate **Jsoup** globally into our Jackson serialization/deserialization flow via `XssStringDeserializer`. Any string input sent to our API payloads is stripped of malicious HTML tags and scripts before reaching our controller logic.

### 2. API Abuse & Brute-Force Defense (Rate Limiting)
*   **AOP Redis Rate Limiting:** We implement a custom `@RateLimit` annotation powered by **Spring AOP (Aspect-Oriented Programming)** and **Redis**. 
*   **Mechanism:** Every endpoint decorated with `@RateLimit` keeps track of request rates per client IP in a Redis memory store. If a client exceeds the limit, the system blocks requests and returns `429 Too Many Requests`. This prevents brute-force login attempts and denial-of-service (DoS) attacks on critical resources.

### 3. Identity and Access Control (Broken Object Level Authorization)
*   **Stateless JWT Authentication:** Managed strictly by **Spring Security**. Access tokens are cryptographically signed, stateless JSON Web Tokens (JWT) containing user authorities and metadata.
*   **Method-Level Security:** Secure endpoints utilize Spring Security's `@PreAuthorize` or standard filter chain controls to enforce strict role-based access control (RBAC), preventing Broken Object Level Authorization (BOLA/IDOR) vulnerabilities.

---

## Reporting a Vulnerability

If you discover a security vulnerability (such as a Broken Object Level Authorization, SQLi, or Rate Limiting bypass) within this repository, **please do not open a public GitHub Issue.** Publicly disclosing vulnerabilities can expose active systems to malicious exploits.

Instead, please report it responsibly by following these steps:

1.  **Contact:** Send a detailed email to the project maintainer at: [binhluu953348@gmail.com](mailto:[binhluu953348@gmail.com])
2.  **Details to Include:**
    *   A description of the vulnerability and its potential impact.
    *   The specific endpoint affected (e.g., `POST /api/v1/bookings` or `POST /api/v1/auth/login`).
    *   A proof-of-concept (PoC) script, HTTP request payload, or step-by-step instructions to reproduce the issue.
    *   Any suggested remediation or patches if available.

We will acknowledge your report within **48 hours**, validate the issue, and work with our development team to deploy a patch in our next CI/CD deployment cycle. 

Thank you for helping keep NovaTicket secure!
