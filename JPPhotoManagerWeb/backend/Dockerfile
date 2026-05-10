FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -q
COPY src ./src
RUN mvn clean package -DskipTests -q

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
WORKDIR /app
COPY --from=build /app/target/photo-manager-1.0.0-SNAPSHOT.jar app.jar
RUN mkdir /thumbnails && chown appuser:appgroup /thumbnails
USER appuser
ENTRYPOINT ["java", "-jar", "app.jar"]
