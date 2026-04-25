# ==========================================
# GIAI ĐOẠN 1: BUILD (Đóng gói ứng dụng)
# Sử dụng image có sẵn Maven và JDK để build code
# ==========================================
FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder

WORKDIR /app

# Copy file pom.xml trước để cache lại các thư viện (dependencies)
# Nếu source code thay đổi nhưng pom.xml không đổi, Docker sẽ không tải lại thư viện
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy toàn bộ source code vào và build ra file .jar
COPY src ./src
RUN mvn clean package -DskipTests

# ==========================================
# GIAI ĐOẠN 2: RUN (Chạy ứng dụng)
# Chỉ sử dụng JRE (Java Runtime) siêu nhẹ để chạy, vứt bỏ toàn bộ source code và Maven
# ==========================================
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# 🛡️ BẢO MẬT: Không chạy ứng dụng bằng quyền Root
# Tạo một user giới hạn quyền tên là 'spring'
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copy file .jar đã được build từ Giai đoạn 1 sang Giai đoạn 2
COPY --from=builder /app/target/*.jar app.jar

# (Tùy chọn) Copy file cấu hình Firebase vào container nếu bạn vẫn dùng cách đọc file vật lý
# COPY --chown=spring:spring firebase.json /app/firebase.json

# Mở cổng 8080 để giao tiếp
EXPOSE 8080

# 🚀 ÉP CÂN RAM & KHỞI ĐỘNG
# -XX:MaxRAMPercentage=75.0 : Chỉ cho phép Java ăn tối đa 75% RAM của Container (Chừa 25% cho hệ điều hành Linux)
# -XX:+UseG1GC : Thuật toán dọn rác tối ưu nhất cho server ít RAM
# -Dspring.profiles.active=prod : Ép Spring Boot chạy cấu hình Prod
ENTRYPOINT ["java", \
            "-XX:MaxRAMPercentage=75.0", \
            "-XX:+UseG1GC", \
            "-Dspring.profiles.active=prod", \
            "-jar", "app.jar"]