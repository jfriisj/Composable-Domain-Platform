#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd -P)"
PROJECT_DIR="$(cd -- "${SCRIPT_DIR}/.." && pwd -P)"
COMPOSE_FILE="${SCRIPT_DIR}/compose.yaml"
DOCKER_SOCKET="/var/run/docker.sock"

fail() {
    printf 'FAIL: %s\n' "$*" >&2
    exit 1
}

require_host() {
    test "$(uname -s)" = "Linux" ||
        fail "initial developer-environment support is Linux only"

    command -v docker >/dev/null 2>&1 ||
        fail "Docker Engine CLI is required on the host"

    docker compose version >/dev/null 2>&1 ||
        fail "Docker Compose plugin is required on the host"

    test -S "${DOCKER_SOCKET}" ||
        fail "local Docker socket ${DOCKER_SOCKET} is required; remote/rootless engines are outside the accepted host boundary"

    LOCAL_UID="$(id -u)"
    LOCAL_GID="$(id -g)"

    test "${LOCAL_UID}" -ne 0 ||
        fail "normal developer work must run as a non-root host user"

    test "${LOCAL_GID}" -ne 0 ||
        fail "normal developer work must use a non-root primary host group"

    DOCKER_GID="$(stat -c '%g' "${DOCKER_SOCKET}")"
    case "${DOCKER_GID}" in
        ''|*[!0-9]*) fail "could not determine Docker socket group id" ;;
    esac

    APP_HOST_PORT="${APP_HOST_PORT:-8080}"
    case "${APP_HOST_PORT}" in
        ''|*[!0-9]*) fail "APP_HOST_PORT must be a numeric TCP port" ;;
    esac
    test "${APP_HOST_PORT}" -ge 1 && test "${APP_HOST_PORT}" -le 65535 ||
        fail "APP_HOST_PORT must be between 1 and 65535"

    export DOCKER_HOST="unix://${DOCKER_SOCKET}"
    docker info >/dev/null 2>&1 ||
        fail "current host user cannot control the accepted local Docker Engine"

    docker_operating_system="$(docker info --format '{{.OperatingSystem}}' 2>/dev/null)" ||
        fail "could not identify the accepted local Docker Engine"

    case "${docker_operating_system}" in
        *"Docker Desktop"*)
            export TESTCONTAINERS_HOST_OVERRIDE="host.docker.internal"
            ;;
        *)
            unset TESTCONTAINERS_HOST_OVERRIDE
            ;;
    esac

    project_base="$(basename -- "${PROJECT_DIR}" | tr '[:upper:]' '[:lower:]' | tr -c 'a-z0-9_-' '-')"
    project_base="${project_base#-}"
    project_base="${project_base%-}"
    test -n "${project_base}" || project_base="cdp"

    export PROJECT_DIR LOCAL_UID LOCAL_GID DOCKER_GID APP_HOST_PORT
    export COMPOSE_PROJECT_NAME="${project_base}-dev-${LOCAL_UID}"
}

compose() {
    docker compose --file "${COMPOSE_FILE}" "$@"
}

postgres_up() {
    if ! compose --profile manual-db up -d --wait --wait-timeout 60 postgres; then
        compose --profile manual-db logs postgres >&2 || true
        fail "optional development PostgreSQL did not become ready"
    fi

    printf 'PASS: optional development PostgreSQL is ready\n'
}

workspace_url() {
    local mapping
    local port

    mapping="$(compose port workspace 3000 2>/dev/null)" ||
        fail "browser workspace is not running or has no published browser port"

    test -n "${mapping}" ||
        fail "browser workspace returned an empty browser port mapping"

    case "${mapping}" in
        127.0.0.1:*)
            port="${mapping#127.0.0.1:}"
            ;;
        *)
            fail "browser workspace is not loopback-only: ${mapping}"
            ;;
    esac

    case "${port}" in
        ''|*[!0-9]*) fail "could not determine browser workspace host port from ${mapping}" ;;
    esac

    test "${port}" -ge 1 && test "${port}" -le 65535 ||
        fail "browser workspace published invalid host port ${port}"

    printf 'http://127.0.0.1:%s\n' "${port}"
}

