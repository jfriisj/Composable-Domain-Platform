FROM eclipse-temurin:21-jdk-noble@sha256:a871f3e3caddad75608fd4531ed8bbca5cc42a27dc1da3ea3a2e554772b0ee15

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

CMD ["bash"]
