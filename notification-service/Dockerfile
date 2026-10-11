FROM eclipse-temurin:21-jre-alpine
RUN apk add --no-cache libc6-compat gcompat snappy
WORKDIR /app
COPY target/notification-service-*.jar app.jar
EXPOSE 5002
ENTRYPOINT ["java", "-jar", "app.jar"]
