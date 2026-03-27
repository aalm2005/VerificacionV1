# 1. Build stage (JDK 17)
FROM maven:3.9.11-eclipse-temurin-17 AS builder
WORKDIR /app

# Copy pom.xml first for dependency caching
COPY pom.xml .
RUN mvn -q -e -DskipTests dependency:go-offline

# Copy the rest of the source code
COPY src ./src

# Build the jar
RUN mvn -q -DskipTests package


# 2. Runtime stage (JRE 17)
FROM eclipse-temurin:17-jre-noble
WORKDIR /app

# Create user and group
RUN groupadd -r app && useradd -r -g app app

# Copy the jar from the builder stage
COPY --from=builder /app/target/*.jar app.jar

# Change ownership
RUN chown -R app:app /app

# Switch to non-root user
USER app

# Expose the application's port
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
