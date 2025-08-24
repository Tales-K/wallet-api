# syntax=docker/dockerfile:1.7
# Build stage
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app
# Copy pom first for better caching
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn -q -DskipTests dependency:go-offline
# Then copy sources
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -q -DskipTests package

# Runtime stage (alpine variant is smaller; plain jre is fine too)
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Create non-root user
RUN addgroup -S app && adduser -S -G app -u 10001 app
USER app

ENV TZ=UTC \
    JAVA_OPTS="-XX:+ExitOnOutOfMemoryError -XX:MaxRAMPercentage=75"

COPY --from=build /app/target/*-SNAPSHOT.jar /app/app.jar
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
