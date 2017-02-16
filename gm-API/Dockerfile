FROM python:2.7.12-alpine

MAINTAINER Stefan Negru <stefan.negru@helsinki.fi>

RUN apk add --update\
    && apk add --no-cache build-base \
    && apk add --no-cache linux-headers \
    && apk add --no-cache bash \
    && apk add --no-cache db-dev \
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
RUN wget  -P /app/java -O gc-rdf2json-indexer.jar http://attx-dev:8081/repository/attx-snapshots/org/uh/attx/gc/gc-rdf2json-indexer/1.0-SNAPSHOT/gc-rdf2json-indexer-1.0-20170216.112513-21.jar gc-rdf2json-indexer.jar

RUN mkdir logs
RUN touch data.db

CMD ./bin/gmapi --host 0.0.0.0 --port 4302
