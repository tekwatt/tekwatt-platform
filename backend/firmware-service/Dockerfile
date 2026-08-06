FROM eclipse-temurin:21-jre
WORKDIR /app
COPY target/firmware-service-*.jar app.jar
EXPOSE 8096
ENTRYPOINT ["java","-jar","/app/app.jar"]
