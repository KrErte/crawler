FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests -B

FROM eclipse-temurin:17-jre
WORKDIR /app

RUN groupadd -r appuser && useradd -r -g appuser -d /app appuser && \
    mkdir -p /app/uploads/cv && chown -R appuser:appuser /app

COPY --from=build --chown=appuser:appuser /app/target/*.jar app.jar

USER appuser
EXPOSE 8080

ENV JAVA_OPTS="-Xms256m -Xmx512m"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
