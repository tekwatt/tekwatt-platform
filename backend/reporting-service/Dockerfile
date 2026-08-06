FROM eclipse-temurin:21-jre
WORKDIR /app
COPY target/reporting-service-*.jar app.jar
EXPOSE 8099
ENTRYPOINT ["java","-jar","/app/app.jar"]
