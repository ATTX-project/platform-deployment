FROM python:2.7.12-alpine

MAINTAINER Stefan Negru <stefan.negru@helsinki.fi>

RUN apk add --update \
    && apk add --no-cache build-base \
    && apk add --no-cache linux-headers \
    && apk add --no-cache bash \
    && apk add --no-cache db-dev \
    && apk add --no-cache wget \
    && apk add --no-cache py-setuptools \
    && apk add --no-cache supervisor \
    && rm -rf /var/cache/apk/*

# JAVA portion reused from https://github.com/docker-library/openjdk/blob/master/8-jdk/alpine/Dockerfile

# Default to UTF-8 file.encoding
ENV LANG C.UTF-8

# add a simple script that can auto-detect the appropriate JAVA_HOME value
# based on whether the JDK or only the JRE is installed
RUN { \
        echo '#!/bin/sh'; \
        echo 'set -e'; \
        echo; \
        echo 'dirname "$(dirname "$(readlink -f "$(which javac || which java)")")"'; \
        } > /usr/local/bin/docker-java-home \
        && chmod +x /usr/local/bin/docker-java-home
ENV JAVA_HOME /usr/lib/jvm/java-1.8-openjdk/jre
ENV PATH $PATH:/usr/lib/jvm/java-1.8-openjdk/jre/bin:/usr/lib/jvm/java-1.8-openjdk/bin

ENV JAVA_VERSION 8u111
ENV JAVA_ALPINE_VERSION 8.111.14-r0

RUN set -x \
    && apk add --no-cache openjdk8-jre="$JAVA_ALPINE_VERSION" \
    && [ "$JAVA_HOME" = "$(docker-java-home)" ] \
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

ARG GMAPI
ARG RDFINDEXER
WORKDIR /app

COPY build/distributions/gm-API-${GMAPI}.tar.gz /app/gm-API.tar.gz
RUN tar -xf gm-API.tar.gz

COPY build/distributions/gc-rdf2json-indexer-all-${RDFINDEXER}-all.jar /app/java/gc-rdf2json-indexer.jar

# Adding user without root privileges alongside a user group
RUN addgroup -S nonroot \
    && adduser -S -g nonroot nonroot

# COPY supervisord specific configurations
# default.conf is needed to have access to port 8080 as port 80 is not allowed with
# to be used by a user with lower privileges
COPY default.conf /etc/nginx/conf.d/default.conf
COPY supervisor.conf /etc/supervisor/supervisord.conf
COPY entrypoint.sh /

RUN mkdir logs \
    && mkdir data \
    && chmod +x /entrypoint.sh

# setting environment variables for entrypoint script
# setting also directories to be owned by the nonroot user
ENV GOSU_USER nonroot:nonroot
ENV GOSU_CHOWN /app /var/log /etc/supervisor /var/run

EXPOSE 4302

VOLUME ["/app/data"]

ENTRYPOINT ["/entrypoint.sh"]
