FROM eclipse-temurin:21-jre
WORKDIR /app
COPY target/payment-service-*.jar app.jar
EXPOSE 8092
ENTRYPOINT ["java","-jar","/app/app.jar"]
