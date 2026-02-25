FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app

COPY pom.xml .
RUN mvn -q -DskipTests dependency:go-offline

COPY src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:21-jre

# ➜ optional aber sauber
RUN addgroup --system spring && adduser --system spring --ingroup spring

WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

# ➜ Upload Ordner anlegen
RUN mkdir -p /app/uploads && chown -R spring:spring /app

USER spring

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
