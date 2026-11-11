# syntax=docker/dockerfile:1

# Imagens fixadas por tag + digest: build reproduzível e imune a troca silenciosa da tag.
# O Dependabot (docker) abre PRs para atualizar os dois.

# ---- Build ----
FROM maven:3.9-eclipse-temurin-21@sha256:99e61abcff91a9b1333463bd8451fb18495d6eba9250ac66a338b518f8278320 AS build
WORKDIR /workspace
# .mvn/maven.config ativa --strict-checksums (falha se um artefato não bater com o checksum)
COPY .mvn .mvn
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn -B -q dependency:go-offline
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -B -q -DskipTests package

# ---- Runtime ----
# JRE em Alpine: sem compilador, sem apt/curl e com muito menos pacotes que a imagem Ubuntu.
FROM eclipse-temurin:21-jre-alpine@sha256:ac05c3c42b7c2f1a52c9bd52f01041fa1958231b15ebeae7420429cbc5baf474

LABEL org.opencontainers.image.title="notify-engine" \
      org.opencontainers.image.description="Motor de regras para notificações" \
      org.opencontainers.image.licenses="MIT"

# Usuário de sistema sem shell e sem home, com UID/GID fixos (compatível com runAsNonRoot).
RUN addgroup -S -g 10001 app \
    && adduser -S -u 10001 -G app -H -s /sbin/nologin app

WORKDIR /app
COPY --from=build --chown=10001:10001 /workspace/target/notify-engine-*.jar app.jar

USER 10001:10001
EXPOSE 8080

# Só consulta o endpoint de saúde local (wget do busybox; não há curl na imagem).
HEALTHCHECK --interval=10s --timeout=5s --start-period=40s --retries=12 \
    CMD wget -q -O /dev/null http://127.0.0.1:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-XX:+ExitOnOutOfMemoryError", "-jar", "app.jar"]
