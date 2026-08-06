FROM eclipse-temurin:21-jre
WORKDIR /app
COPY target/reservation-service-*.jar app.jar
EXPOSE 8089
ENTRYPOINT ["java","-jar","/app/app.jar"]
