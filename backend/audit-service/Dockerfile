FROM eclipse-temurin:21-jre
WORKDIR /app
COPY target/audit-service-*.jar app.jar
EXPOSE 8097
ENTRYPOINT ["java","-jar","/app/app.jar"]
