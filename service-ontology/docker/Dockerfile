FROM python:2.7.12-alpine

MAINTAINER Stefan Negru <stefan.negru@helsinki.fi>

RUN apk add --update \
    && apk add --no-cache build-base \
    && apk add --no-cache linux-headers \
    && apk add --no-cache bash \
    && apk add --no-cache py-setuptools \
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
    && rm -r "$GNUPGHOME" /usr/local/bin/gosu.asc \
    && chmod +x /usr/local/bin/gosu \
    && gosu nobody true \
    && apk del .gosu-deps

ARG ONTOS
WORKDIR /app

COPY build/distributions/ontology-service-${ONTOS}.tar.gz /app
RUN tar -xf ontology-service-${ONTOS}.tar.gz

RUN addgroup -S nonroot \
    && adduser -S -g nonroot nonroot

# COPY supervisord specific configurations
# default.conf is needed to have access to port 8080 as port 80 is not allowed with
# to be used by a user with lower privileges
COPY default.conf /etc/nginx/conf.d/default.conf
COPY supervisor.conf /etc/supervisor/supervisord.conf
COPY entrypoint.sh /

RUN mkdir logs \
    && mkdir /attx-sb-shared \
    && mkdir /attx-sb-shared/ontology \
    && chmod +x /entrypoint.sh

# setting environment variables for entrypoint script
# setting also directories to be owned by the nonroot user
ENV GOSU_USER nonroot:nonroot
ENV GOSU_CHOWN /app /var/log /etc/supervisor /var/run /attx-sb-shared
RUN chmod 777 /attx-sb-shared

EXPOSE 4305

ENTRYPOINT ["/entrypoint.sh"]
