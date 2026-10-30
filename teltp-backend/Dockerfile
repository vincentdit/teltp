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

# ---- run stage: slim JRE, non-root, health check ----
FROM eclipse-temurin:21-jre
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && useradd --uid 1001 --create-home teltp \
    && mkdir -p /var/teltp/content \
    && chown -R teltp:teltp /var/teltp
WORKDIR /app
COPY --from=build --chown=teltp:teltp /app/target/teltp-backend-1.0.0.jar app.jar
USER teltp
EXPOSE 8080
HEALTHCHECK --interval=15s --timeout=5s --start-period=90s --retries=20 \
  CMD curl -fsS http://127.0.0.1:8080/api/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
