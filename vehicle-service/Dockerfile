# Imagen del Vehicle Service.
# Se construye desde la RAÍZ del repo porque el código gRPC se genera a partir de proto/:
#   docker build -f vehicle-service/Dockerfile -t rentar/vehicle-service .

# --- Etapa 1: compilación (Maven + JDK) ---
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

COPY proto/vehiculo.proto proto/comun.proto proto/
COPY vehicle-service/pom.xml vehicle-service/
# Descarga las dependencias en una capa aparte para no repetirlo en cada cambio de código
RUN mvn -f vehicle-service/pom.xml -B -q dependency:go-offline

COPY vehicle-service/src/main vehicle-service/src/main
RUN mvn -f vehicle-service/pom.xml -B -q package -DskipTests

# --- Etapa 2: ejecución (sólo JRE + el jar) ---
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/vehicle-service/target/vehicle-service-*.jar app.jar

# Puerto gRPC
EXPOSE 9091
ENTRYPOINT ["java", "-jar", "app.jar"]
