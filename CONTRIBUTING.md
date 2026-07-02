# Contributing to NovaTicket

Thank you for contributing to NovaTicket! To maintain code quality, security, and traceability throughout our Agile development lifecycle, please adhere to the guidelines outlined in this document.

---

## 1. Branching Strategy & Git Workflow

We use a modified Git Flow branching strategy. All feature development and bug fixes must be linked to a Jira issue key (e.g., `KAN-103`) to ensure automated CI/CD and Jira synchronization work correctly.

### Branch Naming Conventions
*   **Feature Branch:** `feature/[JIRA-KEY]-short-description` (e.g., `feature/KAN-103-vnpay-callback`)
*   **Bug Fix Branch:** `bugfix/[JIRA-KEY]-short-description` (e.g., `bugfix/KAN-104-fix-seat-lock`)
*   **Documentation:** `docs/[JIRA-KEY]-short-description` (e.g., `docs/KAN-105-update-api-docs`)

### Development Steps
1.  Pull the latest updates from the `develop` branch:
    ```bash
    git checkout develop
    git pull origin develop
    ```
2.  Create your branch matching the naming convention:
    ```bash
    git checkout -b feature/KAN-103-vnpay-callback
    ```
3.  Commit your changes and push to GitHub:
    ```bash
    git push origin feature/KAN-103-vnpay-callback
    ```
4.  Open a Pull Request (PR) from your branch to `develop`.

---

## 2. Commit Message Guidelines

We enforce **Conventional Commits** to keep our repository history clean and readable. Every commit message must start with a type, followed by an optional scope, the Jira Issue Key, and a clear description.

### Format
```text
<type>(<scope>): [JIRA-KEY] <description>
```

### Allowed Types
*   `feat`: A new feature (e.g., `feat(auth): [KAN-12] add Google OAuth2 login`)
*   `fix`: A bug fix (e.g., `fix(booking): [KAN-103] resolve null pointer exception in seat selection`)
*   `docs`: Documentation only changes (e.g., `docs(readme): [KAN-15] update database installation guide`)
*   `style`: Code style modifications (formatting, missing semi-colons, no logic changes)
*   `refactor`: Code changes that neither fix a bug nor add a feature
*   `test`: Adding missing tests or correcting existing tests (e.g., `test(api): [KAN-97] add booking state machine tests`)
*   `chore`: Updating build tasks, package manager configs, etc.

---

## 3. Pull Request (PR) Requirements

Before merging any code into `develop` or `main`, your Pull Request must satisfy the following checks:

### 1. Build Verification
*   The Backend project must compile and pass all unit tests:
    ```bash
    mvn clean compile test
    ```
*   The Frontend project must pass linting and build successfully:
    ```bash
    npm run lint
    npm run build
    ```

### 2. CI/CD Pipeline Checks
*   **GitHub Actions:** The automated API and E2E pipeline runs must pass.
*   **SonarCloud Quality Gate:** Static analysis must report **Passed**. No new Critical/Blocker code smells, security vulnerabilities, or bugs are allowed.
*   **Code Coverage:** Unit tests should maintain a minimum of **80%** test coverage in the service layer.

---

## 4. Code Style & Technical Stack Standards

Please respect the technical guidelines defined in the project architecture:
*   **Backend (Spring Boot):** Follow Java 21 conventions. Use Lombok, MapStruct, and keep business logic isolated in the Service Layer. Never execute raw SQL; utilize Spring Data JPA Parameterized Queries.
*   **Frontend (React):** Write clean, responsive components using Tailwind CSS. Follow semantic HTML rules.
*   **Database (PostgreSQL):** Table names should be snake_case and plural (e.g., `bookings`, `movies`). Column names must use snake_case.

Thank you for your cooperation in keeping the NovaTicket codebase clean, secure, and maintainable!
