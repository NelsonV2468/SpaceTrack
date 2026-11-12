FROM node:24-bookworm-slim AS frontend
WORKDIR /workspace
COPY package.json package-lock.json ./
RUN npm ci --ignore-scripts
COPY frontend ./frontend
RUN mkdir -p src/main/resources/static/js && npm run build

FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml ./
RUN mvn -q -B -DskipTests dependency:go-offline
COPY src ./src
COPY --from=frontend /workspace/src/main/resources/static/js/login.bundle.js src/main/resources/static/js/login.bundle.js
# Las pruebas corren en CI/local; aqui se compila el artefacto final.
RUN mvn -q -B -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --uid 10001 spacetrack
COPY --from=build /workspace/target/*.jar app.jar
USER spacetrack
EXPOSE 10000
# El plan gratuito de Render tiene 512 MB: se limita el heap y se reduce el arranque.
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=65", "-XX:+UseSerialGC", "-XX:TieredStopAtLevel=1", "-Xss512k", "-jar", "/app/app.jar"]
