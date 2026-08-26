# Dockerfile de DESENVOLVIMENTO LOCAL — build JVM (sem GraalVM Native Image).
#
# O build nativo é reservado para a nuvem (imagem separada, ex: Dockerfile.cloud).
# Esta imagem compila o jar com Maven e roda com JRE 25.

# ---- Estágio 1: build com Maven + JDK 25 ----
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /app

# Copia apenas o pom primeiro para aproveitar cache de dependências
COPY pom.xml .
RUN mvn -q -B dependency:go-offline || true

# Código-fonte e build
COPY src ./src
RUN mvn -q -B clean package -DskipTests

# ---- Estágio 2: runtime JRE 25 ----
FROM eclipse-temurin:25-jre
WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]