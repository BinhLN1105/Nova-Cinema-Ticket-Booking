# Hướng dẫn tích hợp SonarQube Cloud (SonarCloud) vào NovaTicket Monorepo

Dự án **NovaTicket** là một monorepo gồm nhiều dịch vụ/ngôn ngữ khác nhau:
1. **Backend (Spring Boot)**: Java 21, Maven.
2. **Android App**: Java 17, Gradle.
3. **React Frontend**: JS/TS, Vite.
4. **AI Service**: Python, FastAPI.

Do SonarCloud **không hỗ trợ quét tự động (Automatic Analysis)** cho ngôn ngữ Java (Java cần build và sinh bytecode để phân tích), chúng ta cần sử dụng phương pháp **phân tích qua CI/CD (CI-based Analysis)** bằng **GitHub Actions**.

---

## 🛡️ BƯỚC 1: Cấu hình trên Dashboard SonarCloud

1. Truy cập [sonarcloud.io](https://sonarcloud.io/) và đăng nhập bằng tài khoản GitHub của bạn.
2. Tại organization **LNB** (`binhln115`), chọn **Analyze new project** để liên kết với repository GitHub `BinhLN115/NovaTicket`.
3. **Vô hiệu hóa quét tự động (Automatic Analysis)**:
   - Vào **Project Settings** > **Analysis Method**.
   - **Tắt** tùy chọn **Automatic Analysis** (SonarCloud sẽ báo rằng bạn cần dùng CI/CD).
4. **Tạo SonarCloud Token (`SONAR_TOKEN`)**:
   - Truy cập **My Account** > **Security** > **Generate Token**.
   - Đặt tên token (ví dụ: `NovaTicket-Github-Token`) và sao chép mã token.
5. **Thêm Secret vào GitHub Repository**:
   - Mở repository của bạn trên GitHub.
   - Đi tới **Settings** > **Secrets and variables** > **Actions** > **New repository secret**.
   - Tạo secret có tên: `SONAR_TOKEN` và dán mã token vừa sao chép vào.

---

## 💻 BƯỚC 2: Cấu hình các Component trong Project

Chúng ta sẽ cấu hình từng dịch vụ tương ứng với Project Key trên SonarCloud.

### 2.1 Backend Spring Boot (`Backend/ticket-booking/pom.xml`)

Thêm các thuộc tính cấu hình Sonar và plugin **JaCoCo** (để đo độ bao phủ kiểm thử - Test Coverage) vào file `pom.xml`.

Mở file `Backend/ticket-booking/pom.xml` và thêm các thẻ dưới đây vào phần `<properties>`:

```xml
<!-- SonarCloud Properties -->
<sonar.organization>binhln115</sonar.organization>
<sonar.host.url>https://sonarcloud.io</sonar.host.url>
<sonar.projectKey>binhln115_novaticket-backend</sonar.projectKey>
<sonar.projectName>NovaTicket Backend</sonar.projectName>
<sonar.java.binaries>target/classes</sonar.java.binaries>
<!-- Đường dẫn xuất báo cáo JaCoCo coverage -->
<sonar.coverage.jacoco.xmlReportPaths>${project.build.directory}/site/jacoco/jacoco.xml</sonar.coverage.jacoco.xmlReportPaths>
```

Thêm plugin **JaCoCo** trong phần `<build><plugins>` của `pom.xml` để tự động sinh báo cáo test coverage khi chạy test:

```xml
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.11</version>
    <executions>
        <execution>
            <goals>
                <goal>prepare-agent</goal>
            </goals>
        </execution>
        <execution>
            <id>report</id>
            <phase>test</phase>
            <goals>
                <goal>report</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

> [!TIP]
> **Lệnh quét thủ công tại máy local (Backend):**
> Run lệnh sau từ thư mục `Backend/ticket-booking`:
> ```bash
> mvn clean verify sonar:sonar -Dsonar.token=MÃ_SONAR_TOKEN_CỦA_BẠN
> ```

---

### 2.2 Android App (`App/`)

#### A. Khai báo plugin trong `App/gradle/libs.versions.toml`:
Mở `App/gradle/libs.versions.toml` and thêm plugin sonar vào phần `[plugins]`:
```toml
[plugins]
# ... các plugin hiện tại ...
sonar = { id = "org.sonarqube", version = "5.0.0.4638" }
```

#### B. Khai báo trong root `App/build.gradle`:
Mở `App/build.gradle` (ở thư mục gốc của Android) và áp dụng plugin:
```groovy
plugins {
    // ... các plugin hiện tại ...
    alias(libs.plugins.sonar) apply false
}
```

#### C. Cấu hình trong `App/app/build.gradle`:
Mở `App/app/build.gradle` và cấu hình task sonar:
```groovy
plugins {
    // ... các plugin hiện tại ...
    alias(libs.plugins.sonar)
}

sonar {
    properties {
        property "sonar.projectKey", "binhln115_novaticket-android"
        property "sonar.organization", "binhln115"
        property "sonar.host.url", "https://sonarcloud.io"
        property "sonar.projectName", "NovaTicket Android"
        property "sonar.sources", "src/main/java"
        property "sonar.tests", "src/test/java"
        property "sonar.java.binaries", "build/intermediates/javac/debug/compileDebugJavaWithJavac"
        property "sonar.coverage.jacoco.xmlReportPaths", "build/reports/jacoco/jacocoTestReport/jacocoTestReport.xml"
    }
}
```

> [!TIP]
> Chạy lệnh sau trong PowerShell từ thư mục `App`:
> ```powershell
> $env:SONAR_TOKEN="MÃ_SONAR_TOKEN_CỦA_BẠN"
> ./gradlew assembleDebug sonar
> ```

---

### 2.3 React Frontend (`Frontend/nova-ticketbooking/`)

Tạo mới file cấu hình `sonar-project.properties` trong thư mục `Frontend/nova-ticketbooking/`:

```properties
sonar.organization=binhln115
sonar.projectKey=binhln115_novaticket-frontend
sonar.projectName=NovaTicket Frontend
sonar.host.url=https://sonarcloud.io
sonar.sources=src
sonar.tests=src
sonar.test.inclusions=src/**/*.test.js,src/**/*.test.jsx,src/**/*.test.ts,src/**/*.test.tsx,src/**/*.spec.js,src/**/*.spec.jsx,src/**/*.spec.ts,src/**/*.spec.tsx
sonar.exclusions=node_modules/**,dist/**,build/**,vite.config.js,eslint.config.js,postcss.config.js,tailwind.config.js
sonar.javascript.lcov.reportPaths=coverage/lcov.info
```

---

### 2.4 AI Service (`Backend/ai-booking/`)

Tạo mới file cấu hình `sonar-project.properties` trong thư mục `Backend/ai-booking/`:

```properties
sonar.organization=binhln115
sonar.projectKey=binhln115_novaticket-ai
sonar.projectName=NovaTicket AI
sonar.host.url=https://sonarcloud.io
sonar.sources=.
sonar.exclusions=venv/**,tests/**,__pycache__/**,*.log
sonar.python.version=3.10
```

---

## 🚀 BƯỚC 3: Tạo GitHub Actions Workflow tự động hóa

Tạo file workflow `.github/workflows/sonarcloud.yml` trong thư mục chính của dự án để tự động quét mã nguồn mỗi khi có thay đổi được đẩy lên GitHub:

```yaml
name: 🛡️ SonarCloud Analysis

on:
  push:
    branches:
      - main
      - dev
  pull_request:
    types: [opened, synchronize, reopened]

jobs:
  # Phân tích Backend (Java Maven)
  analyze-backend:
    name: ☕ Analyze Backend (Java)
    runs-on: ubuntu-latest
    steps:
      - name: Checkout Code
        uses: actions/checkout@v4
        with:
          fetch-depth: 0  # SonarCloud cần lịch sử git đầy đủ để hoạt động tốt

      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
          cache: 'maven'

      - name: Build and Test with Maven
        run: |
          cd Backend/ticket-booking
          mvn clean verify sonar:sonar -Dsonar.token=${{ secrets.SONAR_TOKEN }}
        env:
          SONAR_TOKEN: ${{ secrets.SONAR_TOKEN }}
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}

  # Phân tích Android App (Java Gradle)
  analyze-android:
    name: 📱 Analyze Android
    runs-on: ubuntu-latest
    steps:
      - name: Checkout Code
        uses: actions/checkout@v4
        with:
          fetch-depth: 0

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
          cache: 'gradle'

      - name: Build and Run Sonar Scanner
        run: |
          cd App
          chmod +x gradlew
          ./gradlew assembleDebug sonar
        env:
          SONAR_TOKEN: ${{ secrets.SONAR_TOKEN }}
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}

  # Phân tích React Frontend (JS/TS)
  analyze-frontend:
    name: ⚛️ Analyze Frontend
    runs-on: ubuntu-latest
    steps:
      - name: Checkout Code
        uses: actions/checkout@v4
        with:
          fetch-depth: 0

      - name: SonarCloud Scan
        uses: SonarSource/sonarcloud-github-action@master
        with:
          projectBaseDir: Frontend/nova-ticketbooking
        env:
          SONAR_TOKEN: ${{ secrets.SONAR_TOKEN }}
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}

  # Phân tích AI Service (Python)
  analyze-ai:
    name: 🤖 Analyze AI Service
    runs-on: ubuntu-latest
    steps:
      - name: Checkout Code
        uses: actions/checkout@v4
        with:
          fetch-depth: 0

      - name: SonarCloud Scan
        uses: SonarSource/sonarcloud-github-action@master
        with:
          projectBaseDir: Backend/ai-booking
        env:
          SONAR_TOKEN: ${{ secrets.SONAR_TOKEN }}
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
```

---

## 🎯 Kiểm tra kết quả

Sau khi bạn hoàn thành cấu hình và đẩy code lên GitHub:
1. GitHub Actions sẽ tự động chạy song song 4 job phân tích cho Backend, Android, Frontend và AI.
2. Các project riêng biệt sẽ tự động hiển thị trên Dashboard SonarCloud của bạn.
3. Bạn có thể theo dõi trực tiếp các chỉ số về: Bugs, Vulnerabilities, Security Hotspots, Code Smells và Duplications.
