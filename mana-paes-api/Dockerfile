# Dockerfile de PRODUÇÃO/NUVEM — build GraalVM Native Image.
#
# Esta imagem compila o backend como executável nativo (startup rápido,
# baixo consumo de memória). Para desenvolvimento local, usar local.Dockerfile
# (JVM) via docker-compose.yaml.
#
# Build:  docker build -f Dockerfile -t mana-paes-api:native .
# Uso:    docker run --rm -p 8080:8080 -e SPRING_PROFILES_ACTIVE=prod ... mana-paes-api:native

# ---- Estágio 1: Maven (apenas para copiar o binário) ----
FROM maven:3.9-eclipse-temurin-25 AS maven

# ---- Estágio 2: build nativo com GraalVM CE 25 ----
FROM ghcr.io/graalvm/graalvm-community:25 AS build

# Copia o Maven da imagem oficial e ajusta o ambiente para o GraalVM
COPY --from=maven /usr/share/maven /usr/share/maven
ENV JAVA_HOME=/opt/graalvm-community-java25
ENV PATH="${JAVA_HOME}/bin:/usr/share/maven/bin:${PATH}"

WORKDIR /app

# process-aot sobe o contexto Spring durante o build e o perfil prod exige
# secrets sem default (fail-fast). Valores apenas para o estágio de build
# (multistage: so o binario e copiado; nada vaza para a imagem final).
# Em runtime o docker-compose injeta as mesmas variaveis.
ENV SPRING_PROFILES_ACTIVE=prod \
    DB_HOST=postgres DB_PORT=5432 DB_NAME=manapaes DB_USER=manapaes DB_PASSWORD=manapaes \
    RABBIT_HOST=rabbitmq RABBIT_PORT=5672 RABBIT_USER=manapaes RABBIT_PASSWORD=manapaes \
    SMTP_HOST=localhost SMTP_PORT=587 SMTP_USER=none SMTP_PASSWORD=none \
    MAIL_ENABLED=false NOTIFICATIONS_ENABLED=true SCHEDULER_ENABLED=true \
    JWT_SECRET=mana-paes-dev-secret-change-me-0123456789 \
    APP_ENCRYPTION_MASTER_KEY=mana-paes-dev-master-key-32chars! \
    EVOLUTION_API_URL=http://localhost EVOLUTION_API_KEY=none \
    APP_BACKEND_URL=https://dev.api.mana-paes.vgandolfi.dev \
    FRONTEND_URL=https://dev.mana-paes.vgandolfi.dev

# Copia apenas o pom primeiro para aproveitar cache de dependências
COPY pom.xml .
RUN mvn -q -B dependency:go-offline || true

# Código-fonte
COPY src ./src

# Gera o executável nativo (Spring AOT + GraalVM native-image)
# native:compile depende do processo AOT do Spring Boot (perfil -Pnative).
# -Pnative,prod: ativar um profile via -P desativa os activeByDefault (dev).
# O profile prod define spring.profiles.active=prod (necessario para o filtro
# de recursos resolver o placeholder @spring.profiles.active@ em application.yaml).
RUN mvn -q -B -Pnative,prod clean package -DskipTests && \
    mvn -q -B -Pnative,prod native:compile -DskipTests -Dnative.imageName=mana-paes

# ---- Estágio 3: runtime mínimo (glibc + CA certificates) ----
FROM debian:bookworm-slim
RUN apt-get update && apt-get install -y --no-install-recommends ca-certificates \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY --from=build /app/target/mana-paes /app/mana-paes

EXPOSE 8080
ENTRYPOINT ["/app/mana-paes"]