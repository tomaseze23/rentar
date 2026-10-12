# Build context is the repository root because the service compiles shared protos.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

COPY proto/alquiler.proto proto/comun.proto proto/
COPY rental-service/rentalservice/pom.xml rental-service/rentalservice/
RUN mvn -f rental-service/rentalservice/pom.xml -B -q dependency:go-offline

COPY rental-service/rentalservice/src rental-service/rentalservice/src
RUN mvn -f rental-service/rentalservice/pom.xml -B -q package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/rental-service/rentalservice/target/rentalservice-*.jar app.jar

EXPOSE 9093
ENTRYPOINT ["java", "-jar", "app.jar"]
