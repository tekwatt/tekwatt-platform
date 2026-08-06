FROM eclipse-temurin:21-jre
WORKDIR /app
COPY target/invoice-service-*.jar app.jar
EXPOSE 8091
ENTRYPOINT ["java","-jar","/app/app.jar"]
