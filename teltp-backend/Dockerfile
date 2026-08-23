# syntax=docker/dockerfile:1

# ---- build stage: compile the Spring Boot jar with Maven ----
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app
# cache dependencies first
COPY pom.xml .
RUN mvn -q -B -DskipTests dependency:go-offline
# then build
COPY src ./src
RUN mvn -B -DskipTests clean package

# ---- run stage: slim JRE ----
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/teltp-backend-1.0.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
