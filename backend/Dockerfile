# ---------- Stage 1: build the JAR with Maven ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copy pom.xml first and download dependencies. Docker caches this layer,
# so rebuilds after a code change don't re-download the whole internet.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q package -DskipTests

# ---------- Stage 2: small runtime image with only the JRE ----------
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Run as a non-root user (a basic container security practice).
RUN addgroup -S app && adduser -S app -G app
USER app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
