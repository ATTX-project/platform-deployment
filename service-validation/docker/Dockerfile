FROM openjdk:8-jdk-alpine

RUN apk add --update \
    && apk add --no-cache build-base \
    && apk add --no-cache linux-headers \
    && apk add --no-cache bash \
    && apk add --no-cache supervisor \
    && rm -rf /var/cache/apk/*

ENV GOSU_VERSION 1.10
RUN set -x \
    && apk add --no-cache --virtual .gosu-deps \
        dpkg \
        gnupg \
        openssl \
    && dpkgArch="$(dpkg --print-architecture | awk -F- '{ print $NF }')" \
    && wget -O /usr/local/bin/gosu "https://github.com/tianon/gosu/releases/download/$GOSU_VERSION/gosu-$dpkgArch" \
    && wget -O /usr/local/bin/gosu.asc "https://github.com/tianon/gosu/releases/download/$GOSU_VERSION/gosu-$dpkgArch.asc" \
    && export GNUPGHOME="$(mktemp -d)" \
    && gpg --keyserver ipv4.pool.sks-keyservers.net --recv-keys B42F6819007F00F88E364FD4036A9C25BF357DD4 \
    && gpg --batch --verify /usr/local/bin/gosu.asc /usr/local/bin/gosu \
    && rm -r /usr/local/bin/gosu.asc \
    && rm -r -f "$GNUPGHOME" \
    && chmod +x /usr/local/bin/gosu \
    && gosu nobody true \
    && apk del .gosu-deps

ARG SHACL
WORKDIR /app

COPY build/distributions/validation-service-${SHACL}.zip /app
RUN unzip validation-service-${SHACL}.zip

RUN addgroup -S nonroot \
    && adduser -S -g nonroot nonroot

# COPY supervisord specific configurations
# default.conf is needed to have access to port 8080 as port 80 is not allowed with
# to be used by a user with lower privileges
COPY entrypoint.sh /

RUN mkdir /attx-sb-shared \
    && chmod +x /entrypoint.sh \
    && chmod +x /app

# setting environment variables for entrypoint script
# setting also directories to be owned by the nonroot user
ENV GOSU_USER nonroot:nonroot
ENV GOSU_CHOWN /app /var/log /var/run /attx-sb-shared /usr/lib/jvm
RUN chmod 777 /attx-sb-shared

EXPOSE 4306

ENTRYPOINT ["/entrypoint.sh"]
