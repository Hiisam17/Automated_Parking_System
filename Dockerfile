FROM maven:3.9.16-eclipse-temurin-21-alpine AS build
WORKDIR /workspace
COPY pom.xml .
COPY src src
RUN mvn --batch-mode -DskipTests package

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S parking && adduser -S parking -G parking
COPY --from=build /workspace/target/automated-parking-system-0.0.1-SNAPSHOT.jar app.jar
USER parking
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
