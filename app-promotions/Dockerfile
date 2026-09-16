# ---------- Build stage ----------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build

# Cache dependencies separately from source changes
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q -DskipTests package

# ---------- Runtime stage ----------
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

RUN useradd --system --create-home appuser
USER appuser

COPY --from=build /build/target/app-promotions.jar app.jar

# Render (and most PaaS) inject $PORT at runtime; application.yml reads it via ${PORT:8080}
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
