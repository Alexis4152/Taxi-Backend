# ---- Build: compila el JAR con Maven (sin tests; esos corren en local/CI) ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q package -DskipTests

# ---- Runtime: solo el JRE y el JAR ----
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S app && adduser -S app -G app && mkdir -p /app/uploads && chown -R app:app /app
COPY --from=build /build/target/*.jar app.jar
USER app
# Fotos de operadores/pasajeros/taxis y logos: montar un volumen persistente en /app/uploads
ENV UPLOADS_DIR=/app/uploads \
    SERVER_PORT=8081 \
    JAVA_OPTS="-XX:MaxRAMPercentage=75"
EXPOSE 8081
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