workspace_up() {
    local url

    if ! compose up -d --build --wait --wait-timeout 60 workspace; then
        compose logs workspace >&2 || true
        fail "browser workspace did not become ready"
    fi

    url="$(workspace_url)"
    printf 'PASS: browser workspace is ready at %s\n' "${url}"
}

workspace_down() {
    compose stop workspace
    compose rm --force workspace
    printf 'PASS: browser workspace stopped; named editor state retained\n'
}

usage() {
    cat <<'EOF_USAGE'
Usage: ./dev/dev.sh <command>

Commands:
  shell            Enter the project-specific developer shell.
  java-version     Show the Java version inside the developer environment.
  gradle-version   Run the repository Gradle Wrapper version command.
  check            Run the authoritative repository validation inside the environment.
  workspace-up     Build/start the persistent local browser workspace and print its URL.
  workspace-url    Print the running browser workspace URL.
  workspace-shell  Enter a shell in the running browser workspace container.
  workspace-down   Stop/remove the browser workspace container while retaining editor state.
  postgres-up      Start the optional manual-development PostgreSQL service.
  postgres-down    Stop optional PostgreSQL while retaining its disposable data volume.
  boot-run         Start optional PostgreSQL and run the existing platform bootRun workflow.
  down             Stop/remove developer-environment containers and network; retain named volumes.
  reset            Stop/remove the environment and delete disposable developer volumes.
EOF_USAGE
}

require_host

case "${1:-}" in
    shell)
        exec docker compose --file "${COMPOSE_FILE}" run --rm --build dev bash
        ;;
    java-version)
        exec docker compose --file "${COMPOSE_FILE}" run --rm --build -T --interactive=false dev java -version
        ;;
    gradle-version)
        exec docker compose --file "${COMPOSE_FILE}" run --rm --build -T --interactive=false dev ./gradlew --version
        ;;
    check)
        exec docker compose --file "${COMPOSE_FILE}" run --rm --build -T --interactive=false dev ./gradlew --no-daemon check
        ;;
    workspace-up)
        workspace_up
        ;;
    workspace-url)
        workspace_url
        ;;
    workspace-shell)
        workspace_url >/dev/null
        exec docker compose --file "${COMPOSE_FILE}" exec workspace bash
        ;;
    workspace-down)
        workspace_down
        ;;
    postgres-up)
        postgres_up
        ;;
    postgres-down)
        compose --profile manual-db stop postgres
        printf 'PASS: optional development PostgreSQL stopped; data volume retained\n'
        ;;
    boot-run)
        postgres_up
        exec docker compose --file "${COMPOSE_FILE}" run --rm --build --service-ports -T --interactive=false \
            -e PLATFORM_DATABASE_URL='jdbc:postgresql://postgres:5432/platform' \
            -e PLATFORM_DATABASE_USERNAME='platform' \
            -e PLATFORM_DATABASE_PASSWORD='platform' \
            -e PLATFORM_SECURITY_PARTICIPANTS_0_PRINCIPAL='opaque-dev-participant' \
            -e 'PLATFORM_SECURITY_PARTICIPANTS_0_PASSWORDVERIFIER={bcrypt}$2y$10$UHuEV9G1TkCe7DwbtUfopuufpWETOlRJ3QGAds9rQVeZqojZcu13W' \
            -e SERVER_PORT='8080' \
            dev ./gradlew --no-daemon :platform-app:bootRun
        ;;
    down)
        compose --profile manual-db down --remove-orphans
        printf 'PASS: developer environment stopped; named volumes retained\n'
        ;;
    reset)
        compose --profile manual-db down --volumes --remove-orphans
        printf 'PASS: disposable developer-environment volumes removed\n'
        ;;
    *)
        usage
        exit 2
        ;;
esac
