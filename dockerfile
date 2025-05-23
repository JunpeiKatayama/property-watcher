FROM openjdk:21-jdk-slim
WORKDIR /app
COPY build/libs/property_watcher-1.0-SNAPSHOT-all.jar app.jar
COPY config.json config.json
CMD ["java", "-jar", "app.jar"]
