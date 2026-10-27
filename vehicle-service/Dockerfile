# Imagen del Customer Service.
# Se construye desde la RAÍZ del repo porque el código gRPC se genera a partir de proto/rentar/cliente/v1/cliente.proto:
#   docker build -f customer-service/Dockerfile -t rentar/customer-service .

# --- Etapa 1: compilación (Maven + JDK) ---
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

COPY proto/rentar/vehiculo/v1/vehiculo.proto proto/rentar/vehiculo/v1/
COPY proto/rentar/comun/v1/comun.proto proto/rentar/comun/v1/
COPY vehicle-service/pom.xml vehicle-service/
# Descarga las dependencias en una capa aparte para no repetirlo en cada cambio de código
RUN mvn -f vehicle-service/pom.xml -B -q dependency:go-offline

COPY vehicle-service/src vehicle-service/src
RUN mvn -f vehicle-service/pom.xml -B -q package -DskipTests

# --- Etapa 2: ejecución (sólo JRE + el jar) ---
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/vehicle-service/target/vehicle-service-*.jar app.jar

# Puerto gRPC
EXPOSE 9091
ENTRYPOINT ["java", "-jar", "app.jar"]
