FROM eclipse-temurin:21-jre
WORKDIR /app
COPY target/tenant-service-*.jar app.jar
EXPOSE 8085
ENTRYPOINT ["java","-jar","/app/app.jar"]
