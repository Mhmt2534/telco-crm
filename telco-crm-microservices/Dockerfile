# syntax=docker/dockerfile:1

# ════════════════════════════════════════════════════════════════════════
# Shared multi-stage build for every Spring Boot module in the platform.
#
# Performance design:
#   * The `build` stage is IDENTICAL for every service (it builds the whole
#     multi-module reactor once). BuildKit therefore computes it a SINGLE time
#     and reuses the cached result for all images — instead of recompiling
#     per service.
#   * A shared Maven repository cache mount (/root/.m2) avoids re-downloading
#     dependencies on every build.
#   * The `runtime-base` stage (JRE + curl for healthchecks) is also identical
#     across services, so the apt-get step runs only once and is cached.
#
# Only the tiny final stage differs per service: it copies that service's jar.
# Select the module with the SERVICE build argument (Compose passes it via
# build.args). Build context must be the repo root so the parent POM and the
# shared modules (common-core/web/persistence) are available.
# ════════════════════════════════════════════════════════════════════════

# ──── Build stage (shared, cached once for all services) ────
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace

# Copy the whole multi-module project (parent POM + shared + service modules).
COPY . .

# Build the entire reactor once. The cache mount keeps the downloaded Maven
# dependencies between builds so they are fetched only the first time.
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -ntp clean package -DskipTests

# ──── Runtime base (shared, cached once for all services) ────
FROM eclipse-temurin:21-jre AS runtime-base
# curl backs the Compose healthchecks (actuator /actuator/health).
RUN --mount=type=cache,target=/var/cache/apt,sharing=locked \
    --mount=type=cache,target=/var/lib/apt,sharing=locked \
    apt-get update \
    && apt-get install -y --no-install-recommends curl

# ──── Per-service runtime image (only this layer differs per service) ────
FROM runtime-base
ARG SERVICE
WORKDIR /app
COPY --from=build /workspace/${SERVICE}/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
