# Se construye desde la RAÍZ del repo (igual que el resto de los servicios):
#   docker build -f registro-policial/Dockerfile -t rentar/registro-policial .

FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY registro-policial/pom.xml registro-policial/
RUN mvn -f registro-policial/pom.xml -B -q dependency:go-offline
COPY registro-policial/src registro-policial/src
RUN mvn -f registro-policial/pom.xml -B -q package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/registro-policial/target/registro-policial-*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
