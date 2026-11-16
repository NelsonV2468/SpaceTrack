# 1) Frontend Next.js: se exporta como sitio estatico y queda en src/main/resources/webapp.
FROM node:24-bookworm-slim AS frontend
ENV NEXT_TELEMETRY_DISABLED=1
WORKDIR /workspace/frontend
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci --ignore-scripts
COPY frontend ./
RUN npm run build

# 2) Backend Spring Boot: empaqueta el API junto con el frontend exportado.
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml ./
RUN mvn -q -B -DskipTests dependency:go-offline
COPY src ./src
COPY --from=frontend /workspace/src/main/resources/webapp src/main/resources/webapp
# Las pruebas corren en CI/local; aqui se compila el artefacto final.
RUN mvn -q -B -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --uid 10001 spacetrack
COPY --from=build /workspace/target/*.jar app.jar
USER spacetrack
EXPOSE 10000
# Los servidores de Render usan UTC; la operacion es en Bogota y las fechas se guardan como hora local.
ENV TZ=America/Bogota
# El plan gratuito de Render tiene 512 MB: se limita el heap y se reduce el arranque.
ENTRYPOINT ["java", "-Duser.timezone=America/Bogota", "-XX:MaxRAMPercentage=65", "-XX:+UseSerialGC", "-XX:TieredStopAtLevel=1", "-Xss512k", "-jar", "/app/app.jar"]
