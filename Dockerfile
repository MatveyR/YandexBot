FROM openjdk:23-jdk-slim

WORKDIR /app

COPY target/YandexBot-3.4.2.jar app.jar

CMD ["java", "-jar", "app.jar"]