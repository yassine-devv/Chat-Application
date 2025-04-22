# build applicazione
FROM maven:3.8.4-openjdk-17-slim AS build

WORKDIR /webapp

# copio il file pom.xml
COPY pom.xml .

# Scarica le dipendenze per migliorare la cache
RUN mvn dependency:go-offline

# Ora copio il resto del codice e compilo l'app
COPY src ./src
RUN mvn clean package -DskipTests

# STAGE 2: Creazione dell'immagine finale
FROM openjdk:17

# Imposto una directory di lavoro
WORKDIR /webapp

# Copio il file JAR dalla fase di build
COPY --from=build /webapp/target/chat-app-0.0.1-SNAPSHOT.jar demo-chat.jar

# Comando per avviare l'applicazione
CMD ["sh", "-c", "java -jar demo-chat.jar"]
