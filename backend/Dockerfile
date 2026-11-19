# --- Etape 1 : compilation Maven ---
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Les dependances sont telechargees dans une couche separee : elles restent en cache
# tant que pom.xml ne change pas.
COPY pom.xml ./
RUN mvn -B -q dependency:go-offline

COPY src src
# Les tests sont executes par la CI (job backend) ; l'image ne fait que packager.
RUN mvn -B -q -DskipTests package

# --- Etape 2 : image d'execution minimale ---
FROM eclipse-temurin:21-jre
WORKDIR /app

# Utilisateur non-root, proprietaire des dossiers de stockage (volumes montes en compose).
RUN groupadd --system jaankha \
    && useradd --system --gid jaankha --home /app jaankha \
    && mkdir -p /app/uploads /app/private-uploads /app/data \
    && chown -R jaankha:jaankha /app

COPY --from=build --chown=jaankha:jaankha /app/target/*.jar app.jar
USER jaankha

EXPOSE 8080
HEALTHCHECK --interval=15s --timeout=5s --start-period=60s --retries=5 \
    CMD curl -fsS "http://localhost:${SERVER_PORT:-8080}/actuator/health" || exit 1

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
