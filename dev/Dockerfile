FROM eclipse-temurin:21-jdk-noble@sha256:a871f3e3caddad75608fd4531ed8bbca5cc42a27dc1da3ea3a2e554772b0ee15 AS base

ARG USER_UID
ARG USER_GID

RUN set -eux; \
    test "${USER_UID}" -gt 0; \
    test "${USER_GID}" -gt 0; \
    apt-get update; \
    apt-get install -y --no-install-recommends ca-certificates curl git passwd; \
    rm -rf /var/lib/apt/lists/*; \
    if ! getent group "${USER_GID}" >/dev/null; then \
        groupadd --gid "${USER_GID}" developer-host; \
    fi; \
    if getent passwd developer >/dev/null; then \
        test "$(id -u developer)" = "${USER_UID}"; \
    else \
        existing_user="$(getent passwd "${USER_UID}" | cut -d: -f1 || true)"; \
        if [ -n "${existing_user}" ]; then \
            usermod --login developer "${existing_user}"; \
        else \
            useradd --uid "${USER_UID}" --gid "${USER_GID}" --create-home --shell /bin/bash developer; \
        fi; \
    fi; \
    usermod --gid "${USER_GID}" --home /home/developer --move-home developer; \
    mkdir -p /home/developer/.gradle; \
    chown -R "${USER_UID}:${USER_GID}" /home/developer

ENV HOME=/home/developer
ENV GRADLE_USER_HOME=/home/developer/.gradle

USER developer
WORKDIR /home/developer

FROM base AS shell

CMD ["bash"]

FROM base AS workspace

ARG TARGETARCH
ARG OPENVSCODE_VERSION=1.109.5
ARG OPENVSCODE_SHA256_AMD64=b433bf4f0227321a7014d8460d10a8f958adc0f45aa79bd889e84e65e8f88363
ARG OPENVSCODE_SHA256_ARM64=36d9c14036489b63de84ebace837fcacf7e60e669a0dc715802c5443684ea4dc

USER root

RUN set -eux; \
    case "${TARGETARCH}" in \
        amd64) \
            archive_arch="x64"; \
            archive_sha256="${OPENVSCODE_SHA256_AMD64}"; \
            ;; \
        arm64) \
            archive_arch="arm64"; \
            archive_sha256="${OPENVSCODE_SHA256_ARM64}"; \
            ;; \
        *) \
            printf 'Unsupported OpenVSCode Server target architecture: %s\n' "${TARGETARCH}" >&2; \
            exit 1; \
            ;; \
    esac; \
    archive="/tmp/openvscode-server.tar.gz"; \
    curl --fail --show-error --silent --location \
        "https://github.com/gitpod-io/openvscode-server/releases/download/openvscode-server-v${OPENVSCODE_VERSION}/openvscode-server-v${OPENVSCODE_VERSION}-linux-${archive_arch}.tar.gz" \
        --output "${archive}"; \
    printf '%s  %s\n' "${archive_sha256}" "${archive}" | sha256sum --check --strict -; \
    mkdir -p /opt/openvscode-server /home/developer/.openvscode-server; \
    tar --extract --gzip --file "${archive}" --directory /opt/openvscode-server --strip-components=1; \
    rm -f "${archive}"; \
    chown -R "${USER_UID}:${USER_GID}" /home/developer/.openvscode-server

USER developer

EXPOSE 3000

CMD ["/opt/openvscode-server/bin/openvscode-server", "--host", "0.0.0.0", "--port", "3000", "--without-connection-token"]
