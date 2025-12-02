# ==============================================================================
# STAGE 1: Build Application (Secure Maven Build)
# ==============================================================================
FROM eclipse-temurin:21-jdk-jammy AS app-builder

ARG GITHUB_ACTOR

WORKDIR /app

# 1. Copy Maven Wrapper & Configuration
COPY .mvn/ .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw

# 2. Settings XML Generation
RUN echo "<settings><servers>" > settings.xml && \
    echo "  <server><id>github-merkletrie</id><username>${GITHUB_ACTOR}</username><password>\${env.GITHUB_TOKEN}</password></server>" >> settings.xml && \
    echo "  <server><id>github-rlp</id><username>${GITHUB_ACTOR}</username><password>\${env.GITHUB_TOKEN}</password></server>" >> settings.xml && \
    echo "  <server><id>github-cryptoj</id><username>${GITHUB_ACTOR}</username><password>\${env.GITHUB_TOKEN}</password></server>" >> settings.xml && \
    echo "  <server><id>github</id><username>${GITHUB_ACTOR}</username><password>\${env.GITHUB_TOKEN}</password></server>" >> settings.xml && \
    echo "</servers></settings>" >> settings.xml

# 3. Download Dependencies (Permanent Layer for Caching)
RUN --mount=type=secret,id=github_token \
    export GITHUB_TOKEN=$(cat /run/secrets/github_token) && \
    ./mvnw dependency:resolve dependency:resolve-plugins -s settings.xml || true

# 4. Build Package
COPY src ./src

RUN --mount=type=secret,id=github_token \
    export GITHUB_TOKEN=$(cat /run/secrets/github_token) && \
    ./mvnw clean package -DskipTests -s settings.xml

# ==============================================================================
# STAGE 2: Production Runtime
# ==============================================================================
FROM eclipse-temurin:21-jdk-jammy

ENV APP_HOME=/app
ENV APP_DATA_DIR=/app/directory_data

RUN groupadd -r directory && useradd -r -g directory -d ${APP_HOME} -s /sbin/nologin directory

WORKDIR ${APP_HOME}

COPY --from=app-builder /app/target/*.jar ${APP_HOME}/app.jar
COPY scripts/entrypoint.sh /usr/local/bin/entrypoint.sh
RUN chmod +x /usr/local/bin/entrypoint.sh

RUN mkdir -p ${APP_HOME}/overrides \
    && mkdir -p ${APP_HOME}/directory_logs \
    && mkdir -p ${APP_HOME}/directory_data \
    && chown -R directory:directory ${APP_HOME}

EXPOSE 8080 443 80

VOLUME ["/app/directory_data", "/app/directory_logs"]

ENTRYPOINT ["/usr/local/bin/entrypoint.sh"]