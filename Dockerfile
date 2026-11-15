# syntax=docker/dockerfile:1
# Multi-stage сборка: в первом образе Maven собирает jar, во второй (лёгкий JRE) попадает только jar.

# ---------- 1. Сборка ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /src

COPY pom.xml .
COPY domain/ domain/
COPY application/ application/
COPY infrastructure/ infrastructure/
COPY boot/ boot/

# Без BuildKit cache mount: Railway принимает cache mount только в своём формате id
RUN mvn -B -q -DskipTests package

# ---------- 2. Запуск ----------
FROM eclipse-temurin:21-jre
WORKDIR /app

# Приложение не должно работать от root
RUN groupadd --system app && useradd --system --gid app --home /app app

COPY --from=build /src/boot/payment-app/target/payment-app.jar app.jar
USER app

EXPOSE 8080
# MaxRAMPercentage — JVM берёт 75% памяти контейнера, а не всей машины
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
