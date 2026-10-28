FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=build /app/target/payment-service-0.0.1-SNAPSHOT.jar app.jar

USER 10001:10001
ENV SERVER_ADDRESS=0.0.0.0

EXPOSE 8085

ENTRYPOINT ["java", "-jar", "app.jar"]