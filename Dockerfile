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
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Copy built jar from previous stage
COPY --from=build /app/target/*.jar app.jar

# Expose port (the one specified in application.properties)
EXPOSE 8000

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]