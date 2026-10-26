# Stage 1: build the fat jar
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
COPY src ./src
RUN mvn -q -DskipTests package

# Stage 2: minimal runtime
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /build/target/biteexpress-0.0.1-SNAPSHOT.jar /app/app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
