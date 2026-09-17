# syntax=docker/dockerfile:1.7

FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY pom.xml pom.xml
COPY services/incident-service/pom.xml services/incident-service/pom.xml
COPY services/agent-service/pom.xml services/agent-service/pom.xml
COPY services/ops-mcp-server/pom.xml services/ops-mcp-server/pom.xml
COPY services/remediation-service/pom.xml services/remediation-service/pom.xml
RUN --mount=type=cache,target=/root/.m2 \
    mvn -q -pl services/remediation-service -am dependency:go-offline

COPY services/remediation-service/src services/remediation-service/src
RUN --mount=type=cache,target=/root/.m2 \
    mvn -q -pl services/remediation-service -am -DskipTests package

FROM eclipse-temurin:21-jre-jammy AS runtime
WORKDIR /app

RUN groupadd --system app && useradd --system --gid app --home /app app

COPY --from=build /workspace/services/remediation-service/target/remediation-service.jar /app/app.jar

USER app
EXPOSE 8083

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:+UseZGC -Djava.security.egd=file:/dev/./urandom" \
    SPRING_PROFILES_ACTIVE=docker

HEALTHCHECK --interval=15s --timeout=3s --start-period=30s --retries=5 \
  CMD wget --quiet --tries=1 --spider http://localhost:8083/actuator/health/readiness || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
