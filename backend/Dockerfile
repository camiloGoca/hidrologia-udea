FROM maven:3.9.11-eclipse-temurin-17 AS builder

WORKDIR /workspace

COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B package -DskipTests

FROM eclipse-temurin:17-jre

RUN addgroup --system hidrologia && adduser --system --ingroup hidrologia hidrologia

WORKDIR /app

COPY --from=builder /workspace/target/backend-0.0.1-SNAPSHOT.jar app.jar

USER hidrologia

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
