# ---- Build stage: compile the Spring Boot fat jar ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -q -B clean package -DskipTests

# ---- Run stage: lightweight JRE ----
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
# Render injects $PORT; bind Spring Boot to it (defaults to 8080 locally)
# -Xmx caps JVM heap so it fits inside a 512MB free-tier container
ENTRYPOINT ["sh","-c","java -Xmx350m -jar app.jar --server.port=${PORT:-8080}"]