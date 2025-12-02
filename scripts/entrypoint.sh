#!/bin/bash
set -e

# ==============================================================================
# CONFIG
# ==============================================================================
APP_HOME="/app"
LOG_DIR="${APP_HOME}/logs"
DATA_DIR="${APP_HOME}/data"
OVERRIDES_DIR="${APP_HOME}/overrides"
APP_JAR="${APP_HOME}/app.jar"

echo ">>> [BOOT] GoldenEra Directory Service Initialization"

# ==============================================================================
# 1. PERMISSION FIX
# ==============================================================================
echo ">>> [INIT] Fixing permissions for persistence layers..."

mkdir -p "$LOG_DIR" "$DATA_DIR" "$OVERRIDES_DIR"

chown -R directory:directory "$LOG_DIR"
chown -R directory:directory "$DATA_DIR"
chown -R directory:directory "$OVERRIDES_DIR"

# ==============================================================================
# 2. START APPLICATION
# ==============================================================================
echo ">>> [BOOT] Launching Java Application..."

exec su -s /bin/bash directory -c "java \
  -server \
  -XX:+UseZGC \
  -XX:+ZGenerational \
  -XX:MaxRAMPercentage=80.0 \
  -XX:MaxMetaspaceSize=384m \
  -Xss512k \
  -XX:+UseStringDeduplication \
  -XX:+ExitOnOutOfMemoryError \
  -XX:CICompilerCount=2 \
  -Djava.awt.headless=true \
  -Djava.net.preferIPv4Stack=true \
  -DAPP_DATA_DIR=$DATA_DIR \
  -Djava.security.egd=file:/dev/./urandom \
  -cp ${OVERRIDES_DIR}:${APP_JAR} \
  org.springframework.boot.loader.launch.JarLauncher"