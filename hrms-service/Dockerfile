FROM openjdk:17-jdk-slim
WORKDIR /app
COPY target/hrms-service-1.0.0.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=docker"] 