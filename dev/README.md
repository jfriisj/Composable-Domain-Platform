# Developer environment

This directory contains the repository-controlled Linux developer environment. It provides the project JDK and repository tooling inside Docker while keeping application runtime and deployment packaging separate.

## Entry point

Run developer-environment commands from the repository root through:

```text
./dev/dev.sh <command>
```

Do not invoke `docker compose` directly for the developer service. `dev/dev.sh` validates the accepted Linux/Docker host boundary and supplies the host-derived `PROJECT_DIR`, `LOCAL_UID`, `LOCAL_GID`, `DOCKER_GID`, and Compose project name. `compose.yaml` intentionally fails closed when required values are missing.

The host must provide Docker Engine, the Docker Compose plugin, and access to the local `/var/run/docker.sock`. Normal developer work runs as a non-root host user.

## Commands

| Command | Purpose |
| --- | --- |
| `./dev/dev.sh shell` | Enter the project-specific interactive developer shell. |
| `./dev/dev.sh java-version` | Show Java inside the developer environment. |
| `./dev/dev.sh gradle-version` | Run the repository Gradle Wrapper version command. |
| `./dev/dev.sh check` | Run the authoritative repository validation inside the environment. |
| `./dev/dev.sh postgres-up` | Start the optional manual-development PostgreSQL service. |
| `./dev/dev.sh postgres-down` | Stop optional PostgreSQL while retaining its disposable data volume. |
| `./dev/dev.sh boot-run` | Start optional PostgreSQL and run the existing Platform `bootRun` workflow. |
| `./dev/dev.sh down` | Stop/remove developer-environment containers and network while retaining named volumes. |
| `./dev/dev.sh reset` | Stop/remove the environment and delete disposable Gradle/PostgreSQL volumes. |

## Interactive shell

Start an interactive shell with:

```text
./dev/dev.sh shell
```

The repository is bind-mounted into the container at the same absolute path as on the host and is also the container working directory. The container runs with the host user's UID/GID.

The image provides Java 21, Bash, Git, curl, and the supporting packages installed by `dev/Dockerfile`. The mounted repository provides the Gradle Wrapper, so repository Gradle tasks can be run interactively, for example:

```text
java -version
./gradlew --version
./gradlew :event-api:test
```

Use `./dev/dev.sh check` from the host for the canonical repository-controlled root validation.

Docker CLI is not installed in the developer image. Automated Testcontainers workloads access the accepted host Docker daemon through the mounted Docker socket rather than through Docker-in-Docker.

## Docker trust boundary

`/var/run/docker.sock` is bind-mounted into the developer container so Testcontainers and repository code can reach the host Docker daemon.

Access to that socket gives code running in the container authority over the host Docker daemon. The developer container is therefore not an isolation boundary. Docker-in-Docker is not used.

## PostgreSQL

Automated database tests use Testcontainers-owned ephemeral PostgreSQL through the host Docker daemon. The optional Compose PostgreSQL service is only for manual development and is not a dependency of `./dev/dev.sh check`.

Use `postgres-up` and `postgres-down` when manual persistent development state is useful. `boot-run` starts that optional PostgreSQL service before running the existing Platform application.

## VS Code

The repository does not define a `.devcontainer` configuration or an editor-managed container lifecycle.

To use the existing interactive container with VS Code:

1. Start `./dev/dev.sh shell` and keep that shell running.
2. In VS Code with the Dev Containers extension installed, choose `Dev Containers: Attach to Running Container...`.
3. Select the running developer container.
4. Open the repository at the same absolute path used on the host.

Exiting the `shell` command removes its container because the wrapper starts it with `docker compose run --rm`.

This attach workflow does not make `Reopen in Container`, `.devcontainer`, or automatic editor lifecycle part of the accepted repository environment.

## Related references

- [`../docs/tech-stack.md`](../docs/tech-stack.md) — accepted technology and developer-environment constraints.
- [`../docs/workflow.md`](../docs/workflow.md) — canonical development, validation, Git, and PR workflow.
