FROM eclipse-temurin:21-jre
WORKDIR /app
COPY target/notification-service-*.jar app.jar
EXPOSE 8093
ENTRYPOINT ["java","-jar","/app/app.jar"]
