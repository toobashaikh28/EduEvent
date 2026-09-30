# ---- Build stage: compile the Spring Boot fat jar ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
# Cache dependencies in their own layer so rebuilds only re-download when pom.xml changes
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B clean package -DskipTests

# ---- Run stage: lightweight JRE ----
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
# Render injects $PORT; bind Spring Boot to it (defaults to 8080 locally)
# Flags tuned for a 512 MB / shared-CPU container:
#   MaxRAMPercentage=70   heap sized from the container limit (leaves room for metaspace/threads)
#   UseSerialGC           lowest memory + CPU overhead on 1 core
#   TieredStopAtLevel=1   much faster JIT warm-up (faster cold start, less CPU)
#   Xss512k               smaller thread stacks
ENTRYPOINT ["sh","-c","java -XX:MaxRAMPercentage=70 -XX:+UseSerialGC -XX:TieredStopAtLevel=1 -Xss512k -XX:+ExitOnOutOfMemoryError -jar app.jar --server.port=${PORT:-8080}"]
