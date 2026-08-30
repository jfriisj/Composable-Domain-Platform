# Developer environment

This directory contains the repository-controlled Linux developer environment. It provides the project JDK and repository tooling inside Docker while keeping application runtime and deployment packaging separate.

## Entry point

Run developer-environment commands from the repository root through:

```text
./dev/dev.sh <command>
```

Do not invoke `docker compose` directly for supported developer workflows. `dev/dev.sh` validates the accepted Linux/Docker host boundary and supplies the host-derived `PROJECT_DIR`, `LOCAL_UID`, `LOCAL_GID`, `DOCKER_GID`, application host port, and Compose project name. `compose.yaml` intentionally fails closed when required values are missing.

The host must provide Docker Engine, the Docker Compose plugin, and access to the local `/var/run/docker.sock`. Normal developer work runs as a non-root host user.

## Commands

| Command | Purpose |
| --- | --- |
| `./dev/dev.sh shell` | Enter the project-specific interactive developer shell. |
| `./dev/dev.sh java-version` | Show Java inside the developer environment. |
| `./dev/dev.sh gradle-version` | Run the repository Gradle Wrapper version command. |
| `./dev/dev.sh check` | Run the authoritative repository validation inside the environment. |
| `./dev/dev.sh workspace-up` | Build/start the persistent local browser workspace and print its URL. |
| `./dev/dev.sh workspace-url` | Print the running browser workspace URL. |
| `./dev/dev.sh workspace-shell` | Enter a shell in the running browser workspace container. |
| `./dev/dev.sh workspace-down` | Stop/remove the browser workspace container while retaining editor state. |
| `./dev/dev.sh postgres-up` | Start the optional manual-development PostgreSQL service. |
| `./dev/dev.sh postgres-down` | Stop optional PostgreSQL while retaining its disposable data volume. |
| `./dev/dev.sh boot-run` | Start optional PostgreSQL and run the existing Platform `bootRun` workflow. |
| `./dev/dev.sh down` | Stop/remove developer-environment containers and network while retaining named volumes. |
| `./dev/dev.sh reset` | Stop/remove the environment and delete disposable Gradle/PostgreSQL/editor volumes. |

## Interactive shell

Start an interactive shell with:

```text
./dev/dev.sh shell
```

The repository is bind-mounted into the container at the same absolute path as on the host and is also the container working directory. The container runs with the host user's UID/GID.

The shell image provides Java 21, Bash, Git, curl, and the supporting packages installed by `dev/Dockerfile`. The mounted repository provides the Gradle Wrapper, so repository Gradle tasks can be run interactively, for example:

```text
java -version
./gradlew --version
./gradlew :event-api:test
```

Use `./dev/dev.sh check` from the host for the canonical repository-controlled root validation.

Docker CLI is not installed in the developer image. Automated Testcontainers workloads access the accepted host Docker daemon through the mounted Docker socket rather than through Docker-in-Docker.

## Isolated browser workspaces

Parallel write-capable work uses independent Git worktrees in the WSL filesystem. Git owns branch, index, and source isolation; the developer environment does not create or remove worktrees.

Use unique worktree directory basenames. `dev/dev.sh` derives the Compose project name from the current worktree basename and host UID, so separate worktrees receive separate containers, networks, PostgreSQL state, Gradle cache, and browser-editor state.

From an assigned worktree:

```text
./dev/dev.sh workspace-up
./dev/dev.sh workspace-url
./dev/dev.sh workspace-shell
./dev/dev.sh workspace-down
```

`workspace-up` starts OpenVSCode Server for that exact worktree and publishes it through an automatically selected host port bound only to `127.0.0.1`. `workspace-url` reports the actual local URL.

The workspace container is persistent until stopped, while editor state is retained in its worktree-local Compose named volume. `workspace-down` removes only the workspace container and retains editor state. `down` retains named state; `reset` removes it.

One writable Git worktree has at most one independent top-level write-capable agent owner at a time. External agents such as AGY and OpenCode run from WSL against their assigned worktree; they are not installed by the repository developer image and their credentials/session state are not repository-owned. Changes written by an agent are visible immediately in the browser workspace because both use the same WSL working tree.

OpenVSCode Server is browser presentation only. It does not replace or participate in the canonical `./dev/dev.sh check` dependency path.

## Manual application host port

The existing manual `boot-run` flow continues to use host port `8080` by default. Concurrent worktrees can select another loopback host port without changing the application container port:

```text
APP_HOST_PORT=8081 ./dev/dev.sh boot-run
```

Each active manual application run must use a distinct available host port.

## Docker trust boundary

`/var/run/docker.sock` is bind-mounted into developer containers so Testcontainers and repository code can reach the host Docker daemon.

Access to that socket gives code running in the container authority over the host Docker daemon. The developer container and browser workspace are therefore not security isolation boundaries. Docker-in-Docker is not used.

The OpenVSCode browser port is accepted only on host loopback. Remote or LAN exposure is outside the accepted capability.

## PostgreSQL

Automated database tests use Testcontainers-owned ephemeral PostgreSQL through the host Docker daemon. The optional Compose PostgreSQL service is only for manual development and is not a dependency of `./dev/dev.sh check`.

Use `postgres-up` and `postgres-down` when manual persistent development state is useful. `boot-run` starts that optional PostgreSQL service before running the existing Platform application.

Because the Compose project is derived per worktree, optional PostgreSQL state is isolated between worktrees.

## Desktop VS Code

The repository does not define a `.devcontainer` configuration or an editor-managed container lifecycle.

The existing desktop attach workflow remains available:

1. Start `./dev/dev.sh shell` and keep that shell running.
2. In VS Code with the Dev Containers extension installed, choose `Dev Containers: Attach to Running Container...`.
3. Select the running developer container.
4. Open the repository at the same absolute path used on the host.

Exiting the `shell` command removes its container because the wrapper starts it with `docker compose run --rm`.

This attach workflow does not make `Reopen in Container`, `.devcontainer`, or automatic editor lifecycle part of the accepted repository environment.

## Related references

- [`../docs/tech-stack.md`](../docs/tech-stack.md) — accepted technology and developer-environment constraints.
- [`../docs/workflow.md`](../docs/workflow.md) — canonical development, validation, Git, and PR workflow.
