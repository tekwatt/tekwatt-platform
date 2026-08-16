FROM eclipse-temurin:21-jre
WORKDIR /app
COPY target/support-service-*.jar app.jar
EXPOSE 8101
ENTRYPOINT ["java","-jar","/app/app.jar"]
