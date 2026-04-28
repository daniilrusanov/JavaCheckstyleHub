# Stage 1: Build
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Copy Maven configuration and download dependencies
COPY pom.xml .
# Download dependencies (to cache this layer)
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build jar file, skipping tests for speed (or remove -DskipTests)
RUN mvn clean package -DskipTests

# Stage 2: Run
# Using JDK instead of JRE because we need javac compiler for code snippet analysis
FROM eclipse-temurin:17-jdk
WORKDIR /app

# Copy built jar from previous stage
COPY --from=build /app/target/*.jar app.jar

# Expose port (the one specified in application.properties)
EXPOSE 8000

# JVM: respect cgroup memory limits, avoid OOM kills in small Docker environments
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

# Run the application (exec + sh so JAVA_OPTS applies; proper signal handling)
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]