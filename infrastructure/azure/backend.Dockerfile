FROM eclipse-temurin:21-jre

WORKDIR /app
COPY app.jar /app/app.jar

ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=70.0 -XX:+ExitOnOutOfMemoryError"

USER 10001
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
