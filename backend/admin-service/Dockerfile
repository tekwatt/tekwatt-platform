FROM eclipse-temurin:21-jre
WORKDIR /app
COPY target/admin-service-*.jar app.jar
EXPOSE 8100
ENTRYPOINT ["java","-jar","/app/app.jar"]
