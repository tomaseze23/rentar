# Se construye desde la RAÍZ del repo (igual que el resto de los servicios):
#   docker build -f registro-titularidad/Dockerfile -t rentar/registro-titularidad .

FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY proto/rentar/cliente/v1/cliente.proto proto/rentar/cliente/v1/
COPY registro-titularidad/pom.xml registro-titularidad/
RUN mvn -f registro-titularidad/pom.xml -B -q dependency:go-offline
COPY registro-titularidad/src registro-titularidad/src
RUN mvn -f registro-titularidad/pom.xml -B -q package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/registro-titularidad/target/registro-titularidad-*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
