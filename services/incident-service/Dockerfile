# syntax=docker/dockerfile:1.7

# --- Build stage --------------------------------------------------------------
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace

# Copy just the poms first to leverage Docker layer caching for dependencies.
COPY pom.xml pom.xml
COPY services/incident-service/pom.xml services/incident-service/pom.xml
COPY services/agent-service/pom.xml services/agent-service/pom.xml
COPY services/ops-mcp-server/pom.xml services/ops-mcp-server/pom.xml
RUN --mount=type=cache,target=/root/.m2 \
    mvn -q -pl services/incident-service -am dependency:go-offline

# Now copy sources and build.
COPY services/incident-service/src services/incident-service/src
RUN --mount=type=cache,target=/root/.m2 \
    mvn -q -pl services/incident-service -am -DskipTests package

# --- Runtime stage ------------------------------------------------------------
FROM eclipse-temurin:21-jre-jammy AS runtime
WORKDIR /app

# Run as a non-root user.
RUN groupadd --system app && useradd --system --gid app --home /app app

COPY --from=build /workspace/services/incident-service/target/incident-service.jar /app/app.jar

USER app
EXPOSE 8080

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:+UseZGC -Djava.security.egd=file:/dev/./urandom" \
    SPRING_PROFILES_ACTIVE=docker

# Actuator liveness/readiness is exposed at /actuator/health/{liveness,readiness}.
HEALTHCHECK --interval=15s --timeout=3s --start-period=30s --retries=5 \
  CMD wget --quiet --tries=1 --spider http://localhost:8080/actuator/health/readiness || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
