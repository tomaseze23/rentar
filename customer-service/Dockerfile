# Imagen del Customer Service.
# Se construye desde la RAÍZ del repo porque el código gRPC se genera a partir de proto/rentar/cliente/v1/cliente.proto:
#   docker build -f customer-service/Dockerfile -t rentar/customer-service .

# --- Etapa 1: compilación (Maven + JDK) ---
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

COPY proto/rentar/cliente/v1/cliente.proto proto/rentar/cliente/v1/
COPY customer-service/pom.xml customer-service/
# Descarga las dependencias en una capa aparte para no repetirlo en cada cambio de código
RUN mvn -f customer-service/pom.xml -B -q dependency:go-offline

COPY customer-service/src customer-service/src
RUN mvn -f customer-service/pom.xml -B -q package -DskipTests

# --- Etapa 2: ejecución (sólo JRE + el jar) ---
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/customer-service/target/customer-service-*.jar app.jar

# Puerto gRPC
EXPOSE 9092
ENTRYPOINT ["java", "-jar", "app.jar"]
