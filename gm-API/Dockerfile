FROM python:2.7.12-alpine

MAINTAINER Stefan Negru <stefan.negru@helsinki.fi>

RUN apk add --update\
    && apk add --no-cache build-base \
    && apk add --no-cache linux-headers \
    && apk add --no-cache bash \
    && apk add --no-cache db-dev \
    && apk add --no-cahce wget \
    && apk add --no-cache py-setuptools

# reused from https://github.com/docker-library/openjdk/blob/master/8-jdk/alpine/Dockerfile

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
        && apk add --no-cache \
                openjdk8-jre="$JAVA_ALPINE_VERSION" \
        && [ "$JAVA_HOME" = "$(docker-java-home)" ]

EXPOSE 4302
ENV version="0.1"

WORKDIR /app

COPY build/distributions/gm-API-${version}.tar.gz /app
RUN tar -xf gm-API-${version}.tar.gz

# TBC when moving to Jenkins to donwload from artifact repo
RUN wget -P /app/java "http://localhost:8081/repository/attx-releases/org/uh/attx/gc/gc-rdf2json-indexer-all/99/gc-rdf2json-indexer-all-99-all.jar"\
    && mv /app/java/gc-rdf2json-indexer-all-99-all.jar /app/java/gc-rdf2jon-indexer.jar

RUN mkdir logs
RUN touch data.db

CMD ./bin/gmapi --host 0.0.0.0 --port 4302
