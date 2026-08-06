FROM eclipse-temurin:21-jre
WORKDIR /app
COPY target/analytics-service-*.jar app.jar
EXPOSE 8098
ENTRYPOINT ["java","-jar","/app/app.jar"]
