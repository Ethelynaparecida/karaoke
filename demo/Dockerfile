# 1. Fase de Compilação (Build Stage)
# Usa uma imagem oficial do Maven e Java 21 para compilar o código
FROM maven:3.9.6-eclipse-temurin-21 AS build

WORKDIR /app
COPY pom.xml ./
COPY src ./src/

RUN mvn dependency:go-offline



RUN mvn dependency:go-offline

# ----------------------------------------------------
RUN mvn clean install -DskipTests

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY --from=build /app/target/demo-0.0.1-SNAPSHOT.jar app.jar


EXPOSE 8080


ENTRYPOINT ["java", "-jar", "app.jar"]