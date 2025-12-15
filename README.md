# goldenera-directory

**goldenera-directory** is a centralized discovery service for nodes in the GoldenEra blockchain network. 

Each node pings this directory server to register itself. The directory server stores all active nodes and distributes their addresses back to the nodes so they can connect to each other via the P2P layer.

---

## Prerequisites

1. **Install Docker**: https://www.docker.com/
2. **Create a directory** for the directory server and navigate to it:
   ```bash
   mkdir goldenera-directory
   cd goldenera-directory
   ```

---

## Usage

Create a `docker-compose.yml` file:

```yaml
services:
  directory:
    image: ghcr.io/goldeneraglobal/goldenera-directory:latest
    container_name: goldenera_directory
    restart: unless-stopped
    pull_policy: always
    env_file:
      - .env
    environment:
      - LOGGING_FILE=${LOGGING_FILE:-directory.log}
    ports:
      - "${LISTEN_PORT:-8080}:8080"
    volumes:
      - ./directory_data:/app/directory_data
      - ${LOGGING_DIR:-./directory_logs}:/app/directory_logs
    deploy:
      resources:
        limits:
          memory: 1G
          cpus: "2.0"
        reservations:
          memory: 1G
```

Create a `.env` file:

```bash
# Spring profile
SPRING_PROFILES_ACTIVE="prod"

# Directory PORT
LISTEN_PORT=8080

# Directory identity
IDENTITY_FILE="./directory_data/.directory_identity"

# Directory Settings
MAX_REQUESTS_PER_IP_ADDRESS_PER_MINUTE=10
DELETE_INACTIVE_NODE_AFTER_SECONDS=60

# Admin (Change this!)
ADMIN_ACCESS_TOKEN="abc123"

# Logging
LOGGING_DIR="./directory_logs"
LOGGING_FILE="goldenera.log"
LOGGING_LEVEL_ROOT=INFO
LOGGING_LEVEL_GLOBAL_GOLDENERA=INFO
```

Run:

```bash
docker compose up -d
```

---

## License

This project is licensed under the **MIT License** - see the [LICENSE](LICENSE) file for details.
