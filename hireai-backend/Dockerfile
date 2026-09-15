# =========================================================================
# Multi-stage Dockerfile for HireAI Backend
# =========================================================================

# --- Stage 1: Build ---
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app

# Copy Maven wrapper & POM to cache dependencies
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw
RUN ./mvnw dependency:go-offline -B

# Copy source code and build production jar
COPY src/ src/
RUN ./mvnw clean package -DskipTests

# --- Stage 2: Production Runtime ---
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Create a non-privileged user
RUN addgroup -S hireai && adduser -S hireai -G hireai

# Copy jar from build stage
COPY --from=builder /app/target/hireai-backend-*.jar app.jar

# Setup uploads directory for resume storage
RUN mkdir -p uploads && chown -R hireai:hireai /app

USER hireai

EXPOSE 8080

ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
