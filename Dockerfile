# ---------------------------------------------------------------
# Etape 1 : compilation avec Maven (image lourde, jetee ensuite)
# ---------------------------------------------------------------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# On copie d'abord le pom.xml seul : tant qu'il ne change pas, Docker reutilise
# la couche des dependances deja telechargees (build beaucoup plus rapide).
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
# Les tests sont lances a part (mvn test) : ici on construit seulement le .jar
RUN mvn -B -q package -DskipTests

# ---------------------------------------------------------------
# Etape 2 : execution (image legere : juste Java + le .jar)
# ---------------------------------------------------------------
FROM eclipse-temurin:17-jre
WORKDIR /app

# Ne jamais faire tourner l'application en root dans le conteneur
RUN useradd --system --create-home autoloc
COPY --from=build /app/target/autoloc-api-*.jar app.jar
USER autoloc

EXPOSE 8080
# MaxRAMPercentage : Java utilise au plus 75 % de la memoire allouee au conteneur
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
