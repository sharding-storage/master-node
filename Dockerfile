FROM openjdk:21-jdk-slim as builder

WORKDIR /app

COPY gradlew gradlew.bat settings.gradle build.gradle /app/
COPY gradle /app/gradle
RUN chmod +x ./gradlew

COPY src /app/src
RUN ./gradlew bootJar --no-daemon && \
    rm -rf /root/.gradle

FROM openjdk:21-jdk-slim

WORKDIR /app

COPY --from=builder /app/build/libs/master-0.0.1-SNAPSHOT.jar /app/app.jar
EXPOSE 9090
ENTRYPOINT ["java", "-jar", "app.jar"]