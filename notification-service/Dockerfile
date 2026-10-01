FROM eclipse-temurin:21-jre-alpine
RUN apk add --no-cache libc6-compat gcompat snappy
WORKDIR /app
COPY target/notification-service-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 5002
ENTRYPOINT ["java", "-jar", "app.jar"]
