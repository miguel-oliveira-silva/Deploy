# =============================================================================
# DOCKERFILE — user-service
# =============================================================================
# Build multi-stage:
#   1) STAGE "build": usa imagem com Maven + JDK 17 para compilar o projeto
#      e gerar o .jar executável.
#   2) STAGE final: usa apenas um JRE 17 enxuto (sem Maven, sem JDK completo)
#      e copia o .jar pronto. Isso deixa a imagem final bem menor.
# =============================================================================

# ---- STAGE 1: build ----
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# Copia primeiro só o pom.xml para aproveitar cache de camadas do Docker:
# se as dependências não mudarem, o Docker não baixa tudo de novo a cada build.
COPY pom.xml .
RUN mvn -B dependency:go-offline

# Agora copia o código-fonte e compila
COPY src ./src
RUN mvn -B clean package -DskipTests

# ---- STAGE 2: runtime ----
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Cria um usuário não-root por segurança (boa prática em produção)
RUN addgroup --system spring && adduser --system --ingroup spring spring
USER spring:spring

# Copia apenas o .jar final do estágio de build
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8084

# Healthcheck usando o endpoint do Actuator
HEALTHCHECK --interval=15s --timeout=5s --start-period=40s --retries=5 \
  CMD wget -q --spider http://localhost:8084/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
