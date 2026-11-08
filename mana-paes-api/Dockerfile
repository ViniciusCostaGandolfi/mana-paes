# Dockerfile de PRODUÇÃO/NUVEM — build JVM (sem GraalVM Native Image).
#
# Build padrão com Maven + JDK 25 e runtime com JRE 25. O build nativo do
# GraalVM foi removido: o ganho de startup não compensava o tempo de build.
# Para desenvolvimento local existe o local.Dockerfile (equivalente em JVM).
#
# Build:  docker build -f Dockerfile -t mana-paes-api .
# Uso:    docker run --rm -p 8080:8080 -e SPRING_PROFILES_ACTIVE=prod ... mana-paes-api

# ---- Estágio 1: build com Maven + JDK 25 ----
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /workspace

# Copia apenas o pom primeiro para aproveitar cache de dependências
COPY pom.xml ./
RUN mvn -B -q dependency:go-offline || true

# Código-fonte e build (testes e jacoco ficam fora da imagem)
COPY src ./src
RUN mvn -B -q clean package -DskipTests -Djacoco.skip=true

# ---- Estágio 2: runtime JRE 25 ----
FROM eclipse-temurin:25-jre AS runtime
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && if ! getent passwd 1000 >/dev/null 2>&1; then groupadd --gid 1000 app && useradd --uid 1000 --gid 1000 --create-home --home-dir /app --shell /usr/sbin/nologin app; fi \
    && mkdir -p /app
WORKDIR /app

COPY --from=build --chown=1000:1000 /workspace/target/*.jar /app/app.jar

USER 1000
EXPOSE 8080
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:+UseG1GC"
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD curl -fsS http://localhost:8080/actuator/health || exit 1
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
