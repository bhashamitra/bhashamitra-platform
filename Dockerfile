# ---------- Build stage ----------
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copy everything (we'll add .dockerignore next to keep it lean)
COPY . .

# Build the Spring Boot JAR (this will also build the frontend via your Maven plugin)
RUN mvn -pl backend -am -DskipTests clean package


# ---------- Runtime stage ----------
FROM eclipse-temurin:21-jre
WORKDIR /app

# Copy the built jar from the build stage
COPY --from=build /app/backend/target/*.jar /app/app.jar

EXPOSE 8080

# Container-friendly JVM defaults
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
