# ==============================================================================
# STAGE 1: Build Application (Secure Maven Build)
# ==============================================================================
FROM eclipse-temurin:21-jdk-jammy AS app-builder

ARG GITHUB_ACTOR

WORKDIR /app

COPY .mvn/ .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw

RUN echo "<settings><servers>" > settings.xml && \
    echo "  <server><id>github-merkletrie</id><username>${GITHUB_ACTOR}</username><password>\${env.GITHUB_TOKEN}</password></server>" >> settings.xml && \
    echo "  <server><id>github-rlp</id><username>${GITHUB_ACTOR}</username><password>\${env.GITHUB_TOKEN}</password></server>" >> settings.xml && \
    echo "  <server><id>github-cryptoj</id><username>${GITHUB_ACTOR}</username><password>\${env.GITHUB_TOKEN}</password></server>" >> settings.xml && \
    echo "  <server><id>github</id><username>${GITHUB_ACTOR}</username><password>\${env.GITHUB_TOKEN}</password></server>" >> settings.xml && \
    echo "</servers></settings>" >> settings.xml

RUN --mount=type=secret,id=github_token \
    --mount=type=cache,target=/root/.m2 \
    export GITHUB_TOKEN=$(cat /run/secrets/github_token) && \
    ./mvnw dependency:go-offline -s settings.xml || true

COPY src ./src

RUN --mount=type=secret,id=github_token \
    --mount=type=cache,target=/root/.m2 \
    export GITHUB_TOKEN=$(cat /run/secrets/github_token) && \
    ./mvnw clean package -DskipTests -s settings.xml

# ==============================================================================
# STAGE 2: Production Runtime (Java 21)
# ==============================================================================
FROM eclipse-temurin:21-jdk-jammy

ENV APP_HOME=/app
ENV APP_DATA_DIR=/app/data

WORKDIR ${APP_HOME}

RUN groupadd -r directory && useradd -r -g directory -d ${APP_HOME} -s /sbin/nologin directory

COPY --from=app-builder /app/target/*.jar ${APP_HOME}/app.jar

RUN mkdir -p ${APP_HOME}/overrides \
    && mkdir -p ${APP_HOME}/logs \
    && mkdir -p ${APP_HOME}/data \
    && chown -R directory:directory ${APP_HOME}

EXPOSE 8080 443 80
VOLUME ["/app/data", "/app/logs"]

USER directory

ENTRYPOINT ["java", \
  "-server", \
  "-XX:+UseZGC", \
  "-XX:+ZGenerational", \
  "-XX:MaxRAMPercentage=80.0", \
  "-XX:MaxMetaspaceSize=384m", \
  "-Xss512k", \
  "-XX:+UseStringDeduplication", \
  "-XX:+ExitOnOutOfMemoryError", \
  "-XX:CICompilerCount=2", \
  "-Djava.awt.headless=true", \
  "-Djava.net.preferIPv4Stack=true", \
  "-DAPP_DATA_DIR=/app/data", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-cp", "/app/overrides:/app/app.jar", \
  "org.springframework.boot.loader.launch.JarLauncher"]