FROM eclipse-temurin:21-jdk-alpine

# Install curl for healthcheck and gcompat/libc6-compat for snappy-java JNI
RUN apk add --no-cache curl libc6-compat gcompat snappy

# Create non-root user for security
RUN addgroup -S spring && adduser -S spring -G spring

# Set working directory
WORKDIR /app

# Copy JAR from host (built via maven on the host)
COPY target/*.jar app.jar

# Create logs directory
RUN mkdir -p /var/log/auth-service && \
    chown -R spring:spring /var/log/auth-service

# Switch to non-root user
USER spring:spring

# Expose port (changed to 5001 from 8080)
EXPOSE 5001

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:5001/actuator/health || exit 1

# JVM options for container
ENV JAVA_OPTS="-XX:+UseContainerSupport \
               -XX:MaxRAMPercentage=75.0 \
               -XX:+UseG1GC \
               -XX:+ExitOnOutOfMemoryError \
               -Djava.security.egd=file:/dev/./urandom"

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
