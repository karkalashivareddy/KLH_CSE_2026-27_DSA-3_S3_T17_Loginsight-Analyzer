FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY backend/pom.xml backend/pom.xml
RUN mvn -B -f backend/pom.xml -DskipTests dependency:go-offline
COPY backend/src backend/src
RUN mvn -B -f backend/pom.xml -DskipTests package

FROM eclipse-temurin:21-jre-jammy AS runtime
RUN apt-get update \
    && apt-get install --no-install-recommends -y curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system loginsight \
    && useradd --system --gid loginsight --home-dir /app --create-home loginsight
WORKDIR /app
COPY --from=build /workspace/backend/target/loginsight-analyzer-0.1.0-SNAPSHOT.jar /app/loginsight.jar
COPY --chown=loginsight:loginsight sample-data /app/sample-data
ENV SERVER_PORT=8080 \
    LOGINSIGHT_SAMPLE_DATA_DIR=/app/sample-data \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"
USER loginsight
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=5 CMD curl --fail --silent --show-error http://127.0.0.1:8080/api/health >/dev/null || exit 1
ENTRYPOINT ["java", "-jar", "/app/loginsight.jar"]
